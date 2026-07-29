package com.yishenghuang.skry.domain

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.yishenghuang.skry.util.MediaAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

data class VaultStoreResult(
    val fileName: String,
    val redacted: Boolean
)

/**
 * App-private encrypted vault under files/vault/.
 * Uses AndroidX EncryptedFile + Keystore-backed MasterKey.
 */
class VaultService(private val context: Context) {

    private val vaultDir: File
        get() = File(context.filesDir, "vault").also { if (!it.exists()) it.mkdirs() }

    @Suppress("DEPRECATION")
    private fun masterKey(): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    @Suppress("DEPRECATION")
    private fun encryptedFile(file: File): EncryptedFile =
        EncryptedFile.Builder(
            context,
            file,
            masterKey(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

    suspend fun storeRedactedCopy(
        photoId: String,
        sourceUri: Uri,
        findings: List<Finding>
    ): VaultStoreResult = withContext(Dispatchers.IO) {
        val decoded = decodeBitmap(sourceUri, maxSide = 2048)
            ?: error("Unable to decode photo for vault")
        val working = if (decoded.isMutable && decoded.config == Bitmap.Config.ARGB_8888) {
            decoded
        } else {
            decoded.copy(Bitmap.Config.ARGB_8888, true)?.also {
                if (it !== decoded) decoded.recycle()
            } ?: decoded
        }
        val redacted = MosaicEngine.apply(working, findings)
        val jpeg = compressJpeg(redacted, quality = 88)
        if (!redacted.isRecycled) redacted.recycle()

        val fileName = "${photoId}_${System.currentTimeMillis()}.jpg.enc"
        val target = File(vaultDir, fileName)
        encryptedFile(target).openFileOutput().use { out ->
            out.write(jpeg)
        }
        VaultStoreResult(fileName = fileName, redacted = true)
    }

    suspend fun openDecryptedBytes(fileName: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(vaultDir, fileName)
        require(file.exists()) { "Vault file missing" }
        encryptedFile(file).openFileInput().use { it.readBytes() }
    }

    suspend fun deleteVaultFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(vaultDir, fileName)
        !file.exists() || file.delete()
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    private fun decodeBitmap(uri: Uri, maxSide: Int): Bitmap? {
        return MediaAccess.decodeSampledBitmap(
            context = context,
            uri = uri,
            maxSide = maxSide,
            mutable = true,
            scaleModeFit = true
        )
    }
}
