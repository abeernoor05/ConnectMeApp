package com.abeernoor.i221122

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class VanishChatActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var chatName: TextView
    private lateinit var profilePic: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()
    private lateinit var chatId: String
    private lateinit var otherUserId: String
    private lateinit var username: String

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vanish_mode)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        chatId = intent.getStringExtra("chatId") ?: ""
        otherUserId = intent.getStringExtra("userId") ?: ""
        username = intent.getStringExtra("username") ?: "Unknown"

        messagesRecyclerView = findViewById(R.id.messagesContainer)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatName = findViewById(R.id.chatName)
        profilePic = findViewById(R.id.profilePic)

        chatName.text = username
        database.getReference("Users").child(otherUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profileImage = snapshot.child("profileImage").getValue(String::class.java) ?: ""
                if (profileImage.isNotEmpty()) {
                    val storageRef = storage.getReferenceFromUrl(profileImage)
                    val tempFile = File.createTempFile("profile", "jpg")
                    storageRef.getFile(tempFile).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        profilePic.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        profilePic.setImageResource(R.drawable.profile_placeholder)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@VanishChatActivity, "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        messageAdapter = MessageAdapter(messagesList, auth.currentUser?.uid ?: "", { message ->
            Toast.makeText(this, "Cannot edit messages in Vanish Mode", Toast.LENGTH_SHORT).show()
        }, { message ->
            deleteMessage(message.messageId)
        })
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messageAdapter

        val backButton: ImageView = findViewById(R.id.backButton)
        val viewProfileButton: TextView = findViewById(R.id.viewProfileButton)

        backButton.setOnClickListener {
            deleteVanishMessages()
            finish()
        }
        viewProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra("userId", otherUserId)
            })
        }

        sendButton.setOnClickListener { sendMessage() }
        findViewById<ImageView>(R.id.attachMedia).setOnClickListener {
            pickImage.launch("image/*")
        }

        loadMessages()
    }

    private fun sendMessage() {
        val content = messageInput.text.toString().trim()
        if (content.isEmpty()) return

        val currentUserId = auth.currentUser?.uid ?: return
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            type = "text",
            content = content,
            timestamp = System.currentTimeMillis(),
            vanishMode = true
        )

        database.getReference("Messages").child(chatId).child("messages").child(messageId).setValue(message)
        database.getReference("Messages").child(chatId).child("lastMessage").setValue(mapOf(
            "content" to content,
            "type" to "text",
            "timestamp" to System.currentTimeMillis()
        ))
        database.getReference("Chats").child(currentUserId).child(chatId).setValue(true)
        database.getReference("Chats").child(otherUserId).child(chatId).setValue(true)

        messageInput.text.clear()
    }

    private fun uploadImage(uri: Uri) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageId = UUID.randomUUID().toString()
        val storageRef = storage.getReference("chat_images/$chatId/$messageId")

        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val message = Message(
                    messageId = messageId,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    type = "image",
                    content = downloadUrl.toString(),
                    timestamp = System.currentTimeMillis(),
                    vanishMode = true
                )
                database.getReference("Messages").child(chatId).child("messages").child(messageId).setValue(message)
                database.getReference("Messages").child(chatId).child("lastMessage").setValue(mapOf(
                    "content" to downloadUrl.toString(),
                    "type" to "image",
                    "timestamp" to System.currentTimeMillis()
                ))
                database.getReference("Chats").child(currentUserId).child(chatId).setValue(true)
                database.getReference("Chats").child(otherUserId).child(chatId).setValue(true)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteMessage(messageId: String) {
        database.getReference("Messages").child(chatId).child("messages").child(messageId).removeValue()
        database.getReference("Messages").child(chatId).child("messages").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.childrenCount > 0) {
                        val lastMessage = snapshot.children.first().getValue(Message::class.java)
                        if (lastMessage != null) {
                            database.getReference("Messages").child(chatId).child("lastMessage").setValue(mapOf(
                                "content" to lastMessage.content,
                                "type" to lastMessage.type,
                                "timestamp" to lastMessage.timestamp
                            ))
                        }
                    } else {
                        database.getReference("Messages").child(chatId).child("lastMessage").removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VanishChatActivity, "Error updating last message: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadMessages() {
        database.getReference("Messages").child(chatId).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messagesList.clear()
                    for (messageSnap in snapshot.children) {
                        val message = messageSnap.getValue(Message::class.java) ?: continue
                        if (message.vanishMode && !message.seen && message.receiverId == auth.currentUser?.uid) {
                            database.getReference("Messages").child(chatId).child("messages")
                                .child(message.messageId).child("seen").setValue(true)
                        }
                        if (message.vanishMode && message.seen) {
                            database.getReference("Messages").child(chatId).child("messages")
                                .child(message.messageId).removeValue()
                        } else {
                            messagesList.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                    if (messagesList.isNotEmpty()) {
                        messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VanishChatActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteVanishMessages() {
        database.getReference("Messages").child(chatId).child("messages")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (messageSnap in snapshot.children) {
                        val message = messageSnap.getValue(Message::class.java) ?: continue
                        if (message.vanishMode) {
                            messageSnap.ref.removeValue()
                        }
                    }
                    database.getReference("Messages").child(chatId).child("messages").limitToLast(1)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(lastSnapshot: DataSnapshot) {
                                if (lastSnapshot.childrenCount > 0) {
                                    val lastMessage = lastSnapshot.children.first().getValue(Message::class.java)
                                    if (lastMessage != null) {
                                        database.getReference("Messages").child(chatId).child("lastMessage").setValue(mapOf(
                                            "content" to lastMessage.content,
                                            "type" to lastMessage.type,
                                            "timestamp" to lastMessage.timestamp
                                        ))
                                    }
                                } else {
                                    database.getReference("Messages").child(chatId).child("lastMessage").removeValue()
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@VanishChatActivity, "Error updating last message: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VanishChatActivity, "Error deleting messages: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}