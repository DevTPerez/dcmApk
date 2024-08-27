package com.example.dcmapk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class VideoActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private val videoList = mutableListOf<File>()
    private var currentVideoIndex = 0
    private lateinit var videoViewOption: String
    private val FREEZE_BUFFER_TIME: Long = 10000 // 10 segundos de buffer após a duração do vídeo
    private var freezeCheckRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        // Inicializa SharedPreferences
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        videoViewOption = sharedPreferences.getString("videoViewOption", "") ?: ""
        Log.d("Inicio de video view", "inicio do videoview")

        videoView = findViewById(R.id.videoView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            loadVideosAndPlay()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideosAndPlay()
        } else {
            Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadVideosAndPlay() {
        loadVideos()
        if (videoList.isNotEmpty()) {
            playVideo()
            Toast.makeText(this, "Vídeo encontrado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Nenhum vídeo encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadVideos() {
        val directoryPath = if (videoViewOption == "Landscape") {
            File(Environment.getExternalStorageDirectory().toString() + "/Movies")
        } else {
            File(Environment.getExternalStorageDirectory().toString() + "/Movies/vertical")
        }

        Log.d("VideoActivity", "Directory path: ${directoryPath.absolutePath}")
        if (directoryPath.exists() && directoryPath.isDirectory) {
            val files = directoryPath.listFiles { file -> file.extension.equals("mp4", true) }
            if (files != null) {
                videoList.addAll(files)
                Log.d("VideoActivity", "Videos found: ${files.size}")
            } else {
                Log.d("VideoActivity", "No videos found in the directory")
            }
        } else {
            Log.d("VideoActivity", "Directory does not exist or is not a directory")
        }
    }

    private fun playVideo() {
        enableFullScreenMode()

        if (videoList.isNotEmpty()) {
            val videoFile = videoList[currentVideoIndex]
            Log.d("VideoActivity", "Playing video: ${videoFile.absolutePath}")

            if (videoFile.canRead()) {
                videoView.setVideoURI(Uri.fromFile(videoFile))

                val layoutParams = videoView.layoutParams
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                videoView.layoutParams = layoutParams

                videoView.setOnPreparedListener { mp: MediaPlayer ->
                    Log.d("VideoActivity", "Video is prepared and starting")
                    mp.start()

                    // Cancel any previous freeze check and start a new one
                    freezeCheckRunnable?.let { handler.removeCallbacks(it) }
                    monitorVideoFreeze(videoFile)
                }

                videoView.setOnCompletionListener {
                    currentVideoIndex = (currentVideoIndex + 1) % videoList.size
                    playVideo() // Reproduz o próximo vídeo
                }

                videoView.setOnErrorListener { mp, what, extra ->
                    Log.e("VideoActivity", "Error occurred: what=$what, extra=$extra")
                    handleVideoError() // Trata o erro e reinicia a aplicação
                    true // Indica que o erro foi tratado
                }

                videoView.start()
            } else {
                Log.e("VideoActivity", "Cannot read video file: ${videoFile.absolutePath}")
                Toast.makeText(this, "Cannot read video file: ${videoFile.name}", Toast.LENGTH_SHORT).show()
                handleVideoError() // Trata a situação de erro de leitura
            }
        } else {
            Log.e("VideoActivity", "Video list is empty.")
            Toast.makeText(this, "No videos available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableFullScreenMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    suspend fun stopVideoView() {
        if (videoView.isPlaying) {
            videoView.stopPlayback()
        }
    }

    private fun handleVideoError() {
        Toast.makeText(this, "Erro ao reproduzir vídeo", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, WaitingScreenActivity::class.java)
        startActivity(intent)

        Handler(Looper.getMainLooper()).postDelayed({
            restartApp(this)
        }, 2000) // Ajuste o tempo conforme necessário
    }

    private fun restartApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        if (context is Activity) {
            context.finishAffinity()
        }
    }

    private fun monitorVideoFreeze(videoFile: File) {
        videoView.setOnPreparedListener { mp: MediaPlayer ->
            mp.start()
            // Aguarde um pouco para garantir que a duração do vídeo esteja disponível
            handler.postDelayed({
                val videoDuration = videoView.duration
                val freezeCheckTime = videoDuration + FREEZE_BUFFER_TIME

                freezeCheckRunnable = Runnable {
                    // Verifica se o vídeo está ainda em reprodução
                    if (videoView.isPlaying) {
                        Log.e("VideoActivity", "Video parece estar congelado. Arquivo: ${videoFile.name}")
                        restartApp(this)
                    }
                }
                handler.postDelayed(freezeCheckRunnable!!, freezeCheckTime)
            }, 1000) // Aguarda 1 segundo para garantir que a duração seja válida
        }
    }
}
