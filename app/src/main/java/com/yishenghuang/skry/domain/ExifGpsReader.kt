package com.yishenghuang.skry.domain

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

data class GpsExifResult(
    val hasGps: Boolean,
    val latitude: Double?,
    val longitude: Double?
)

object ExifGpsReader {
    fun read(context: Context, uri: Uri): GpsExifResult {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                read(stream)
            } ?: GpsExifResult(false, null, null)
        }.getOrDefault(GpsExifResult(false, null, null))
    }

    fun read(stream: InputStream): GpsExifResult {
        val exif = ExifInterface(stream)
        val latLong = FloatArray(2)
        val has = exif.getLatLong(latLong)
        return if (has) {
            GpsExifResult(true, latLong[0].toDouble(), latLong[1].toDouble())
        } else {
            GpsExifResult(false, null, null)
        }
    }
}
