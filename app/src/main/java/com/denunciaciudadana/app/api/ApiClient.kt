package com.denunciaciudadana.app.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object for API communication that provides properly configured
 * Retrofit instances for different API services.
 */
object ApiClient {
    private const val BASE_URL = "https://rapid-davita-ivanguerrero-378087cc.koyeb.app/api/v1/"
    private const val TIMEOUT_SECONDS = 30L
    
    /**
     * Configures and provides an OkHttpClient instance with logging and timeouts.
     */
    private val okHttpClient by lazy {
        OkHttpClient.Builder().apply {
            addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }.build()
    }
    
    /**
     * Shared Retrofit instance configured with the base URL and common settings.
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * API service for all endpoints in the application.
     */
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    /**
     * @deprecated Use apiService instead. This property is kept for backward compatibility.
     */
    @Deprecated("Use apiService instead", ReplaceWith("apiService"))
    val noiseApiService: ApiService by lazy {
        apiService
    }
}