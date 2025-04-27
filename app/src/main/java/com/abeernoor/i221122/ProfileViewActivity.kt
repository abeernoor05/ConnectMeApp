package com.abeernoor.i221122

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ProfileViewActivity : AppCompatActivity() {

    private var targetUserId: Int? = null

    // Commented out Firebase variables
    /*
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_view)

        val sessionManager = SessionManager(this)

        // Commented out Firebase initialization
        /*
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        */

        val backButton: ImageView = findViewById(R.id.backButton)
        val profileImage: ImageView = findViewById(R.id.profileImage)
        val usernameText: TextView = findViewById(R.id.usernameText)
        val nameText: TextView = findViewById(R.id.nameText)
        val bioText: TextView = findViewById(R.id.bioText)
        val followersCount: TextView = findViewById(R.id.followersCount)
        val followingCount: TextView = findViewById(R.id.followingCount)
        val followButton: Button = findViewById(R.id.followButton)
        val messageButton: Button = findViewById(R.id.messageButton)

        val username = intent.getStringExtra("username") ?: run {
            Toast.makeText(this, "Username not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        backButton.setOnClickListener {
            finish()
        }

        messageButton.setOnClickListener {
            Toast.makeText(this, "Messaging not implemented", Toast.LENGTH_SHORT).show()
            // TODO: Implement messaging if needed
        }

        // Load user data
        val requestQueue = Volley.newRequestQueue(this)
        val getUserUrl = "http://192.168.1.11/ConnectMe/Profile/getUserByUsername.php"
        val getUserJson = JSONObject().apply {
            put("username", username)
        }

        val getUserRequest = JsonObjectRequest(
            Request.Method.POST, getUserUrl, getUserJson,
            { response ->
                Log.d("ProfileViewActivity", "Get user response: $response")
                if (response.getBoolean("success")) {
                    val user = response.getJSONObject("user")
                    targetUserId = user.getInt("id")
                    usernameText.text = user.optString("username", "Unknown")
                    nameText.text = user.optString("name", "")
                    bioText.text = user.optString("bio", "No bio available")
                    followersCount.text = "${user.optInt("follower_count", 0)} Followers"
                    followingCount.text = "${user.optInt("following_count", 0)} Following"

                    val profileImageStr = user.optString("profile_image", "")
                    if (profileImageStr.isNotEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(profileImageStr, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            profileImage.setImageResource(R.drawable.profile_placeholder)
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.profile_placeholder)
                    }

                    // Check follow status
                    val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: return@JsonObjectRequest
                    checkFollowStatus(currentUserId, targetUserId!!, followButton)
                } else {
                    Toast.makeText(this, response.optString("message", "User not found"), Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            { error ->
                Log.e("ProfileViewActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to load user"}", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
        requestQueue.add(getUserRequest)

        followButton.setOnClickListener {
            val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: run {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }
            if (targetUserId == null) {
                Toast.makeText(this, "Target user not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (followButton.text) {
                "Follow" -> {
                    sendFollowRequest(currentUserId, targetUserId!!, followButton)
                }
                "Requested" -> {
                    cancelFollowRequest(currentUserId, targetUserId!!, followButton)
                }
                "Unfollow" -> {
                    unfollowUser(currentUserId, targetUserId!!, followButton)
                }
            }
        }
    }

    private fun checkFollowStatus(currentUserId: Int, targetUserId: Int, followButton: Button) {
        val requestQueue = Volley.newRequestQueue(this)
        val checkStatusUrl = "http://192.168.1.11/ConnectMe/Profile/checkFollowStatus.php"
        val checkStatusJson = JSONObject().apply {
            put("current_user_id", currentUserId)
            put("target_user_id", targetUserId)
        }

        val checkStatusRequest = JsonObjectRequest(
            Request.Method.POST, checkStatusUrl, checkStatusJson,
            { response ->
                Log.d("ProfileViewActivity", "Check status response: $response")
                if (response.getBoolean("success")) {
                    when {
                        response.getBoolean("is_following") -> followButton.text = "Unfollow"
                        response.getBoolean("has_requested") -> followButton.text = "Requested"
                        else -> followButton.text = "Follow"
                    }
                } else {
                    followButton.text = "Follow"
                    Toast.makeText(this, response.optString("message", "Failed to check follow status"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileViewActivity", "Volley error: ${error.message}")
                followButton.text = "Follow"
                Toast.makeText(this, "Error: ${error.message ?: "Failed to check status"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(checkStatusRequest)
    }

    private fun sendFollowRequest(requesterId: Int, targetId: Int, followButton: Button) {
        val requestQueue = Volley.newRequestQueue(this)
        val sendRequestUrl = "http://192.168.1.11/ConnectMe/Profile/sendFollowRequest.php"
        val sendRequestJson = JSONObject().apply {
            put("requester_id", requesterId)
            put("target_id", targetId)
        }

        val sendRequest = JsonObjectRequest(
            Request.Method.POST, sendRequestUrl, sendRequestJson,
            { response ->
                Log.d("ProfileViewActivity", "Send follow request response: $response")
                if (response.getBoolean("success")) {
                    followButton.text = "Requested"
                    Toast.makeText(this, "Follow request sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, response.optString("message", "Failed to send request"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileViewActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to send request"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(sendRequest)
    }

    private fun cancelFollowRequest(requesterId: Int, targetId: Int, followButton: Button) {
        val requestQueue = Volley.newRequestQueue(this)
        val cancelRequestUrl = "http://192.168.1.11/ConnectMe/Profile/handleFollowRequest.php"
        val cancelRequestJson = JSONObject().apply {
            put("requester_id", requesterId)
            put("target_id", targetId)
            put("is_accepted", false)
        }

        val cancelRequest = JsonObjectRequest(
            Request.Method.POST, cancelRequestUrl, cancelRequestJson,
            { response ->
                Log.d("ProfileViewActivity", "Cancel follow request response: $response")
                if (response.getBoolean("success")) {
                    followButton.text = "Follow"
                    Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, response.optString("message", "Failed to cancel request"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileViewActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to cancel request"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(cancelRequest)
    }

    private fun unfollowUser(followerId: Int, followedId: Int, followButton: Button) {
        val requestQueue = Volley.newRequestQueue(this)
        val unfollowUrl = "http://192.168.1.11/ConnectMe/Profile/unfollowUser.php"
        val unfollowJson = JSONObject().apply {
            put("follower_id", followerId)
            put("followed_id", followedId)
        }

        val unfollowRequest = JsonObjectRequest(
            Request.Method.POST, unfollowUrl, unfollowJson,
            { response ->
                Log.d("ProfileViewActivity", "Unfollow response: $response")
                if (response.getBoolean("success")) {
                    followButton.text = "Follow"
                    Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, response.optString("message", "Failed to unfollow"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileViewActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to unfollow"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(unfollowRequest)
    }

    // Commented out Firebase methods
    /*
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
    */
}