package com.example.medidordecibelesapp.activities

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medidordecibelesapp.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Activity for capturing photos and videos using the device camera.
 * Supports switching between front and back camera, and toggling between photo and video modes.
 */
class CameraCaptureActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CameraCaptureActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val CAPTURE_MODE_PHOTO = 0
        private const val CAPTURE_MODE_VIDEO = 1
        private const val CAPTURED_MEDIA_URI_KEY = "captured_media_uri"
        
        // Required permissions for camera functionality
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
    
    // UI components
    private lateinit var viewFinder: PreviewView
    private lateinit var backButton: ImageButton
    private lateinit var btnCapture: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnSwitchMode: ImageButton

    // Camera components
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    
    // Camera state
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var captureMode = CAPTURE_MODE_PHOTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera_capture)
        
        setupWindowInsets()
        initializeViews()
        initializeCameraExecutor()
        checkCameraPermissions()
        setupClickListeners()
    }
    
    /**
     * Set up window insets to respect system UI areas.
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    /**
     * Initialize UI components.
     */
    private fun initializeViews() {
        try {
            viewFinder = findViewById(R.id.viewFinder)
            backButton = findViewById(R.id.backButton)
            btnCapture = findViewById(R.id.btnCapture)
            btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
            btnSwitchMode = findViewById(R.id.btnSwitchMode)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            showErrorAndFinish("Error initializing camera interface")
        }
    }
    
    /**
     * Initialize the camera executor service.
     */
    private fun initializeCameraExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    /**
     * Check camera permissions and request them if not granted.
     */
    private fun checkCameraPermissions() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    /**
     * Set up click listeners for UI components.
     */
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        btnCapture.setOnClickListener {
            if (captureMode == CAPTURE_MODE_PHOTO) {
                takePhoto()
            } else {
                captureVideo()
            }
        }

        btnSwitchCamera.setOnClickListener {
            toggleCamera()
        }

        btnSwitchMode.setOnClickListener {
            toggleCaptureMode()
        }
    }
    
    /**
     * Toggle between front and back camera.
     */
    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera()
    }
    
    /**
     * Toggle between photo and video capture modes.
     */
    private fun toggleCaptureMode() {
        captureMode = if (captureMode == CAPTURE_MODE_PHOTO) {
            Toast.makeText(this, "Modo video activado", Toast.LENGTH_SHORT).show()
            CAPTURE_MODE_VIDEO
        } else {
            Toast.makeText(this, "Modo foto activado", Toast.LENGTH_SHORT).show()
            CAPTURE_MODE_PHOTO
        }
        startCamera()
    }

    /**
     * Take a photo and save it to the media store.
     */
    private fun takePhoto() {
        // Check that imageCapture is initialized
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture not initialized")
            return
        }

        try {
            // Create output options with timestamp filename
            val contentValues = createMediaContentValues("image/jpeg", "Pictures/MedidorDecibelesApp")
            
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build()
    
            // Capture the image
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                createImageCaptureCallback()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up photo capture", e)
            showError("Error al preparar la captura")
        }
    }
    
    /**
     * Create callback for image capture result handling.
     */
    private fun createImageCaptureCallback(): ImageCapture.OnImageSavedCallback {
        return object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Error capturing photo: ${exc.message}", exc)
                showError("Error al capturar foto")
            }
    
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                handleMediaCaptureSuccess(output.savedUri)
            }
        }
    }

    /**
     * Start or stop video recording.
     */
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: run {
            Log.e(TAG, "VideoCapture not initialized")
            return
        }

        btnCapture.isEnabled = false

        val currentRecording = recording
        if (currentRecording != null) {
            // Stop the current recording
            currentRecording.stop()
            recording = null
            return
        }

        try {
            // Create output options with timestamp filename
            val contentValues = createMediaContentValues("video/mp4", "Movies/MedidorDecibelesApp")
            
            val mediaStoreOutputOptions = MediaStoreOutputOptions
                .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build()
    
            // Start recording
            recording = videoCapture.output
                .prepareRecording(this, mediaStoreOutputOptions)
                .start(ContextCompat.getMainExecutor(this), videoRecordEventListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up video capture", e)
            btnCapture.isEnabled = true
            showError("Error al preparar la grabación")
        }
    }
    
    /**
     * Listener for video recording events.
     */
    private val videoRecordEventListener = { recordEvent: VideoRecordEvent ->
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                btnCapture.setBackgroundResource(android.R.drawable.ic_media_pause)
                btnCapture.isEnabled = true
            }
            is VideoRecordEvent.Finalize -> {
                if (!recordEvent.hasError()) {
                    handleMediaCaptureSuccess(recordEvent.outputResults.outputUri)
                } else {
                    Log.e(TAG, "Video recording error: ${recordEvent.error}")
                    showError("Error en grabación de video: ${recordEvent.error}")
                    recording?.close()
                    recording = null
                }
                btnCapture.setBackgroundResource(android.R.drawable.ic_menu_camera)
                btnCapture.isEnabled = true
            }
            else -> {
                // Other recording events can be handled here
            }
        }
    }
    
    /**
     * Create ContentValues for media storage with timestamp filename.
     */
    private fun createMediaContentValues(mimeType: String, relativePath: String): ContentValues {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
            
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            }
        }
    }
    
    /**
     * Handle successful media capture by returning its URI.
     */
    private fun handleMediaCaptureSuccess(uri: Uri?) {
        val savedUri = uri ?: Uri.EMPTY
        val msg = "Media saved: $savedUri"
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        Log.d(TAG, msg)
        
        // Return the URI of captured media
        val resultIntent = Intent().apply {
            putExtra(CAPTURED_MEDIA_URI_KEY, savedUri.toString())
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Initialize the camera and bind use cases according to current mode.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
                
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
    
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()
    
                setupCameraUseCases(cameraProvider, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up camera", e)
                showError("Error al iniciar la cámara")
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    /**
     * Set up camera use cases based on capture mode.
     */
    private fun setupCameraUseCases(
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        preview: Preview
    ) {
        if (captureMode == CAPTURE_MODE_PHOTO) {
            setupPhotoCapture(cameraProvider, cameraSelector, preview)
        } else {
            setupVideoCapture(cameraProvider, cameraSelector, preview)
        }
    }
    
    /**
     * Set up photo capture use case.
     */
    private fun setupPhotoCapture(
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        preview: Preview
    ) {
        imageCapture = ImageCapture.Builder().build()
        videoCapture = null
        
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
    }
    
    /**
     * Set up video capture use case.
     */
    private fun setupVideoCapture(
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        preview: Preview
    ) {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)
        imageCapture = null
        
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
    }

    /**
     * Check if all required permissions are granted.
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                showErrorAndFinish("Se requieren permisos para usar la cámara")
            }
        }
    }
    
    /**
     * Show an error message.
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show an error message and finish the activity.
     */
    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}