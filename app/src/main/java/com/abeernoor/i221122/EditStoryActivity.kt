package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
// import com.google.firebase.storage.FirebaseStorage
// import com.google.firebase.firestore.FirebaseFirestore

class EditStoryActivity : AppCompatActivity() {

    // private val storage = FirebaseStorage.getInstance()
    // private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_story)

        val sessionManager = SessionManager(this)
        val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val storyPreview = findViewById<ImageView>(R.id.storyPreview)
        val videoPreview = findViewById<VideoView>(R.id.videoPreview)
        val closeButton = findViewById<ImageView>(R.id.closeButton)
        val postButton = findViewById<TextView>(R.id.postButton)

        val imageBase64 = intent.getStringExtra("image")
        val videoUriString = intent.getStringExtra("videoUri")

        if (imageBase64 != null) {
            try {
                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                storyPreview.setImageBitmap(bitmap)
                storyPreview.visibility = View.VISIBLE
                videoPreview.visibility = View.GONE
            } catch (e: Exception) {
                storyPreview.setImageResource(R.drawable.profile_placeholder)
                Log.e("EditStoryActivity", "Failed to display image", e)
            }
        } else if (videoUriString != null) {
            val videoUri = Uri.parse(videoUriString)
            storyPreview.visibility = View.GONE
            videoPreview.visibility = View.VISIBLE
            videoPreview.setVideoURI(videoUri)
            videoPreview.start()
        } else {
            Toast.makeText(this, "No media to display", Toast.LENGTH_SHORT).show()
            finish()
        }

        closeButton.setOnClickListener {
            finish()
        }

        postButton.setOnClickListener {
            if (imageBase64 != null) {
                postStory(currentUserId, imageBase64, false)
            } else if (videoUriString != null) {
                try {
                    val videoUri = Uri.parse(videoUriString)
                    val inputStream = contentResolver.openInputStream(videoUri)
                    val tempFile = File.createTempFile("story", ".mp4", cacheDir)
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    val videoBytes = tempFile.readBytes()
                    val videoBase64 = Base64.encodeToString(videoBytes, Base64.DEFAULT)
                    postStory(currentUserId, videoBase64, true)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to process video: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("EditStoryActivity", "Error processing video", e)
                }
            }

            /*
            // Firebase logic for posting story
            if (imageBase64 != null) {
                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val storageRef = storage.reference.child("stories/$currentUserId/${System.currentTimeMillis()}.jpg")
                storageRef.putBytes(decodedBytes)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            val storyData = hashMapOf(
                                "user_id" to currentUserId.toString(),
                                "story_content" to uri.toString(),
                                "is_video" to false,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("stories").add(storyData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Story posted", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to post story: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else if (videoUriString != null) {
                val videoUri = Uri.parse(videoUriString)
                val storageRef = storage.reference.child("stories/$currentUserId/${System.currentTimeMillis()}.mp4")
                storageRef.putFile(videoUri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            val storyData = hashMapOf(
                                "user_id" to currentUserId.toString(),
                                "story_content" to uri.toString(),
                                "is_video" to true,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("stories").add(storyData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Story posted", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to post story: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            */
        }
    }

    private fun postStory(userId: Int, content: String, isVideo: Boolean) {
        val requestQueue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.11/ConnectMe/Story/postStory.php"
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
            put("story_content", content)
            put("is_video", isVideo)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Story posted", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to post story: ${response.optString("message")}", Toast.LENGTH_SHORT).show()
                    Log.e("EditStoryActivity", "Failed to post story: ${response.optString("message")}")
                }
            },
            { error ->
                Toast.makeText(this, "Error posting story: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("EditStoryActivity", "Volley error: ${error.message}")
            }
        )
        requestQueue.add(request)
    }
}