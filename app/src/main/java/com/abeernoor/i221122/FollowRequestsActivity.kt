package com.abeernoor.i221122

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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FollowRequestsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tabNotifications: TextView
    private lateinit var backButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val requestList = mutableListOf<FollowRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_requests)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        recyclerView = findViewById(R.id.recyclerViewFollowRequests)
        tabNotifications = findViewById(R.id.tabNotifications)
        backButton = findViewById(R.id.backButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = FollowRequestAdapter(requestList) { request, isAccepted ->
            handleFollowRequest(request, isAccepted)
        }
        recyclerView.adapter = adapter

        tabNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        backButton.setOnClickListener {
            finish()
        }

        val currentUserId = auth.currentUser?.uid ?: return
        database.getReference("FollowRequests").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestList.clear()
                    for (requestSnapshot in snapshot.children) {
                        val requesterId = requestSnapshot.key ?: continue
                        val username = requestSnapshot.child("username").getValue(String::class.java) ?: "Unknown"
                        requestList.add(FollowRequest(requesterId, username))
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FollowRequestsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleFollowRequest(request: FollowRequest, isAccepted: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val requesterId = request.requesterId

        if (isAccepted) {
            // Add to Followers and Following
            database.getReference("Followers").child(currentUserId).child(requesterId).setValue(true)
            database.getReference("Following").child(requesterId).child(currentUserId).setValue(true)

            // Increase follower count of the user being followed (currentUserId)
            database.getReference("Users").child(currentUserId).child("followerCount").get()
                .addOnSuccessListener { snapshot ->
                    val currentCount = snapshot.getValue(Int::class.java) ?: 0
                    database.getReference("Users").child(currentUserId).child("followerCount")
                        .setValue(currentCount + 1)
                }

            // Increase following count of the requester (requesterId)
            database.getReference("Users").child(requesterId).child("followingCount").get()
                .addOnSuccessListener { snapshot ->
                    val currentCount = snapshot.getValue(Int::class.java) ?: 0
                    database.getReference("Users").child(requesterId).child("followingCount")
                        .setValue(currentCount + 1)
                }
        }

        // Remove the request
        database.getReference("FollowRequests").child(currentUserId).child(requesterId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, if (isAccepted) "Request accepted" else "Request rejected", Toast.LENGTH_SHORT).show()
            }
    }
}

data class FollowRequest(
    val requesterId: String,
    val username: String
)