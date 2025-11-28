package com.example.gestioneventosdsm.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gestioneventosdsm.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var loginGoogleButton: Button // Declare Google login button

    // --- Google Sign-In ---
    private lateinit var googleSignInClient: GoogleSignInClient

    // ---Facebook Sign In---
    private lateinit var callbackManager: CallbackManager
    private lateinit var loginFacebookButton: LoginButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()


        // If the user is already logged in, go straight to MainActivity
        if (auth.currentUser != null) {
            goToEventList()
            return // Stop further execution of onCreate
        }

        // --- Configure Google Sign-In ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Important: Get this from google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- Initialize Views ---
        loginEmail = findViewById(R.id.loginEmail)
        loginPassword = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.loginButton)
        loginGoogleButton = findViewById(R.id.loginGoogle) // Initialize Google login button

        // --- Set Click Listeners ---
        loginButton.setOnClickListener {
            val email = loginEmail.text.toString().trim()
            val password = loginPassword.text.toString().trim()
            signInWithEmail(email, password)
        }

        loginGoogleButton.setOnClickListener {
            // Start the Google Sign-In flow
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
        callbackManager = CallbackManager.Factory.create()

        // --- 3. Configura el botón de Facebook ---
        loginFacebookButton = findViewById(R.id.loginFacebookButton)

        loginFacebookButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "Facebook onSuccess: ${loginResult.accessToken.token}")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "Facebook onCancel.")
                Toast.makeText(baseContext, "Inicio de sesión con Facebook cancelado.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Log.e(TAG, "Facebook onError.", error)
                Toast.makeText(baseContext, "Error al iniciar sesión con Facebook.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data) // Pasa el resultado al SDK de Facebook
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI
                    Log.d(TAG, "signInWithCredential-Facebook:success")
                    val user = auth.currentUser
                    Toast.makeText(this, "Inicio de sesión con Facebook exitoso.", Toast.LENGTH_SHORT).show()
                    goToEventList()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential-Facebook:failure", task.exception)
                    Toast.makeText(baseContext, "Fallo en la autenticación con Firebase.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // --- ActivityResultLauncher for Google Sign-In ---
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                showAlertDialog("Google Sign-In Failed", "Error: ${e.statusCode}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, navigate to main activity
                    Log.d(TAG, "signInWithCredential-Google:success")
                    Toast.makeText(this, "Google Sign-In Successful.", Toast.LENGTH_SHORT).show()
                    goToEventList()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential-Google:failure", task.exception)
                    showAlertDialog("Authentication Failed", "Could not sign in with Google.")
                }
            }
    }

    private fun signInWithEmail(email: String, password: String) {
        // (Your existing email/password sign-in logic)
        if (email.isEmpty() || password.isEmpty()) {
            showAlertDialog("Error", "Por favor, complete todos los campos")
            return
        }
        // ... (rest of your validation)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    // Navigate to MainActivity
                    //goToMainActivity()
                    // Navigate to EventList
                    goToEventList()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    showAlertDialog("Authentication Failed", "Error: ${task.exception?.message}")
                }
            }
    }

//    private fun goToMainActivity() {
//        val intent = Intent(this, MainActivity::class.java)
//        startActivity(intent)
//        finish() // Finish LoginActivity so the user can't go back
//    }

    private fun goToEventList(){
        val intent = Intent(this, TaskListActivity::class.java)
        startActivity(intent)
        finish() // Finish LoginActivity so the user can't go back
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}