package com.example.gestioneventosdsm.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gestioneventosdsm.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val nameEditText = findViewById<EditText>(R.id.registerNameEditText)
        val emailEditText = findViewById<EditText>(R.id.registerEmailEditText)
        val passwordEditText = findViewById<EditText>(R.id.registerPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validaciones
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showAlertDialog("Error", "Por favor, complete todos los campos.")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showAlertDialog("Error", "La contraseña debe tener al menos 6 caracteres.")
                return@setOnClickListener
            }

            // Crear el usuario en Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser

                        // Añadir el nombre al perfil del usuario
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d(TAG, "User profile updated.")
                            }
                        }

                        // Opcional: Enviar correo de verificación
                        user?.sendEmailVerification()

                        Toast.makeText(baseContext, "Cuenta creada con éxito. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()

                        // Redirigir al Login para que el usuario inicie sesión
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()

                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        showAlertDialog("Fallo en el registro", "Error: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
