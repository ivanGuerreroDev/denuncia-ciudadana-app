package com.example.medidordecibelesapp.activities

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
import com.example.medidordecibelesapp.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.File

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

        if (photoUri == null) {
            Toast.makeText(this, "Falta capturar la foto", Toast.LENGTH_SHORT).show()
            return
        }

        // Usar la ubicación seleccionada si está disponible, sino usar la ubicación actual
        val locationToUse = if (selectedLocation != null) {
            // Ya tenemos una ubicación seleccionada del mapa
            "Lat: ${selectedLocation?.latitude}, Lng: ${selectedLocation?.longitude}"
        } else if (currentLocation != null) {
            // O usando la ubicación actual
            "Lat: ${currentLocation!!.latitude}, Lng: ${currentLocation!!.longitude}"
        } else {
            Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isBlank()) {
            Toast.makeText(this, "Escribe una descripción del daño", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "¡Alerta enviada!\nUbicación: $locationToUse", Toast.LENGTH_LONG).show()

        // Aquí podrías guardar en Firebase, SQLite, o enviar al servidor
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
