package com.denunciaciudadana.app.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.denunciaciudadana.app.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val sharedPreferencesFile = "AppThemePrefs"
    private val themeKey = "isDarkTheme"
    private lateinit var emergencyDrawerBehavior: BottomSheetBehavior<View>
    private lateinit var overlayView: View
    private val REQUEST_PHONE_CALL = 1

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
            
            // Configuramos el BottomSheet para las llamadas de emergencia
            setupEmergencyDrawer()
            
            // Configurar click listeners para las CardViews
            val reportNoiseCard: androidx.cardview.widget.CardView = findViewById(R.id.reportNoiseCard)
            reportNoiseCard.setOnClickListener {
                openDecibelMeasurement()
            }

            val reportDamageCard: androidx.cardview.widget.CardView = findViewById(R.id.reportDamageCard)
            reportDamageCard.setOnClickListener {
                openDamageActivity()
            }

            val crimeReportCard: androidx.cardview.widget.CardView = findViewById(R.id.crimeReportCard)
            crimeReportCard.setOnClickListener {
                openCrimeReport()
            }

            // Configurar click listener para la lista de reportes
            val reportsListCard: androidx.cardview.widget.CardView = findViewById(R.id.reportsListCard)
            reportsListCard.setOnClickListener {
                openReportsList()
            }

            Log.d(TAG, "onCreate: MainActivity setup complete")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MainActivity", e)
        }
    }

    private fun setupEmergencyDrawer() {
        Log.d(TAG, "Setting up emergency drawer")
        try {
            // Obtenemos la referencia al layout incluido
            val emergencyDrawerInclude = findViewById<View>(R.id.emergency_drawer_include)
            Log.d(TAG, "Emergency drawer include found: ${emergencyDrawerInclude != null}")
            
            if (emergencyDrawerInclude != null) {
                // El drawer es el elemento raíz del layout incluido
                emergencyDrawerBehavior = BottomSheetBehavior.from(emergencyDrawerInclude)
                Log.d(TAG, "BottomSheetBehavior obtenido correctamente")
                
                // Por defecto, ocultamos el bottom sheet
                emergencyDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                Log.d(TAG, "Estado inicial del drawer configurado a HIDDEN")
                
                // Obtenemos el overlay (fondo semi-transparente)
                overlayView = findViewById(R.id.overlayView)
                Log.d(TAG, "Overlay view encontrado: ${overlayView != null}")
                
                // Configuramos el botón de emergencia
                val emergencyButton = findViewById<View>(R.id.emergencyButton)
                Log.d(TAG, "Emergency button encontrado: ${emergencyButton != null}")
                
                emergencyButton.setOnClickListener {
                    Log.d(TAG, "Emergency button clicked - intentando mostrar drawer")
                    showEmergencyDrawer()
                }
                
                // Configuramos el comportamiento del overlay
                overlayView.setOnClickListener {
                    Log.d(TAG, "Overlay clicked - ocultando drawer")
                    hideEmergencyDrawer()
                }
                
                // Escuchamos los cambios de estado del bottom sheet
                emergencyDrawerBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        Log.d(TAG, "Drawer state changed to: $newState")
                        when (newState) {
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                overlayView.visibility = View.GONE
                                Log.d(TAG, "Drawer hidden - overlay ocultado")
                            }
                            BottomSheetBehavior.STATE_EXPANDED -> {
                                Log.d(TAG, "Drawer expandido completamente")
                            }
                        }
                    }
                    
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // Log menos frecuente para no saturar el logcat
                        if (slideOffset % 0.1f < 0.01f) {
                            Log.d(TAG, "Drawer slide offset: $slideOffset")
                        }
                        // Ajustamos la opacidad del overlay según el deslizamiento
                        if (slideOffset > -0.8f) {
                            overlayView.visibility = View.VISIBLE
                            overlayView.alpha = (slideOffset + 0.8f) * 0.8f
                        } else {
                            overlayView.visibility = View.GONE
                        }
                    }
                })
                
                // Configuramos los click listeners para los servicios de emergencia
                setupEmergencyServiceCallButtons(emergencyDrawerInclude)
                Log.d(TAG, "Setup de botones de emergencia completado")
            } else {
                Log.e(TAG, "Error: emergency_drawer_include not found")
                Toast.makeText(this, "Error al cargar el panel de emergencia", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupEmergencyDrawer", e)
            Toast.makeText(this, "Error al configurar el panel de emergencia", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupEmergencyServiceCallButtons(drawerView: View) {
        // Configuramos los botones para llamar a los servicios de emergencia
        val emergencyServiceCard = drawerView.findViewById<View>(R.id.emergencyServiceCard)
        emergencyServiceCard.setOnClickListener {
            Log.d(TAG, "Emergency service clicked")
            dialPhoneNumber("911") // Emergencias
        }
        
        val policeServiceCard = drawerView.findViewById<View>(R.id.policeServiceCard)
        policeServiceCard.setOnClickListener {
            Log.d(TAG, "Police service clicked")
            dialPhoneNumber("104") // Policía Nacional
        }
        
        val fireServiceCard = drawerView.findViewById<View>(R.id.fireServiceCard)
        fireServiceCard.setOnClickListener {
            Log.d(TAG, "Fire service clicked")
            dialPhoneNumber("103") // Cuerpo de Bomberos
        }
        
        val ambulanceServiceCard = drawerView.findViewById<View>(R.id.ambulanceServiceCard)
        ambulanceServiceCard.setOnClickListener {
            Log.d(TAG, "Ambulance service clicked")
            dialPhoneNumber("*455") // Cruz Roja
        }
        
        val sinaprocServiceCard = drawerView.findViewById<View>(R.id.sinaprocServiceCard)
        sinaprocServiceCard.setOnClickListener {
            Log.d(TAG, "Sinaproc service clicked")
            dialPhoneNumber("*335") // Sinaproc (usando número genérico, reemplazar con el correcto)
        }
        
        val atttServiceCard = drawerView.findViewById<View>(R.id.atttServiceCard)
        atttServiceCard.setOnClickListener {
            Log.d(TAG, "ATTT service clicked")
            dialPhoneNumber("5119320") // ATTT
        }
        
        val atencionCiudadanaServiceCard = drawerView.findViewById<View>(R.id.atencionCiudadanaServiceCard)
        atencionCiudadanaServiceCard.setOnClickListener {
            Log.d(TAG, "Atención Ciudadana service clicked")
            dialPhoneNumber("311") // Atención Ciudadana
        }
    }
    
    private fun dialPhoneNumber(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$phoneNumber")
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(intent)
                // Ocultar el drawer después de iniciar la llamada
                hideEmergencyDrawer()
            } else {
                // Solicitar permiso si no está concedido
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CALL_PHONE),
                    REQUEST_PHONE_CALL
                )
                Log.d(TAG, "Phone permission requested")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating phone call", e)
            Toast.makeText(this, "No se pudo iniciar la llamada", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PHONE_CALL) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Volvemos a intentar la llamada después de conceder el permiso
                // Esta es una simplificación. En un caso real, guardaríamos el número para volver a llamar.
                Toast.makeText(this, "Permiso concedido. Puedes intentar llamar de nuevo.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Se requiere permiso para realizar llamadas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmergencyDrawer() {
        Log.d(TAG, "Showing emergency drawer")
        try {
            // Primero hacemos visible el overlay
            overlayView.visibility = View.VISIBLE
            Log.d(TAG, "Overlay set to VISIBLE")
            
            // Luego expandimos el bottom sheet
            emergencyDrawerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            Log.d(TAG, "Drawer state set to EXPANDED")
            
            // Forzar un refresco de la UI
            overlayView.post {
                Log.d(TAG, "UI refreshed after expanding drawer")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar el drawer de emergencia", e)
            Toast.makeText(this, "Error al mostrar el panel de emergencia", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun hideEmergencyDrawer() {
        Log.d(TAG, "Hiding emergency drawer")
        try {
            // Ocultamos el bottom sheet
            emergencyDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            Log.d(TAG, "Drawer state set to HIDDEN")
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar el drawer de emergencia", e)
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
    
    fun openDecibelMeasurement() {
        Log.d(TAG, "Opening DecibelMeasurementActivity")
        val intent = Intent(this, DecibelMeasurementActivity::class.java)
        startActivity(intent)
    }


    fun openDamageActivity() {
        Log.d(TAG, "Opening DamageActivity")
        val intent = Intent(this, DamageActivity::class.java)
        startActivity(intent)
    }

    fun openCrimeReport() {
        Log.d(TAG, "Opening CrimeReportActivity")
        val intent = Intent(this, CrimeReportActivity::class.java)
        startActivity(intent)
    }

    fun openReportsList() {
        Log.d(TAG, "Opening ReportsListActivity")
        val intent = Intent(this, ReportsListActivity::class.java)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: MainActivity resumed")
    }
}