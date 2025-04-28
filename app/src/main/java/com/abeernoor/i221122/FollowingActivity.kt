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

class FollowingActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowingAdapter
    private val followingList = mutableListOf<Following>()
    private lateinit var followingTextView: TextView
    private lateinit var followerTextView: TextView

    // Commented out Firebase variables
    /*
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    */

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_following)

        val sessionManager = SessionManager(this)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Commented out Firebase initialization
        /*
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        */

        followerTextView = findViewById(R.id.tvFollowers)
        followingTextView = findViewById(R.id.tvFollowing)

        followerTextView.setOnClickListener {
            val intent = Intent(this, FollowersActivity::class.java)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.recyclerViewFollowing)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowingAdapter(followingList)
        recyclerView.adapter = adapter

        val userId = sessionManager.getUserId() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        loadFollowing(userId)
    }

    private fun loadFollowing(userId: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val getFollowingUrl = "http://192.168.1.11/ConnectMe/Profile/getFollowing.php"
        val getFollowingJson = JSONObject().apply {
            put("user_id", userId)
        }

        val getFollowingRequest = JsonObjectRequest(
            Request.Method.POST, getFollowingUrl, getFollowingJson,
            { response ->
                Log.d("FollowingActivity", "Get following response: $response")
                if (response.getBoolean("success")) {
                    followingList.clear()
                    val followingArray = response.getJSONArray("following")
                    followingTextView.text = "${followingArray.length()} Following"

                    for (i in 0 until followingArray.length()) {
                        val following = followingArray.getJSONObject(i)
                        val name = following.optString("name", "Unknown")
                        val profileImage = following.optString("profile_image", "")
                        followingList.add(Following(name, profileImage))
                    }
                    adapter.notifyDataSetChanged()

                    // Fetch followers count separately
                    fetchFollowersCount(userId)
                } else {
                    Toast.makeText(this, response.optString("message", "No following found"), Toast.LENGTH_SHORT).show()
                    followingTextView.text = "0 Following"
                }
            },
            { error ->
                Log.e("FollowingActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to load following"}", Toast.LENGTH_SHORT).show()
                followingTextView.text = "0 Following"
            }
        )
        requestQueue.add(getFollowingRequest)
    }

    private fun fetchFollowersCount(userId: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val getFollowersUrl = "http://192.168.1.11/ConnectMe/Profile/getFollowers.php"
        val getFollowersJson = JSONObject().apply {
            put("user_id", userId)
        }

        val getFollowersRequest = JsonObjectRequest(
            Request.Method.POST, getFollowersUrl, getFollowersJson,
            { response ->
                Log.d("FollowingActivity", "Get followers response: $response")
                if (response.getBoolean("success")) {
                    val followersArray = response.getJSONArray("followers")
                    followerTextView.text = "${followersArray.length()} Followers"
                } else {
                    followerTextView.text = "0 Followers"
                }
            },
            { error ->
                Log.e("FollowingActivity", "Volley error: ${error.message}")
                followerTextView.text = "0 Followers"
            }
        )
        requestQueue.add(getFollowersRequest)
    }

    // Commented out Firebase methods
    /*
    private fun loadFollowing() {
        val userId = auth.currentUser?.uid ?: return
        val followersRef = database.child("Followers").child(userId)
        val followingRef = database.child("Following").child(userId)

        followingRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                followingList.clear()
                val count = snapshot.childrenCount.toInt()
                followingTextView.text = "$count Following"
                for (child in snapshot.children) {
                    val followingId = child.key
                    if (followingId != null) {
                        fetchUserDetails(followingId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowingActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        followersRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                followerTextView.text = "$count Followers"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowingActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserDetails(userId: String) {
        database.child("Users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").value.toString() ?: "Unknown"
                val profilePic = snapshot.child("profileImage").value.toString() ?: ""
                followingList.add(Following(name, profilePic))
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowingActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    */
}