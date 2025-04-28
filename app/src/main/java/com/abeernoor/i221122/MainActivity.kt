package com.abeernoor.i221122

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
// import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {
    private lateinit var storiesAdapter: StoryAdapter
    private lateinit var postsAdapter: postAdapter
    private val storiesList = mutableListOf<story>()
    private val postsList = mutableListOf<Post>()
    private val TAG = "MainActivity"
    // private val db = FirebaseFirestore.getInstance()

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)
        Log.d(TAG, "onCreate called")

        val storiesRecyclerView = findViewById<RecyclerView>(R.id.storiesRecyclerView)
        val postsRecyclerView = findViewById<RecyclerView>(R.id.postsRecyclerView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val dmsIcon = findViewById<ImageView>(R.id.communityIcon)
        val notifIcon = findViewById<ImageView>(R.id.heartIcon)

        // Initialize SessionManager
        val sessionManager = SessionManager(this)
        val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Set online status
        updateOnlineStatus(currentUserId, "online")

        dmsIcon.setOnClickListener {
            Log.d(TAG, "DMS icon clicked")
            startActivity(Intent(this, DmsActivity::class.java))
        }

        logoutButton.setOnClickListener {
            Log.d(TAG, "Logout button clicked")
            updateOnlineStatus(currentUserId, "offline")
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        notifIcon.setOnClickListener {
            Log.d(TAG, "Notifications icon clicked")
            startActivity(Intent(this, FollowRequestsActivity::class.java))
        }

        // Stories setup
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        storiesAdapter = StoryAdapter(storiesList, this)
        storiesRecyclerView.adapter = storiesAdapter

        // Posts setup
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postsAdapter = postAdapter(postsList)
        postsRecyclerView.adapter = postsAdapter

        // Bottom navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d(TAG, "Nav home selected")
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
                R.id.nav_profile -> {
                    Log.d(TAG, "Nav profile selected")
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Load stories and posts
        loadStories(currentUserId)
        loadPosts(currentUserId)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        val sessionManager = SessionManager(this)
        sessionManager.getUserId()?.toIntOrNull()?.let { updateOnlineStatus(it, "offline") }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        val sessionManager = SessionManager(this)
        sessionManager.getUserId()?.toIntOrNull()?.let { userId ->
            updateOnlineStatus(userId, "online")
            loadStories(userId)
            loadPosts(userId)
        }
    }

    private fun updateOnlineStatus(userId: Int, status: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.11/ConnectMe/Story/updateOnlineStatus.php"
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
            put("status", status)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    Log.d(TAG, "Status updated: $status")
                } else {
                    Log.e(TAG, "Failed to update status: ${response.optString("message")}")
                }
            },
            { error ->
                Log.e(TAG, "Volley error updating status: ${error.message}")
            }
        )
        requestQueue.add(request)
    }

    private fun loadStories(userId: Int) {
        val requestQueue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.11/ConnectMe/Story/getStories.php"
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                Log.d(TAG, "Get stories response: $response")
                if (response.getBoolean("success")) {
                    storiesList.clear()
                    val storiesArray = response.getJSONArray("stories")
                    for (i in 0 until storiesArray.length()) {
                        val storyJson = storiesArray.getJSONObject(i)
                        val story = story(
                            userId = storyJson.getString("user_id"),
                            username = storyJson.getString("username"),
                            image = storyJson.getString("profile_image"),
                            storyContent = storyJson.getString("story_content"),
                            videoUrl = if (storyJson.getBoolean("is_video")) storyJson.getString("story_content") else "",
                            timestamp = storyJson.getLong("timestamp"),
                            isUserStory = storyJson.getBoolean("is_user_story"),
                            onlineStatus = storyJson.getString("status") == "online"
                        )
                        storiesList.add(story)
                    }
                    storiesList.sortBy { !it.isUserStory }
                    storiesAdapter.notifyDataSetChanged()
                    Log.d(TAG, "Loaded ${storiesList.size} stories")
                } else {
                    Log.e(TAG, "Failed to load stories: ${response.optString("message")}")
                }
            },
            { error ->
                Log.e(TAG, "Volley error loading stories: ${error.message}, data: ${String(error.networkResponse?.data ?: byteArrayOf())}")
            }
        )
        requestQueue.add(request)
    }

    private fun loadPosts(userId: Int) {
        postsList.clear()
        postsAdapter.notifyDataSetChanged()
        val requestQueue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.11/ConnectMe/Profile/getPosts.php"
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                Log.d(TAG, "Get posts response: $response")
                if (response.getBoolean("success")) {
                    val postsArray = response.getJSONArray("posts")
                    Log.d(TAG, "Received ${postsArray.length()} posts")
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
                        Log.d(TAG, "Added post $i: postId=${post.postId}, imageLength=${post.postImage.length}")
                    }
                    Log.d(TAG, "Loaded ${postsList.size} posts into postsList")
                    postsAdapter.notifyDataSetChanged()
                } else {
                    Log.e(TAG, "Failed to load posts: ${response.optString("message")}")
                    Toast.makeText(this, "Failed to load posts: ${response.optString("message")}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e(TAG, "Volley error loading posts: ${error.message}, data: ${String(error.networkResponse?.data ?: byteArrayOf())}")
                Toast.makeText(this, "Error loading posts: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }
}