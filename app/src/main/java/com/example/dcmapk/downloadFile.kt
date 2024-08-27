package com.example.dcmapk

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

suspend fun downloadFile(url: String, outputFile: File) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val inputStream: InputStream? = response.body?.byteStream()
            val outputStream = FileOutputStream(outputFile)
            Log.d("DownloadFile", "Iniciando download de $url")

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("DownloadFile", "Download concluído: ${outputFile.absolutePath}")
        } else {
            throw Exception("Failed to download file: ${response.message}")
        }
    }
}

