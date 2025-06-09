package pl.pw.geoapp

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class FirestoreAuthManager(private val context: Context) {
    companion object {
        private const val TAG = "FirestoreAuthManager"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Callback for authentication and collection creation results
    interface AuthCallback {
        fun onSuccess(collectionName: String, password: String)
        fun onFailure(errorMessage: String)
    }

    interface AccessCallback {
        fun onSuccess()
        fun onFailure(errorMessage: String)
    }

    // Sign in anonymously and create a protected collection
    fun createProtectedCollection(callback: AuthCallback) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Anonymous sign-in successful, UID: ${auth.currentUser?.uid}")
                    val password = generateRandomPassword()
                    val collectionName = "protected_${UUID.randomUUID().toString().replace("-", "")}"
                    createCollection(collectionName, password, callback)
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Log.w(TAG, "Anonymous sign-in failed: $errorMessage", task.exception)
                    callback.onFailure("Authentication failed: $errorMessage")
                }
            }
    }

    // Write to a protected collection after verifying the password
    fun writeToProtectedCollection(collectionName: String, password: String, data: Map<String, Any>, callback: AccessCallback) {
        verifyPassword(collectionName, password) { isValid ->
            if (isValid) {
                db.collection(collectionName).document()
                    .set(data)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully wrote to collection '$collectionName'")
                        callback.onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error writing to collection: ${e.message}", e)
                        callback.onFailure("Write failed: ${e.message}")
                    }
            } else {
                Log.w(TAG, "Invalid password for collection '$collectionName'")
                callback.onFailure("Invalid password")
            }
        }
    }

    // Read from a protected collection after verifying the password
    fun readFromProtectedCollection(collectionName: String, password: String, callback: AccessCallback) {
        verifyPassword(collectionName, password) { isValid ->
            if (isValid) {
                db.collection(collectionName)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            Log.d(TAG, "Document: ${document.id} => ${document.data}")
                        }
                        callback.onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error reading collection: ${e.message}", e)
                        callback.onFailure("Read failed: ${e.message}")
                    }
            } else {
                Log.w(TAG, "Invalid password for collection '$collectionName'")
                callback.onFailure("Invalid password")
            }
        }
    }

    private fun createCollection(collectionName: String, password: String, callback: AuthCallback) {
        val metadata = hashMapOf(
            "password" to password,
            "createdBy" to auth.currentUser?.uid,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection(collectionName).document("metadata")
            .set(metadata)
            .addOnSuccessListener {
                Log.d(TAG, "Created collection '$collectionName' with password: $password")
                callback.onSuccess(collectionName, password)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error creating collection: ${e.message}", e)
                callback.onFailure("Failed to create collection: ${e.message}")
            }
    }

    private fun generateRandomPassword(length: Int = 16): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes).substring(0, length)
    }

    private fun verifyPassword(collectionName: String, password: String, callback: (Boolean) -> Unit) {
        db.collection(collectionName).document("metadata")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedPassword = document.getString("password")
                    callback(storedPassword == password)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error verifying password: ${e.message}", e)
                callback(false)
            }
    }
}