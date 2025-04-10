package com.example.medidordecibelesapp.activities

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.example.medidordecibelesapp.R
import com.example.medidordecibelesapp.api.ApiClient
import com.example.medidordecibelesapp.database.DBHelper
import com.example.medidordecibelesapp.models.AccusationDataItem
import com.example.medidordecibelesapp.models.NoiseAccusation
import com.example.medidordecibelesapp.models.NoiseAccusationResponse

class DecibelMeasurementActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private val audioFileName = "noise_recording.3gp"
    private var audioFile: File? = null

    private lateinit var dbHelper: DBHelper
    private val PERMISSION_REQUEST_CODE = 123
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var largeDecibelTextView: TextView
    private lateinit var noiseDescriptionTextView: TextView
    private lateinit var decibelProgressBar: ProgressBar
    private lateinit var captureButton: Button
    private lateinit var backButton: ImageButton
    private var currentLocation: Location? = null
    private var isRecording = false
    private var currentDecibels: Double = 0.0
    
    // Handler para controlar el tiempo de grabación
    private val handler = Handler(Looper.getMainLooper())
    private var recordingDurationMs = 10000 // 10 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_decibel_measurement)

        dbHelper = DBHelper(this)
        Log.d("Database", "Database path: ${dbHelper.getDatabasePath()}")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize views safely with null checking
        initializeViews()

        // Set up click listeners
        setupClickListeners()

        Log.d("AudioMeasurement", "Starting audio measurement")
        checkAudioPermission()
    }

    private fun initializeViews() {
        largeDecibelTextView = findViewById(R.id.largeDecibelTextView)
            ?: throw NullPointerException("Could not find largeDecibelTextView")
            
        noiseDescriptionTextView = findViewById(R.id.noiseDescriptionTextView)
            ?: throw NullPointerException("Could not find noiseDescriptionTextView")
            
        decibelProgressBar = findViewById(R.id.decibelProgressBar)
            ?: throw NullPointerException("Could not find decibelProgressBar")

        captureButton = findViewById(R.id.captureButton)
            ?: throw NullPointerException("Could not find captureButton")

        backButton = findViewById(R.id.backButton)
            ?: throw NullPointerException("Could not find backButton")
    }

    private fun setupClickListeners() {
        captureButton.setOnClickListener {
            reportNoise()
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

    private fun startDecibelMeasurement() {
        try {
            if (mediaRecorder == null) {
                // Inicialización clara del MediaRecorder
                mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                
                audioFile = File(cacheDir, audioFileName)
                
                // Configuración correcta del MediaRecorder para capturar amplitud
                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // Cambiado a AMR_NB que es más compatible
                    setAudioChannels(1) // Mono
                    setAudioSamplingRate(16000) // Tasa de muestreo estándar
                    setAudioEncodingBitRate(16000) // Bitrate adecuado para AMR_NB
                    setOutputFile(audioFile?.absolutePath)
                    
                    try {
                        Log.d("AudioMeasurement", "Preparing MediaRecorder...")
                        prepare()
                        start()
                        isRecording = true
                        Log.d("AudioMeasurement", "MediaRecorder started successfully with AMR_NB encoding")
                    } catch (e: IOException) {
                        Log.e("AudioMeasurement", "Error preparing MediaRecorder", e)
                        e.printStackTrace()
                        release()
                        mediaRecorder = null
                        return
                    }
                }

                // Add initial delay to ensure MediaRecorder is ready
                handler.postDelayed({
                    val measurementRunnable = object : Runnable {
                        override fun run() {
                            try {
                                val amplitude = mediaRecorder?.maxAmplitude ?: 0
                                Log.d("AudioMeasurement", "Raw amplitude: $amplitude")
                                
                                // Ajustar la fórmula de cálculo de decibeles para evitar valores muy bajos
                                currentDecibels = if (amplitude > 1) {
                                    // Esta fórmula convierte la amplitud a decibeles con un valor base mínimo
                                    val db = 20 * Math.log10(amplitude.toDouble() / 32767.0) + 90.0
                                    // Asegurar un valor mínimo de 30 dB para evitar mostrar 0
                                    Math.max(db, 30.0)
                                } else {
                                    30.0  // Un valor base para cuando hay silencio
                                }

                                // Actualizar la UI con el nuevo valor
                                runOnUiThread {
                                    // Actualizar todos los elementos visuales
                                    updateDecibelDisplay(currentDecibels)
                                }
                                
                                // Programar la próxima lectura
                                handler.postDelayed(this, 100) // Update every 100ms
                            } catch (e: Exception) {
                                Log.e("AudioMeasurement", "Error reading amplitude", e)
                            }
                        }
                    }
                    handler.post(measurementRunnable)
                }, 1000) // Aumentado a 1 segundo para dar más tiempo al MediaRecorder para inicializarse correctamente

                startLocationUpdates()
            } else {
                Log.d("AudioMeasurement", "MediaRecorder is already running")
            }
        } catch (e: Exception) {
            Log.e("AudioMeasurement", "Error in startDecibelMeasurement", e)
            e.printStackTrace()
        }
    }
    
    private fun updateDecibelDisplay(decibels: Double) {
        // Actualizar los textos de decibeles
        largeDecibelTextView.text = String.format("%.0f", decibels)
        
        // Actualizar la barra de progreso (max=120 como configuramos en el XML)
        val progress = decibels.toInt().coerceIn(0, 120)
        decibelProgressBar.progress = progress
        
        // Actualizar la descripción del nivel de ruido
        val description = when {
            decibels < 40 -> "Silencioso"
            decibels < 50 -> "Bajo"
            decibels < 65 -> "Conversación normal"
            decibels < 80 -> "Ruidoso"
            decibels < 90 -> "Muy ruidoso"
            decibels < 100 -> "Peligroso"
            else -> "Extremadamente ruidoso"
        }
        noiseDescriptionTextView.text = description
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

    private fun reportNoise() {
        if (currentDecibels <= 0) {
            Toast.makeText(
                this,
                "No hay medición de audio activa",
                Toast.LENGTH_SHORT
            ).show()
            startDecibelMeasurement()
            return
        }

        if (currentLocation == null) {
            Toast.makeText(
                this,
                "Esperando ubicación...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Deshabilitar el botón para evitar múltiples envíos
        captureButton.isEnabled = false
        captureButton.text = "Grabando..."

        // Grabar 10 segundos de audio
        // Nota: Ya estamos grabando, solo necesitamos detener después de 10s y enviar el reporte
        handler.postDelayed({
            stopRecordingAndSendReport()
        }, recordingDurationMs.toLong())
    }

    private fun stopRecordingAndSendReport() {
        try {
            // Detener la grabación y liberar recursos
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            
            // Enviar el reporte al API
            sendNoiseReport()
            
        } catch (e: Exception) {
            Log.e("AudioMeasurement", "Error stopping recording", e)
            Toast.makeText(
                this,
                "Error al detener la grabación: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            
            // Reactivar el botón
            captureButton.isEnabled = true
            captureButton.text = "Reportar Ruido"
        }
    }

    private fun sendNoiseReport() {
        val location = currentLocation
        if (location == null || audioFile == null) {
            Log.e("API", "Location or audio file is null")
            resetRecordingState()
            return
        }

        // Crear formato de ubicación
        val locationString = "${location.latitude},${location.longitude}"
        
        // Crear lista de datos para la acusación
        val accusationDataList = listOf(
            AccusationDataItem("ubication", locationString),
            AccusationDataItem("decibels_measurement", currentDecibels.toString())
        )
        
        // Crear objeto para la petición
        val noiseAccusation = NoiseAccusation(
            accusationTypeId = 2,
            accusationData = accusationDataList
        )

        // Primera petición POST para reportar el ruido
        ApiClient.noiseApiService.reportNoise(noiseAccusation).enqueue(object : Callback<NoiseAccusationResponse> {
            override fun onResponse(call: Call<NoiseAccusationResponse>, response: Response<NoiseAccusationResponse>) {
                if (response.isSuccessful) {
                    val accusationResponse = response.body()
                    if (accusationResponse != null) {
                        // Obtener el ID de la acusación y enviar el archivo de audio
                        uploadAudioFile(accusationResponse.id)
                    } else {
                        handleApiError("La respuesta no contenía datos")
                    }
                } else {
                    handleApiError("Error en la respuesta: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<NoiseAccusationResponse>, t: Throwable) {
                handleApiError("Error de red: ${t.message}")
            }
        })
    }

    private fun uploadAudioFile(accusationId: String) {
        val file = audioFile ?: return
        
        // Preparar el archivo para el envío
        val requestFile = file.asRequestBody("audio/3gpp".toMediaTypeOrNull())
        val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)

        // Segunda petición PATCH para subir el archivo de audio
        ApiClient.noiseApiService.uploadAudioFile(accusationId, audioPart).enqueue(object : Callback<NoiseAccusationResponse> {
            override fun onResponse(call: Call<NoiseAccusationResponse>, response: Response<NoiseAccusationResponse>) {
                if (response.isSuccessful) {
                    handleApiSuccess("Reporte de ruido enviado correctamente")
                } else {
                    handleApiError("Error al subir el archivo de audio: ${response.code()}")
                }
                resetRecordingState()
            }

            override fun onFailure(call: Call<NoiseAccusationResponse>, t: Throwable) {
                handleApiError("Error de red al subir el audio: ${t.message}")
                resetRecordingState()
            }
        })
    }

    private fun handleApiSuccess(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleApiError(errorMessage: String) {
        Log.e("API", errorMessage)
        runOnUiThread {
            Toast.makeText(
                this,
                "Error: $errorMessage",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun resetRecordingState() {
        // Reactivar el botón y reiniciar la grabación
        runOnUiThread {
            captureButton.isEnabled = true
            captureButton.text = "Reportar Ruido"
            
            // Volver a iniciar la medición de decibeles
            startDecibelMeasurement()
        }
    }
    
    override fun onDestroy() {
        Log.d("AudioMeasurement", "Stopping audio measurement")
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}