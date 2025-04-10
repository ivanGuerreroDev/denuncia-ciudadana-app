package com.example.medidordecibelesapp.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a key-value pair for accusation data.
 *
 * @property key The data field name/identifier
 * @property value The actual value for the data field
 */
data class AccusationDataItem(
    @SerializedName("key")
    val key: String,
    
    @SerializedName("value")
    val value: String
)