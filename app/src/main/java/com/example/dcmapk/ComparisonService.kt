package com.example.dcmapk

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.VideoView
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class ComparisonService : Service() {

    private var serviceJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var videoView: VideoView
    private var status = "Ativo"
    private var isAtive = "True"



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceJob = coroutineScope.launch {



            while (isActive) {
                delay(TimeUnit.MINUTES.toMillis(5)) // A cada 5 minutos
                //requisicao para heartbeat
                sendHeartbeatRequest(context = this@ComparisonService)

                Log.d("ComparisonService", "Verificação de Status")

                verifyStatus()
                Log.d("ComparisonService", verifyStatus().toString())
                if(status !== verifyStatus().toString()){
                    isAtive = "false"
                    val suspendedIntent = Intent(this@ComparisonService, SuspendedActivity::class.java)
                    startActivity(suspendedIntent)

                }



                Log.d("ComparisonService", isAtive)
                Log.d("ComparisonService", "comparacao iniciada")




                try {
                    val comparisonResult = doComparison()

                    if (!comparisonResult) {
                        Log.d("ComparisonService", "Local and remote videos are not the same, starting download and comparison")


                        // Criar um Intent para iniciar a WaitingScreenActivity
                        val activityIntent = Intent(this@ComparisonService, WaitingScreenActivity::class.java)

                        // Criar um PendingIntent para iniciar a atividade de forma assíncrona
                        val pendingIntent = PendingIntent.getActivity(this@ComparisonService, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                        // Enviar o PendingIntent
                        try {
                            pendingIntent.send()
                        } catch (e: PendingIntent.CanceledException) {
                            Log.e("ComparisonService", "PendingIntent for activity canceled: ${e.message}")
                        }
                    } else {
                        Log.d("ComparisonService", "Activity is in foreground, skipping comparison")
                    }
                } catch (e: Exception) {
                    Log.e("ComparisonService", "Error during comparison: ${e.message}")
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
    private suspend fun doComparison(): Boolean {
        val clientPreference = PreferencesManager.getClientPreference(this@ComparisonService)
            ?: throw IllegalStateException("Client preference is null")
        val locatePreference = PreferencesManager.getLocatePreference(this@ComparisonService)
            ?: throw IllegalStateException("Locate preference is null")
        val modulePreference = PreferencesManager.getModulePreference(this@ComparisonService)
            ?: throw IllegalStateException("Module preference is null")
        val compareVideosUri =
            "https://backend-hl1u.onrender.com/folder?client=/$clientPreference/$locatePreference/$modulePreference"

        return compareLocalAndRemoteVideos(this@ComparisonService, compareVideosUri)
    }

    private suspend fun verifyStatus (): Any {
        val clientPreference = PreferencesManager.getClientPreference(this@ComparisonService)
            ?: throw IllegalStateException("Client preference is null")
        val getStatus = "https://backend-hl1u.onrender.com/getStatus/$clientPreference/"

        return getStatusClient(getStatus)
    }

}
