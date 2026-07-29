package com.yishenghuang.skry.util

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.roundToInt

/** Gallery permission + MediaStore helpers for API 26–current. */
object MediaAccess {

    fun requiredReadPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        Build.VERSION.SDK_INT >= 29 -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun hasGalleryAccess(context: Context): Boolean =
        requiredReadPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun imagesCollectionUri(): Uri =
        if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    fun contentUriForId(id: Long): Uri =
        ContentUris.withAppendedId(imagesCollectionUri(), id)

    /**
     * Decode a downsampled software bitmap.
     * [scaleMode] true = fit inside maxSide; false = shrink only when longer than maxSide
     * (matches prior PrivacyScanner / VaultService behavior).
     */
    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        maxSide: Int,
        mutable: Boolean,
        scaleModeFit: Boolean
    ): Bitmap? {
        return if (Build.VERSION.SDK_INT >= 28) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val longest = max(w, h).coerceAtLeast(1)
                    if (scaleModeFit) {
                        val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    } else {
                        val scale = if (longest > maxSide) longest.toFloat() / maxSide else 1f
                        decoder.setTargetSize(
                            (w / scale).toInt().coerceAtLeast(1),
                            (h / scale).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.isMutableRequired = mutable
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }.getOrNull()
        } else {
            decodeWithBitmapFactory(context, uri, maxSide, mutable, scaleModeFit)
        }
    }

    private fun decodeWithBitmapFactory(
        context: Context,
        uri: Uri,
        maxSide: Int,
        mutable: Boolean,
        scaleModeFit: Boolean
    ): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val w = bounds.outWidth.coerceAtLeast(1)
        val h = bounds.outHeight.coerceAtLeast(1)
        val longest = max(w, h)
        var sample = 1
        while (longest / sample > maxSide) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = mutable
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return@runCatching null

        val bw = bitmap.width
        val bh = bitmap.height
        val bl = max(bw, bh)
        if (bl <= maxSide) return@runCatching bitmap

        val scale = if (scaleModeFit) {
            maxSide.toFloat() / bl
        } else {
            maxSide.toFloat() / bl
        }
        val tw = (bw * scale).roundToInt().coerceAtLeast(1)
        val th = (bh * scale).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap, tw, th, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }.getOrNull()

    sealed class DeleteOutcome {
        data object Deleted : DeleteOutcome()
        data class NeedsUserConfirmation(val intentSender: IntentSender) : DeleteOutcome()
        data object Failed : DeleteOutcome()
    }

    fun deleteMedia(context: Context, uris: List<Uri>): DeleteOutcome {
        if (uris.isEmpty()) return DeleteOutcome.Failed
        return when {
            Build.VERSION.SDK_INT >= 30 -> {
                runCatching {
                    val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
                    DeleteOutcome.NeedsUserConfirmation(request.intentSender)
                }.getOrDefault(DeleteOutcome.Failed)
            }
            else -> deleteLegacy(context, uris)
        }
    }

    private fun deleteLegacy(context: Context, uris: List<Uri>): DeleteOutcome {
        var needsConfirmation: IntentSender? = null
        var deletedAny = false
        for (uri in uris) {
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) deletedAny = true
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) {
                    needsConfirmation = e.userAction.actionIntent.intentSender
                    break
                }
            }
        }
        return when {
            needsConfirmation != null ->
                DeleteOutcome.NeedsUserConfirmation(needsConfirmation)
            deletedAny -> DeleteOutcome.Deleted
            else -> DeleteOutcome.Failed
        }
    }
}
