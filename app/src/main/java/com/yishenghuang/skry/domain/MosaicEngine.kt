package com.yishenghuang.skry.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Permanent pixelation for sensitive regions. Boxes are normalized 0..1
 * relative to the bitmap (same convention as [Finding] UI overlays).
 */
object MosaicEngine {

    fun apply(
        source: Bitmap,
        findings: List<Finding>,
        blockSize: Int = 28
    ): Bitmap {
        val mutable = if (source.isMutable) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
        }
        val boxes = findings.mapNotNull { it.toPixelRect(mutable.width, mutable.height) }
        if (boxes.isEmpty()) {
            // No OCR boxes — soft-mosaic center band so vault export is never a raw leak.
            val padX = (mutable.width * 0.12f).roundToInt()
            val padY = (mutable.height * 0.18f).roundToInt()
            mosaicRegion(
                bitmap = mutable,
                region = Rect(padX, padY, mutable.width - padX, mutable.height - padY),
                blockSize = blockSize
            )
        } else {
            boxes.forEach { mosaicRegion(mutable, expand(it, mutable.width, mutable.height), blockSize) }
        }
        return mutable
    }

    private fun Finding.toPixelRect(width: Int, height: Int): Rect? {
        val l = boxLeft ?: return null
        val t = boxTop ?: return null
        val r = boxRight ?: return null
        val b = boxBottom ?: return null
        return Rect(
            (l * width).roundToInt().coerceIn(0, width - 1),
            (t * height).roundToInt().coerceIn(0, height - 1),
            (r * width).roundToInt().coerceIn(1, width),
            (b * height).roundToInt().coerceIn(1, height)
        ).takeIf { it.width() > 2 && it.height() > 2 }
    }

    private fun expand(rect: Rect, width: Int, height: Int, padRatio: Float = 0.08f): Rect {
        val padX = max(8, (rect.width() * padRatio).roundToInt())
        val padY = max(8, (rect.height() * padRatio).roundToInt())
        return Rect(
            max(0, rect.left - padX),
            max(0, rect.top - padY),
            min(width, rect.right + padX),
            min(height, rect.bottom + padY)
        )
    }

    internal fun mosaicRegion(bitmap: Bitmap, region: Rect, blockSize: Int) {
        if (region.width() <= 0 || region.height() <= 0) return
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val canvas = Canvas(bitmap)
        var y = region.top
        while (y < region.bottom) {
            var x = region.left
            val blockH = min(blockSize, region.bottom - y)
            while (x < region.right) {
                val blockW = min(blockSize, region.right - x)
                val color = averageColor(bitmap, x, y, blockW, blockH)
                paint.color = color
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + blockW).toFloat(),
                    (y + blockH).toFloat(),
                    paint
                )
                x += blockSize
            }
            y += blockSize
        }
    }

    private fun averageColor(bitmap: Bitmap, x: Int, y: Int, w: Int, h: Int): Int {
        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0
        val step = max(1, min(w, h) / 4)
        var yy = y
        while (yy < y + h) {
            var xx = x
            while (xx < x + w) {
                val c = bitmap.getPixel(xx.coerceIn(0, bitmap.width - 1), yy.coerceIn(0, bitmap.height - 1))
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
                count++
                xx += step
            }
            yy += step
        }
        if (count == 0) return 0xFF000000.toInt()
        return (0xFF shl 24) or
            ((r / count).toInt() shl 16) or
            ((g / count).toInt() shl 8) or
            (b / count).toInt()
    }
}
