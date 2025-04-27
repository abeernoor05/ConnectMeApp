package com.abeernoor.i221122

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import kotlin.math.min

class ProfileActivity : AppCompatActivity() {
    // Commented out Firebase variables
    /*
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    */

    private lateinit var profilePic: ImageView
    private lateinit var username: TextView
    private lateinit var bio: TextView
    private lateinit var postCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var postImages: List<ImageView>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Commented out Firebase initialization
        /*
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        */

        val sessionManager = SessionManager(this)

        // UI Elements
        profilePic = findViewById(R.id.profilePic)
        username = findViewById(R.id.username)
        bio = findViewById(R.id.bio)
        postCount = findViewById(R.id.postCount)
        followersCount = findViewById(R.id.followersCount)
        followingCount = findViewById(R.id.followingCount)
        val editIcon: ImageView = findViewById(R.id.editIcon)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        postImages = listOf(
            findViewById(R.id.post1),
            findViewById(R.id.post2),
            findViewById(R.id.post3),
            findViewById(R.id.post4),
            findViewById(R.id.post5),
            findViewById(R.id.post6)
        )

        // Get current user ID
        val currentUserId = sessionManager.getUserId() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Load initial data
        loadUserData(currentUserId)
        loadPosts(currentUserId)

        // Activity result launcher for EditProfileActivity
        val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadUserData(currentUserId) // Reload user data after editing
                loadPosts(currentUserId)    // Reload posts if needed
            }
        }

        // Navigation setup
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    true
                }
                R.id.nav_post -> {
                    startActivity(Intent(this, PostActivity::class.java))
                    true
                }
                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }

        followersCount.setOnClickListener {
            startActivity(Intent(this, FollowersActivity::class.java))
        }
        followingCount.setOnClickListener {
            startActivity(Intent(this, FollowingActivity::class.java))
        }

        editIcon.setOnClickListener {
            editProfileLauncher.launch(Intent(this, EditProfileActivity::class.java))
        }
    }

    private fun loadUserData(userId: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val getProfileUrl = "http://192.168.1.11/ConnectMe/Profile/getProfile.php"
        val getProfileJson = JSONObject().apply {
            put("user_id", userId)
        }

        val getProfileRequest = JsonObjectRequest(
            Request.Method.POST, getProfileUrl, getProfileJson,
            { response ->
                Log.d("ProfileActivity", "Get profile response: $response")
                if (response.getBoolean("success")) {
                    val user = response.getJSONObject("user")
                    val usernameStr = user.optString("username", "Unknown")
                    val bioStr = user.optString("bio", "No bio yet")
                    val image = user.optString("profile_image", "")
                    val followerCount = user.optInt("follower_count", 0)
                    val followingCount = user.optInt("following_count", 0)

                    username.text = usernameStr
                    bio.text = bioStr
                    followersCount.text = followerCount.toString()
                    this.followingCount.text = followingCount.toString()

                    if (image.isNotEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            profilePic.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("ProfileActivity", "Error decoding image: ${e.message}")
                            profilePic.setImageResource(R.drawable.profile_placeholder)
                        }
                    } else {
                        profilePic.setImageResource(R.drawable.profile_placeholder)
                    }
                } else {
                    Toast.makeText(this, response.optString("message", "User data not found"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to load profile"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(getProfileRequest)
    }

    private fun loadPosts(userId: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val getPostsUrl = "http://192.168.1.11/ConnectMe/Profile/getPosts.php"
        val getPostsJson = JSONObject().apply {
            put("user_id", userId)
        }

        val getPostsRequest = JsonObjectRequest(
            Request.Method.POST, getPostsUrl, getPostsJson,
            { response ->
                Log.d("ProfileActivity", "Get posts response: $response")
                if (response.getBoolean("success")) {
                    val postsArray = response.getJSONArray("posts")
                    val posts = mutableListOf<String>()
                    for (i in 0 until postsArray.length()) {
                        val post = postsArray.getJSONObject(i)
                        val imageUrl = post.optString("image_url", "")
                        if (imageUrl.isNotEmpty()) {
                            posts.add(imageUrl)
                        }
                    }

                    postCount.text = posts.size.toString()

                    for (i in 0 until min(posts.size, 6)) {
                        try {
                            val decodedBytes = Base64.decode(posts[i], Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            postImages[i].setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("ProfileActivity", "Error decoding post image: ${e.message}")
                            postImages[i].setImageResource(R.drawable.post_placeholder)
                        }
                    }
                    for (i in posts.size until 6) {
                        postImages[i].setImageResource(R.drawable.post_placeholder)
                    }
                } else {
                    Toast.makeText(this, response.optString("message", "No posts found"), Toast.LENGTH_SHORT).show()
                    postCount.text = "0"
                    postImages.forEach { it.setImageResource(R.drawable.post_placeholder) }
                }
            },
            { error ->
                Log.e("ProfileActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error loading posts: ${error.message ?: "Failed to load posts"}", Toast.LENGTH_SHORT).show()
                postCount.text = "0"
                postImages.forEach { it.setImageResource(R.drawable.post_placeholder) }
            }
        )
        requestQueue.add(getPostsRequest)
    }

    // Commented out Firebase methods
    /*
    private fun loadUserData(userId: String) {
        database.getReference("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        username.text = user.username
                        bio.text = if (user.bio.isNotEmpty()) user.bio else "No bio yet"
                        followersCount.text = user.followerCount.toString()
                        followingCount.text = user.followingCount.toString()

                        if (user.image.isNotEmpty()) {
                            try {
                                val decodedBytes = Base64.decode(user.image, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                profilePic.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                profilePic.setImageResource(R.drawable.profile_placeholder)
                            }
                        } else {
                            profilePic.setImageResource(R.drawable.profile_placeholder)
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadPosts(userId: String) {
        database.getReference("Posts").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val posts = mutableListOf<String>()
                    for (postSnapshot in snapshot.children) {
                        val imageUrl = postSnapshot.child("imageUrl").getValue(String::class.java)
                        if (imageUrl != null) {
                            posts.add(imageUrl)
                        }
                    }

                    postCount.text = posts.size.toString()

                    for (i in 0 until minOf(posts.size, 6)) {
                        try {
                            val decodedBytes = Base64.decode(posts[i], Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            postImages[i].setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            postImages[i].setImageResource(R.drawable.post_placeholder)
                        }
                    }
                    for (i in posts.size until 6) {
                        postImages[i].setImageResource(R.drawable.post_placeholder)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Error loading posts: ${error.message}", Toast.LENGTH_SHORT).show()
                    postCount.text = "0"
                    postImages.forEach { it.setImageResource(R.drawable.post_placeholder) }
                }
            })
    }
    */
}