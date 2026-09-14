package com.example.diettracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class AuthScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "AuthScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        val emailInput = findViewById<EditText>(R.id.Email)
        val passwordInput = findViewById<EditText>(R.id.Password)
        val signInBtn = findViewById<Button>(R.id.SignInButton)
        val signUpBtn = findViewById<Button>(R.id.SignUpButton)

        signInBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (validateInputs(email, password)) {
                signIn(email, password)
            }
        }

        signUpBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (validateInputs(email, password)) {
                signUp(email, password)
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener
                    val db = Firebase.firestore

                    val userData = hashMapOf(
                        "DisplayName" to (user.email ?: ""),
                        "DailyCalorieGoal" to 1919,
                        "MacroGoal" to hashMapOf(
                            "Carb" to 120,
                            "Fat" to 30,
                            "Protein" to 100,
                        ),
                    )

                    val logData = hashMapOf("action" to "signUp", "time" to Timestamp.now())
                    db.collection("logs").add(logData)

                    db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d(TAG, "DocumentSnapshot added with ID: $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                        }

                    navigateToMainScreen()
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val logData = hashMapOf("action" to "signin", "time" to Timestamp.now())
                    val db = Firebase.firestore
                    db.collection("logs").add(logData)

                    navigateToMainScreen()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun navigateToMainScreen() {
        val intent = Intent(this, MainScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
