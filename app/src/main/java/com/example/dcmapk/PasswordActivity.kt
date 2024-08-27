package com.example.dcmapk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PasswordActivity: AppCompatActivity() {

    private lateinit var passwordView: EditText
    private val correctPassword = "100782"
    private var enteredPassword = ""

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)

        passwordView = findViewById(R.id.password_view)

        val buttonIds = listOf(
            R.id.button_0, R.id.button_1, R.id.button_2, R.id.button_3,
            R.id.button_4, R.id.button_5, R.id.button_6, R.id.button_7,
            R.id.button_8, R.id.button_9
        )

        buttonIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener { button ->
                val digit = (button as Button).text.toString()
                onDigitPressed(digit)
            }
        }

        findViewById<Button>(R.id.button_ok).setOnClickListener {
            onOkPressed()
        }

        findViewById<Button>(R.id.button_cancel).setOnClickListener {
            onCancelPressed()
        }
    }

    private fun onDigitPressed(digit: String) {
        enteredPassword += digit
        passwordView.setText(enteredPassword)
    }

    private fun onOkPressed() {
        if (enteredPassword == correctPassword) {
            Toast.makeText(this, "Senha correta!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
        } else {
            Toast.makeText(this, "Senha incorreta. Tente novamente.", Toast.LENGTH_SHORT).show()
        }
        // Limpar a senha após verificação
        enteredPassword = ""
        passwordView.setText("")
        finish()
    }

    private fun onCancelPressed() {
        enteredPassword = ""
        passwordView.setText("")
        finish()
    }
}