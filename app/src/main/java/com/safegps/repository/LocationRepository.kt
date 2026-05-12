package com.safegps.repository

import android.location.Location
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val pendingQueue = mutableListOf<LocationData>()

    suspend fun sendLocation(location: Location) {
        val userId = auth.currentUser?.uid ?: "anonymous_user" // Fallback for testing

        val data = LocationData(
            lat = location.latitude,
            lng = location.longitude,
            accuracy = location.accuracy,
            speed = location.speed,
            timestamp = FieldValue.serverTimestamp(),
            deviceId = Build.ID
        )

        try {
            firestore
                .collection("users")
                .document(userId)
                .collection("locations")
                .add(data)
                .await()
        } catch (e: Exception) {
            pendingQueue.add(data)
            firestore.enableNetwork()
        }
    }
}
