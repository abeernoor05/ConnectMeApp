package com.abeernoor.i221122

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class PostActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private var selectedImageBitmap: Bitmap? = null
    private val TAG = "PostActivity"

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            launchImagePicker()
        } else {
            Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Permissions denied: $permissions")
        }
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "ImagePicker result: ${result.resultCode}, data: ${result.data}")
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            Log.d(TAG, "Selected URI: $uri")
            uri?.let {
                selectedImageUri = uri
                selectedImageBitmap = uriToBitmap(uri)
                selectedImageBitmap?.let { bitmap ->
                    val selectedImage = findViewById<ImageView>(R.id.selectedImage)
                    selectedImage.setImageBitmap(bitmap)
                    Log.d(TAG, "Image set successfully")
                } ?: run {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Failed to load bitmap from URI")
                }
            } ?: run {
                Toast.makeText(this, "No image selected", Toast.LENGTH_LONG).show()
                Log.e(TAG, "No URI provided")
            }
        } else {
            Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Image picker failed with result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        Log.d(TAG, "onCreate called")

        val images = listOf(
            R.drawable.post1, R.drawable.post2, R.drawable.post3, R.drawable.post4,
            R.drawable.post5, R.drawable.post6, R.drawable.post7, R.drawable.post8, R.drawable.post9,
            R.drawable.post10
        )

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        recyclerView.adapter = photoGridAdapter(images)

        val closeButton = findViewById<ImageView>(R.id.close)
        val nextButton = findViewById<TextView>(R.id.btnNext)
        val iconExtra1 = findViewById<ImageView>(R.id.iconExtra1)

        iconExtra1.setOnClickListener {
            Log.d(TAG, "Gallery icon clicked")
            checkPermissionsAndLaunchPicker()
        }

        closeButton.setOnClickListener {
            Log.d(TAG, "Close button clicked")
            finish()
        }

        nextButton.setOnClickListener {
            Log.d(TAG, "Next button clicked, selectedImageUri: $selectedImageUri, selectedImageBitmap: $selectedImageBitmap")
            if (selectedImageUri != null && selectedImageBitmap != null) {
                try {
                    val intent = Intent(this, NextActivity::class.java)
                    intent.putExtra("image_uri", selectedImageUri.toString())
                    startActivity(intent)
                    Log.d(TAG, "Intent started for NextActivity with URI: $selectedImageUri")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting NextActivity: ${e.message}", e)
                    Toast.makeText(this, "Error navigating to next screen: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_LONG).show()
                Log.e(TAG, "No image selected")
            }
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

    private fun checkPermissionsAndLaunchPicker() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            launchImagePicker()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun launchImagePicker() {
        Log.d(TAG, "Launching image picker")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePicker.launch(intent)
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
}