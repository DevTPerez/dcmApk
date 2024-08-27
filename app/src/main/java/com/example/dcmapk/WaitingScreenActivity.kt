package com.example.dcmapk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.launch
import java.io.File



class WaitingScreenActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var imagemLogo: ImageView
    private var isRotated = "true"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_screen)

        //receber informaçao de orientação da tela

        // Inicializa SharedPreferences
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val videoViewOption = sharedPreferences.getString("videoViewOption", "")
        val clientPreference = PreferencesManager.getClientPreference(this)
        val locatePreference = PreferencesManager.getLocatePreference(this)
        val modulePreference = PreferencesManager.getModulePreference(this)

        Log.d("preferencias", "$clientPreference/$locatePreference/$modulePreference")

        // Ajusta a orientação conforme a preferência salva
        enableFullScreenMode()

        //receber imagem variavel

        imagemLogo = findViewById(R.id.image_logo)
        // Define a imagem com base na preferential do cliente
        val variableImage = when (clientPreference) {
            "widex" -> if (videoViewOption == "Portrait") R.drawable.widexbgv else R.drawable.widexbgh


            "dcmadmin" -> R.drawable.plmlogo
            else -> R.drawable.dcmlogo // Imagem padrão
        }
        imagemLogo.setImageResource(variableImage)

        //parar atividade verificação
        val serviceIntent = Intent(this, ComparisonService::class.java)
        stopService(serviceIntent)

        //verificar videos


        lifecycleScope.launch {
            val localVideos = listLocalVideos()
            Log.d("MainActivity", "Local videos: $localVideos")
        }
        lifecycleScope.launch {

            val responseBody =
                downloadJsonFromUrl("https://backend-hl1u.onrender.com/folder?client=/$clientPreference/$locatePreference/$modulePreference")
            val remoteVideos = parseVideosFromJson(responseBody)
            Log.d("MainActivity", "Remote videos: $remoteVideos")

        }

        lifecycleScope.launch {
            try {
                // Realiza a comparação inicial
                if(videoViewOption == "Portrait") {
                    isRotated = "false"
                    rotateVideosInMoviesDirectory()
                }
                if(isRotated == "true"){
                doComparisonAndHandleResult()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error launching VideoActivity: ${e.message}")
            }
        }
    }

    private fun rotateVideosInMoviesDirectory() {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)

        if (!moviesDir.exists() || !moviesDir.isDirectory) {
            Log.d("WaitingScreenActivity", "Directory does not exist or is not a directory")
            return
        }

        // Criar o diretório 'vertical' se não existir
        val verticalDir = File(moviesDir, "vertical")
        if (!verticalDir.exists()) {
            verticalDir.mkdir()
        }

        // Lista todos os arquivos no diretório Movies
        val files = moviesDir.listFiles()
        val verticalFiles = verticalDir.listFiles()

        if (files != null && verticalFiles != null) {
            for (file in files) {
                if (file.isFile && file.extension.equals("mp4", true)) {
                    // Constrói o caminho para o novo arquivo na pasta 'vertical'
                    val rotatedFile = File(verticalDir, "${file.nameWithoutExtension}.mp4")

                    // Constrói o comando FFmpeg para rotacionar o vídeo em 270 graus
                    val command = arrayOf(
                        "-y",
                        "-i", file.absolutePath,
                        "-c", "copy",
                        "-metadata:s:v:0", "rotate=90",
                        rotatedFile.absolutePath  // Salva o vídeo rotacionado no novo arquivo
                    )

                    // Executa o comando FFmpeg
                    val session = FFmpegKit.execute(command)
                    val returnCode = session.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        Log.d("WaitingScreenActivity", "Rotation completed successfully: ${file.name}")
                    } else {
                        val error = session.failStackTrace
                        Log.e("WaitingScreenActivity", "Error rotating ${file.name}: $error")
                    }
                }
            }
        }
        isRotated = "true"
    }


    private suspend fun doComparisonAndHandleResult() {
        try {
            val comparisonResult = doComparison()
            Log.d("comparacao", "iniciando comparacao")
            if (comparisonResult) {
                // Abrir a atividade de vídeos
                val videoIntent = Intent(this, VideoActivity::class.java)
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(videoIntent)
                    finish()
                }, 10000)
            } else {
                // Verificar a permissão antes de iniciar o download novamente
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkPermission()) {
                        startDownloadAndCompare()
                        Log.d("checkPermission", "startDownloadAndCompare")
                    } else {
                        requestPermission()
                        Log.d("requestPermission", "requestPermission")
                    }
                } else {
                    startDownloadAndCompare()
                    Log.d("else", "startDownloadAndCompare")
                    // Sem verificação de permissão aqui para versões anteriores ao M
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during comparison: ${e.message}")
        }
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this@WaitingScreenActivity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permissão concedida, iniciar o download novamente
                    lifecycleScope.launch {
                        startDownloadAndCompare()
                    }
                } else {
                    Toast.makeText(
                        this@WaitingScreenActivity,
                        "Permissão negada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

   private suspend fun doComparison(): Boolean {
        val clientPreference = PreferencesManager.getClientPreference(this@WaitingScreenActivity)
            ?: throw IllegalStateException("Client preference is null")
        val locatePreference = PreferencesManager.getLocatePreference(this@WaitingScreenActivity)
            ?: throw IllegalStateException("Locate preference is null")
        val modulePreference = PreferencesManager.getModulePreference(this@WaitingScreenActivity)
            ?: throw IllegalStateException("Module preference is null")
        val compareVideosUri =
            "https://backend-hl1u.onrender.com/folder?client=/$clientPreference/$locatePreference/$modulePreference"

        return compareLocalAndRemoteVideos(this@WaitingScreenActivity, compareVideosUri)
    }

    private suspend fun startDownloadAndCompare() {
        try {
            // Iniciar o download
            startDownloadAndUnzip()

            // Após o download, fazer a comparação novamente
            val comparisonResult = doComparison()

            if (comparisonResult) {
                // Abrir a atividade de vídeos
                val videoIntent = Intent(this@WaitingScreenActivity, VideoActivity::class.java)
                startActivity(videoIntent)
                finish()
            } else {
                // Deletar arquivos locais e tentar o download novamente
                deleteFilesFromMoviesDirectory()
                Log.d("MainActivity", "Local and remote videos are not the same after download")

            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during download or comparison: ${e.message}")
            // Lidar com o erro durante o download ou comparação aqui
        }
    }

    private suspend fun startDownloadAndUnzip() {
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val videoViewOption = sharedPreferences.getString("videoViewOption", "")
        val clientPreference = PreferencesManager.getClientPreference(this)
        val locatePreference = PreferencesManager.getLocatePreference(this)
        val modulePreference = PreferencesManager.getModulePreference(this)
        Log.d("iniciando dld", "iniciando dld")
        // URL de download
        val downloadUrl =
            "https://backend-hl1u.onrender.com/download/$clientPreference/$locatePreference/$modulePreference"
        Log.d("linkDld", downloadUrl)

        // Arquivo de saída para salvar o download
        val outputFile = File(filesDir, "downloadedFile.zip")
        Log.d("outputFile", outputFile.absolutePath)

        // Inicia o download
        lifecycleScope.launch {
            try {
                downloadFile(downloadUrl, outputFile)
                Toast.makeText(this@WaitingScreenActivity, "Download concluído", Toast.LENGTH_SHORT)
                    .show()

                val moviesDir = File(Environment.getExternalStorageDirectory().toString())
                val unzipTargetDir = File(moviesDir, "Movies")
                unzipFile(outputFile, unzipTargetDir)

                Toast.makeText(
                    this@WaitingScreenActivity,
                    "Descompactação concluída",
                    Toast.LENGTH_SHORT
                ).show()
                val comparisonResult = doComparison()
                // Após a descompactação, fazer a comparação novamente

                if (comparisonResult) {
                    // Abrir a atividade de vídeos
                    if(videoViewOption == "Portrait") {
                        isRotated = "false"
                        rotateVideosInMoviesDirectory()
                    }
                    val videoIntent = Intent(this@WaitingScreenActivity, VideoActivity::class.java)
                    startActivity(videoIntent)
                    finish()
                } else {
                    // Se os vídeos não forem iguais, lidar com isso aqui
                    deleteFilesFromMoviesDirectory()
                    Log.d("MainActivity", "Local and remote videos are not the same after download")
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@WaitingScreenActivity,
                    "Falha no download: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }


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
}