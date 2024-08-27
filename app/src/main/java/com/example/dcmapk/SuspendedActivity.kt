package com.example.dcmapk

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SuspendedActivity  : AppCompatActivity() {



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Definir o conteúdo da atividade a partir do layout XML
        setContentView(R.layout.activity_suspended)


    }
}