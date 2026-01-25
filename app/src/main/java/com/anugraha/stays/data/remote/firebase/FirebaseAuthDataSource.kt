package com.anugraha.stays.data.remote.firebase

import com.anugraha.stays.domain.model.User
import com.anugraha.stays.util.Constants
import com.anugraha.stays.util.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun login(email: String, password: String): NetworkResult<User> {
        return try {
            if (!Constants.ALLOWED_EMAILS.contains(email)) {
                return NetworkResult.Error("Unauthorized access. Only admin users can log in.")
            }

            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val user = User(
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName,
                    uid = firebaseUser.uid
                )
                NetworkResult.Success(user)
            } else {
                NetworkResult.Error("Login failed. Please try again.")
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                e.message ?: "Authentication failed. Please check your credentials."
            )
        }
    }

    suspend fun logout(): NetworkResult<Unit> {
        return try {
            firebaseAuth.signOut()
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Logout failed")
        }
    }

    fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser
        return firebaseUser?.let {
            User(
                email = it.email ?: "",
                displayName = it.displayName,
                uid = it.uid
            )
        }
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}