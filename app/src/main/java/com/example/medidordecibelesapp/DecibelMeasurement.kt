package com.example.medidordecibelesapp

data class DecibelMeasurement(
    val id: Long = 0,
    val decibels: Double,
    val timestamp: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)