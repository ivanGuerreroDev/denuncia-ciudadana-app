package com.example.medidordecibelesapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
}