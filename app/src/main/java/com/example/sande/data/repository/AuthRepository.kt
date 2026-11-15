package com.example.sande.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication + Firestore access.
 *
 * Responsibilities:
 *  - Individual user identity (independent of business membership)
 *  - Register / sign in / sign out / password reset
 *  - Session persistence (Firebase persists the session natively)
 *  - Business membership resolution for the signed-in user
 *
 * Business membership is read from:
 *   businesses/{businessId}/members/{uid}
 * where businessId is discovered from the user's membership index at
 *   users/{uid}/businesses/{businessId}
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    data class Session(
        val uid: String,
        val email: String?,
        val idToken: String?,
        val displayName: String?
    )

    /** Fires whenever Firebase signs a user in or out (including on app start). */
    val authState: StateFlow<Session?> = _session

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _session.update {
                if (user == null) null else user.toSession()
            }
        }
    }

    private fun FirebaseUser.toSession(): Session = Session(
        uid = uid,
        email = email,
        idToken = null,
        displayName = displayName
    )

    /**
     * Creates a new account. Firebase requires a password of at least 6 characters;
     * the blueprint specifies 8, so we enforce 8 here.
     */
    suspend fun register(email: String, password: String, displayName: String?): Result<Session> {
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        if (!email.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address"))
        }
        return runCatching {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("No user returned")
            if (!displayName.isNullOrBlank()) {
                user.updateProfile(
                    com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                ).await()
            }
            user.toSession().also { _session.value = it }
        }
    }

    /** Signs in an existing user. */
    suspend fun signIn(email: String, password: String): Result<Session> {
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter your password"))
        }
        return runCatching {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("No user returned")
            user.toSession().also { _session.value = it }
        }
    }

    /** Signs out and clears the local session. */
    suspend fun signOut() {
        firebaseAuth.signOut()
        _session.value = null
    }

    /** Sends a password reset email. */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        if (!email.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address"))
        }
        return runCatching {
            firebaseAuth.sendPasswordResetEmail(email).await()
        }
    }

    /** Reloads the current user to pick up disabled/deleted account state. */
    suspend fun reloadAccount(): Result<Unit> = runCatching {
        firebaseAuth.currentUser?.reload()?.await()
    }

    /**
     * Resolves the businesses this user belongs to.
     * Reads users/{uid}/businesses/{businessId} membership index documents.
     */
    suspend fun resolveBusinessIds(): Result<List<String>> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: return@runCatching emptyList()
        val snapshot = firestore.collection("users").document(uid)
            .collection("businesses").get().await()
        snapshot.documents.map { it.id }
    }

    val currentUser: String? get() = _session.value?.uid
    val isSignedIn: Boolean get() = _session.value != null
}
