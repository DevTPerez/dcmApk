package com.example.dcmapk

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import android.provider.Settings

data class VersionInfo(val version: String, val url: String)

class UpdateManager(private val context: Context) {

    private val currentVersion = "1.0" // Substitua pela lógica de obtenção da versão atual do app

    fun checkForUpdates() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://backend-hl1u.onrender.com/getVersion")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                // Handle failure
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (!response.isSuccessful) {
                    println("Unexpected code $response")
                    return
                }

                val responseBody = response.body?.string()
                println("Server response: $responseBody") // Log para verificar a resposta do servidor

                responseBody?.let {
                    try {
                        val versionInfo = Gson().fromJson(it, VersionInfo::class.java)
                        if (versionInfo.version != currentVersion) {
                            downloadAndUpdate(versionInfo.url)
                        } else {
                            println("Versão atual é a mais recente")
                        }
                    } catch (e: Exception) {
                        println("Erro ao fazer parsing do JSON: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun downloadAndUpdate(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Atualização do Aplicativo")
            .setDescription("Baixando nova versão do aplicativo")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "new_version.apk")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Registrar um BroadcastReceiver para capturar quando o download estiver completo e iniciar a instalação
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Log.d("UpdateManager", "Download completed with ID: $id")
                    handleDownloadCompletion(downloadManager, id)
                }
            }
        }

        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    @SuppressLint("Range")
    private fun handleDownloadCompletion(downloadManager: DownloadManager, downloadId: Long) {
        checkInstallPermission()


        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val uri = downloadManager.getUriForDownloadedFile(downloadId)
                if (uri != null) {
                    installApk(uri)
                } else {
                    Log.e("UpdateManager", "Erro: URI de arquivo baixado é nula")
                }
            } else {
                val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                Log.e("UpdateManager", "Download failed with status: $status and reason: $reason")
            }
        }
        cursor.close()
    }

    private fun installApk(uri: Uri) {
        checkInstallPermission()
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }
    private fun checkInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val packageURI = Uri.parse("package:${context.packageName}")
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI)
            context.startActivity(intent)
        }
    }
}
