package com.abeernoor.i221122

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.UUID

class RegisterActivity : AppCompatActivity() {
    // Commented out Firebase implementation
    /*
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
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        val sessionManager = SessionManager(this)
        val emailField: EditText = findViewById(R.id.email)
        val passwordField: EditText = findViewById(R.id.password)
        val nameField: EditText = findViewById(R.id.name)
        val userNameField: EditText = findViewById(R.id.username)
        val phoneNumberField: EditText = findViewById(R.id.phoneNum)
        val registerButton: Button = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val name = nameField.text.toString().trim()
            val username = userNameField.text.toString().trim()
            val phoneNumber = phoneNumberField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || phoneNumber.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = UUID.randomUUID().toString()
            val requestQueue = Volley.newRequestQueue(this)
            val url = "http://192.168.1.11/ConnectMe/Login_SignUp/signUp.php"
            val jsonBody = JSONObject().apply {
                put("id", userId)
                put("email", email)
                put("password", password)
                put("name", name)
                put("username", username)
                put("phone", phoneNumber)
            }

            val jsonRequest = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    if (response.getBoolean("success")) {
                        sessionManager.saveUserSession(userId, username)
                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, EditProfileActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
            requestQueue.add(jsonRequest)

            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }
}