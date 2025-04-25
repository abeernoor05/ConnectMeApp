package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String,
    private val onEditClick: (Message) -> Unit,
    private val onDeleteClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderProfilePic: ImageView = itemView.findViewById(R.id.senderProfilePic)
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageImage: ImageView = itemView.findViewById(R.id.messageImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.visibility = if (message.type == "text") View.VISIBLE else View.GONE
        holder.messageImage.visibility = if (message.type == "image") View.VISIBLE else View.GONE

        if (message.type == "text") {
            holder.messageText.text = message.content
        } else if (message.type == "image") {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(message.content)
            val tempFile = File.createTempFile("message", "jpg")
            storageRef.getFile(tempFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                holder.messageImage.setImageBitmap(bitmap)
            }.addOnFailureListener {
                holder.messageImage.setImageResource(R.drawable.profile_placeholder)
            }
        }

        if (message.senderId == currentUserId) {
            holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_RTL
            holder.messageText.setBackgroundResource(R.drawable.message_bubble_right)
            holder.senderProfilePic.visibility = View.GONE
        } else {
            holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_LTR
            holder.messageText.setBackgroundResource(R.drawable.message_bubble_left)
            holder.senderProfilePic.visibility = View.VISIBLE
            FirebaseDatabase.getInstance().getReference("Users").child(message.senderId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val profileImage = snapshot.child("profileImage").getValue(String::class.java) ?: ""
                        if (profileImage.isNotEmpty()) {
                            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(profileImage)
                            val tempFile = File.createTempFile("profile", "jpg")
                            storageRef.getFile(tempFile).addOnSuccessListener {
                                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                holder.senderProfilePic.setImageBitmap(bitmap)
                            }.addOnFailureListener {
                                holder.senderProfilePic.setImageResource(R.drawable.profile_placeholder)
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        holder.itemView.setOnLongClickListener {
            if (message.senderId == currentUserId) {
                onEditClick(message)
                onDeleteClick(message)
            }
            true
        }
    }

    override fun getItemCount(): Int = messages.size
}