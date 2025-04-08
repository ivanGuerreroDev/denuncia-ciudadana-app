package com.example.medidordecibelesapp

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CrimeReportActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crime_report)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()

    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
            ?: throw NullPointerException("Could not find backButton")

    }

    private fun setupClickListeners() {

        backButton.setOnClickListener {
            Log.d("CrimeReportActivity", "Botón de regreso presionado")
            finish()
        }

    }

}