package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileViewActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var targetUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_view)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val backButton: ImageView = findViewById(R.id.backButton)
        val profileImage: ImageView = findViewById(R.id.profileImage)
        val usernameText: TextView = findViewById(R.id.usernameText)
        val nameText: TextView = findViewById(R.id.nameText)
        val bioText: TextView = findViewById(R.id.bioText)
        val followersCount: TextView = findViewById(R.id.followersCount)
        val followingCount: TextView = findViewById(R.id.followingCount)
        val followButton: Button = findViewById(R.id.followButton)
        val messageButton: Button = findViewById(R.id.messageButton)

        val username = intent.getStringExtra("username") ?: return

        backButton.setOnClickListener {
            finish()
        }

        database.getReference("Users").orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                targetUserId = userSnapshot.key
                                usernameText.text = user.username
                                nameText.text = user.name
                                bioText.text = user.bio.ifEmpty { "No bio available" }
                                followersCount.text = "${user.followerCount} Followers"
                                followingCount.text = "${user.followingCount} Following"

                                if (user.image.isNotEmpty()) {
                                    try {
                                        val decodedBytes = Base64.decode(user.image, Base64.DEFAULT)
                                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                        profileImage.setImageBitmap(bitmap)
                                    } catch (e: Exception) {
                                        profileImage.setImageResource(R.drawable.profile_placeholder)
                                    }
                                } else {
                                    profileImage.setImageResource(R.drawable.profile_placeholder)
                                }

                                val currentUserId = auth.currentUser?.uid
                                if (currentUserId != null && targetUserId != null) {
                                    database.getReference("Following").child(currentUserId).child(targetUserId!!)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(followingSnapshot: DataSnapshot) {
                                                if (followingSnapshot.exists()) {
                                                    followButton.text = "Unfollow"
                                                } else {
                                                    database.getReference("FollowRequests").child(targetUserId!!).child(currentUserId)
                                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                                            override fun onDataChange(requestSnapshot: DataSnapshot) {
                                                                followButton.text = if (requestSnapshot.exists()) "Requested" else "Follow"
                                                            }

                                                            override fun onCancelled(error: DatabaseError) {}
                                                        })
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this@ProfileViewActivity, "User not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileViewActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        followButton.setOnClickListener {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null || targetUserId == null) {
                Toast.makeText(this, "Error: User not logged in or target not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (followButton.text) {
                "Follow" -> {
                    database.getReference("Users").child(currentUserId).get()
                        .addOnSuccessListener { snapshot ->
                            val currentUser = snapshot.getValue(User::class.java)
                            val requesterUsername = currentUser?.username ?: "Unknown"

                            val requestData = mutableMapOf<String, Any>()
                            requestData["username"] = requesterUsername
                            requestData["timestamp"] = System.currentTimeMillis()

                            database.getReference("FollowRequests").child(targetUserId!!).child(currentUserId)
                                .setValue(requestData)
                                .addOnSuccessListener {
                                    followButton.text = "Requested"
                                    sendFollowRequestNotification(targetUserId!!, username)
                                    Toast.makeText(this, "Follow request sent", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to fetch username", Toast.LENGTH_SHORT).show()
                        }
                }
                "Requested" -> {
                    database.getReference("FollowRequests").child(targetUserId!!).child(currentUserId)
                        .removeValue()
                        .addOnSuccessListener {
                            followButton.text = "Follow"
                            Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show()
                        }
                }
                "Unfollow" -> {
                    Toast.makeText(this, "Unfollow clicked (to be fully implemented)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        messageButton.setOnClickListener {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null || targetUserId == null) {
                Toast.makeText(this, "Error: User not logged in or target not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate chatId (sorted user IDs)
            val chatId = if (currentUserId < targetUserId!!) {
                "${currentUserId}_${targetUserId}"
            } else {
                "${targetUserId}_${currentUserId}"
            }

            // Update Chats node for both users
            database.getReference("Chats").child(currentUserId).child(chatId).setValue(true)
            database.getReference("Chats").child(targetUserId!!).child(chatId).setValue(true)

            // Start ActualChatActivity with chatId, targetUserId, and username
            val intent = Intent(this, ActualChatActivity::class.java).apply {
                putExtra("chatId", chatId)
                putExtra("userId", targetUserId)
                putExtra("username", username)
            }
            startActivity(intent)
        }
    }

    private fun sendFollowRequestNotification(targetUserId: String, targetUsername: String) {
        database.getReference("Users").child(targetUserId).child("fcmToken")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val token = snapshot.getValue(String::class.java)
                    if (token != null) {
                        val senderUsername = auth.currentUser?.let {
                            database.getReference("Users").child(it.uid).get()
                                .addOnSuccessListener { snap ->
                                    snap.getValue(User::class.java)?.username ?: "Someone"
                                }
                        } ?: "Someone"
                        val message = "$senderUsername wants to follow you"

                        val intent = Intent(this@ProfileViewActivity, FollowRequestsActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(
                            this@ProfileViewActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val notification = NotificationCompat.Builder(this@ProfileViewActivity, "follow_channel")
                            .setSmallIcon(R.drawable.notif)
                            .setContentTitle("Follow Request")
                            .setContentText(message)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()

                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1, notification)
                    } else {
                        Toast.makeText(this@ProfileViewActivity, "Target user has no FCM token", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileViewActivity, "Error fetching token", Toast.LENGTH_SHORT).show()
                }
            })
    }
}