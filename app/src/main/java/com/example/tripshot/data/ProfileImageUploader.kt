package com.example.tripshot.util

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object ProfileImageUploader {

    suspend fun uploadProfileImage(
        userId: String,
        imageUri: Uri
    ): String {
        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("profileImages")
            .child("$userId.jpg")

        storageRef.putFile(imageUri).await()

        return storageRef.downloadUrl.await().toString()
    }
}