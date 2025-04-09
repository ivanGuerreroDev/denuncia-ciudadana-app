package com.example.medidordecibelesapp
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

    object RetrofitClient {
        private const val BASE_URL = "https://rapid-davita-ivanguerrero-378087cc.koyeb.app/api/v1"

        val api: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
