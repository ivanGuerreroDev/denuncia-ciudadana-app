package com.denunciaciudadana.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Data class representing a single decibel measurement.
 * Stores the measurement value along with metadata like timestamp and location.
 *
 * @property id Unique identifier for the measurement (default = 0 for new measurements)
 * @property decibels The measured decibel level
 * @property timestamp When the measurement was taken (Unix timestamp as string)
 * @property latitude Optional latitude coordinate where measurement was taken
 * @property longitude Optional longitude coordinate where measurement was taken
 */
data class DecibelMeasurement(
    val id: Long = 0,
    val decibels: Double,
    val timestamp: String,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    /**
     * Gets a formatted date string from the timestamp.
     *
     * @return Formatted date string (e.g., "9 Apr 2025 14:30:45")
     */
    fun getFormattedDate(): String {
        return try {
            val timestampLong = timestamp.toLong()
            val date = Date(timestampLong)
            val formatter = SimpleDateFormat("d MMM yyyy HH:mm:ss", Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            timestamp // Return raw timestamp if parsing fails
        }
    }
    
    /**
     * Gets the formatted decibel value as a string.
     *
     * @param includeSuffix Whether to include "dB" suffix (default true)
     * @return Formatted decibel string (e.g., "65 dB")
     */
    fun getFormattedDecibels(includeSuffix: Boolean = true): String {
        val roundedValue = decibels.roundToInt()
        return if (includeSuffix) "$roundedValue dB" else roundedValue.toString()
    }
    
    /**
     * Gets a description of the noise level based on decibel value.
     *
     * @return String description of noise level
     */
    fun getNoiseDescription(): String {
        return when {
            decibels < 40 -> "Silencioso"
            decibels < 50 -> "Bajo"
            decibels < 65 -> "Conversación normal"
            decibels < 80 -> "Ruidoso"
            decibels < 90 -> "Muy ruidoso"
            decibels < 100 -> "Peligroso"
            else -> "Extremadamente ruidoso"
        }
    }
    
    /**
     * Checks if this measurement has location data.
     *
     * @return True if both latitude and longitude are not null
     */
    fun hasLocation(): Boolean {
        return latitude != null && longitude != null
    }
    
    /**
     * Gets a formatted location string.
     *
     * @return Formatted location or "No location" if unavailable
     */
    fun getFormattedLocation(): String {
        return if (hasLocation()) {
            String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude)
        } else {
            "No location"
        }
    }

    companion object {
        /**
         * Creates a DecibelMeasurement object with the current timestamp.
         *
         * @param decibels The measured decibel value
         * @param latitude Optional latitude where measurement was taken
         * @param longitude Optional longitude where measurement was taken
         * @return A new DecibelMeasurement instance with the current timestamp
         */
        fun createWithCurrentTimestamp(
            decibels: Double,
            latitude: Double? = null,
            longitude: Double? = null
        ): DecibelMeasurement {
            val currentTimestamp = System.currentTimeMillis().toString()
            return DecibelMeasurement(0, decibels, currentTimestamp, latitude, longitude)
        }
    }
}