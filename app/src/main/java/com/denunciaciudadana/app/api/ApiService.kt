package com.denunciaciudadana.app.api

import com.denunciaciudadana.app.models.Accusation
import com.denunciaciudadana.app.models.AccusationResponse
import com.denunciaciudadana.app.models.PortraitRequest
import com.denunciaciudadana.app.models.PortraitResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

/**
 * Unified API service interface for all endpoints in the application.
 * Used with Retrofit to communicate with the server.
 */
interface ApiService {
    /**
     * Sends a general accusation report to the server.
     *
     * @param body The request body containing accusation details
     * @return A Response object with the server's response
     */
    @POST("accusations")
    suspend fun sendAccusation(@Body body: Accusation): Response<AccusationResponse>
    
    /**
     * Uploads an audio file for an existing noise accusation.
     *
     * @param id The ID of the noise accusation
     * @param audioFile The audio file as a MultipartBody.Part
     * @return A Response object with the server's response
     */
    @Multipart
    @POST("accusations/{id}/upload")
    suspend fun uploadAudioFile(
        @Path("id") id: Int,
        @Part audioFile: MultipartBody.Part
    ): Response<AccusationResponse>
    
    /**
     * Generates a facial composite portrait based on provided facial features.
     *
     * @param request The portrait request containing facial feature details
     * @return A Response object with the portrait generation response
     */
    @POST("portrait/generate")
    suspend fun generatePortrait(@Body request: PortraitRequest): Response<PortraitResponse>
}