package com.abeernoor.i221122

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity: AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)
        auth = FirebaseAuth.getInstance()
        database=FirebaseDatabase.getInstance()
        val emailField: EditText = findViewById(R.id.email)
        val passwordField: EditText = findViewById(R.id.password)
        val nameField: EditText = findViewById(R.id.name)
        val userNameField: EditText = findViewById(R.id.username)
        val phoneNumberField: EditText = findViewById(R.id.phoneNum)
        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            val email=emailField.text.toString().trim()
            val password=passwordField.text.toString().trim()
            val name=nameField.text.toString().trim()
            val userName=userNameField.text.toString().trim()
            val phoneNumber=phoneNumberField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{task ->
                if (task.isSuccessful){
                    val userID=auth.currentUser?.uid
                    if(userID!=null){
                        val user =User(
                            userId= userID,
                            name = name,
                            username = userName,
                            email = email,
                            phoneNumber = phoneNumber
                        )
                        val databaseRef=database.getReference("Users")
                        databaseRef.child(userID).setValue(user).addOnCompleteListener {
                            if(it.isSuccessful){
                                Toast.makeText(this,"User registered successfully",Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this,EditProfileActivity::class.java))
                                finish()
                            }else{
                                Toast.makeText(
                                    this,
                                    "Failed to save user data: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()                            }
                        }
                    }
                }
            }
        }
    }

}

