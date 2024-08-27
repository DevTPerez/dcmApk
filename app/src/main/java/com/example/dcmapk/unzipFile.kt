package com.example.dcmapk

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

suspend fun unzipFile(zipFile: File, targetDirectory: File) {
    withContext(Dispatchers.IO) {
        targetDirectory.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(targetDirectory, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
            zis.closeEntry()
        }
        Log.d("UnzipFile", "Descompactação concluída em ${targetDirectory.absolutePath}")
    }
}