package com.abeernoor.i221122

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
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
    // private val db = FirebaseFirestore.getInstance()

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        val storiesRecyclerView = findViewById<RecyclerView>(R.id.storiesRecyclerView)
        val postsRecyclerView = findViewById<RecyclerView>(R.id.postsRecyclerView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val dmsIcon = findViewById<ImageView>(R.id.communityIcon)
        val notifIcon = findViewById<ImageView>(R.id.heartIcon)

        // Initialize SessionManager
        val sessionManager = SessionManager(this)
        val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Set online status
        updateOnlineStatus(currentUserId, "online")

        dmsIcon.setOnClickListener {
            startActivity(Intent(this, DmsActivity::class.java))
        }

        logoutButton.setOnClickListener {
            updateOnlineStatus(currentUserId, "offline")
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        notifIcon.setOnClickListener {
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
                R.id.nav_home -> true
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
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Load stories and posts
        loadStories(currentUserId)
        // loadPosts() // Implement if needed
    }

    override fun onPause() {
        super.onPause()
        val sessionManager = SessionManager(this)
        sessionManager.getUserId()?.toIntOrNull()?.let { updateOnlineStatus(it, "offline") }
    }

    override fun onResume() {
        super.onResume()
        val sessionManager = SessionManager(this)
        sessionManager.getUserId()?.toIntOrNull()?.let { updateOnlineStatus(it, "online") }
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
                    Log.d("MainActivity", "Status updated: $status")
                } else {
                    Log.e("MainActivity", "Failed to update status: ${response.optString("message")}")
                }
            },
            { error ->
                Log.e("MainActivity", "Volley error updating status: ${error.message}")
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
                Log.d("MainActivity", "Get stories response: $response")
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
                    // Ensure current user's story is first
                    storiesList.sortBy { !it.isUserStory }
                    storiesAdapter.notifyDataSetChanged()
                } else {
                    Log.e("MainActivity", "Failed to load stories: ${response.optString("message")}")
                }
            },
            { error ->
                Log.e("MainActivity", "Volley error: ${error.message}, data: ${String(error.networkResponse?.data ?: byteArrayOf())}")
            }
        )
        requestQueue.add(request)

        /*
        // Firebase logic for loading stories
        storiesList.clear()
        // Add current user's story placeholder
        db.collection("users").document(currentUserId.toString()).get()
            .addOnSuccessListener { userDoc ->
                val username = userDoc.getString("username") ?: ""
                val profileImage = userDoc.getString("profile_image") ?: ""
                storiesList.add(
                    story(
                        userId = currentUserId.toString(),
                        username = username,
                        image = profileImage,
                        storyContent = "",
                        videoUrl = "",
                        timestamp = 0L,
                        isUserStory = true,
                        onlineStatus = true
                    )
                )
                // Load stories from followed users
                db.collection("followers").document(currentUserId.toString())
                    .collection("following").get()
                    .addOnSuccessListener { followingDocs ->
                        val followedIds = followingDocs.documents.map { it.id }
                        db.collection("stories")
                            .whereIn("user_id", followedIds + currentUserId.toString())
                            .whereGreaterThan("timestamp", System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener { storyDocs ->
                                for (doc in storyDocs) {
                                    val storyData = doc.data
                                    db.collection("users").document(storyData["user_id"] as String).get()
                                        .addOnSuccessListener { userDoc ->
                                            storiesList.add(
                                                story(
                                                    userId = storyData["user_id"] as String,
                                                    username = userDoc.getString("username") ?: "",
                                                    image = userDoc.getString("profile_image") ?: "",
                                                    storyContent = storyData["story_content"] as String,
                                                    videoUrl = storyData["video_url"] as String? ?: "",
                                                    timestamp = storyData["timestamp"] as Long,
                                                    isUserStory = storyData["user_id"] == currentUserId.toString(),
                                                    onlineStatus = userDoc.getString("status") == "online"
                                                )
                                            )
                                            storiesList.sortBy { !it.isUserStory }
                                            storiesAdapter.notifyDataSetChanged()
                                        }
                                }
                            }
                    }
            }
        */
    }
}