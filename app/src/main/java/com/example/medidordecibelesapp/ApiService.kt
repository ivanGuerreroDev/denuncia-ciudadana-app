package com.example.medidordecibelesapp
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
        @POST("/accusations")
        suspend fun sendAccusation(@Body body: RequestBody): Response<Unit>

        //No se si es el id del tipo de denuncia o es el id de la denuncia en si
        @POST("/accusations/{id}/upload")
        suspend fun uploadFile(
            @Part file: MultipartBody.Part
        ): Response<Unit>
    }

