package com.example.gestioeventosdsm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    // UI elements
    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var loginGoogleButton: Button // Google login button

    // Google Sign-In client
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // If the user is already authenticated, skip login screen
        if (auth.currentUser != null) {
            goToMainActivity()
            return // Prevents executing the rest of onCreate
        }

        // --- Google Sign-In Configuration ---
        // Build configuration requesting ID token and email
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) 
            // Token required for Firebase authentication
            .requestEmail() // Requesting user's email
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- Initialize UI Components ---
        loginEmail = findViewById(R.id.loginEmail)
        loginPassword = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.loginButton)
        loginGoogleButton = findViewById(R.id.loginGoogle)

        // --- Email/Password Login ---
        loginButton.setOnClickListener {
            val email = loginEmail.text.toString().trim()
            val password = loginPassword.text.toString().trim()
            signInWithEmail(email, password)
        }

        // --- Google Login ---
        loginGoogleButton.setOnClickListener {
            // Launch Google Sign-In intent
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    // Handles result of Google Sign-In intent
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // Retrieve Google account information
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)

                    // Authenticate with Firebase using the Google ID token
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Error during Google Sign-In
                    Log.w(TAG, "Google sign in failed", e)
                    showAlertDialog("Google Sign-In Failed", "Error: ${e.statusCode}")
                }
            }
        }

    // Authenticate with Firebase using Google credentials
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Successful login → go to main app screen
                    Log.d(TAG, "signInWithCredential-Google:success")
                    Toast.makeText(this, "Google Sign-In Successful.", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    // Failed login
                    Log.w(TAG, "signInWithCredential-Google:failure", task.exception)
                    showAlertDialog("Authentication Failed", "Could not sign in with Google.")
                }
            }
    }

    // Email/Password Authentication
    private fun signInWithEmail(email: String, password: String) {

        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            showAlertDialog("Error", "Por favor, complete todos los campos")
            return
        }

        // Firebase email/password login
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    showAlertDialog("Authentication Failed", "Error: ${task.exception?.message}")
                }
            }
    }

    // Redirects to MainActivity and closes the login screen
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Prevents returning to login screen
    }

    // Universal method for showing alert dialogs
    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    companion object {
        private const val TAG = "LoginActivity" // Log tag for debugging
    }
}
