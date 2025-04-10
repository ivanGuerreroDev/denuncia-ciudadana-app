package com.denunciaciudadana.app.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a response from the portrait generation API.
 *
 * @property imageUrl URL to the generated portrait image
 * @property prompt The prompt that was used to generate the portrait
 * @property createdAt Timestamp when the portrait was created
 */
data class PortraitResponse(
    @SerializedName("imageUrl")
    val imageUrl: String,
    
    @SerializedName("prompt")
    val prompt: String,
    
    @SerializedName("createdAt")
    val createdAt: String
)