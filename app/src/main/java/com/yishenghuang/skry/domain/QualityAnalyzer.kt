package com.yishenghuang.skry.domain

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class QualityResult(
    val pHash: String,
    val isBlurry: Boolean,
    val isUnderExposed: Boolean,
    val isOverExposed: Boolean,
    val isLowQuality: Boolean,
    val isLongScreenshot: Boolean,
    val isExpiredScreenshot: Boolean,
    val qualityScore: Float
)

/**
 * Local image-quality heuristics for Smart Cleaner (no OpenCV).
 */
object QualityAnalyzer {
    private const val BLUR_VARIANCE_THRESHOLD = 110.0
    private const val UNDER_EXPOSE_MEAN = 45.0
    private const val OVER_EXPOSE_MEAN = 210.0
    private const val LONG_ASPECT = 3.0
    private const val EXPIRED_SCREENSHOT_DAYS = 7L

    fun analyze(
        bitmap: Bitmap,
        intrinsicWidth: Int,
        intrinsicHeight: Int,
        isScreenshot: Boolean,
        dateAddedSeconds: Long,
        hasSensitiveScreenshotFinding: Boolean
    ): QualityResult {
        val sample = downsampleForAnalysis(bitmap, 256)
        val gray = toGray(sample)
        val variance = laplacianVariance(gray, sample.width, sample.height)
        val mean = meanLuminance(gray)
        val isBlurry = variance < BLUR_VARIANCE_THRESHOLD
        val isUnder = mean < UNDER_EXPOSE_MEAN
        val isOver = mean > OVER_EXPOSE_MEAN
        val aspectW = max(intrinsicWidth, 1).toDouble()
        val aspectH = max(intrinsicHeight, 1).toDouble()
        val aspect = max(aspectW, aspectH) / min(aspectW, aspectH)
        val isLong = isScreenshot && aspect >= LONG_ASPECT
        val ageDays = ((System.currentTimeMillis() / 1000L) - dateAddedSeconds) / 86_400L
        val isExpired = isScreenshot && hasSensitiveScreenshotFinding && ageDays >= EXPIRED_SCREENSHOT_DAYS

        // Higher is better: sharp + mid exposure + not tiny.
        val sharpness = (variance / 250.0).coerceIn(0.0, 1.0)
        val exposure = 1.0 - (abs(mean - 128.0) / 128.0).coerceIn(0.0, 1.0)
        val score = ((sharpness * 0.65 + exposure * 0.35) * 100.0).toFloat()

        val pHash = computePHash(bitmap)

        if (sample !== bitmap && !sample.isRecycled) sample.recycle()

        return QualityResult(
            pHash = pHash,
            isBlurry = isBlurry,
            isUnderExposed = isUnder,
            isOverExposed = isOver,
            isLowQuality = isBlurry || isUnder || isOver,
            isLongScreenshot = isLong,
            isExpiredScreenshot = isExpired,
            qualityScore = score
        )
    }

    /** 64-bit perceptual hash as 16-char hex. */
    fun computePHash(bitmap: Bitmap): String {
        val small = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val pixels = IntArray(64)
        small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        if (small !== bitmap) small.recycle()
        val lum = DoubleArray(64) { i ->
            val c = pixels[i]
            0.299 * ((c shr 16) and 0xFF) +
                0.587 * ((c shr 8) and 0xFF) +
                0.114 * (c and 0xFF)
        }
        val avg = lum.average()
        var bits = 0L
        lum.forEachIndexed { index, value ->
            if (value >= avg) bits = bits or (1L shl index)
        }
        return bits.toULong().toString(16).padStart(16, '0')
    }

    fun hammingDistance(a: String, b: String): Int {
        if (a.length != b.length) return Int.MAX_VALUE
        val x = a.toULongOrNull(16) ?: return Int.MAX_VALUE
        val y = b.toULongOrNull(16) ?: return Int.MAX_VALUE
        return (x xor y).countOneBits()
    }

    private fun downsampleForAnalysis(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    }

    private fun toGray(bitmap: Bitmap): DoubleArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        return DoubleArray(pixels.size) { i ->
            val c = pixels[i]
            0.299 * ((c shr 16) and 0xFF) +
                0.587 * ((c shr 8) and 0xFF) +
                0.114 * (c and 0xFF)
        }
    }

    private fun meanLuminance(gray: DoubleArray): Double =
        if (gray.isEmpty()) 0.0 else gray.average()

    /** Approximate Laplacian variance via second-difference energy. */
    private fun laplacianVariance(gray: DoubleArray, width: Int, height: Int): Double {
        if (width < 3 || height < 3) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val lap =
                    gray[i - width] +
                        gray[i + width] +
                        gray[i - 1] +
                        gray[i + 1] -
                        4.0 * gray[i]
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        if (count == 0) return 0.0
        val mean = sum / count
        return (sumSq / count) - mean * mean
    }
}
