package com.example.medidordecibelesapp

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface NoiseApiService {
    @POST("/api/v1/accusations")
    fun reportNoise(@Body accusation: NoiseAccusation): Call<NoiseAccusationResponse>
    
    @Multipart
    @PATCH("/api/v1/accusations/{id}/upload")
    fun uploadAudioFile(
        @Path("id") id: String,
        @Part audioFile: MultipartBody.Part
    ): Call<NoiseAccusationResponse>
}