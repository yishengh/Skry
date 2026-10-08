package com.yishenghuang.skry.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Keep large gallery selections below Android saved-instance-state transaction limits. */
object SavedUriList {
    fun encode(values: List<String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(GZIPOutputStream(bytes)).use { output ->
            output.writeInt(values.size)
            values.forEach(output::writeUTF)
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray?): ArrayList<String> {
        if (bytes == null) return arrayListOf()
        return runCatching {
            DataInputStream(GZIPInputStream(ByteArrayInputStream(bytes))).use { input ->
                val size = input.readInt()
                require(size in 0..1_000_000)
                ArrayList<String>(size).apply { repeat(size) { add(input.readUTF()) } }
            }
        }.getOrElse { arrayListOf() }
    }
}
