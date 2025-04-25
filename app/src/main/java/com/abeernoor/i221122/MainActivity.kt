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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storiesAdapter: StoryAdapter
    private lateinit var postsAdapter: postAdapter
    private val storiesList = mutableListOf<story>()
    private val postsList = mutableListOf<Post>()

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val storiesRecyclerView = findViewById<RecyclerView>(R.id.storiesRecyclerView)
        val postsRecyclerView = findViewById<RecyclerView>(R.id.postsRecyclerView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val dmsIcon = findViewById<ImageView>(R.id.communityIcon)
        val notifIcon = findViewById<ImageView>(R.id.heartIcon)

        // Set online status
        val currentUserId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("Users").child(currentUserId)
        userRef.child("onlineStatus").setValue(true)
        userRef.child("onlineStatus").onDisconnect().setValue(false)

        dmsIcon.setOnClickListener {
            startActivity(Intent(this, DmsActivity::class.java))
        }
        logoutButton.setOnClickListener {
            auth.signOut()
            userRef.child("onlineStatus").setValue(false)
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
        loadStories()
        loadPosts()
    }

    private fun loadStories() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Add user's story (first item)
        database.getReference("Users").child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val user = userSnapshot.getValue(User::class.java)
                if (user != null) {
                    val onlineStatus = userSnapshot.child("onlineStatus").getValue(Boolean::class.java) ?: false
                    Log.d("MainActivity", "Current user DP: ${user.image}")
                    storiesList.removeAll { it.isUserStory } // Remove old user story
                    storiesList.add(0, story(
                        userId = currentUserId,
                        username = user.username,
                        image = user.image, // Profile picture
                        storyContent = "", // Will be fetched in StoryAdapter
                        videoUrl = "",
                        timestamp = 0L,
                        isUserStory = true,
                        onlineStatus = onlineStatus
                    ))
                    storiesAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error fetching user: ${error.message}")
            }
        })

        // Load stories from followed users
        database.getReference("Following").child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followedUsers = snapshot.children.mapNotNull { it.key }
                storiesList.removeAll { !it.isUserStory } // Clear old stories except user's
                for (userId in followedUsers) {
                    database.getReference("Stories").child(userId).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(storySnapshot: DataSnapshot) {
                            for (storySnap in storySnapshot.children) {
                                val storyImage = storySnap.child("image").getValue(String::class.java) ?: ""
                                val videoUrl = storySnap.child("videoUrl").getValue(String::class.java) ?: ""
                                val timestamp = storySnap.child("timestamp").getValue(Long::class.java) ?: 0L
                                if (System.currentTimeMillis() - timestamp < 24 * 60 * 60 * 1000) { // Less than 24 hours
                                    database.getReference("Users").child(userId).addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(userSnap: DataSnapshot) {
                                            val username = userSnap.child("username").getValue(String::class.java) ?: "Unknown"
                                            val profileImage = userSnap.child("image").getValue(String::class.java) ?: ""
                                            val onlineStatus = userSnap.child("onlineStatus").getValue(Boolean::class.java) ?: false
                                            Log.d("MainActivity", "Followed user $username DP: $profileImage")
                                            storiesList.add(story(
                                                userId = userId,
                                                username = username,
                                                image = profileImage, // Profile picture
                                                storyContent = storyImage, // Story content
                                                videoUrl = videoUrl,
                                                timestamp = timestamp,
                                                isUserStory = false,
                                                onlineStatus = onlineStatus
                                            ))
                                            storiesAdapter.notifyDataSetChanged()
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e("MainActivity", "Error fetching user $userId: ${error.message}")
                                        }
                                    })
                                } else {
                                    // Delete expired story
                                    storySnap.ref.removeValue()
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("MainActivity", "Error fetching stories: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error fetching following: ${error.message}")
            }
        })
    }

    private fun loadPosts() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Clear existing posts
        postsList.clear()

        // Load user's posts and posts from followed users
        database.getReference("Following").child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followedUsers = snapshot.children.mapNotNull { it.key } + currentUserId
                postsList.clear()
                for (userId in followedUsers) {
                    database.getReference("Posts").orderByChild("userId").equalTo(userId)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(postSnapshot: DataSnapshot) {
                                for (postSnap in postSnapshot.children) {
                                    // Fetch current user profile image
                                    database.getReference("Users").child(userId).addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(userSnap: DataSnapshot) {
                                            val profileImage = userSnap.child("image").getValue(String::class.java) ?: ""
                                            val username = userSnap.child("username").getValue(String::class.java) ?: "Unknown"
                                            val post = convertToPost(postSnap, profileImage, username)
                                            if (post != null) {
                                                // Avoid duplicates
                                                if (!postsList.any { it.postId == post.postId }) {
                                                    postsList.add(post)
                                                }
                                                // Sort posts by timestamp (newest first)
                                                postsList.sortByDescending { it.timestamp }
                                                postsAdapter.notifyDataSetChanged()
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e("MainActivity", "Error fetching user $userId: ${error.message}")
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("MainActivity", "Error fetching posts: ${error.message}")
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error fetching following: ${error.message}")
            }
        })
    }

    private fun convertToPost(postSnap: DataSnapshot, profileImage: String, username: String): Post? {
        return try {
            val postMap = postSnap.value as? Map<String, Any> ?: return null
            val postId = postMap["postId"]?.toString() ?: ""
            val userId = postMap["userId"]?.toString() ?: ""
            val postImage = postMap["postImage"]?.toString() ?: ""
            val caption = postMap["caption"]?.toString() ?: ""
            val timestamp = postMap["timestamp"]?.toString()?.toLongOrNull() ?: 0L

            // Handle likes
            val likesList = mutableListOf<String>()
            when (val likesData = postMap["likes"]) {
                is List<*> -> {
                    likesList.addAll(likesData.filterIsInstance<String>())
                }
                is HashMap<*, *> -> {
                    likesList.addAll(likesData.keys.filterIsInstance<String>())
                }
            }

            // Handle comments
            val commentsList = mutableListOf<Comment>()
            when (val commentsData = postMap["comments"]) {
                is List<*> -> {
                    commentsList.addAll(commentsData.filterIsInstance<Map<String, Any>>().mapNotNull { commentMap ->
                        Comment(
                            commentId = commentMap["commentId"]?.toString() ?: "",
                            userId = commentMap["userId"]?.toString() ?: "",
                            username = commentMap["username"]?.toString() ?: "",
                            text = commentMap["text"]?.toString() ?: "",
                            timestamp = commentMap["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                        )
                    })
                }
                is HashMap<*, *> -> {
                    commentsList.addAll(commentsData.values.filterIsInstance<Map<String, Any>>().mapNotNull { commentMap ->
                        Comment(
                            commentId = commentMap["commentId"]?.toString() ?: "",
                            userId = commentMap["userId"]?.toString() ?: "",
                            username = commentMap["username"]?.toString() ?: "",
                            text = commentMap["text"]?.toString() ?: "",
                            timestamp = commentMap["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                        )
                    })
                }
            }

            Post(
                postId = postId,
                userId = userId,
                username = username,
                profileImage = profileImage,
                postImage = postImage,
                caption = caption,
                timestamp = timestamp,
                likes = likesList,
                comments = commentsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}