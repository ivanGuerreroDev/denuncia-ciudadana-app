package com.example.medidordecibelesapp.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a noise complaint accusation to be sent to the server.
 *
 * @property accusationTypeId The type identifier for noise complaints (default: 2)
 * @property accusationData List of key-value data points associated with this accusation
 */
data class NoiseAccusation(
    @SerializedName("accusationTypeId")
    val accusationTypeId: Int = 2,
    
    @SerializedName("accusationData")
    val accusationData: List<AccusationDataItem>
)