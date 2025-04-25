package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class EditStoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_story)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        val storyPreview = findViewById<ImageView>(R.id.storyPreview)
        val videoPreview = findViewById<VideoView>(R.id.videoPreview)
        val closeButton = findViewById<ImageView>(R.id.closeButton)
        val postButton = findViewById<TextView>(R.id.postButton)

        val imageBase64 = intent.getStringExtra("image")
        val videoUriString = intent.getStringExtra("videoUri")

        if (imageBase64 != null) {
            // Display image
            try {
                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                storyPreview.setImageBitmap(bitmap)
                storyPreview.visibility = android.view.View.VISIBLE
                videoPreview.visibility = android.view.View.GONE
            } catch (e: Exception) {
                storyPreview.setImageResource(R.drawable.profile_placeholder)
                Log.e("EditStoryActivity", "Failed to display image", e)
            }
        } else if (videoUriString != null) {
            // Display video
            val videoUri = Uri.parse(videoUriString)
            storyPreview.visibility = android.view.View.GONE
            videoPreview.visibility = android.view.View.VISIBLE
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
            val currentUserId = auth.currentUser?.uid ?: run {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (imageBase64 != null) {
                // Save image story
                val storyData = mapOf(
                    "image" to imageBase64,
                    "timestamp" to System.currentTimeMillis()
                )
                val storyRef = database.getReference("Stories").child(currentUserId).push()
                storyRef.setValue(storyData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Story posted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to post story: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("EditStoryActivity", "Failed to post image story", e)
                    }
            } else if (videoUriString != null) {
                // Upload video to Firebase Storage
                val videoUri = Uri.parse(videoUriString)
                val storageRef = storage.reference.child("stories/$currentUserId/${System.currentTimeMillis()}.mp4")
                storageRef.putFile(videoUri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            val storyData = mapOf(
                                "videoUrl" to downloadUri.toString(),
                                "timestamp" to System.currentTimeMillis()
                            )
                            val storyRef = database.getReference("Stories").child(currentUserId).push()
                            storyRef.setValue(storyData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Story posted", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to post story: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("EditStoryActivity", "Failed to post video story", e)
                                }
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to get video URL: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("EditStoryActivity", "Failed to get video URL", e)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to upload video: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("EditStoryActivity", "Failed to upload video", e)
                    }
            }
        }
    }
}