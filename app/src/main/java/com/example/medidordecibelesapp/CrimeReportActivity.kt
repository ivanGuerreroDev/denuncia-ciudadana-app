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
import android.view.ViewGroup
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import android.widget.LinearLayout

class CrimeReportActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var sentCrimeReportButton: Button
    private lateinit var attachEvidenceButton: MaterialButton
    private lateinit var noFilesTextView: TextView
    private lateinit var attachedFilesRecyclerView: RecyclerView
    private lateinit var filesAdapter: AttachedFilesAdapter
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
        setupRecyclerView()
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
        noFilesTextView = findViewById(R.id.noFilesTextView)
        attachedFilesRecyclerView = findViewById(R.id.attachedFilesRecyclerView)
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
    
    private fun setupRecyclerView() {
        filesAdapter = AttachedFilesAdapter(this) { position, uri ->
            // Manejar eliminación de archivos
            filesAdapter.removeFile(position)
            
            // Actualizar la lista principal de archivos
            attachFileUris = filesAdapter.getFiles()
            
            // Mostrar u ocultar vistas según si hay archivos
            updateFilesVisibility()
        }
        
        attachedFilesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CrimeReportActivity)
            adapter = filesAdapter
        }
    }
    
    private fun updateFilesVisibility() {
        if (filesAdapter.getFiles().isEmpty()) {
            noFilesTextView.visibility = View.VISIBLE
            attachedFilesRecyclerView.visibility = View.GONE
        } else {
            noFilesTextView.visibility = View.GONE
            attachedFilesRecyclerView.visibility = View.VISIBLE
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

                    data?.let {
                        if (it.clipData != null) {
                            val count = it.clipData!!.itemCount
                            for (i in 0 until count) {
                                val fileUri = it.clipData!!.getItemAt(i).uri
                                uris.add(fileUri)
                            }
                        } else if (it.data != null) {
                            val uri = it.data!!
                            uris.add(uri)
                        }
                    }

                    if (uris.isNotEmpty()) {
                        // Añadir los archivos al adaptador
                        filesAdapter.addFiles(uris)
                        
                        // Actualizar la lista principal de URIs
                        attachFileUris = filesAdapter.getFiles()
                        
                        // Actualizar visibilidad de las vistas
                        updateFilesVisibility()
                        
                        Toast.makeText(this, "Archivo(s) añadido(s)", Toast.LENGTH_SHORT).show()
                    }
                }
                
                CAMERA_CAPTURE_REQUEST_CODE -> {
                    data?.getStringExtra("captured_media_uri")?.let { uriString ->
                        val mediaUri = Uri.parse(uriString)
                        
                        // Añadir el archivo capturado al adaptador
                        filesAdapter.addFiles(listOf(mediaUri))
                        
                        // Actualizar la lista principal de URIs
                        attachFileUris = filesAdapter.getFiles()
                        
                        // Actualizar visibilidad de las vistas
                        updateFilesVisibility()
                        
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
            // En lugar de usar AlertDialog, crearemos un FragmentDialog personalizado
            val dialog = MapDialogFragment()
            dialog.setLocationSelectedListener { location ->
                // Cuando se selecciona una ubicación, actualizar el campo
                selectedLocation = location
                locationField.setText("Lat: ${location.latitude}, Lng: ${location.longitude}")
            }
            dialog.show(supportFragmentManager, "map_dialog")
        } catch (e: Exception) {
            Log.e("CrimeReportActivity", "Error al inicializar el mapa: ${e.message}")
            Toast.makeText(this@CrimeReportActivity, "Error al cargar el mapa: ${e.message}", Toast.LENGTH_SHORT).show()
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

