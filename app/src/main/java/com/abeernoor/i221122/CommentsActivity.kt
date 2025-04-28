package com.abeernoor.i221122

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
// import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.database.DataSnapshot
// import com.google.firebase.database.DatabaseError
// import com.google.firebase.database.FirebaseDatabase
// import com.google.firebase.database.ValueEventListener

class CommentsActivity : AppCompatActivity() {

    // private lateinit var auth: FirebaseAuth
    // private lateinit var database: FirebaseDatabase
    private val commentsList = mutableListOf<Comment>()
    private lateinit var commentsAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        // auth = FirebaseAuth.getInstance()
        // database = FirebaseDatabase.getInstance()

        val postId = intent.getStringExtra("postId") ?: run {
            finish()
            return
        }

        val recyclerView = findViewById<RecyclerView>(R.id.commentsRecyclerView)
        val closeButton = findViewById<ImageView>(R.id.closeButton)
        val commentInput = findViewById<EditText>(R.id.commentInput)
        val postCommentButton = findViewById<Button>(R.id.postCommentButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        commentsAdapter = CommentAdapter(commentsList)
        recyclerView.adapter = commentsAdapter

        closeButton.setOnClickListener {
            finish()
        }

        postCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                val sessionManager = SessionManager(this)
                val currentUserId = sessionManager.getUserId()?.toIntOrNull() ?: run {
                    return@setOnClickListener
                }

                val requestQueue = Volley.newRequestQueue(this)
                val url = "http://192.168.1.11/ConnectMe/Profile/addComment.php"
                val jsonBody = JSONObject().apply {
                    put("post_id", postId)
                    put("user_id", currentUserId)
                    put("text", commentText)
                }

                val request = JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    { response ->
                        if (response.getBoolean("success")) {
                            commentInput.text.clear()
                            loadComments(postId) // Refresh comments
                        }
                    },
                    { error ->
                        // Handle error
                    }
                )
                requestQueue.add(request)
            }
        }

        // Load comments
        loadComments(postId)
    }

    private fun loadComments(postId: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.11/ConnectMe/Profile/getPosts.php"
        val jsonBody = JSONObject().apply {
            put("user_id", SessionManager(this@CommentsActivity).getUserId()?.toIntOrNull() ?: 0)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    commentsList.clear()
                    val postsArray = response.getJSONArray("posts")
                    for (i in 0 until postsArray.length()) {
                        val postJson = postsArray.getJSONObject(i)
                        if (postJson.getString("post_id") == postId) {
                            val commentsArray = postJson.getJSONArray("comments")
                            for (j in 0 until commentsArray.length()) {
                                val commentJson = commentsArray.getJSONObject(j)
                                commentsList.add(
                                    Comment(
                                        commentId = commentJson.getString("comment_id"),
                                        userId = commentJson.getString("user_id"),
                                        username = commentJson.getString("username"),
                                        text = commentJson.getString("text"),
                                        timestamp = commentJson.getLong("timestamp")
                                    )
                                )
                            }
                            break
                        }
                    }
                    commentsAdapter.notifyDataSetChanged()
                }
            },
            { error ->
                // Handle error
            }
        )
        requestQueue.add(request)
    }
}