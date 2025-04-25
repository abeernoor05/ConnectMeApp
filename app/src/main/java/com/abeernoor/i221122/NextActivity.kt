package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NextActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val closeButton = findViewById<ImageView>(R.id.closeNext)
        val shareButton = findViewById<Button>(R.id.btnShare)
        val captionEditText = findViewById<EditText>(R.id.etCaption)
        val recyclerViewImages = findViewById<RecyclerView>(R.id.recyclerViewImages)

        val imageBase64 = intent.getStringExtra("image") ?: ""
        if (imageBase64.isEmpty()) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Display the selected image in the RecyclerView
        val images = listOf(imageBase64)
        recyclerViewImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewImages.adapter = ImagePreviewAdapter(images)

        closeButton.setOnClickListener {
            finish()
        }

        shareButton.setOnClickListener {
            val caption = captionEditText.text.toString()
            val currentUserId = auth.currentUser?.uid ?: run {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Fetch the user's username and profile image
            database.getReference("Users").child(currentUserId).get().addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                val profileImage = snapshot.child("image").getValue(String::class.java) ?: ""

                // Create a new post
                val postRef = database.getReference("Posts").push()
                val postId = postRef.key ?: return@addOnSuccessListener
                val post = Post(
                    postId = postId,
                    userId = currentUserId,
                    username = username,
                    profileImage = profileImage,
                    postImage = imageBase64,
                    caption = caption,
                    timestamp = System.currentTimeMillis()
                )


                // Save the post to Firebase
                postRef.setValue(post).addOnSuccessListener {
                    Toast.makeText(this, "Post shared successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to share post: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}