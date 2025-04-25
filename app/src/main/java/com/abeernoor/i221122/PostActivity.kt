package com.abeernoor.i221122

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream

class PostActivity : AppCompatActivity() {

    private var selectedImageBitmap: Bitmap? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            launchImagePicker()
        } else {
            Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show()
        }
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                Log.d("PostActivity", "Selected URI: $uri")
                selectedImageBitmap = uriToBitmap(uri)
                selectedImageBitmap?.let { bitmap ->
                    val selectedImage = findViewById<ImageView>(R.id.selectedImage)
                    selectedImage.setImageBitmap(bitmap)
                } ?: run {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

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
        val iconExtra1 = findViewById<ImageView>(R.id.iconExtra1) // Camera icon for gallery

        iconExtra1.setOnClickListener {
            checkPermissionsAndLaunchPicker()
        }

        closeButton.setOnClickListener {
            finish()
        }

        nextButton.setOnClickListener {
            if (selectedImageBitmap != null) {
                val base64Image = bitmapToBase64(selectedImageBitmap!!)
                val intent = Intent(this, NextActivity::class.java)
                intent.putExtra("image", base64Image)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            }
        }
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
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePicker.launch(intent)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}