package com.example.medidordecibelesapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val sharedPreferencesFile = "AppThemePrefs"
    private val themeKey = "isDarkTheme"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing MainActivity")
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)
            
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            val changeThemeButton: ImageButton = findViewById(R.id.changeThemeButton)

            val sharedPreferences = getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
            val isDarkTheme = sharedPreferences.getBoolean(themeKey, false)

            applyTheme(changeThemeButton, isDarkTheme)

            changeThemeButton.setOnClickListener {
                val currentTheme = sharedPreferences.getBoolean(themeKey, false)
                val newTheme = !currentTheme
                sharedPreferences.edit().putBoolean(themeKey, newTheme).apply()
                applyTheme(changeThemeButton, newTheme)
            }

            val reportNoiseButton: ImageButton = findViewById(R.id.reportNoiseButton)
            reportNoiseButton.setOnClickListener { openDecibelMeasurement(it) }


            val reportDamageButton: ImageButton = findViewById(R.id.reportDamageButton)
            reportDamageButton.setOnClickListener { openDamageActivity(it) }

            val reportCrimeButton: ImageButton = findViewById(R.id.reportCrimeButton)
            reportCrimeButton.setOnClickListener { openCrimeReport(it)}


            Log.d(TAG, "onCreate: MainActivity setup complete")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MainActivity", e)
        }
    }

    private fun applyTheme(imageView: ImageView, isDarkTheme: Boolean){
        if(isDarkTheme){
            imageView.setImageResource(R.drawable.icon_light)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }else{
            imageView.setImageResource(R.drawable.icon_dark)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        delegate.applyDayNight()
    }
    
    fun openDecibelMeasurement(view: View) {
        Log.d(TAG, "Opening DecibelMeasurementActivity")
        val intent = Intent(this, DecibelMeasurementActivity::class.java)
        startActivity(intent)
    }


    fun openDamageActivity(view: View) {
        Log.d(TAG, "Opening DamageActivity")
        val intent = Intent(this, DamageActivity::class.java)

    fun openCrimeReport(view: View) {
        Log.d(TAG, "Opening CrimeReportActivity")
        val intent = Intent(this, CrimeReportActivity::class.java )

        startActivity(intent)
    }
}