package com.abeernoor.i221122

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {
    private var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val sessionManager = SessionManager(this)

        // UI Elements
        val doneButton: TextView = findViewById(R.id.doneButton)
        val tvUsername: TextView = findViewById(R.id.tvUsername)
        val etName: EditText = findViewById(R.id.etName)
        val etUsername: EditText = findViewById(R.id.etUsername)
        val etContact: EditText = findViewById(R.id.etContact)
        val etBio: EditText = findViewById(R.id.etBio)
        val profileImage: ImageView = findViewById(R.id.profileImage)
        val editProfileImage: ImageView = findViewById(R.id.editProfileImage)

        // Get current user ID
        val currentUserId = sessionManager.getUserId() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Load user data from web API
        val requestQueue = Volley.newRequestQueue(this)
        val getProfileUrl = "http://192.168.1.11/ConnectMe/Profile/getProfile.php" // Aligned with signUp.php
        val getProfileJson = JSONObject().apply {
            put("user_id", currentUserId)
        }

        val getProfileRequest = JsonObjectRequest(
            Request.Method.POST, getProfileUrl, getProfileJson,
            { response ->
                Log.d("EditProfileActivity", "Get profile response: $response")
                if (response.getBoolean("success")) {
                    val user = response.getJSONObject("user")
                    val username = user.optString("username", sessionManager.getUsername() ?: "")
                    val name = user.optString("name", "")
                    val contactNumber = user.optString("phone", "")
                    val bio = user.optString("bio", "")
                    val image = user.optString("profile_image", "")

                    tvUsername.text = username
                    etName.setText(name)
                    etUsername.setText(username)
                    etContact.setText(contactNumber)
                    etBio.setText(bio)

                    // Load profile image
                    if (image.isNotEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            profileImage.setImageBitmap(bitmap)
                            selectedImageBitmap = bitmap
                        } catch (e: Exception) {
                            Log.e("EditProfileActivity", "Error decoding image: ${e.message}")
                            profileImage.setImageResource(R.drawable.profile_placeholder)
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.profile_placeholder)
                    }
                } else {
                    Toast.makeText(this, response.optString("message", "User data not found"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("EditProfileActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to load profile"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(getProfileRequest)

        // Image picker launcher
        val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageBitmap = uriToBitmap(this, it)
                selectedImageBitmap?.let { bitmap ->
                    profileImage.setImageBitmap(bitmap)
                } ?: Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle image edit click
        editProfileImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // Handle Done button click
        doneButton.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newUsername = etUsername.text.toString().trim()
            val newContact = etContact.text.toString().trim()
            val newBio = etBio.text.toString().trim()

            // Validation
            if (newUsername.isEmpty()) {
                etUsername.error = "Username cannot be empty"
                return@setOnClickListener
            }

            // Prepare updated data
            val updateJson = JSONObject().apply {
                put("user_id", currentUserId)
                put("name", newName)
                put("username", newUsername)
                put("phone", newContact)
                put("bio", newBio)
                selectedImageBitmap?.let {
                    put("profile_image", bitmapToBase64(it))
                }
            }

            // Update profile via web API
            val updateProfileUrl = "http://192.168.1.11/ConnectMe/Profile/updateProfile.php"
            val updateProfileRequest = JsonObjectRequest(
                Request.Method.POST, updateProfileUrl, updateJson,
                { response ->
                    Log.d("EditProfileActivity", "Update profile response: $response")
                    if (response.getBoolean("success")) {
                        sessionManager.markProfileSetupComplete()
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, response.optString("message", "Failed to update profile"), Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Log.e("EditProfileActivity", "Volley error: ${error.message}")
                    Toast.makeText(this, "Error: ${error.message ?: "Failed to update profile"}", Toast.LENGTH_SHORT).show()
                }
            )
            requestQueue.add(updateProfileRequest)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}