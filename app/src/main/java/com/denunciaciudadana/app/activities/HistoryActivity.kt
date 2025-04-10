package com.denunciaciudadana.app.activities

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import com.denunciaciudadana.app.R

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Ocultar la lista de mediciones ya que no se usará más
        val recyclerView = findViewById<RecyclerView>(R.id.measurementsRecyclerView)
        recyclerView.visibility = View.GONE

        // Crear un TextView para el mensaje dinámicamente
        val messageTextView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Esta funcionalidad ya no está disponible"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 100, 0, 0)
        }

        // Obtener el LinearLayout principal de manera segura
        val parentView = recyclerView.parent
        if (parentView is LinearLayout) {
            // Agregar el TextView al LinearLayout padre, justo antes del RecyclerView
            val recyclerViewIndex = parentView.indexOfChild(recyclerView)
            parentView.addView(messageTextView, recyclerViewIndex)
        }

        // Configurar botón de regreso
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}