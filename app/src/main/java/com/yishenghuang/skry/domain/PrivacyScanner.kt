package com.yishenghuang.skry.domain

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yishenghuang.skry.util.MediaAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class PrivacyScanOutcome(
    val findings: List<Finding>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ocrTextLength: Int = 0,
    val quality: QualityResult? = null
)

class PrivacyScanner(
    private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scan(
        uri: Uri,
        isScreenshot: Boolean,
        intrinsicWidth: Int,
        intrinsicHeight: Int,
        dateAddedSeconds: Long
    ): PrivacyScanOutcome = withContext(Dispatchers.IO) {
        val findings = mutableListOf<Finding>()

        val gps = ExifGpsReader.read(context, uri)
        if (gps.hasGps) {
            findings += Finding(
                type = FindingType.LOCATION_EXIF,
                label = "Location EXIF",
                confidence = 0.95f,
                snippet = "Precise GPS still embedded"
            )
        }

        val rotation = readRotationDegrees(uri)
        val bitmap = decodeSampledBitmap(uri, maxSide = 1600)
            ?: return@withContext PrivacyScanOutcome(
                findings = findings,
                latitude = gps.latitude,
                longitude = gps.longitude
            )

        var ocrLen = 0
        var quality: QualityResult? = null
        var ownedCopy: Bitmap? = null
        try {
            val argb = if (bitmap.config == Bitmap.Config.ARGB_8888) {
                bitmap
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)?.also { ownedCopy = it } ?: bitmap
            }
            val image = InputImage.fromBitmap(argb, rotation)
            val visionText = recognizer.process(image).await()
            val text = visionText.text.orEmpty()
            ocrLen = text.length
            val blocks = PrivacyRules.blocksFromMlKit(visionText, argb.width, argb.height)
            findings += PrivacyRules.analyze(
                fullText = text,
                blocks = blocks,
                imageWidth = argb.width,
                imageHeight = argb.height,
                isScreenshot = isScreenshot
            )
            val distinct = findings.distinctBy { it.type }
            quality = QualityAnalyzer.analyze(
                bitmap = argb,
                intrinsicWidth = intrinsicWidth.coerceAtLeast(argb.width),
                intrinsicHeight = intrinsicHeight.coerceAtLeast(argb.height),
                isScreenshot = isScreenshot,
                dateAddedSeconds = dateAddedSeconds,
                hasSensitiveScreenshotFinding = distinct.any {
                    it.type == FindingType.SENSITIVE_SCREENSHOT
                }
            )
            PrivacyScanOutcome(
                findings = distinct,
                latitude = gps.latitude,
                longitude = gps.longitude,
                ocrTextLength = ocrLen,
                quality = quality
            )
        } finally {
            ownedCopy?.takeIf { !it.isRecycled }?.recycle()
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    fun close() {
        recognizer.close()
    }

    private fun readRotationDegrees(uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
    }

    private fun decodeSampledBitmap(uri: Uri, maxSide: Int): Bitmap? {
        return MediaAccess.decodeSampledBitmap(
            context = context,
            uri = uri,
            maxSide = maxSide,
            mutable = false,
            scaleModeFit = false
        )
    }
}
