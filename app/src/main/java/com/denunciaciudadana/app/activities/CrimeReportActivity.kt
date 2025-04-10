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
import com.denunciaciudadana.app.activities.CameraCaptureActivity
import com.denunciaciudadana.app.activities.RetratoHabladoActivity
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
    }
    
    // UI components
    private lateinit var backButton: ImageButton
    private lateinit var sentCrimeReportButton: Button
    private lateinit var attachEvidenceButton: MaterialButton
    private lateinit var noFilesTextView: TextView
    private lateinit var attachedFilesRecyclerView: RecyclerView
    
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
        if (filesAdapter.getFiles().isEmpty()) {
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
        
        try {
            val accusationDataList = listOf(
                AccusationDataItem("fullName", fullNameField.text.toString()),
                AccusationDataItem("identification", identificationField.text.toString()),
                AccusationDataItem("phoneNumber", phoneNumberField.text.toString()),
                AccusationDataItem("location", locationField.text.toString()),
                AccusationDataItem("eventDescription", eventDescriptionField.text.toString())
            )
            val crimeAccusation = Accusation(
                accusationTypeId = 1,
                accusationData = accusationDataList
            )
            // Send API request
            lifecycleScope.launch {
                try {
                    val response: Response<AccusationResponse> = ApiClient.apiService.sendAccusation(crimeAccusation)
                    handleApiResponse(response)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception sending report: ${e.message}", e)
                    showError("Error al enviar reporte: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing report data", e)
            showError("Error al preparar los datos del reporte")
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
        startActivity(intent)
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

