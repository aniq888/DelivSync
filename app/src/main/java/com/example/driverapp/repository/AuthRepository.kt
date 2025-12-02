package com.example.driverapp.repository

import android.util.Log
import com.example.driverapp.models.Driver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val driversCollection = db.collection("drivers")

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        driver: Driver
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                // Save driver profile to Firestore
                val driverWithId = driver.copy(id = user.uid)
                driversCollection.document(user.uid).set(driverWithId).await()
                Result.success(user)
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign up error", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithPhone(
        phoneNumber: String,
        verificationId: String,
        code: String,
        driver: Driver
    ): Result<FirebaseUser> {
        return try {
            // Note: Phone auth requires additional setup with Firebase Phone Auth
            // This is a simplified version - you'll need to implement full phone auth flow
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(
                verificationId,
                code
            )
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                val driverWithId = driver.copy(id = user.uid, phoneNumber = phoneNumber)
                driversCollection.document(user.uid).set(driverWithId).await()
                Result.success(user)
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Phone sign up error", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in error", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentDriver(): Driver? {
        val user = auth.currentUser ?: return null
        return try {
            val document = driversCollection.document(user.uid).get().await()
            Driver.fromDocument(document)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get driver error", e)
            null
        }
    }

    suspend fun updateDriverProfile(driver: Driver): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            driversCollection.document(user.uid).set(driver, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Update profile error", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
}

