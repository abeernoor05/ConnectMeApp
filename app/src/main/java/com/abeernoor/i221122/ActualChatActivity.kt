package com.abeernoor.i221122

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

class ActualChatActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var chatName: TextView
    private lateinit var profilePic: ImageView
    private lateinit var uploadMedia: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()
    private lateinit var chatId: String
    private lateinit var otherUserId: String
    private lateinit var username: String
    private lateinit var screenshotObserver: ContentObserver

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleMediaUpload(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            pickMedia.launch("*/*")
        } else {
            Toast.makeText(this, "Permission denied. Cannot access media.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_example)

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
        uploadMedia = findViewById(R.id.mediaButton)

        chatName.text = username
        database.getReference("Users").child(otherUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profileImage = snapshot.child("profileImage").getValue(String::class.java) ?: ""
                if (profileImage.isNotEmpty()) {
                    try {
                        val storageRef = storage.getReferenceFromUrl(profileImage)
                        val tempFile = File.createTempFile("profile", "jpg")
                        storageRef.getFile(tempFile).addOnSuccessListener {
                            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                            profilePic.setImageBitmap(bitmap)
                        }.addOnFailureListener {
                            profilePic.setImageResource(R.drawable.profile_placeholder)
                        }
                    } catch (e: Exception) {
                        profilePic.setImageResource(R.drawable.profile_placeholder)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ActualChatActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        messageAdapter = MessageAdapter(messagesList, auth.currentUser?.uid ?: "", { message ->
            if (System.currentTimeMillis() - message.timestamp < 5 * 60 * 1000) {
                messageInput.setText(message.content)
                sendButton.setOnClickListener {
                    editMessage(message.messageId, messageInput.text.toString())
                    messageInput.text.clear()
                    sendButton.setOnClickListener { sendMessage() }
                }
            } else {
                Toast.makeText(this, "Cannot edit message after 5 minutes", Toast.LENGTH_SHORT).show()
            }
        }, { message ->
            if (System.currentTimeMillis() - message.timestamp < 5 * 60 * 1000) {
                deleteMessage(message.messageId)
            } else {
                Toast.makeText(this, "Cannot delete message after 5 minutes", Toast.LENGTH_SHORT).show()
            }
        })
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messageAdapter

        val voiceCallButton: ImageView = findViewById(R.id.voiceCall)
        val videoCallButton: ImageView = findViewById(R.id.videoCall)
        val backButton: ImageView = findViewById(R.id.backButton)
        val vanishMode: TextView = findViewById(R.id.vanishMode)
        val viewProfileButton: TextView = findViewById(R.id.viewProfileButton)

        voiceCallButton.setOnClickListener {
            startActivity(Intent(this, VoiceCallActivity::class.java))
        }
        videoCallButton.setOnClickListener {
            startActivity(Intent(this, VideoCallActivity::class.java))
        }
        backButton.setOnClickListener { finish() }
        vanishMode.setOnClickListener {
            database.getReference("Messages").child(chatId).child("vanishMode").setValue(true)
            startActivity(Intent(this, VanishChatActivity::class.java).apply {
                putExtra("chatId", chatId)
                putExtra("userId", otherUserId)
                putExtra("username", username)
            })
        }
        viewProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra("userId", otherUserId)
            })
        }

        sendButton.setOnClickListener { sendMessage() }
        uploadMedia.setOnClickListener {
            requestMediaPermissions()
        }

        loadMessages()
        setupScreenshotDetection()
    }

    private fun setupScreenshotDetection() {
        val contentResolver: ContentResolver = contentResolver
        screenshotObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let {
                    val path = getFilePathFromUri(it)
                    if (path != null && (path.contains("Screenshot") || path.contains("screen_shot"))) {
                        sendScreenshotNotification()
                    }
                }
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver
        )
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    private fun sendScreenshotNotification() {
        val currentUserId = auth.currentUser?.uid ?: return
        val notificationId = UUID.randomUUID().toString()
        val notification = Notification(
            notificationId = notificationId,
            type = "screenshot",
            chatId = chatId,
            senderId = currentUserId,
            receiverId = otherUserId,
            message = "$username took a screenshot in your chat",
            timestamp = System.currentTimeMillis()
        )
        database.getReference("Notifications").child(otherUserId).child(notificationId).setValue(notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(screenshotObserver)
    }

    private fun requestMediaPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions)
    }

    private fun handleMediaUpload(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        when {
            mimeType?.startsWith("image/") == true -> uploadImage(uri)
            mimeType?.startsWith("video/") == true -> uploadVideo(uri)
            else -> Toast.makeText(this, "Unsupported media type", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImage(uri: Uri) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageId = UUID.randomUUID().toString()
        val storageRef = storage.getReference("chat_images/$chatId/$messageId")

        try {
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
                        vanishMode = false
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
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadVideo(uri: Uri) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageId = UUID.randomUUID().toString()
        val storageRef = storage.getReference("chat_videos/$chatId/$messageId")

        try {
            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val message = Message(
                        messageId = messageId,
                        senderId = currentUserId,
                        receiverId = otherUserId,
                        type = "video",
                        content = downloadUrl.toString(),
                        timestamp = System.currentTimeMillis(),
                        vanishMode = false
                    )
                    database.getReference("Messages").child(chatId).child("messages").child(messageId).setValue(message)
                    database.getReference("Messages").child(chatId).child("lastMessage").setValue(mapOf(
                        "content" to downloadUrl.toString(),
                        "type" to "video",
                        "timestamp" to System.currentTimeMillis()
                    ))
                    database.getReference("Chats").child(currentUserId).child(chatId).setValue(true)
                    database.getReference("Chats").child(otherUserId).child(chatId).setValue(true)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Video upload failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing video", Toast.LENGTH_SHORT).show()
        }
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
            vanishMode = false
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

    private fun editMessage(messageId: String, newContent: String) {
        database.getReference("Messages").child(chatId).child("messages").child(messageId).child("content").setValue(newContent)
        database.getReference("Messages").child(chatId).child("lastMessage").child("content").setValue(newContent)
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
                    Toast.makeText(this@ActualChatActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
                        if (!message.seen && message.receiverId == auth.currentUser?.uid) {
                            database.getReference("Messages").child(chatId).child("messages")
                                .child(message.messageId).child("seen").setValue(true)
                        }
                        messagesList.add(message)
                    }
                    messageAdapter.notifyDataSetChanged()
                    if (messagesList.isNotEmpty()) {
                        messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ActualChatActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}