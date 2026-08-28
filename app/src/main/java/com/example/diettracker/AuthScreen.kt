package com.example.diettracker

import android.content.ContentValues.TAG
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class AuthScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the button from the layout
        val signInBtn = findViewById<Button>(R.id.SignInButton)
        val signUpBtn = findViewById<Button>(R.id.SignUpButton)
        auth = Firebase.auth
        signInBtn.setOnClickListener {
            signIn(findViewById<EditText>(R.id.Email).getText().toString(), findViewById<EditText>(R.id.Password).getText().toString())
        }
        signUpBtn.setOnClickListener {
            signUp(findViewById<EditText>(R.id.Email).getText().toString(), findViewById<EditText>(R.id.Password).getText().toString())

        }


    }
    private fun signUp(email: String, password: String) {
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    val db = Firebase.firestore

                    val userData = hashMapOf(
                        "DisplayName" to user?.email.toString(),
                        "DailyCalorieGoal" to 1919,
                        "MacroGoal" to hashMapOf(
                            "Carb" to 120,
                            "Fat" to 30,
                            "Protein" to 100
                        )
                    )

                    // Add a new document with a generated ID
                    db.collection("users").document(user!!.uid)
                        .set(userData)
                        .addOnSuccessListener { documentReference ->
                            Log.d("Firestore", "DocumentSnapshot added with ID: ${user.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error adding document", e)
                        }


                    val intent = Intent(this, MainScreen::class.java)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        // [END create_user_with_email]
    }

    private fun signIn(email: String, password: String) {
        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    val intent = Intent(this, MainScreen::class.java)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        // [END sign_in_with_email]
    }
}