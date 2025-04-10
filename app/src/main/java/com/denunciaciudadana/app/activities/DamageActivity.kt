package com.denunciaciudadana.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.denunciaciudadana.app.R
import com.denunciaciudadana.app.api.ApiClient
import com.denunciaciudadana.app.database.DBHelper
import com.denunciaciudadana.app.models.Accusation
import com.denunciaciudadana.app.models.AccusationDataItem
import com.denunciaciudadana.app.models.AccusationResponse
import com.denunciaciudadana.app.models.Report
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DamageActivity : AppCompatActivity() {

    private lateinit var photoImageView: ImageView
    private lateinit var capturePhotoButton: Button
    private lateinit var submitReportButton: Button
    private lateinit var descriptionEditText: EditText
    private lateinit var backButton: ImageButton
    private lateinit var locationField: EditText

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var photoUri: Uri? = null
    private var photoFile: File? = null
    // Variable para la ubicación seleccionada en el mapa
    private var selectedLocation: LatLng? = null

    private val CAMERA_REQUEST_CODE = 1001
    private val PERMISSION_REQUEST_CODE = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_damage)

        // Inicializar vistas
        photoImageView = findViewById(R.id.photoImageView)
        capturePhotoButton = findViewById(R.id.capturePhotoButton)
        submitReportButton = findViewById(R.id.submitReportButton)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        backButton = findViewById(R.id.backButton)
        locationField = findViewById(R.id.locationField)

        // Cliente para obtener ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar el botón de volver
        backButton.setOnClickListener {
            finish() // Cierra esta actividad y vuelve a la anterior
        }

        // Botón de captura de foto
        capturePhotoButton.setOnClickListener {
            openCamera()
        }

        // Campo de ubicación
        locationField.setOnClickListener {
            showLocationMapModal()
        }

        // Botón de envío de reporte
        submitReportButton.setOnClickListener {
            submitDamageReport()
        }

        // Verificar permisos
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                currentLocation = it
            }
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de cámara no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Crear archivo temporal para la foto
        photoFile = File.createTempFile("damage_", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            photoFile!!
        )

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && photoUri != null) {
            photoImageView.setImageURI(photoUri)
        }
    }

    private fun submitDamageReport() {
        val description = descriptionEditText.text.toString()

        if (photoFile == null || photoUri == null) {
            Toast.makeText(this, "Falta capturar la foto", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar que tengamos una ubicación
        if (selectedLocation == null && currentLocation == null) {
            Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isBlank()) {
            Toast.makeText(this, "Escribe una descripción del daño", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar diálogo de progreso mientras se envían los datos
        val progressDialog = android.app.ProgressDialog(this).apply {
            setTitle("Enviando")
            setMessage("Enviando reporte, por favor espere...")
            setCancelable(false)
            show()
        }

        try {
            // Obtener datos de ubicación
            val locationString = if (selectedLocation != null) {
                "${selectedLocation!!.latitude},${selectedLocation!!.longitude}"
            } else {
                "${currentLocation!!.latitude},${currentLocation!!.longitude}"
            }

            // Crear lista de datos para la acusación
            val accusationDataList = mutableListOf(
                AccusationDataItem("location", locationString),
                AccusationDataItem("description", description)
            )
            
            // Crear objeto para la petición con accusationTypeId = 3 para daños
            val damageAccusation = Accusation(
                accusationTypeId = 3,
                accusationData = accusationDataList
            )

            // Enviar datos al servidor usando lifecycleScope para manejar la coroutine
            lifecycleScope.launch {
                try {
                    // Primero enviamos la acusación principal
                    val response = ApiClient.apiService.sendAccusation(damageAccusation)
                    
                    if (response.isSuccessful) {
                        val accusationId = response.body()?.data?.id
                        if (accusationId != null && photoFile != null) {
                            // Actualizar mensaje de progreso
                            progressDialog.setMessage("Subiendo foto...")
                            
                            // Subir la foto
                            val uploadResult = uploadPhotoFile(accusationId, photoFile!!)
                            
                            runOnUiThread {
                                progressDialog.dismiss()
                                if (uploadResult) {
                                    // Guardar el reporte en la base de datos local
                                    saveReportToLocalDatabase(accusationId.toString(), accusationDataList, photoUri.toString())
                                    showSuccessAndFinish("¡Reporte enviado con éxito!")
                                } else {
                                    // Guardar el reporte en la base de datos local sin la foto
                                    saveReportToLocalDatabase(accusationId.toString(), accusationDataList, null)
                                    showSuccessAndFinish("¡Reporte enviado, pero hubo un problema al subir la foto!")
                                }
                            }
                        } else {
                            // No hay archivo de foto o no se recibió ID
                            runOnUiThread {
                                progressDialog.dismiss()
                                if (accusationId != null) {
                                    // Guardar el reporte en la base de datos local
                                    saveReportToLocalDatabase(accusationId.toString(), accusationDataList, null)
                                }
                                showSuccessAndFinish("¡Reporte enviado correctamente!")
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                        Log.e("DamageActivity", "Error: ${response.code()} - $errorBody")
                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(this@DamageActivity, 
                                "Error: ${response.code()}", 
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DamageActivity", "Excepción al enviar reporte: ${e.message}", e)
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this@DamageActivity, 
                            "Error al enviar reporte: ${e.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DamageActivity", "Error al preparar datos de reporte", e)
            progressDialog.dismiss()
            Toast.makeText(this, "Error al preparar los datos del reporte", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Sube un archivo de foto al servidor para una acusación específica.
     * 
     * @param accusationId El ID de la acusación a la que se adjuntará la foto
     * @param photoFile El archivo de foto a subir
     * @return true si la carga fue exitosa, false en caso contrario
     */
    private suspend fun uploadPhotoFile(accusationId: Int, photoFile: File): Boolean {
        return try {
            // Obtener el nombre del archivo
            val fileName = photoFile.name
            Log.d("DamageActivity", "Subiendo foto: $fileName para acusación: $accusationId")
            
            // Determinar el tipo MIME para la imagen
            val mimeType = "image/jpeg"
            
            // Crear MultipartBody.Part para el archivo
            val requestFile = photoFile.asRequestBody(mimeType.toMediaTypeOrNull())
            
            val filePart = MultipartBody.Part.createFormData(
                "file", // Nombre del parámetro esperado por el servidor
                fileName, 
                requestFile
            )
            
            // Enviar archivo a la API
            val response = ApiClient.apiService.uploadAudioFile(accusationId, filePart)
            
            if (response.isSuccessful) {
                Log.d("DamageActivity", "Foto subida con éxito: $fileName")
                true
            } else {
                Log.e("DamageActivity", "Error al subir foto: ${response.code()} - ${response.errorBody()?.string()}")
                false
            }
            
        } catch (e: Exception) {
            Log.e("DamageActivity", "Excepción al subir foto: ${e.message}", e)
            false
        }
    }
    
    /**
     * Muestra un mensaje de éxito y finaliza la actividad.
     */
    private fun showSuccessAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    /**
     * Guarda el reporte en la base de datos local.
     *
     * @param accusationId El ID del reporte.
     * @param accusationDataList La lista de datos del reporte.
     * @param photoUri La URI de la foto, si está disponible.
     */
    private fun saveReportToLocalDatabase(accusationId: String, accusationDataList: List<AccusationDataItem>, photoUri: String?) {
        try {
            // Crear la lista de adjuntos (puede estar vacía)
            val attachments = if (photoUri != null) listOf(photoUri) else emptyList()
            
            // Obtener la fecha y hora actual
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            
            // Crear el objeto Report
            val report = Report(
                id = accusationId,
                timestamp = timestamp,
                status = "Enviado",  // Estado inicial del reporte
                accusationData = accusationDataList,
                attachments = attachments
            )
            
            // Crear instancia del DBHelper y guardar el reporte
            val dbHelper = DBHelper(this)
            dbHelper.saveReport(report)
            
            Log.d("DamageActivity", "Reporte guardado localmente con ID: $accusationId")
        } catch (e: Exception) {
            Log.e("DamageActivity", "Error guardando reporte localmente: ${e.message}", e)
        }
    }

    // Permisos concedidos o denegados
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                getLocation()
            } else {
                Toast.makeText(this, "Se requieren permisos para continuar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Mostrar el modal con el mapa para seleccionar ubicación
    private fun showLocationMapModal() {
        try {
            // En lugar de usar AlertDialog, crearemos un FragmentDialog personalizado
            val dialog = MapDialogFragment()
            dialog.setLocationSelectedListener { location ->
                // Cuando se selecciona una ubicación, actualizar el campo
                selectedLocation = location
                locationField.setText("Lat: ${location.latitude}, Lng: ${location.longitude}")
            }
            dialog.show(supportFragmentManager, "map_dialog")
        } catch (e: Exception) {
            Log.e("DamageActivity", "Error al inicializar el mapa: ${e.message}")
            Toast.makeText(this, "Error al cargar el mapa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Clase interna que maneja el diálogo del mapa como un FragmentDialog
    class MapDialogFragment : androidx.fragment.app.DialogFragment(), OnMapReadyCallback {
        private var googleMap: GoogleMap? = null
        private var selectedLocation: LatLng? = null
        private var locationSelectedListener: ((LatLng) -> Unit)? = null
        
        fun setLocationSelectedListener(listener: (LatLng) -> Unit) {
            locationSelectedListener = listener
        }
        
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.modal_location_map, container, false)
        }
        
        override fun onStart() {
            super.onStart()
            // Establecer tamaño del diálogo (casi pantalla completa)
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            
            // Configurar botones
            view.findViewById<Button>(R.id.btnCancelLocation).setOnClickListener {
                dismiss()
            }
            
            view.findViewById<Button>(R.id.btnConfirmLocation).setOnClickListener {
                selectedLocation?.let { location ->
                    locationSelectedListener?.invoke(location)
                }
                dismiss()
            }
            
            // Inicializar el mapa
            val mapFragment = childFragmentManager.findFragmentById(R.id.mapLocation) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
        
        override fun onMapReady(map: GoogleMap) {
            googleMap = map
            
            // Configurar el mapa
            context?.let { ctx ->
                if (ActivityCompat.checkSelfPermission(
                        ctx,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap?.isMyLocationEnabled = true
                }
            }
            
            // Permitir seleccionar una ubicación en el mapa
            googleMap?.setOnMapClickListener { latLng ->
                // Limpiar marcadores anteriores
                googleMap?.clear()
                // Añadir un nuevo marcador
                googleMap?.addMarker(MarkerOptions().position(latLng))
                // Guardar la ubicación seleccionada
                selectedLocation = latLng
            }
        }
    }
}
