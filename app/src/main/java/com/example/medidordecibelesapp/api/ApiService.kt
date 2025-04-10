package com.example.medidordecibelesapp.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface defining general API endpoints for the application.
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
    suspend fun sendAccusation(@Body body: RequestBody): Response<Unit>

    /**
     * Uploads a file attachment for an existing accusation.
     *
     * @param accusationId The ID of the accusation
     * @param file The file to upload
     * @return A Response object with the server's response
     */
    @Multipart
    @POST("accusations/{accusationId}/upload")
    suspend fun uploadFile(
        @Path("accusationId") accusationId: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>
}