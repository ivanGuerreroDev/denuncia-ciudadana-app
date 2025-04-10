package com.example.medidordecibelesapp.api

import com.example.medidordecibelesapp.models.NoiseAccusation
import com.example.medidordecibelesapp.models.NoiseAccusationResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

/**
 * Interface defining API endpoints for noise-related reports and operations.
 * Used with Retrofit to communicate with the server.
 */
interface NoiseApiService {
    /**
     * Submits a new noise accusation report to the server.
     *
     * @param accusation The noise accusation details to be reported
     * @return A Call object with the server response
     */
    @POST("accusations")
    fun reportNoise(@Body accusation: NoiseAccusation): Call<NoiseAccusationResponse>
    
    /**
     * Uploads an audio file for an existing noise accusation.
     *
     * @param id The ID of the noise accusation
     * @param audioFile The audio file as a MultipartBody.Part
     * @return A Call object with the server response
     */
    @Multipart
    @PATCH("accusations/{id}/upload")
    fun uploadAudioFile(
        @Path("id") id: String,
        @Part audioFile: MultipartBody.Part
    ): Call<NoiseAccusationResponse>
}