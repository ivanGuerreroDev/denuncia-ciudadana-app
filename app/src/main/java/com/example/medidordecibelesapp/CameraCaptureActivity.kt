package com.example.medidordecibelesapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCaptureActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var backButton: ImageButton
    private lateinit var btnCapture: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnSwitchMode: ImageButton

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var captureMode = CAPTURE_MODE_PHOTO

    companion object {
        private const val TAG = "CameraCaptureActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        private const val CAPTURE_MODE_PHOTO = 0
        private const val CAPTURE_MODE_VIDEO = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera_capture)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar vistas
        viewFinder = findViewById(R.id.viewFinder)
        backButton = findViewById(R.id.backButton)
        btnCapture = findViewById(R.id.btnCapture)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnSwitchMode = findViewById(R.id.btnSwitchMode)

        // Verificar permisos
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Configurar listeners
        setupClickListeners()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

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
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera()
        }

        btnSwitchMode.setOnClickListener {
            captureMode = if (captureMode == CAPTURE_MODE_PHOTO) {
                Toast.makeText(this, "Modo video activado", Toast.LENGTH_SHORT).show()
                CAPTURE_MODE_VIDEO
            } else {
                Toast.makeText(this, "Modo foto activado", Toast.LENGTH_SHORT).show()
                CAPTURE_MODE_PHOTO
            }
            startCamera()
        }
    }

    private fun takePhoto() {
        // Verificar que imageCapture está inicializado
        val imageCapture = imageCapture ?: return

        // Crear nombre de archivo con timestamp
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MedidorDecibelesApp")
            }
        }

        // Crear opciones de salida para guardar en MediaStore
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        // Capturar la imagen
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Error al capturar foto: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.EMPTY
                    val msg = "Foto guardada: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    
                    // Devolver el URI de la imagen capturada
                    val resultIntent = Intent()
                    resultIntent.putExtra("captured_media_uri", savedUri.toString())
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        )
    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        // Deshabilitar el botón durante la grabación
        btnCapture.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Detener la grabación actual
            curRecording.stop()
            recording = null
            return
        }

        // Crear nombre de archivo con timestamp
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MedidorDecibelesApp")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // Iniciar grabación
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        btnCapture.setBackgroundResource(android.R.drawable.ic_media_pause)
                        btnCapture.isEnabled = true
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video grabado con éxito: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                            
                            // Devolver el URI del video capturado
                            val resultIntent = Intent()
                            resultIntent.putExtra("captured_media_uri", recordEvent.outputResults.outputUri.toString())
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Error en grabación de video: " +
                                    "${recordEvent.error}")
                        }
                        btnCapture.setBackgroundResource(android.R.drawable.ic_menu_camera)
                        btnCapture.isEnabled = true
                    }
                }
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configurar el preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            // Seleccionar cámara frontal o trasera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                // Desenlazar casos de uso antes de reenlazar
                cameraProvider.unbindAll()

                if (captureMode == CAPTURE_MODE_PHOTO) {
                    // Configurar captura de imagen
                    imageCapture = ImageCapture.Builder().build()
                    videoCapture = null
                    
                    // Enlazar casos de uso a la cámara
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )
                } else {
                    // Configurar captura de video
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()
                    videoCapture = VideoCapture.withOutput(recorder)
                    imageCapture = null
                    
                    // Enlazar casos de uso a la cámara
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, videoCapture
                    )
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Error al vincular casos de uso", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

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
                Toast.makeText(this,
                    "Se requieren permisos para usar la cámara",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}