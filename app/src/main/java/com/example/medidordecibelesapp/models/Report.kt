package com.example.medidordecibelesapp.models

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class representing a noise accusation report.
 * Stores all relevant information about a noise complaint report including
 * its metadata, measurements, and associated attachments.
 *
 * @property id Unique identifier for the report
 * @property timestamp When the report was created (Unix timestamp as string)
 * @property status Current status of the report (e.g., "Enviado", "Procesando", "Completado")
 * @property accusationData List of key-value data points containing report details
 * @property attachments List of file paths for any attachments (audio recordings, images, etc.)
 */
data class Report(
    val id: String,
    val timestamp: String,
    val status: String,
    val accusationData: List<AccusationDataItem>,
    val attachments: List<String> = emptyList()
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    fun getFormattedTimestamp(): String {
        val date = Date(timestamp.toLong())
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

    /**
     * Formats the date in a human-readable format
     * @return String representation of the date
     */
    fun getFormattedDate(): String {
        try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            
            val parsedDate = inputFormat.parse(timestamp)
            return parsedDate?.let { outputFormat.format(it) } ?: timestamp
        } catch (e: Exception) {
            // If there's any error in parsing, return the original timestamp string
            return timestamp
        }
    }
}