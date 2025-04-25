package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class ChatAdapter(
    private val chatList: List<ChatModel>,
    private val onChatClick: (ChatModel) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val username: TextView = itemView.findViewById(R.id.chatUsername)
        val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        holder.username.text = chat.username
        holder.lastMessage.text = when (chat.lastMessageType) {
            "image" -> "Sent an image"
            "post" -> "Shared a post"
            else -> chat.lastMessage
        }

        if (chat.profileImage.isNotEmpty()) {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(chat.profileImage)
            val tempFile = File.createTempFile("profile", "jpg")
            storageRef.getFile(tempFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                holder.profileImage.setImageBitmap(bitmap)
            }.addOnFailureListener {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        holder.itemView.setOnClickListener { onChatClick(chat) }
    }

    override fun getItemCount(): Int = chatList.size
}