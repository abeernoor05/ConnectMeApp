package com.abeernoor.i221122

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class NextActivity : AppCompatActivity() {

    private var selectedImageBitmap: Bitmap? = null
    private val TAG = "NextActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)
        Log.d(TAG, "onCreate called")

        val imageUriString = intent.getStringExtra("image_uri")
        Log.d(TAG, "Received imageUri: $imageUriString")
        val imageBase64 = if (imageUriString != null) {
            try {
                val uri = Uri.parse(imageUriString)
                selectedImageBitmap = uriToBitmap(uri)
                selectedImageBitmap?.let { bitmapToBase64(it) } ?: run {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Failed to decode bitmap from URI")
                    ""
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error processing URI: ${e.message}", e)
                ""
            }
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_LONG).show()
            Log.e(TAG, "No image URI provided")
            ""
        }

        // Display the selected image in the RecyclerView
        val images = if (imageBase64.isNotEmpty()) listOf(imageBase64) else emptyList()
        val recyclerViewImages = findViewById<RecyclerView>(R.id.recyclerViewImages)
        recyclerViewImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewImages.adapter = ImagePreviewAdapter(images)
        Log.d(TAG, "RecyclerView set with ${images.size} images")

        val closeButton = findViewById<ImageView>(R.id.closeNext)
        val shareButton = findViewById<Button>(R.id.btnShare)
        val captionEditText = findViewById<EditText>(R.id.etCaption)

        closeButton.setOnClickListener {
            Log.d(TAG, "Close button clicked")
            finish()
        }

        shareButton.setOnClickListener {
            Log.d(TAG, "Share button clicked")
            if (imageBase64.isEmpty()) {
                Toast.makeText(this, "No valid image to share", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Cannot share: empty imageBase64")
                return@setOnClickListener
            }

            val caption = captionEditText.text.toString()
            val sessionManager = SessionManager(this)
            val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: run {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
                Log.e(TAG, "No user logged in")
                return@setOnClickListener
            }

            val requestQueue = Volley.newRequestQueue(this)
            val url = "http://192.168.1.11/ConnectMe/Profile/postPost.php"
            val jsonBody = JSONObject().apply {
                put("user_id", currentUserId)
                put("post_image", imageBase64)
                put("caption", caption)
            }

            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    if (response.getBoolean("success")) {
                        Toast.makeText(this, "Post shared successfully", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Post shared successfully")
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to share post: ${response.optString("message")}", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Failed to share post: ${response.optString("message")}")
                    }
                },
                { error ->
                    Toast.makeText(this, "Error sharing post: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Volley error: ${error.message}")
                }
            )
            requestQueue.add(request)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        // Recycle bitmap to free memory
        selectedImageBitmap?.recycle()
        selectedImageBitmap = null
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: IOException) {
            Log.e(TAG, "Error decoding bitmap: ${e.message}", e)
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}