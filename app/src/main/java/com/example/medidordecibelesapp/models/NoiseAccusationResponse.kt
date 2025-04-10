package com.example.medidordecibelesapp.models

import com.google.gson.annotations.SerializedName

/**
 * Represents the server response after submitting a noise accusation.
 *
 * @property id The unique identifier assigned to the accusation
 * @property status The current status of the submitted accusation
 */
data class NoiseAccusationResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("status")
    val status: String
)