package com.denunciaciudadana.app.activities

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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.denunciaciudadana.app.activities.CameraCaptureActivity
import com.denunciaciudadana.app.activities.RetratoHabladoActivity
import com.denunciaciudadana.app.database.DBHelper
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import com.denunciaciudadana.app.adapters.AttachedFilesAdapter
import com.denunciaciudadana.app.api.ApiClient
import com.denunciaciudadana.app.R
import com.denunciaciudadana.app.models.Accusation
import com.denunciaciudadana.app.models.AccusationDataItem
import com.denunciaciudadana.app.models.AccusationResponse
import com.denunciaciudadana.app.models.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Activity for reporting crime incidents.
 * Allows users to submit crime reports with personal information,
 * location data, and supporting evidence files.
 */
class CrimeReportActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CrimeReportActivity"
        private const val FILE_REQUEST_CODE = 123
        private const val PERMISSION_REQUEST_CODE = 456
        private const val CAMERA_CAPTURE_REQUEST_CODE = 789
        private const val RETRATO_HABLADO_REQUEST_CODE = 101
    }
    
    // UI components
    private lateinit var backButton: ImageButton
    private lateinit var sentCrimeReportButton: Button
    private lateinit var attachEvidenceButton: MaterialButton
    private lateinit var noFilesTextView: TextView
    private lateinit var attachedFilesRecyclerView: RecyclerView
    
    // Retrato hablado components
    private lateinit var portraitCardView: CardView
    private lateinit var portraitImageView: ImageView
    private lateinit var removePortraitButton: ImageButton
    
    // Form fields
    private lateinit var fullNameField: EditText
    private lateinit var identificationField: EditText
    private lateinit var phoneNumberField: EditText
    private lateinit var locationField: EditText
    private lateinit var eventDescriptionField: EditText
    
    // Data
    private lateinit var filesAdapter: AttachedFilesAdapter
    private var attachedFileUris: List<Uri> = emptyList()
    private var selectedLocation: LatLng? = null
    private var retratoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crime_report)
        
        setupWindowInsets()
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
    }
    
    /**
     * Set up window insets to properly handle system UI.
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Initialize all UI component references.
     */
    private fun initializeViews() {
        try {
            // Buttons
            backButton = findViewById(R.id.backButton)
            sentCrimeReportButton = findViewById(R.id.sentCrimeReportButton)
            attachEvidenceButton = findViewById(R.id.attachEvidenceButton)
            
            // Text views
            noFilesTextView = findViewById(R.id.noFilesTextView)
            
            // Form fields
            fullNameField = findViewById(R.id.fullName)
            identificationField = findViewById(R.id.identification)
            phoneNumberField = findViewById(R.id.phoneNumber)
            locationField = findViewById(R.id.locationField)
            eventDescriptionField = findViewById(R.id.eventsDescription)
            
            // RecyclerView
            attachedFilesRecyclerView = findViewById(R.id.attachedFilesRecyclerView)
            
            // Retrato hablado views
            portraitCardView = findViewById(R.id.portraitCardView)
            portraitImageView = findViewById(R.id.portraitImageView)
            removePortraitButton = findViewById(R.id.removePortraitButton)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            showErrorAndFinish("Error initializing UI components")
        }
    }
    
    /**
     * Set up click listeners for buttons and interactive elements.
     */
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            Log.d(TAG, "Back button pressed")
            finish()
        }
        
        sentCrimeReportButton.setOnClickListener {
            sendCrimeReport()
        }
        
        attachEvidenceButton.setOnClickListener {
            showEvidenceOptionsDialog()
        }
        
        locationField.setOnClickListener {
            showLocationMapDialog()
        }
        
        removePortraitButton.setOnClickListener {
            removePortrait()
        }
    }
    
    /**
     * Set up RecyclerView for attached files.
     */
    private fun setupRecyclerView() {
        filesAdapter = AttachedFilesAdapter(this) { position, uri ->
            removeAttachedFile(position)
        }
        
        attachedFilesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CrimeReportActivity)
            adapter = filesAdapter
        }
        
        updateFilesVisibility()
    }
    
    /**
     * Handles removing a file from the attachment list.
     */
    private fun removeAttachedFile(position: Int) {
        filesAdapter.removeFile(position)
        attachedFileUris = filesAdapter.getFiles()
        updateFilesVisibility()
    }
    
    /**
     * Update visibility of files-related UI components.
     */
    private fun updateFilesVisibility() {
        if (filesAdapter.getFiles().isEmpty() && portraitCardView.visibility != View.VISIBLE) {
            noFilesTextView.visibility = View.VISIBLE
            attachedFilesRecyclerView.visibility = View.GONE
        } else {
            noFilesTextView.visibility = View.GONE
            attachedFilesRecyclerView.visibility = View.VISIBLE
        }
    }

    /**
     * Send the crime report to the server.
     */
    private fun sendCrimeReport() {
        if (!validateFormFields()) {
            return
        }
        
        // Mostrar diálogo de progreso mientras se envían los datos
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Enviando")
            .setMessage("Enviando reporte, por favor espere...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        try {
            val accusationDataList = mutableListOf(
                AccusationDataItem("fullName", fullNameField.text.toString()),
                AccusationDataItem("identification", identificationField.text.toString()),
                AccusationDataItem("phoneNumber", phoneNumberField.text.toString()),
                AccusationDataItem("location", locationField.text.toString()),
                AccusationDataItem("eventDescription", eventDescriptionField.text.toString())
            )
            
            // Añadir el retrato si existe
            retratoUrl?.let {
                accusationDataList.add(AccusationDataItem("portrait", it))
            }
            
            val crimeAccusation = Accusation(
                accusationTypeId = 1,
                accusationData = accusationDataList
            )
            // Send API request
            lifecycleScope.launch {
                try {
                    // Primero enviamos la acusación principal
                    val response: Response<AccusationResponse> = ApiClient.apiService.sendAccusation(crimeAccusation)
                    
                    if (response.isSuccessful) {
                        val accusationId = response.body()?.data?.id
                        if (accusationId != null && attachedFileUris.isNotEmpty()) {
                            // Actualizar mensaje de progreso
                            runOnUiThread {
                                progressDialog.setMessage("Subiendo archivos adjuntos (0/${attachedFileUris.size})...")
                            }
                            
                            // Subir cada archivo adjunto
                            var allFilesUploaded = true
                            var filesUploaded = 0
                            
                            // Lista para guardar las rutas de los archivos adjuntos
                            val attachmentPaths = mutableListOf<String>()
                            
                            for ((index, fileUri) in attachedFileUris.withIndex()) {
                                try {
                                    // Actualizar mensaje con el progreso
                                    runOnUiThread {
                                        progressDialog.setMessage("Subiendo archivos adjuntos (${index+1}/${attachedFileUris.size})...")
                                    }
                                    
                                    val uploadResult = uploadFile(accusationId, fileUri)
                                    if (uploadResult) {
                                        filesUploaded++
                                        
                                        // Añadir la ruta del archivo a la lista de adjuntos para guardar en la base de datos local
                                        fileUri.toString().let { path ->
                                            attachmentPaths.add(path)
                                        }
                                    } else {
                                        allFilesUploaded = false
                                        Log.e(TAG, "Error al subir archivo: $fileUri")
                                    }
                                } catch (e: Exception) {
                                    allFilesUploaded = false
                                    Log.e(TAG, "Excepción al subir archivo: ${e.message}", e)
                                }
                            }
                            
                            // Guardar el reporte en la base de datos local
                            saveReportToLocalDatabase(accusationDataList, attachmentPaths, accusationId)
                            
                            // Mostrar mensaje según si todos los archivos se subieron correctamente
                            val finalMessage = if (allFilesUploaded) {
                                "Reporte enviado con éxito"
                            } else {
                                "Reporte enviado, pero solo se pudieron subir $filesUploaded de ${attachedFileUris.size} archivos"
                            }
                            
                            // Ocultar diálogo de progreso y mostrar mensaje de éxito antes de terminar
                            runOnUiThread {
                                progressDialog.dismiss()
                                showSuccessAndFinish(finalMessage)
                            }
                        } else {
                            // No hay archivos adjuntos, guardamos el reporte sin adjuntos
                            saveReportToLocalDatabase(accusationDataList, emptyList(), accusationId)
                            
                            // No hay archivos adjuntos, mostrar éxito directamente
                            runOnUiThread {
                                progressDialog.dismiss()
                                showSuccessAndFinish("Reporte enviado con éxito")
                            }
                        }
                    } else {
                        Log.e(TAG, "Error: ${response.code()} - ${response.errorBody()?.string()}")
                        runOnUiThread {
                            progressDialog.dismiss()
                            showError("Error: ${response.code()}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception sending report: ${e.message}", e)
                    runOnUiThread {
                        progressDialog.dismiss()
                        showError("Error al enviar reporte: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing report data", e)
            progressDialog.dismiss()
            showError("Error al preparar los datos del reporte")
        }
    }
    
    /**
     * Save the report to the local database for offline access.
     * 
     * @param accusationData The list of data items in the report
     * @param attachments The list of attachment file paths
     * @param serverId The server ID of the report (if available)
     */
    private fun saveReportToLocalDatabase(
        accusationData: List<AccusationDataItem>, 
        attachments: List<String>, 
        serverId: Int?
    ) {
        try {
            // Generate a unique ID for the report
            val reportId = serverId?.toString() ?: UUID.randomUUID().toString()
            
            // Get current timestamp in a standardized format
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(Date())
            
            // Create a Report object
            val report = Report(
                id = reportId,
                timestamp = timestamp,
                status = "sent",  // Initial status is "sent" since we've sent it to the server
                accusationData = accusationData,
                attachments = attachments
            )
            
            // Save to local database
            val dbHelper = DBHelper(this)
            dbHelper.saveReport(report)
            Log.d(TAG, "Report saved to local database with ID: $reportId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving report to local database: ${e.message}", e)
        }
    }
    
    /**
     * Validate all form fields before submission.
     * 
     * @return True if all fields are valid, false otherwise
     */
    private fun validateFormFields(): Boolean {
        // Validate required fields
        if (fullNameField.text.isNullOrBlank()) {
            showError("Por favor ingrese su nombre completo")
            return false
        }
        
        if (eventDescriptionField.text.isNullOrBlank()) {
            showError("Por favor ingrese la descripción del evento")
            return false
        }
        
        return true
    }
    
    /**
     * Handle API response after submitting a report.
     */
    private fun handleApiResponse(response: Response<AccusationResponse>) {
        if (response.isSuccessful) {
            Log.d(TAG, "Report sent successfully")
            runOnUiThread {
                showSuccessAndFinish("Reporte enviado con éxito")
            }
        } else {
            Log.e(TAG, "Error: ${response.code()} - ${response.errorBody()?.string()}")
            runOnUiThread {
                showError("Error: ${response.code()}")
            }
        }
    }
    
    /**
     * Check and request required permissions for file access.
     */
    private fun checkFilePermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        Log.d(TAG, "Permissions to request: ${permissionsToRequest.joinToString()}")
        
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
    
    /**
     * Open the system file picker for selecting evidence files.
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(
            Intent.createChooser(intent, "Seleccionar archivo"), 
            FILE_REQUEST_CODE
        )
    }

    /**
     * Handle result from file picker and camera activities.
     */
    @SuppressLint("SetTextI18n")
    @Deprecated("This method is deprecated. Consider using the Activity Result API instead.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                FILE_REQUEST_CODE -> {
                    handleFilePickerResult(data)
                }
                
                CAMERA_CAPTURE_REQUEST_CODE -> {
                    handleCameraCaptureResult(data)
                }
                
                RETRATO_HABLADO_REQUEST_CODE -> {
                    handleRetratoHabladoResult(data)
                }
            }
        }
    }
    
    /**
     * Process results from file picker.
     */
    private fun handleFilePickerResult(data: Intent?) {
        val uris = mutableListOf<Uri>()

        data?.let {
            if (it.clipData != null) {
                // Multiple files selected
                val count = it.clipData!!.itemCount
                for (i in 0 until count) {
                    val fileUri = it.clipData!!.getItemAt(i).uri
                    uris.add(fileUri)
                }
            } else if (it.data != null) {
                // Single file selected
                val uri = it.data!!
                uris.add(uri)
            }
        }

        if (uris.isNotEmpty()) {
            addFilesToAdapter(uris)
            showToast("Archivo(s) añadido(s)")
        }
    }
    
    /**
     * Process results from camera capture.
     */
    private fun handleCameraCaptureResult(data: Intent?) {
        data?.getStringExtra("captured_media_uri")?.let { uriString ->
            val mediaUri = Uri.parse(uriString)
            addFilesToAdapter(listOf(mediaUri))
            showToast("Archivo añadido")
        }
    }
    
    /**
     * Process results from retrato hablado activity.
     */
    private fun handleRetratoHabladoResult(data: Intent?) {
        val shouldAddToReport = data?.getBooleanExtra("add_to_report", false) ?: false
        if (shouldAddToReport) {
            data?.getStringExtra("retrato_uri")?.let { retratoUrlString ->
                retratoUrl = retratoUrlString
                displayPortrait(retratoUrlString)
                showToast("Retrato añadido al reporte")
            }
        }
    }
    
    /**
     * Display the portrait in the card view.
     */
    private fun displayPortrait(portraitUrl: String) {
        portraitCardView.visibility = View.VISIBLE
        
        // Cargar la imagen con Glide
        Glide.with(this)
            .load(portraitUrl)
            .placeholder(R.drawable.ic_crime)
            .error(R.drawable.ic_crime)
            .into(portraitImageView)
            
        // Actualizar la visibilidad de la sección de archivos
        updateFilesVisibility()
    }
    
    /**
     * Remove portrait from the report.
     */
    private fun removePortrait() {
        retratoUrl = null
        portraitCardView.visibility = View.GONE
        updateFilesVisibility()
        showToast("Retrato eliminado")
    }
    
    /**
     * Add files to the adapter and update UI.
     */
    private fun addFilesToAdapter(uris: List<Uri>) {
        filesAdapter.addFiles(uris)
        attachedFileUris = filesAdapter.getFiles()
        updateFilesVisibility()
    }

    /**
     * Handle permission request results.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && 
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openFilePicker()
        } else {
            showToast("Permiso denegado para acceder a archivos")
        }
    }

    /**
     * Get file name from URI.
     */
    fun getFileName(uri: Uri): String {
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
    
    /**
     * Show dialog for selecting evidence attachment options.
     */
    private fun showEvidenceOptionsDialog() {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.modal_evidence_options)
            .create()
            
        dialog.show()
        
        // Set up listeners for each option button
        dialog.findViewById<Button>(R.id.btnCapturarCamara)?.setOnClickListener {
            openCameraCapture()
            dialog.dismiss()
        }
        
        dialog.findViewById<Button>(R.id.btnSeleccionarArchivo)?.setOnClickListener {
            dialog.dismiss()
            checkFilePermissions()
        }
        
        dialog.findViewById<Button>(R.id.btnRetratoHablado)?.setOnClickListener {
            openRetratoHablado()
            dialog.dismiss()
        }
    }
    
    /**
     * Open camera capture activity.
     */
    private fun openCameraCapture() {
        val intent = Intent(this, CameraCaptureActivity::class.java)
        startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE)
    }
    
    /**
     * Open sketch portrait activity.
     */
    private fun openRetratoHablado() {
        val intent = Intent(this, RetratoHabladoActivity::class.java)
        startActivityForResult(intent, RETRATO_HABLADO_REQUEST_CODE)
    }
    
    /**
     * Show location map dialog for selecting incident location.
     */
    private fun showLocationMapDialog() {
        try {
            val dialog = MapDialogFragment()
            dialog.setLocationSelectedListener { location ->
                selectedLocation = location
                locationField.setText("Lat: ${location.latitude}, Lng: ${location.longitude}")
            }
            dialog.show(supportFragmentManager, "map_dialog")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing map: ${e.message}", e)
            showError("Error al cargar el mapa: ${e.message}")
        }
    }
    
    /**
     * Show a toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show error message.
     */
    private fun showError(message: String) {
        Log.e(TAG, message)
        showToast(message)
    }
    
    /**
     * Show error message and finish activity.
     */
    private fun showErrorAndFinish(message: String) {
        showToast(message)
        finish()
    }
    
    /**
     * Show success message and finish activity.
     */
    private fun showSuccessAndFinish(message: String) {
        showToast(message)
        finish()
    }

    /**
     * Upload a file to the server for a given accusation ID.
     * 
     * @param accusationId The ID of the accusation to attach the file to
     * @param fileUri The URI of the file to upload
     * @return True if upload was successful, false otherwise
     */
    private suspend fun uploadFile(accusationId: Int, fileUri: Uri): Boolean {
        try {
            // Obtener el nombre del archivo
            val fileName = getFileName(fileUri)
            Log.d(TAG, "Subiendo archivo: $fileName para acusación: $accusationId")
            
            // Preparar el archivo para envío
            val inputStream = contentResolver.openInputStream(fileUri) ?: return false
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            // Determinar el tipo MIME
            val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
            
            // Crear MultipartBody.Part para el archivo
            val requestFile = okhttp3.RequestBody.create(
                mimeType.toMediaType(),
                bytes
            )
            
            val filePart = okhttp3.MultipartBody.Part.createFormData(
                "file", // Nombre del parámetro esperado por el servidor
                fileName, 
                requestFile
            )
            
            // Enviar archivo a la API
            val response = ApiClient.apiService.uploadAudioFile(accusationId, filePart)
            
            return if (response.isSuccessful) {
                Log.d(TAG, "Archivo subido con éxito: $fileName")
                true
            } else {
                Log.e(TAG, "Error al subir archivo: ${response.code()} - ${response.errorBody()?.string()}")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al subir archivo: ${e.message}", e)
            return false
        }
    }

    /**
     * Fragment dialog that shows a map for location selection.
     */
    class MapDialogFragment : androidx.fragment.app.DialogFragment(), OnMapReadyCallback {
        private var googleMap: GoogleMap? = null
        private var selectedLocation: LatLng? = null
        private var locationSelectedListener: ((LatLng) -> Unit)? = null
        
        /**
         * Set the listener for location selection events.
         */
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
            // Set dialog size (almost full screen)
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            
            // Configure buttons
            view.findViewById<Button>(R.id.btnCancelLocation).setOnClickListener {
                dismiss()
            }
            
            view.findViewById<Button>(R.id.btnConfirmLocation).setOnClickListener {
                selectedLocation?.let { location ->
                    locationSelectedListener?.invoke(location)
                }
                dismiss()
            }
            
            // Initialize the map
            val mapFragment = childFragmentManager
                .findFragmentById(R.id.mapLocation) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
        
        override fun onMapReady(map: GoogleMap) {
            googleMap = map
            
            // Configure map
            context?.let { ctx ->
                if (ActivityCompat.checkSelfPermission(
                        ctx,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap?.isMyLocationEnabled = true
                }
            }
            
            // Allow selecting a location on the map
            googleMap?.setOnMapClickListener { latLng ->
                // Clear previous markers
                googleMap?.clear()
                // Add new marker
                googleMap?.addMarker(MarkerOptions().position(latLng))
                // Save selected location
                selectedLocation = latLng
            }
        }
    }
}

