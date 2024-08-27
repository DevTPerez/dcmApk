package com.example.dcmapk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


suspend fun listLocalVideos(): List<String> {
    return withContext(Dispatchers.IO) {

        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val videoFiles = moviesDir.listFiles { file ->
            file.isFile && (file.extension.equals("mp4", ignoreCase = true) || file.extension.equals("mkv", ignoreCase = true))
        }
        val videoNames = videoFiles?.map { file -> file.name }?.sorted() ?: emptyList()

        Log.d("LocalVideos", "Local video files: $videoNames")

        videoNames
    }
}


suspend fun downloadJsonFromUrl(url: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                response.body?.string() ?: throw IOException("Empty response body")
            }
        } catch (e: IOException) {
            Log.e("downloadJsonFromUrl", "Error downloading JSON: ${e.message}")
            "" // Retornar uma string vazia em caso de erro
        }
    }
}


fun parseVideosFromJson(responseBody: String?): List<String> {
    val listaDeVideos = mutableListOf<String>()

    responseBody?.let {
        try {
            val jsonObject = JSONObject(it)
            val videosArray = jsonObject.getJSONArray("videos")
            for (i in 0 until videosArray.length()) {
                val videoUrl = videosArray.getString(i)
                listaDeVideos.add(videoUrl)
            }
        } catch (e: JSONException) {
            Log.e("MainActivity", "Error parsing JSON: ${e.message}")
        }
    }

    return listaDeVideos
}

@SuppressLint("LongLogTag")
suspend fun compareLocalAndRemoteVideos(context: Context, downloadUrl: String): Boolean {
    return try {
        // Lista local de vídeos
        val localVideos = listLocalVideos().sorted()

        // Baixar e parsear a lista remota de vídeos
        val remoteResponse = downloadJsonFromUrl(downloadUrl)
        var remoteVideos = parseVideosFromJson(remoteResponse).sorted()

        // Comparar as listas ordenadas

        if(remoteVideos.isEmpty()){
            remoteVideos = localVideos
        }

        localVideos == remoteVideos


    } catch (e: IOException) {
        Log.e("compareLocalAndRemoteVideos", "IO Exception: ${e.message}")
        false // Retornar false ou qualquer valor default apropriado
    } catch (e: Exception) {
        Log.e("compareLocalAndRemoteVideos", "Exception: ${e.message}")
        false // Retornar false ou qualquer valor default apropriado
    }
}

suspend fun main(context: Context): Boolean {
    return try {
        val clientPreference = PreferencesManager.getClientPreference(context) ?: throw IllegalStateException("Client preference is null")
        val locatePreference = PreferencesManager.getLocatePreference(context) ?: throw IllegalStateException("Locate preference is null")
        val modulePreference = PreferencesManager.getModulePreference(context) ?: throw IllegalStateException("Module preference is null")
        val compareVideosUri = "https://backend-hl1u.onrender.com/folder?client=/$clientPreference/$locatePreference/$modulePreference"

        val comparisonResult = compareLocalAndRemoteVideos(context, compareVideosUri)
        Log.d("ComparisonResult", "Local and remote videos are the same: $comparisonResult")
        comparisonResult // Retornar o resultado da comparação
    } catch (e: IllegalStateException) {
        Log.e("main", "Preferences Error: ${e.message}")
        false // Retornar false se houver erro de preferências
    } catch (e: Exception) {
        Log.e("main", "Error in main: ${e.message}")
        false // Retornar false se ocorrer qualquer outra exceção
    }
}

suspend fun getStatusClient(url: String): Any {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseString = response.body ?: "Ativo"
                val jsonObject = JsonParser.parseString(responseString.toString()).asJsonObject
                val result = jsonObject.get("status").asString

                return@withContext (result)
            } else {
                "Ativo"
            }
        } catch (e: IllegalStateException) {
            Log.e("main", "Preferences Error: ${e.message}")
            "Ativo" // Retornar false se houver erro de preferências
        } catch (e: Exception) {
            Log.e("main", "Error in main: ${e.message}")
            "Ativo" // Retornar false se ocorrer qualquer outra exceção
        }
    }
}

data class HeartbeatRequest(
    val storeName: String,
    val screenName: String
)

fun sendHeartbeatRequest(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val clientPreference = PreferencesManager.getClientPreference(context)
        val locatePreference = PreferencesManager.getLocatePreference(context)
        val modulePreference = PreferencesManager.getModulePreference(context)

        if (clientPreference == null || locatePreference == null || modulePreference == null) {
            Log.e("HeartbeatRequest", "One or more preferences are null")
            return@launch
        }

        val storeName = locatePreference
        val screenName = modulePreference

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter = moshi.adapter(HeartbeatRequest::class.java)

        val requestBody = HeartbeatRequest(storeName, screenName)
        val json = jsonAdapter.toJson(requestBody)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://backend-hl1u.onrender.com/heartbeat?username=${clientPreference}")
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("HeartbeatRequest", "Unexpected code ${response.code}")
            } else {
                Log.i("HeartbeatRequest", response.body?.string() ?: "No response body")
            }
        } catch (e: SocketTimeoutException) {
            Log.e("HeartbeatRequest", "Request timed out: ${e.message}", e)
        } catch (e: IOException) {
            Log.e("HeartbeatRequest", "Network error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("HeartbeatRequest", "Unexpected error: ${e.message}", e)
        }
    }
}
