package com.safegps.repository

import com.google.firebase.firestore.FieldValue

data class LocationData(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Any? = null,
    val deviceId: String = ""
)
