package com.example.dcmapk

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.JobIntentService

class MainActivity : AppCompatActivity() {

    private lateinit var settingButton: ImageView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val updateManager = UpdateManager(this)



        // Definir o conteúdo da atividade a partir do layout XML
        setContentView(R.layout.activity_main)

        // Inicializar componentes da interface do usuário
        initializeUIComponents()




        navigateBasedOnPreferences()
    }


    private fun navigateBasedOnPreferences() {
        val clientPreference = PreferencesManager.getClientPreference(this)
        val locatePreference = PreferencesManager.getLocatePreference(this)
        val modulePreference = PreferencesManager.getModulePreference(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (clientPreference == null && locatePreference == null && modulePreference == null) {
                startActivity(Intent(this, PasswordActivity::class.java))
                Log.d("chamando no main", "settings")
            } else {
                startActivity(Intent(this, WaitingScreenActivity::class.java))
                Log.d("chamando no main", "waiting")
            }

        }, 5000)
    }


    private fun initializeUIComponents() {
        settingButton = findViewById(R.id.settings_button)
        settingButton.setOnClickListener {
            startActivity(Intent(this, PasswordActivity::class.java))
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed detected")
            context?.let {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    BootService.enqueueWork(it, Intent(it, BootService::class.java))
                }else{
                    val mainActivityIntent = Intent(context, MainActivity::class.java)
                    mainActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(mainActivityIntent)
                }
            }
        }
    }
}

class BootService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        mainActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(mainActivityIntent)
    }

    companion object {
        private const val JOB_ID = 1000

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, BootService::class.java, JOB_ID, intent)
        }
    }
}
