package com.example.medidordecibelesapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.fasterxml.jackson.databind.ObjectMapper
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
    private lateinit var eventDescription: EditText

    companion object {
        private const val FILE_REQUEST_CODE = 123
        private const val PERMISSION_REQUEST_CODE = 456
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
            checkAudioPermission()
        }

    }

    private fun sentData() {
            val values = mapOf(
                "accusationTypeId" to 1,
                "accusationData" to listOf(
                    mapOf("key" to "fullName", "value" to fullName.text.toString()),
                    mapOf("key" to "identification", "value" to identification.text.toString()),
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

        if (requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK) {
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
                attachFileUris = uris
                val namesText = fileNames.joinToString(separator = "\n")
                attachFileTextView.text = "Archivos seleccionados:\n$namesText"
                Log.d("FileSelection", "URIs seleccionadas: $uris")
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
            if (cut != -1 && cut!! >= 0) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "archivo desconocido"
    }

}

