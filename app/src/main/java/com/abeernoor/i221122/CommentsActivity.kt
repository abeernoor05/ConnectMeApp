package com.abeernoor.i221122

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CommentsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val commentsList = mutableListOf<Comment>()
    private lateinit var commentsAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

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
                val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
                database.getReference("Users").child(currentUserId).get().addOnSuccessListener { snapshot ->
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    val commentId = database.getReference("Posts").child(postId).child("comments").push().key ?: return@addOnSuccessListener
                    val newComment = Comment(
                        commentId = commentId,
                        userId = currentUserId,
                        username = username,
                        text = commentText,
                        timestamp = System.currentTimeMillis()
                    )

                    // Fetch the current comments, handle both List and HashMap cases
                    database.getReference("Posts").child(postId).child("comments")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val currentComments = mutableListOf<Comment>()
                                when (val commentsData = snapshot.value) {
                                    is List<*> -> {
                                        // If it's already a list
                                        for (commentData in commentsData) {
                                            if (commentData is Map<*, *>) {
                                                val comment = snapshot.getValue(Comment::class.java)
                                                if (comment != null) {
                                                    currentComments.add(comment)
                                                }
                                            }
                                        }
                                    }
                                    is HashMap<*, *> -> {
                                        // If it's a HashMap (old data)
                                        for ((_, commentData) in commentsData) {
                                            if (commentData is Map<*, *>) {
                                                val comment = Comment(
                                                    commentId = commentData["commentId"]?.toString() ?: "",
                                                    userId = commentData["userId"]?.toString() ?: "",
                                                    username = commentData["username"]?.toString() ?: "",
                                                    text = commentData["text"]?.toString() ?: "",
                                                    timestamp = commentData["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                                                )
                                                currentComments.add(comment)
                                            }
                                        }
                                    }
                                }
                                currentComments.add(newComment)
                                database.getReference("Posts").child(postId).child("comments")
                                    .setValue(currentComments).addOnSuccessListener {
                                        commentInput.text.clear()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
        }

        // Load comments
        database.getReference("Posts").child(postId).child("comments")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    commentsList.clear()
                    when (val commentsData = snapshot.value) {
                        is List<*> -> {
                            for (commentData in commentsData) {
                                if (commentData is Map<*, *>) {
                                    val comment = snapshot.getValue(Comment::class.java)
                                    if (comment != null) {
                                        commentsList.add(comment)
                                    }
                                }
                            }
                        }
                        is HashMap<*, *> -> {
                            for ((_, commentData) in commentsData) {
                                if (commentData is Map<*, *>) {
                                    val comment = Comment(
                                        commentId = commentData["commentId"]?.toString() ?: "",
                                        userId = commentData["userId"]?.toString() ?: "",
                                        username = commentData["username"]?.toString() ?: "",
                                        text = commentData["text"]?.toString() ?: "",
                                        timestamp = commentData["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                                    )
                                    commentsList.add(comment)
                                }
                            }
                        }
                    }
                    commentsAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}