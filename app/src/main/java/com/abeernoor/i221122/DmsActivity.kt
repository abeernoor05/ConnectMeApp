package com.abeernoor.i221122

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
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

class DmsActivity : AppCompatActivity() {
    private lateinit var backIcon: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var dmsTab: TextView
    private lateinit var requestsTab: TextView
    private lateinit var searchBar: EditText
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val chatList = mutableListOf<ChatModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dm)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        backIcon = findViewById(R.id.backIcon)
        menuIcon = findViewById(R.id.menuIcon)
        dmsTab = findViewById(R.id.dmsTab)
        requestsTab = findViewById(R.id.requestsTab)
        searchBar = findViewById(R.id.searchBar)
        chatsRecyclerView = findViewById(R.id.chatsRecyclerView)

        chatAdapter = ChatAdapter(chatList) { chat ->
            val intent = Intent(this, ActualChatActivity::class.java).apply {
                putExtra("chatId", chat.chatId)
                putExtra("userId", chat.userId)
                putExtra("username", chat.username)
            }
            startActivity(intent)
        }
        chatsRecyclerView.layoutManager = LinearLayoutManager(this)
        chatsRecyclerView.adapter = chatAdapter

        backIcon.setOnClickListener { finish() }
        menuIcon.setOnClickListener {
            Toast.makeText(this, "New chat feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        dmsTab.setOnClickListener {
            dmsTab.setTextColor(getColor(R.color.black))
            requestsTab.setTextColor(getColor(com.google.android.material.R.color.material_dynamic_neutral20))
            loadChats()
        }
        requestsTab.setOnClickListener {
            dmsTab.setTextColor(getColor(com.google.android.material.R.color.material_dynamic_neutral20))
            requestsTab.setTextColor(getColor(R.color.black))
            Toast.makeText(this, "Requests feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        loadChats()
    }

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return
        chatList.clear()
        database.getReference("Chats").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatList.clear()
                    for (chatSnap in snapshot.children) {
                        val chatId = chatSnap.key ?: continue
                        val otherUserId = chatId.split("_").filter { it != currentUserId }[0]
                        fetchChatDetails(chatId, otherUserId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DmsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchChatDetails(chatId: String, otherUserId: String) {
        database.getReference("Users").child(otherUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnap: DataSnapshot) {
                    val username = userSnap.child("username").getValue(String::class.java) ?: "Unknown"
                    val profileImage = userSnap.child("profileImage").getValue(String::class.java) ?: ""

                    database.getReference("Messages").child(chatId).child("lastMessage")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(messageSnap: DataSnapshot) {
                                val lastMessage = messageSnap.child("content").getValue(String::class.java) ?: ""
                                val lastMessageType = messageSnap.child("type").getValue(String::class.java) ?: "text"
                                val timestamp = messageSnap.child("timestamp").getValue(Long::class.java) ?: 0L
                                chatList.add(ChatModel(chatId, otherUserId, username, profileImage, lastMessage, lastMessageType, timestamp))
                                chatList.sortByDescending { it.timestamp }
                                chatAdapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@DmsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DmsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}