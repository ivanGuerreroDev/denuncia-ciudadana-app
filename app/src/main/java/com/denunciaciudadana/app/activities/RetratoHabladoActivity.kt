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
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.lang.Exception
import com.denunciaciudadana.app.R

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
    private lateinit var imgRetrato: ImageView
    private lateinit var backButton: ImageButton

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
    }

    private fun setupClickListeners() {
        btnGenerarRetrato.setOnClickListener {
            if (validateForm()) {
                generarRetrato()
            }
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

        val jsonObject = JSONObject().apply {
            // Rasgos faciales generales
            put("genero", edtGenero.text.toString())
            put("formaRostro", edtFormaRostro.text.toString())
            put("ojos", edtOjos.text.toString())
            put("nariz", edtNariz.text.toString())
            put("boca", edtBoca.text.toString())
            put("orejas", edtOrejas.text.toString())
            
            // Características del cabello
            put("colorCabello", edtColorCabello.text.toString())
            put("longitudCabello", edtLongitudCabello.text.toString())
            put("estiloCabello", edtEstiloCabello.text.toString())
            put("distribucionCabello", edtDistribucionCabello.text.toString())
            
            // Tez y características de la piel
            put("colorPiel", edtColorPiel.text.toString())
            put("marcasPiel", edtMarcasPiel.text.toString())
            put("texturaPiel", edtTexturaPiel.text.toString())
            
            // Otras características distintivas
            put("accesorios", edtAccesorios.text.toString())
            put("velloFacial", edtVelloFacial.text.toString())
            put("expresionFacial", edtExpresionFacial.text.toString())
            
            // Edad aproximada y contexto
            put("edad", edtEdad.text.toString())
            put("contextoVestimenta", edtContextoVestimenta.text.toString())
            
            // Características especiales adicionales
            put("caracteristicasEspeciales", edtCaracteristicasEspeciales.text.toString())
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        lifecycleScope.launch {
            try {
                // Aquí se haría la llamada real a la API
                // val response: Response<RetratoResponse> = ApiClient.apiService.generarRetrato(requestBody)
                
                // Simulamos una respuesta exitosa para propósitos de demostración
                // En una implementación real, procesaríamos la respuesta de la API
                simulateSuccessResponse()
                
            } catch (e: Exception) {
                Log.e("RetratoHablado", "Error al generar retrato: ${e.message}")
                Toast.makeText(this@RetratoHabladoActivity, "Error al generar el retrato", Toast.LENGTH_SHORT).show()
                btnGenerarRetrato.isEnabled = true
                btnGenerarRetrato.text = "Generar Retrato"
            }
        }
    }

    private fun simulateSuccessResponse() {
        // Esta función simula una respuesta exitosa de la API
        // En una implementación real, procesaríamos la imagen recibida de la API
        
        // Simulamos un retardo para dar sensación de procesamiento
        android.os.Handler().postDelayed({
            // Mostramos una imagen de ejemplo (en una implementación real, usaríamos la imagen de la API)
            imgRetrato.visibility = View.VISIBLE
            imgRetrato.setImageResource(R.drawable.ic_crime) // Usamos un icono existente como placeholder
            
            btnGenerarRetrato.isEnabled = true
            btnGenerarRetrato.text = "Generar Retrato"
            
            // Creamos un intent con el resultado
            val resultIntent = Intent()
            // En una implementación real, guardaríamos la imagen y pasaríamos su URI
            // Por ahora, pasamos un URI ficticio
            resultIntent.putExtra("retrato_uri", Uri.parse("content://com.denunciaciudadana.app/retrato_generado"))
            setResult(RESULT_OK, resultIntent)
            
            Toast.makeText(this, "Retrato generado con éxito", Toast.LENGTH_SHORT).show()
        }, 2000)
    }
}