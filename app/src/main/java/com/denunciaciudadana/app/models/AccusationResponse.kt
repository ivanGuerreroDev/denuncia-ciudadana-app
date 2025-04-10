package com.denunciaciudadana.app.models

import com.google.gson.annotations.SerializedName

/**
 * Represents the server response after submitting a noise accusation.
 *
 * @property id The unique identifier assigned to the accusation
 * @property status The current status of the submitted accusation
 */
data class AccusationResponse(
    @SerializedName("data")
    val data: AccusationDataResponse,
    
    @SerializedName("accusationData")
    val accusationData: List<AccusationDataItem>,
)

data class AccusationDataResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("createdDate")
    val status: String
)