package com.abeernoor.i221122

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
// import com.google.firebase.storage.FirebaseStorage
// import com.google.firebase.firestore.FirebaseFirestore

class AddStoryActivity : AppCompatActivity() {

    private var selectedImageBitmap: Bitmap? = null
    private var selectedVideoUri: Uri? = null
    // private val storage = FirebaseStorage.getInstance()
    // private val db = FirebaseFirestore.getInstance()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            launchMediaPicker()
        } else {
            Toast.makeText(this, "Permission denied. Cannot access media.", Toast.LENGTH_SHORT).show()
        }
    }

    private val mediaPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                Log.d("AddStoryActivity", "Selected URI: $uri")
                val mimeType = contentResolver.getType(uri)
                Log.d("AddStoryActivity", "MIME Type: $mimeType")
                if (mimeType?.startsWith("video/") == true) {
                    selectedVideoUri = uri
                    selectedImageBitmap = null
                    val storyPreview = findViewById<ImageView>(R.id.storyPreview)
                    val videoPreview = findViewById<VideoView>(R.id.videoPreview)
                    storyPreview.visibility = View.GONE
                    videoPreview.visibility = View.VISIBLE
                    videoPreview.setVideoURI(uri)
                    videoPreview.start()
                } else {
                    selectedImageBitmap = uriToBitmap(this, uri)
                    selectedVideoUri = null
                    val storyPreview = findViewById<ImageView>(R.id.storyPreview)
                    val videoPreview = findViewById<VideoView>(R.id.videoPreview)
                    storyPreview.visibility = View.VISIBLE
                    videoPreview.visibility = View.GONE
                    selectedImageBitmap?.let { bitmap ->
                        storyPreview.setImageBitmap(bitmap)
                    } ?: run {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_story)

        val sessionManager = SessionManager(this)
        val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val storyPreview = findViewById<ImageView>(R.id.storyPreview)
        val videoPreview = findViewById<VideoView>(R.id.videoPreview)
        val closeButton = findViewById<ImageView>(R.id.closeButton)
        val nextButton = findViewById<TextView>(R.id.nextButton)
        val galleryButton = findViewById<ImageView>(R.id.galleryButton)
        val postButton = findViewById<Button>(R.id.postButton)
        val storyButton = findViewById<Button>(R.id.storyButton)

        galleryButton.setOnClickListener {
            checkPermissionsAndLaunchPicker()
        }

        closeButton.setOnClickListener {
            finish()
        }

        nextButton.setOnClickListener {
            if (selectedImageBitmap != null || selectedVideoUri != null) {
                val intent = Intent(this, EditStoryActivity::class.java)
                if (selectedImageBitmap != null) {
                    val base64Image = bitmapToBase64(selectedImageBitmap!!)
                    intent.putExtra("image", base64Image)
                } else if (selectedVideoUri != null) {
                    intent.putExtra("videoUri", selectedVideoUri.toString())
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please select an image or video", Toast.LENGTH_SHORT).show()
            }

            /*
            // Firebase logic for uploading story
            if (selectedImageBitmap != null) {
                val baos = ByteArrayOutputStream()
                selectedImageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()
                val storageRef = storage.reference.child("stories/$currentUserId/${System.currentTimeMillis()}.jpg")
                storageRef.putBytes(data)
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
                        Toast.makeText(this, "Failed to upload story: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else if (selectedVideoUri != null) {
                val storageRef = storage.reference.child("stories/$currentUserId/${System.currentTimeMillis()}.mp4")
                storageRef.putFile(selectedVideoUri!!)
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
                        Toast.makeText(this, "Failed to upload story: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            */
        }

        postButton.setOnClickListener {
            startActivity(Intent(this, PostActivity::class.java))
            finish()
        }

        storyButton.setOnClickListener {
            // Already in story mode
        }
    }

    private fun checkPermissionsAndLaunchPicker() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            launchMediaPicker()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun launchMediaPicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        mediaPicker.launch(intent)
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