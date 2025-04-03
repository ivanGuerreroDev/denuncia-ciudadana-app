package com.example.medidordecibelesapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DecibelDbHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MeasurementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dbHelper = DecibelDbHelper(this)
        recyclerView = findViewById(R.id.measurementsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val measurements = dbHelper.getAllMeasurements()
        adapter = MeasurementAdapter(measurements)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private inner class MeasurementAdapter(private val measurements: List<DecibelMeasurement>) :
        RecyclerView.Adapter<MeasurementAdapter.MeasurementViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeasurementViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_measurement, parent, false)
            return MeasurementViewHolder(view)
        }

        override fun onBindViewHolder(holder: MeasurementViewHolder, position: Int) {
            val measurement = measurements[position]
            holder.bind(measurement)
        }

        override fun getItemCount() = measurements.size

        inner class MeasurementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val decibelValueTextView: TextView = itemView.findViewById(R.id.decibelValueTextView)
            private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
            private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)

            fun bind(measurement: DecibelMeasurement) {
                decibelValueTextView.text = String.format("%.1f dB", measurement.decibels)
                
                // Format timestamp for better readability
                try {
                    val timestamp = measurement.timestamp.toLong()
                    val date = Date(timestamp)
                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    timestampTextView.text = formatter.format(date)
                } catch (e: Exception) {
                    timestampTextView.text = measurement.timestamp
                }

                // Display location if available
                if (measurement.latitude != null && measurement.longitude != null) {
                    locationTextView.text = String.format(
                        "Lat: %.6f, Long: %.6f",
                        measurement.latitude,
                        measurement.longitude
                    )
                    locationTextView.visibility = View.VISIBLE
                } else {
                    locationTextView.text = "Ubicación no disponible"
                    locationTextView.visibility = View.VISIBLE
                }
            }
        }
    }
}