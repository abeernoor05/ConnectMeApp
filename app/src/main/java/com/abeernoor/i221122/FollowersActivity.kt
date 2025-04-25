package com.abeernoor.i221122

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FollowersActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowersAdapter
    private val followersList = mutableListOf<Follower>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var  followingTextView: TextView
    private lateinit var followersTextView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        followingTextView= findViewById(R.id.tvFollowing)
        followersTextView = findViewById(R.id.tvFollowers)

        followingTextView.setOnClickListener {
            val intent = Intent(this, FollowingActivity::class.java)
            startActivity(intent)
        }

//        followersList.add(Follower("Maryam Ijaz", R.drawable.profile_placeholder))
//        followersList.add(Follower("Fabeha Batool", R.drawable.profile_placeholder))
//        followersList.add(Follower("Hareem noor", R.drawable.profile_placeholder))

        recyclerView = findViewById(R.id.recyclerViewFollowers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowersAdapter(followersList)
        recyclerView.adapter = adapter

        loadFollowers()
    }
    private fun loadFollowers()
    {
        val userId=auth.currentUser?.uid
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
                    val name = snapshot.child("name").value.toString()?: "Unknown"
                    val profilePic = snapshot.child("profileImage").value.toString()?: ""
                    followersList.add(Follower(name,profilePic))
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FollowersActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
