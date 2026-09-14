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

/**
 * AuthScreen handles user authentication including Sign In and Sign Up
 * using Firebase Authentication, initializes user profile and audit logs in Cloud Firestore,
 * and handles secure navigation to the MainScreen.
 */
class AuthScreen : AppCompatActivity() {

    // Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "AuthScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout styling
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth_screen)

        // Adjust padding to fit system bars (status & navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Authentication
        auth = Firebase.auth

        // Cache view references
        val emailInput = findViewById<EditText>(R.id.Email)
        val passwordInput = findViewById<EditText>(R.id.Password)
        val signInBtn = findViewById<Button>(R.id.SignInButton)
        val signUpBtn = findViewById<Button>(R.id.SignUpButton)

        // Handle Sign In button click
        signInBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (validateInputs(email, password)) {
                signIn(email, password)
            }
        }

        // Handle Sign Up button click
        signUpBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (validateInputs(email, password)) {
                signUp(email, password)
            }
        }
    }

    /**
     * Validates that email and password fields are non-empty before proceeding with authentication.
     * Displays a Toast message if validation fails.
     */
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

    /**
     * Registers a new user with Firebase Auth. Upon success, creates a default user document
     * (with daily calorie and macronutrient goals) and an audit log in Cloud Firestore,
     * then navigates to the MainScreen.
     */
    private fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener
                    val db = Firebase.firestore

                    // Prepare initial user profile data with default diet goals
                    val userData = hashMapOf(
                        "DisplayName" to (user.email ?: ""),
                        "DailyCalorieGoal" to 1919,
                        "MacroGoal" to hashMapOf(
                            "Carb" to 120,
                            "Fat" to 30,
                            "Protein" to 100,
                        ),
                    )

                    // Write audit log to Firestore logs collection
                    val logData = hashMapOf("action" to "signUp", "time" to Timestamp.now())
                    db.collection("logs").add(logData)

                    // Write user profile document to Firestore users collection
                    db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d(TAG, "DocumentSnapshot added with ID: $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                        }

                    // Navigate to MainScreen
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

    /**
     * Authenticates an existing user with Firebase Auth. Upon success, logs the sign-in
     * action to Cloud Firestore and navigates to the MainScreen.
     */
    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")

                    // Write audit log to Firestore logs collection
                    val logData = hashMapOf("action" to "signin", "time" to Timestamp.now())
                    val db = Firebase.firestore
                    db.collection("logs").add(logData)

                    // Navigate to MainScreen
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

    /**
     * Navigates to MainScreen and clears the Activity backstack so the user
     * cannot press the Back button to return to the AuthScreen.
     */
    private fun navigateToMainScreen() {
        val intent = Intent(this, MainScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
