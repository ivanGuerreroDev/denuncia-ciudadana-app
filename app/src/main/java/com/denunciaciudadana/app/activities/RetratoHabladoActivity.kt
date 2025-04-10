package com.denunciaciudadana.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.denunciaciudadana.app.R
import com.denunciaciudadana.app.api.ApiClient
import com.denunciaciudadana.app.models.PortraitRequest
import com.denunciaciudadana.app.models.PortraitResponse
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.lang.Exception

class RetratoHabladoActivity : AppCompatActivity() {

    // Rasgos faciales generales
    private lateinit var edtGenero: TextInputEditText
    private lateinit var edtFormaRostro: TextInputEditText
    private lateinit var edtOjos: TextInputEditText
    private lateinit var edtNariz: TextInputEditText
    private lateinit var edtBoca: TextInputEditText
    private lateinit var edtOrejas: TextInputEditText
    
    // Características del cabello
    private lateinit var edtColorCabello: TextInputEditText
    private lateinit var edtLongitudCabello: TextInputEditText
    private lateinit var edtEstiloCabello: TextInputEditText
    private lateinit var edtDistribucionCabello: TextInputEditText
    
    // Tez y características de la piel
    private lateinit var edtColorPiel: TextInputEditText
    private lateinit var edtMarcasPiel: TextInputEditText
    private lateinit var edtTexturaPiel: TextInputEditText
    
    // Otras características distintivas
    private lateinit var edtAccesorios: TextInputEditText
    private lateinit var edtVelloFacial: TextInputEditText
    private lateinit var edtExpresionFacial: TextInputEditText
    
    // Edad aproximada y contexto
    private lateinit var edtEdad: TextInputEditText
    private lateinit var edtContextoVestimenta: TextInputEditText
    
    // Características especiales adicionales
    private lateinit var edtCaracteristicasEspeciales: TextInputEditText
    
    private lateinit var btnGenerarRetrato: Button
    private lateinit var btnAgregarAlReporte: Button
    private lateinit var imgRetrato: ImageView
    private lateinit var backButton: ImageButton
    
    // Variable para almacenar la URL del retrato generado
    private var retratoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_retrato_hablado)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        // Rasgos faciales generales
        edtGenero = findViewById(R.id.edtGenero)
        edtFormaRostro = findViewById(R.id.edtFormaRostro)
        edtOjos = findViewById(R.id.edtOjos)
        edtNariz = findViewById(R.id.edtNariz)
        edtBoca = findViewById(R.id.edtBoca)
        edtOrejas = findViewById(R.id.edtOrejas)
        
        // Características del cabello
        edtColorCabello = findViewById(R.id.edtColorCabello)
        edtLongitudCabello = findViewById(R.id.edtLongitudCabello)
        edtEstiloCabello = findViewById(R.id.edtEstiloCabello)
        edtDistribucionCabello = findViewById(R.id.edtDistribucionCabello)
        
        // Tez y características de la piel
        edtColorPiel = findViewById(R.id.edtColorPiel)
        edtMarcasPiel = findViewById(R.id.edtMarcasPiel)
        edtTexturaPiel = findViewById(R.id.edtTexturaPiel)
        
        // Otras características distintivas
        edtAccesorios = findViewById(R.id.edtAccesorios)
        edtVelloFacial = findViewById(R.id.edtVelloFacial)
        edtExpresionFacial = findViewById(R.id.edtExpresionFacial)
        
        // Edad aproximada y contexto
        edtEdad = findViewById(R.id.edtEdad)
        edtContextoVestimenta = findViewById(R.id.edtContextoVestimenta)
        
        // Características especiales adicionales
        edtCaracteristicasEspeciales = findViewById(R.id.edtCaracteristicasEspeciales)
        
        btnGenerarRetrato = findViewById(R.id.btnGenerarRetrato)
        btnAgregarAlReporte = findViewById(R.id.btnAgregarAlReporte)
        imgRetrato = findViewById(R.id.imgRetrato)
        
        // Botón de regreso
        try {
            backButton = findViewById(R.id.backButton)
            backButton.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.e("RetratoHablado", "No se encontró el botón de regreso: ${e.message}")
        }
        
        // Inicialmente, el botón de añadir al reporte está oculto
        btnAgregarAlReporte.visibility = View.GONE
    }

    private fun setupClickListeners() {
        btnGenerarRetrato.setOnClickListener {
            if (validateForm()) {
                generarRetrato()
            }
        }
        
        btnAgregarAlReporte.setOnClickListener {
            agregarRetratoAlReporte()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validar campos obligatorios (rasgos faciales básicos)
        if (edtGenero.text.toString().isEmpty()) {
            edtGenero.error = "Campo requerido"
            isValid = false
        }

        if (edtEdad.text.toString().isEmpty()) {
            edtEdad.error = "Campo requerido"
            isValid = false
        }

        if (edtColorPiel.text.toString().isEmpty()) {
            edtColorPiel.error = "Campo requerido"
            isValid = false
        }

        if (edtColorCabello.text.toString().isEmpty()) {
            edtColorCabello.error = "Campo requerido"
            isValid = false
        }

        if (edtFormaRostro.text.toString().isEmpty()) {
            edtFormaRostro.error = "Campo requerido"
            isValid = false
        }
        
        if (edtOjos.text.toString().isEmpty()) {
            edtOjos.error = "Campo requerido"
            isValid = false
        }

        return isValid
    }

    private fun generarRetrato() {
        // Mostrar progreso
        btnGenerarRetrato.isEnabled = false
        btnGenerarRetrato.text = "Generando..."
        
        // Crear el objeto PortraitRequest con los datos del formulario
        val portraitRequest = PortraitRequest(
            genero = edtGenero.text.toString(),
            formaRostro = edtFormaRostro.text.toString(),
            ojos = edtOjos.text.toString(),
            nariz = edtNariz.text.toString(),
            boca = edtBoca.text.toString(),
            orejas = edtOrejas.text.toString(),
            colorCabello = edtColorCabello.text.toString(),
            longitudCabello = edtLongitudCabello.text.toString(),
            estiloCabello = edtEstiloCabello.text.toString(),
            distribucionCabello = edtDistribucionCabello.text.toString(),
            colorPiel = edtColorPiel.text.toString(),
            marcasPiel = edtMarcasPiel.text.toString(),
            texturaPiel = edtTexturaPiel.text.toString(),
            accesorios = edtAccesorios.text.toString(),
            velloFacial = edtVelloFacial.text.toString(),
            expresionFacial = edtExpresionFacial.text.toString(),
            edad = edtEdad.text.toString(),
            contextoVestimenta = edtContextoVestimenta.text.toString(),
            caracteristicasEspeciales = edtCaracteristicasEspeciales.text.toString()
        )

        lifecycleScope.launch {
            try {
                // Llamada real a la API usando el servicio definido
                val response = ApiClient.apiService.generatePortrait(portraitRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val portraitResponse = response.body()!!
                    handleSuccessResponse(portraitResponse)
                } else {
                    Log.e("RetratoHablado", "Error en la respuesta: ${response.code()} - ${response.message()}")
                    Toast.makeText(
                        this@RetratoHabladoActivity,
                        "Error al generar el retrato: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetButton()
                }
            } catch (e: Exception) {
                Log.e("RetratoHablado", "Error al generar retrato: ${e.message}")
                Toast.makeText(
                    this@RetratoHabladoActivity,
                    "Error al generar el retrato: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                resetButton()
            }
        }
    }

    private fun resetButton() {
        btnGenerarRetrato.isEnabled = true
        btnGenerarRetrato.text = "Generar Retrato"
    }

    private fun handleSuccessResponse(portraitResponse: PortraitResponse) {
        // Almacenar la URL del retrato generado
        retratoUrl = portraitResponse.imageUrl
        
        // Mostrar la imagen recibida de la API
        imgRetrato.visibility = View.VISIBLE
        
        // Cargar la imagen con Glide desde la URL proporcionada
        Glide.with(this)
            .load(portraitResponse.imageUrl)
            .placeholder(R.drawable.ic_crime) // Mostrar placeholder mientras carga
            .error(R.drawable.ic_crime) // Mostrar si hay error al cargar
            .into(imgRetrato)
        
        resetButton()
        
        // Mostrar el botón para añadir al reporte
        btnAgregarAlReporte.visibility = View.VISIBLE
        
        // Crear un intent con el resultado
        val resultIntent = Intent()
        resultIntent.putExtra("retrato_uri", portraitResponse.imageUrl)
        resultIntent.putExtra("retrato_prompt", portraitResponse.prompt)
        resultIntent.putExtra("retrato_fecha", portraitResponse.createdAt)
        setResult(RESULT_OK, resultIntent)
        
        Toast.makeText(this, "Retrato generado con éxito", Toast.LENGTH_SHORT).show()
        
        // Registro en log para depuración
        Log.d("RetratoHablado", "Retrato generado: ${portraitResponse.imageUrl}")
        Log.d("RetratoHablado", "Prompt: ${portraitResponse.prompt}")
    }
    
    private fun agregarRetratoAlReporte() {
        if (retratoUrl != null) {
            val resultIntent = Intent()
            resultIntent.putExtra("retrato_uri", retratoUrl)
            resultIntent.putExtra("add_to_report", true)
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "Retrato añadido al reporte", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error: No hay retrato para añadir", Toast.LENGTH_SHORT).show()
        }
    }
}