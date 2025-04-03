package com.example.medidordecibelesapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.io.IOException

class DecibelMeasurementActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mediaRecorder: MediaRecorder? = null
    private val decibelFileName = "decibel_log.txt"

    private lateinit var dbHelper: DecibelDbHelper
    private val PERMISSION_REQUEST_CODE = 123
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var decibelTextView: TextView
    private lateinit var captureButton: Button
    private lateinit var historyButton: Button
    private lateinit var backButton: ImageButton
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_decibel_measurement)

        dbHelper = DecibelDbHelper(this)
        Log.d("Database", "Database path: ${dbHelper.getDatabasePath()}")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize views safely with null checking
        initializeViews()

        // Set up the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Set up click listeners
        setupClickListeners()

        Log.d("AudioMeasurement", "Starting audio measurement")
        checkAudioPermission()
    }

    private fun initializeViews() {
        decibelTextView = findViewById(R.id.decibelTextView)
            ?: throw NullPointerException("Could not find decibelTextView")

        captureButton = findViewById(R.id.captureButton)
            ?: throw NullPointerException("Could not find captureButton")

        historyButton = findViewById(R.id.historyButton)
            ?: throw NullPointerException("Could not find historyButton")

        backButton = findViewById(R.id.backButton)
            ?: throw NullPointerException("Could not find backButton")
    }

    private fun setupClickListeners() {
        captureButton.setOnClickListener {
            saveCurrentMeasurement()
        }

        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun checkAudioPermission() {
        val permissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        Log.d("AudioMeasurement", "Permissions to request: ${permissionsToRequest.joinToString()}")
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest,
                PERMISSION_REQUEST_CODE
            )
        } else {
            startDecibelMeasurement()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allPermissionsGranted) {
                startDecibelMeasurement()
            } else {
                Toast.makeText(
                    this,
                    "Se requieren todos los permisos para medir los decibeles",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private var currentDecibels: Double = 0.0
    private fun startDecibelMeasurement() {
        try {
            if (mediaRecorder == null) {
                val outputFile = File(cacheDir, "temp_audio_record.3gp")
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFile.absolutePath)
                    try {
                        prepare()
                        start()
                        Log.d("AudioMeasurement", "MediaRecorder started successfully with AAC encoding")
                    } catch (e: IOException) {
                        Log.e("AudioMeasurement", "Error preparing MediaRecorder", e)
                        e.printStackTrace()
                        release()
                        mediaRecorder = null
                        return
                    }
                }

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                // Add initial delay to ensure MediaRecorder is ready
                handler.postDelayed({
                    val measurementRunnable = object : Runnable {
                        override fun run() {
                            try {
                                val amplitude = mediaRecorder?.maxAmplitude ?: 0
                                currentDecibels = if (amplitude > 0) {
                                    20 * Math.log10(amplitude.toDouble() / 32767.0) + 90.0
                                } else {
                                    0.0
                                }

                                decibelTextView.text = String.format("%.1f dB", currentDecibels)
                                handler.postDelayed(this, 100) // Update every 100ms
                            } catch (e: Exception) {
                                Log.e("AudioMeasurement", "Error reading amplitude", e)
                            }
                        }
                    }
                    handler.post(measurementRunnable)
                }, 1000) // 1 second initial delay

                startLocationUpdates()
            } else {
                Log.d("AudioMeasurement", "MediaRecorder is already running")
            }
        } catch (e: Exception) {
            Log.e("AudioMeasurement", "Error in startDecibelMeasurement", e)
            e.printStackTrace()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(1000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocation = location
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }

    private fun saveDecibelToFile(decibels: Double) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, decibelFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        try {
            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let { documentUri ->
                contentResolver.openOutputStream(documentUri, "wa")?.use { outputStream ->
                    outputStream.write("Decibeles: $decibels db\n".toByteArray())
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving to MediaStore", e)
        }
    }

    private fun saveCurrentMeasurement() {
        try {
            val timestamp = System.currentTimeMillis().toString()

            Log.d("AudioMeasurement", "Current decibel value before saving: $currentDecibels")

            if (currentDecibels <= 0) {
                Log.w("AudioMeasurement", "Attempting to save zero or negative decibel value")
                Toast.makeText(
                    this,
                    "No hay medición de audio activa",
                    Toast.LENGTH_SHORT
                ).show()
                startDecibelMeasurement()
                return
            }

            if (currentLocation != null) {
                dbHelper.addDecibelMeasurement(
                    currentDecibels,
                    timestamp,
                    currentLocation!!.latitude,
                    currentLocation!!.longitude
                )
                Log.d("AudioMeasurement", "Measurement saved with location. Decibels: $currentDecibels, Location: (${currentLocation!!.latitude}, ${currentLocation!!.longitude})")
                Toast.makeText(
                    this,
                    "Medición guardada con ubicación",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                dbHelper.addDecibelMeasurement(currentDecibels, timestamp)
                Log.d("AudioMeasurement", "Measurement saved without location. Decibels: $currentDecibels")
                Toast.makeText(
                    this,
                    "Medición guardada sin ubicación",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e("AudioMeasurement", "Error saving measurement. Current decibels: $currentDecibels", e)
            Toast.makeText(
                this,
                "Error al guardar la medición: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        Log.d("AudioMeasurement", "Stopping audio measurement")
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}