package com.abeernoor.i221122

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class FollowersActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowersAdapter
    private val followersList = mutableListOf<Follower>()
    private lateinit var followingTextView: TextView
    private lateinit var followersTextView: TextView

    // Commented out Firebase variables
    /*
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    */

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)

        val sessionManager = SessionManager(this)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Commented out Firebase initialization
        /*
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        */

        followingTextView = findViewById(R.id.tvFollowing)
        followersTextView = findViewById(R.id.tvFollowers)

        followingTextView.setOnClickListener {
            val intent = Intent(this, FollowingActivity::class.java)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.recyclerViewFollowers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowersAdapter(followersList)
        recyclerView.adapter = adapter

        val userId = sessionManager.getUserId() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        loadFollowers(userId)
    }

    private fun loadFollowers(userId: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val getFollowersUrl = "http://192.168.1.11/ConnectMe/Profile/getFollowers.php"
        val getFollowersJson = JSONObject().apply {
            put("user_id", userId)
        }

        val getFollowersRequest = JsonObjectRequest(
            Request.Method.POST, getFollowersUrl, getFollowersJson,
            { response ->
                Log.d("FollowersActivity", "Get followers response: $response")
                if (response.getBoolean("success")) {
                    followersList.clear()
                    val followersArray = response.getJSONArray("followers")
                    followersTextView.text = "${followersArray.length()} Followers"

                    for (i in 0 until followersArray.length()) {
                        val follower = followersArray.getJSONObject(i)
                        val name = follower.optString("name", "Unknown")
                        val profileImage = follower.optString("profile_image", "")
                        followersList.add(Follower(name, profileImage))
                    }
                    adapter.notifyDataSetChanged()

                    // Fetch following count separately
                    fetchFollowingCount(userId)
                } else {
                    Toast.makeText(this, response.optString("message", "No followers found"), Toast.LENGTH_SHORT).show()
                    followersTextView.text = "0 Followers"
                }
            },
            { error ->
                Log.e("FollowersActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to load followers"}", Toast.LENGTH_SHORT).show()
                followersTextView.text = "0 Followers"
            }
        )
        requestQueue.add(getFollowersRequest)
    }

    private fun fetchFollowingCount(userId: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val getFollowingUrl = "http://192.168.1.11/ConnectMe/Profile/getFollowing.php"
        val getFollowingJson = JSONObject().apply {
            put("user_id", userId)
        }

        val getFollowingRequest = JsonObjectRequest(
            Request.Method.POST, getFollowingUrl, getFollowingJson,
            { response ->
                Log.d("FollowersActivity", "Get following response: $response")
                if (response.getBoolean("success")) {
                    val followingArray = response.getJSONArray("following")
                    followingTextView.text = "${followingArray.length()} Following"
                } else {
                    followingTextView.text = "0 Following"
                }
            },
            { error ->
                Log.e("FollowersActivity", "Volley error: ${error.message}")
                followingTextView.text = "0 Following"
            }
        )
        requestQueue.add(getFollowingRequest)
    }

    // Commented out Firebase methods
    /*
    private fun loadFollowers() {
        val userId = auth.currentUser?.uid
        val followersRef = database.child("Followers").child(userId!!)
        val followingRef = database.child("Following").child(userId)
        followersRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                followersList.clear()
                val count = snapshot.childrenCount.toInt()
                followersTextView.text = "$count Followers"
                for (child in snapshot.children) {
                    val followerId = child.key
                    if (followerId != null) {
                        fetchUserDetails(followerId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowersActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        followingRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                followingTextView.text = "$count Following"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowersActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserDetails(userId: String) {
        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").value.toString() ?: "Unknown"
                    val profilePic = snapshot.child("profileImage").value.toString() ?: ""
                    followersList.add(Follower(name, profilePic))
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FollowersActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    */
}