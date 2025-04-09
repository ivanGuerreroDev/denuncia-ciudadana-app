package com.example.medidordecibelesapp

import com.google.gson.annotations.SerializedName

data class AccusationDataItem(
    @SerializedName("key")
    val key: String,
    
    @SerializedName("value")
    val value: String
)

data class NoiseAccusation(
    @SerializedName("accusationTypeId")
    val accusationTypeId: Int = 2,
    
    @SerializedName("accusationData")
    val accusationData: List<AccusationDataItem>
)

data class NoiseAccusationResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("status")
    val status: String
)