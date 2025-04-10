package com.denunciaciudadana.app.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a request to generate a facial composite portrait.
 * Contains all facial characteristics needed to generate a portrait.
 */
data class PortraitRequest(
    @SerializedName("genero")
    val genero: String = "",
    
    @SerializedName("formaRostro")
    val formaRostro: String = "",
    
    @SerializedName("ojos")
    val ojos: String = "",
    
    @SerializedName("nariz")
    val nariz: String = "",
    
    @SerializedName("boca")
    val boca: String = "",
    
    @SerializedName("orejas")
    val orejas: String = "",
    
    @SerializedName("colorCabello")
    val colorCabello: String = "",
    
    @SerializedName("longitudCabello")
    val longitudCabello: String = "",
    
    @SerializedName("estiloCabello")
    val estiloCabello: String = "",
    
    @SerializedName("distribucionCabello")
    val distribucionCabello: String = "",
    
    @SerializedName("colorPiel")
    val colorPiel: String = "",
    
    @SerializedName("marcasPiel")
    val marcasPiel: String = "",
    
    @SerializedName("texturaPiel")
    val texturaPiel: String = "",
    
    @SerializedName("accesorios")
    val accesorios: String = "",
    
    @SerializedName("velloFacial")
    val velloFacial: String = "",
    
    @SerializedName("expresionFacial")
    val expresionFacial: String = "",
    
    @SerializedName("edad")
    val edad: String = "",
    
    @SerializedName("contextoVestimenta")
    val contextoVestimenta: String = "",
    
    @SerializedName("caracteristicasEspeciales")
    val caracteristicasEspeciales: String = ""
)