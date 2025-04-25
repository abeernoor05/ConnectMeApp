package com.abeernoor.i221122

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationsActivity : AppCompatActivity() {

    private lateinit var followReqRecyclerView: RecyclerView
    private lateinit var tabFollowRequests: TextView
    private lateinit var backButton: ImageView
    private lateinit var notificationsAdapter: NotificationsAdapter
    private val notificationsList = mutableListOf<Notification>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        followReqRecyclerView = findViewById(R.id.recyclerViewfollowReqNotifications)
        tabFollowRequests = findViewById(R.id.tabFollowRequests)
        backButton = findViewById(R.id.backButton)

        notificationsAdapter = NotificationsAdapter(notificationsList)
        followReqRecyclerView.layoutManager = LinearLayoutManager(this)
        followReqRecyclerView.adapter = notificationsAdapter

        tabFollowRequests.setOnClickListener {
            val intent = Intent(this, FollowRequestsActivity::class.java)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            finish()
        }

        loadNotifications()
    }

    private fun loadNotifications() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("Notifications").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notificationsList.clear()
                    for (notificationSnap in snapshot.children) {
                        val notification = notificationSnap.getValue(Notification::class.java)
                        if (notification != null) {
                            notificationsList.add(notification)
                        }
                    }
                    notificationsList.sortByDescending { it.timestamp }
                    notificationsAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
}