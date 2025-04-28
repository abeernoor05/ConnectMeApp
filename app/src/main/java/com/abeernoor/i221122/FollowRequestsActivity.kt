package com.abeernoor.i221122

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

class FollowRequestsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabNotifications: TextView
    private lateinit var backButton: ImageView
    private val requestList = mutableListOf<FollowRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_requests)

        val sessionManager = SessionManager(this)

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

        val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("FollowRequestsActivity", "Current User ID: $currentUserId")
        loadFollowRequests(currentUserId)
    }

    private fun loadFollowRequests(userId: Int) {
        val requestQueue = Volley.newRequestQueue(this)
        val getRequestsUrl = "http://192.168.1.11/ConnectMe/Profile/getFollowRequests.php"
        val getRequestsJson = JSONObject().apply {
            put("target_id", userId)
        }

        val getRequestsRequest = JsonObjectRequest(
            Request.Method.POST, getRequestsUrl, getRequestsJson,
            { response ->
                Log.d("FollowRequestsActivity", "Get requests response: $response")
                if (response.getBoolean("success")) {
                    requestList.clear()
                    val requestsArray = response.getJSONArray("requests")
                    for (i in 0 until requestsArray.length()) {
                        val request = requestsArray.getJSONObject(i)
                        val requesterId = request.getInt("requester_id")
                        val username = request.optString("username", "Unknown")
                        val profileImage = request.optString("profile_image", "")
                        requestList.add(FollowRequest(requesterId, username, profileImage))
                    }
                    recyclerView.adapter?.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, response.optString("message", "No follow requests"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("FollowRequestsActivity", "Volley error: ${error.message}, networkResponse: ${error.networkResponse?.statusCode}, data: ${String(error.networkResponse?.data ?: byteArrayOf())}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to load requests"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(getRequestsRequest)
    }

    private fun handleFollowRequest(request: FollowRequest, isAccepted: Boolean) {
        val currentUserId = SessionManager(this).getUserId()?.toIntOrNull() ?: return
        val requesterId = request.requesterId

        Log.d("FollowRequestsActivity", "Handling request: requesterId=$requesterId, targetId=$currentUserId, isAccepted=$isAccepted")

        val requestQueue = Volley.newRequestQueue(this)
        val handleRequestUrl = "http://192.168.1.11/ConnectMe/Profile/handleFollowRequests.php"
        val handleRequestJson = JSONObject().apply {
            put("requester_id", requesterId)
            put("target_id", currentUserId)
            put("is_accepted", isAccepted)
        }

        val handleRequest = JsonObjectRequest(
            Request.Method.POST, handleRequestUrl, handleRequestJson,
            { response ->
                Log.d("FollowRequestsActivity", "Handle request response: $response")
                if (response.getBoolean("success")) {
                    Toast.makeText(this, if (isAccepted) "Request accepted" else "Request rejected", Toast.LENGTH_SHORT).show()
                    // Remove from list
                    val position = requestList.indexOf(request)
                    if (position != -1) {
                        requestList.removeAt(position)
                        recyclerView.adapter?.notifyItemRemoved(position)
                    }
                } else {
                    val message = response.optString("message", "Failed to process request")
                    Log.e("FollowRequestsActivity", "Handle request failed: $message")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("FollowRequestsActivity", "Volley error: ${error.message}, networkResponse: ${error.networkResponse?.statusCode}, data: ${String(error.networkResponse?.data ?: byteArrayOf())}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to process request"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(handleRequest)
    }
}