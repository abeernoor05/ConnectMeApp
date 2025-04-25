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

class FollowingActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowingAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var followingTextView: TextView
    private  lateinit var followerTextView:TextView
    private val followingList = mutableListOf<Following>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_following)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        followerTextView = findViewById<TextView>(R.id.tvFollowers)
        followingTextView=findViewById<TextView>(R.id.tvFollowing)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        followerTextView.setOnClickListener {
            val intent = Intent(this, FollowersActivity::class.java)
            startActivity(intent)
        }
//        followingList.add(Following("Maryam Ijaz", R.drawable.profile_placeholder))
//        followingList.add(Following("Fabeha Batool", R.drawable.profile_placeholder))
//        followingList.add(Following("Hareem noor", R.drawable.profile_placeholder))

        recyclerView = findViewById(R.id.recyclerViewFollowing)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowingAdapter(followingList)
        recyclerView.adapter = adapter

        loadFollowing()

    }

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
                val name = snapshot.child("name").value.toString()?:"Unknown"
                val profilePic = snapshot.child("profileImage").value.toString()?: ""
                followingList.add(Following(name, profilePic))
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowingActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
