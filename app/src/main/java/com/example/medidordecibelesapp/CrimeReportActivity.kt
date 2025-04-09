package com.example.medidordecibelesapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class CrimeReportActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var sentCrimeReportButton: Button
    private lateinit var attachEvidenceButton: MaterialButton
    private lateinit var attachFileTextView: TextView
    private var attachFileUris: List<Uri> = emptyList()
    private lateinit var fullName: EditText
    private lateinit var identification: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var locationField: EditText
    private lateinit var eventDescription: EditText
    
    // Variables para la ubicación
    private var selectedLocation: LatLng? = null
    private lateinit var locationDialog: AlertDialog

    companion object {
        private const val FILE_REQUEST_CODE = 123
        private const val PERMISSION_REQUEST_CODE = 456
        private const val CAMERA_CAPTURE_REQUEST_CODE = 789
    }

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
        fullName = findViewById(R.id.fullName)
        identification = findViewById(R.id.identification)
        phoneNumber = findViewById(R.id.phoneNumber)
        locationField = findViewById(R.id.locationField)
        eventDescription = findViewById(R.id.eventsDescription)
        sentCrimeReportButton = findViewById(R.id.sentCrimeReportButton)
        attachEvidenceButton = findViewById(R.id.attachEvidenceButton)
        attachFileTextView = findViewById(R.id.attachFileTextView)

    }

    private fun setupClickListeners() {

        backButton.setOnClickListener {
            Log.d("CrimeReportActivity", "Botón de regreso presionado")
            finish()
        }
        sentCrimeReportButton.setOnClickListener {
            //sentData()
        }
        attachEvidenceButton.setOnClickListener {
            showEvidenceOptionsModal()
        }
        
        // Configurar el campo de ubicación para abrir el modal del mapa
        locationField.setOnClickListener {
            showLocationMapModal()
        }

    }

    private fun sentData() {
            val values = mapOf(
                "accusationTypeId" to 1,
                "accusationData" to listOf(
                    mapOf("key" to "fullName", "value" to fullName.text.toString()),
                    mapOf("key" to "identification", "value" to identification.text.toString()),
                    mapOf("key" to "phoneNumber", "value" to phoneNumber.text.toString()),
                    mapOf("key" to "location", "value" to locationField.text.toString()),
                    mapOf("key" to "eventDescription", "value" to eventDescription.text.toString())
                )
            )

            val objectMapper = ObjectMapper()
            val requestBodyJson: String = objectMapper.writeValueAsString(values)
            val mediaType = "application/json".toMediaType()
            val requestBody = requestBodyJson.toRequestBody(mediaType)

            lifecycleScope.launch {
                try {
                    val response: Response<Unit> = RetrofitClient.api.sendAccusation(requestBody)
                    if (response.isSuccessful) {
                        Log.d("API", "Enviado con éxito")
                    } else {
                        Log.e("API", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("API", "Excepción al enviar: ${e.message}")
                }
            }
    }
    private fun checkAudioPermission() {
        val permissions = arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO
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
            openFilePicker()
        }
    }
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Seleccionar archivo"), FILE_REQUEST_CODE)
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                FILE_REQUEST_CODE -> {
                    val uris = mutableListOf<Uri>()
                    val fileNames = mutableListOf<String>()

                    data?.let {
                        if (it.clipData != null) {
                            val count = it.clipData!!.itemCount
                            for (i in 0 until count) {
                                val fileUri = it.clipData!!.getItemAt(i).uri
                                uris.add(fileUri)
                                fileNames.add(getFileName(fileUri))
                            }
                        } else if (it.data != null) {
                            val uri = it.data!!
                            uris.add(uri)
                            fileNames.add(getFileName(uri))
                        }
                    }

                    if (uris.isNotEmpty()) {
                        // Añadir los nuevos archivos a la lista existente
                        val updatedUris = (attachFileUris + uris).distinct()
                        attachFileUris = updatedUris
                        
                        // Obtener los nombres de todos los archivos seleccionados
                        val allFileNames = updatedUris.map { getFileName(it) }
                        val namesText = allFileNames.joinToString(separator = "\n")
                        
                        attachFileTextView.text = "Archivos seleccionados:\n$namesText"
                        Log.d("FileSelection", "URIs seleccionadas: $updatedUris")
                        Toast.makeText(this, "Archivo(s) añadido(s)", Toast.LENGTH_SHORT).show()
                    }
                }
                
                CAMERA_CAPTURE_REQUEST_CODE -> {
                    data?.getStringExtra("captured_media_uri")?.let { uriString ->
                        val mediaUri = Uri.parse(uriString)
                        
                        // Añadir el nuevo archivo a la lista existente
                        val updatedUris = (attachFileUris + mediaUri).distinct()
                        attachFileUris = updatedUris
                        
                        // Obtener los nombres de todos los archivos seleccionados
                        val allFileNames = updatedUris.map { getFileName(it) }
                        val namesText = allFileNames.joinToString(separator = "\n")
                        
                        // Actualizar el texto mostrado con todos los archivos
                        attachFileTextView.text = "Archivos seleccionados:\n$namesText"
                        
                        Log.d("CameraCapture", "Media capturada: $mediaUri")
                        Toast.makeText(this, "Archivo añadido", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFilePicker()
        } else {
            Toast.makeText(this, "Permiso denegado para acceder a archivos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Archivo desconocido"
    }
    
    private fun showEvidenceOptionsModal() {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.modal_evidence_options)
            .create()
            
        dialog.show()
        
        // Configurar los listeners para cada botón del modal
        dialog.findViewById<Button>(R.id.btnCapturarCamara)?.setOnClickListener {
            // Abrir la actividad de captura de cámara
            val intent = Intent(this, CameraCaptureActivity::class.java)
            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE)
            dialog.dismiss()
        }
        
        dialog.findViewById<Button>(R.id.btnSeleccionarArchivo)?.setOnClickListener {
            dialog.dismiss()
            checkAudioPermission() // Reutilizamos la función existente para seleccionar archivos
        }
        
        dialog.findViewById<Button>(R.id.btnRetratoHablado)?.setOnClickListener {
            // Lanzar la actividad de Retrato Hablado
            val intent = Intent(this, RetratoHabladoActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }
    }
    
    private fun showLocationMapModal() {
        try {
            // Crear el diálogo con el layout del mapa
            val dialogView = LayoutInflater.from(this).inflate(R.layout.modal_location_map, null)
            
            // Crear el diálogo con la vista inflada
            locationDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            // Configurar los botones antes de mostrar el diálogo
            dialogView.findViewById<Button>(R.id.btnCancelLocation)?.setOnClickListener {
                locationDialog.dismiss()
            }
            
            dialogView.findViewById<Button>(R.id.btnConfirmLocation)?.setOnClickListener {
                // Guardar la ubicación seleccionada
                selectedLocation?.let {
                    locationField.setText("Lat: ${it.latitude}, Lng: ${it.longitude}")
                }
                locationDialog.dismiss()
            }
            
            // Mostrar el diálogo
            locationDialog.show()
            
            // Inicializar el mapa después de que el diálogo se muestre
            val mapFragment = SupportMapFragment()
            supportFragmentManager.beginTransaction()
                .add(mapFragment, "mapFragment")
                .commitNow() // Usar commitNow para asegurar que se complete inmediatamente
            
            // Reemplazar el contenedor del mapa con el fragmento
            val mapContainer = dialogView.findViewById<FrameLayout>(R.id.mapLocation)
            if (mapContainer != null) {
                // Usar el childFragmentManager del fragmento para manejar el mapa
                supportFragmentManager.beginTransaction()
                    .replace(R.id.mapLocation, mapFragment)
                    .commitNow() // Usar commitNow para asegurar que se complete inmediatamente
                
                mapFragment.getMapAsync(object : OnMapReadyCallback {
                    override fun onMapReady(googleMap: GoogleMap) {
                        // Configurar el mapa
                        if (ActivityCompat.checkSelfPermission(this@CrimeReportActivity, 
                                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            googleMap.isMyLocationEnabled = true
                        }
                        
                        // Si ya hay una ubicación seleccionada, mostrarla en el mapa
                        selectedLocation?.let {
                            googleMap.addMarker(MarkerOptions().position(it))
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                        }
                        
                        // Permitir seleccionar una ubicación en el mapa
                        googleMap.setOnMapClickListener { latLng ->
                            // Limpiar marcadores anteriores
                            googleMap.clear()
                            // Añadir un nuevo marcador
                            googleMap.addMarker(MarkerOptions().position(latLng))
                            // Guardar la ubicación seleccionada
                            selectedLocation = latLng
                        }
                    }
                })
            } else {
                Log.e("CrimeReportActivity", "No se encontró el contenedor del mapa")
                Toast.makeText(this@CrimeReportActivity, "Error al cargar el mapa", Toast.LENGTH_SHORT).show()
                locationDialog.dismiss()
            }
        } catch (e: Exception) {
            Log.e("CrimeReportActivity", "Error al inicializar el mapa: ${e.message}")
            Toast.makeText(this@CrimeReportActivity, "Error al cargar el mapa", Toast.LENGTH_SHORT).show()
            if (::locationDialog.isInitialized) {
                locationDialog.dismiss()
            }
        }
    }

}

