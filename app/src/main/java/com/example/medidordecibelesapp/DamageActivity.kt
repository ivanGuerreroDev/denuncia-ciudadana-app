package com.example.medidordecibelesapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.*
import java.io.File

class DamageActivity : AppCompatActivity() {

    private lateinit var photoImageView: ImageView
    private lateinit var capturePhotoButton: Button
    private lateinit var submitReportButton: Button
    private lateinit var descriptionEditText: EditText

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var photoUri: Uri? = null
    private var photoFile: File? = null

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

        // Cliente para obtener ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Botón de captura de foto
        capturePhotoButton.setOnClickListener {
            openCamera()
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

        if (currentLocation == null) {
            Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isBlank()) {
            Toast.makeText(this, "Escribe una descripción del daño", Toast.LENGTH_SHORT).show()
            return
        }

        val lat = currentLocation!!.latitude
        val lon = currentLocation!!.longitude
        Toast.makeText(this, "¡Alerta enviada!\nUbicación: ($lat, $lon)", Toast.LENGTH_LONG).show()

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
}
