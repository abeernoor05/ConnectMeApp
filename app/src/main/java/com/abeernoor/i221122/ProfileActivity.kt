package com.abeernoor.i221122

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject

class ProfileActivity : AppCompatActivity() {
    private lateinit var profilePic: ImageView
    private lateinit var username: TextView
    private lateinit var bio: TextView
    private lateinit var postCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: postAdapter
    private val postsList = mutableListOf<Post>()
    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        Log.d(TAG, "onCreate called")

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
        recyclerView = findViewById(R.id.recyclerViewPosts)

        // RecyclerView setup
        recyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = postAdapter(postsList)
        recyclerView.adapter = postAdapter

        // Get current user ID
        val currentUserId = sessionManager.getUserId() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Load initial data
        loadUserData(currentUserId)

        // Activity result launcher for EditProfileActivity
        val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadUserData(currentUserId) // Reload user data after editing
            }
        }

        // Navigation setup
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    Log.d(TAG, "Nav profile selected")
                    true
                }
                R.id.nav_search -> {
                    Log.d(TAG, "Nav search selected")
                    startActivity(Intent(this, SearchActivity::class.java))
                    true
                }
                R.id.nav_post -> {
                    Log.d(TAG, "Nav post selected")
                    startActivity(Intent(this, PostActivity::class.java))
                    true
                }
                R.id.nav_contacts -> {
                    Log.d(TAG, "Nav contacts selected")
                    startActivity(Intent(this, ContactsActivity::class.java))
                    true
                }
                R.id.nav_home -> {
                    Log.d(TAG, "Nav home selected")
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }

        followersCount.setOnClickListener {
            Log.d(TAG, "Followers count clicked")
            startActivity(Intent(this, FollowersActivity::class.java))
        }
        followingCount.setOnClickListener {
            Log.d(TAG, "Following count clicked")
            startActivity(Intent(this, FollowingActivity::class.java))
        }

        editIcon.setOnClickListener {
            Log.d(TAG, "Edit icon clicked")
            editProfileLauncher.launch(Intent(this, EditProfileActivity::class.java))
        }
    }

    private fun loadUserData(userId: String) {
        postsList.clear()
        postAdapter.notifyDataSetChanged()
        val requestQueue = Volley.newRequestQueue(this)
        val getProfileUrl = "http://192.168.1.11/ConnectMe/Profile/getProfile.php"
        val getProfileJson = JSONObject().apply {
            put("user_id", userId)
        }

        val getProfileRequest = JsonObjectRequest(
            Request.Method.POST, getProfileUrl, getProfileJson,
            { response ->
                Log.d(TAG, "Get profile response: $response")
                if (response.getBoolean("success")) {
                    // Update user profile
                    val user = response.getJSONObject("user")
                    val usernameStr = user.optString("username", "Unknown")
                    val bioStr = user.optString("bio", "No bio yet")
                    val image = user.optString("profile_image", "")
                    val followerCountValue = user.optInt("follower_count", 0)
                    val followingCountValue = user.optInt("following_count", 0)

                    username.text = usernameStr
                    bio.text = bioStr
                    followersCount.text = followerCountValue.toString()
                    followingCount.text = followingCountValue.toString()

                    try {
                        if (image.isNotEmpty()) {
                            val decodedBytes = Base64.decode(image, Base64.DEFAULT)
                            val bitmap = decodeBitmap(decodedBytes)
                            if (bitmap != null) {
                                profilePic.setImageBitmap(bitmap)
                                Log.d(TAG, "Profile image loaded")
                            } else {
                                profilePic.setImageResource(R.drawable.profile_placeholder)
                                Log.e(TAG, "Failed to decode profile image")
                            }
                        } else {
                            profilePic.setImageResource(R.drawable.profile_placeholder)
                            Log.d(TAG, "No profile image")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decoding profile image: ${e.message}", e)
                        profilePic.setImageResource(R.drawable.profile_placeholder)
                    }

                    // Load posts with fallback
                    try {
                        val postsArray = response.optJSONArray("posts")
                        if (postsArray != null) {
                            for (i in 0 until postsArray.length()) {
                                val postJson = postsArray.getJSONObject(i)
                                val likesArray = postJson.getJSONArray("likes")
                                val likes = mutableListOf<String>()
                                for (j in 0 until likesArray.length()) {
                                    likes.add(likesArray.getString(j))
                                }
                                val commentsArray = postJson.getJSONArray("comments")
                                val comments = mutableListOf<Comment>()
                                for (j in 0 until commentsArray.length()) {
                                    val commentJson = commentsArray.getJSONObject(j)
                                    comments.add(
                                        Comment(
                                            commentId = commentJson.getString("comment_id"),
                                            userId = commentJson.getString("user_id"),
                                            username = commentJson.getString("username"),
                                            text = commentJson.getString("text"),
                                            timestamp = commentJson.getLong("timestamp")
                                        )
                                    )
                                }
                                val post = Post(
                                    postId = postJson.getString("post_id"),
                                    userId = postJson.getString("user_id"),
                                    username = postJson.getString("username"),
                                    profileImage = postJson.getString("profile_image"),
                                    postImage = postJson.getString("post_image"),
                                    caption = postJson.getString("caption"),
                                    timestamp = postJson.getLong("timestamp"),
                                    likes = likes,
                                    comments = comments
                                )
                                postsList.add(post)
                                Log.d(TAG, "Added post $i: postId=${post.postId}")
                            }
                            Log.d(TAG, "Loaded ${postsList.size} posts")
                        } else {
                            Log.w(TAG, "No posts array in response")
                        }
                        postCount.text = postsList.size.toString()
                        postAdapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing posts: ${e.message}", e)
                        postCount.text = "0"
                        postAdapter.notifyDataSetChanged()
                    }
                } else {
                    val message = response.optString("message", "User data not found")
                    Log.e(TAG, "Profile load failed: $message")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    postCount.text = "0"
                    postAdapter.notifyDataSetChanged()
                }
            },
            { error ->
                val errorMessage = error.message ?: "Failed to load profile"
                val errorData = String(error.networkResponse?.data ?: byteArrayOf())
                Log.e(TAG, "Volley error: $errorMessage, data: $errorData")
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                postCount.text = "0"
                postAdapter.notifyDataSetChanged()
            }
        )
        requestQueue.add(getProfileRequest)
    }

    private fun decodeBitmap(byteArray: ByteArray): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, this)
                inSampleSize = calculateInSampleSize(this, 1024, 1024)
                inJustDecodeBounds = false
            }
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap: ${e.message}", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
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