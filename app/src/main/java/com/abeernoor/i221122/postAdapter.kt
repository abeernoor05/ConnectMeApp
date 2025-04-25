package com.abeernoor.i221122

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class postAdapter(private val posts: MutableList<Post>) : RecyclerView.Adapter<postAdapter.PostViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val username: TextView = itemView.findViewById(R.id.username)
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
        val postUsername: TextView = itemView.findViewById(R.id.postUsername)
        val caption: TextView = itemView.findViewById(R.id.postCaption)
        val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        val commentButton: ImageView = itemView.findViewById(R.id.commentButton)
        val likesCount: TextView = itemView.findViewById(R.id.likesCount)
        val commentsCount: TextView = itemView.findViewById(R.id.commentsCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.username.text = post.username
        holder.postUsername.text = post.username
        holder.caption.text = post.caption
        holder.likesCount.text = "${post.likes.size} likes"
        holder.commentsCount.text = "${post.comments.size} comments"

        // Load profile image
        if (post.profileImage.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(post.profileImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        // Load post image
        if (post.postImage.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(post.postImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.post_placeholder)
            }
        } else {
            holder.postImage.setImageResource(R.drawable.post_placeholder)
        }

        // Like button logic
        val currentUserId = auth.currentUser?.uid ?: return
        if (post.likes.contains(currentUserId)) {
            holder.likeButton.setImageResource(R.drawable.heart)
        } else {
            holder.likeButton.setImageResource(R.drawable.hollow_heart)
        }

        holder.likeButton.setOnClickListener {
            val updatedLikes = post.likes.toMutableList()
            if (updatedLikes.contains(currentUserId)) {
                updatedLikes.remove(currentUserId)
                holder.likeButton.setImageResource(R.drawable.hollow_heart)
            } else {
                updatedLikes.add(currentUserId)
                holder.likeButton.setImageResource(R.drawable.heart)
            }
            post.likes.clear()
            post.likes.addAll(updatedLikes)
            holder.likesCount.text = "${post.likes.size} likes"
            database.getReference("Posts").child(post.postId).child("likes").setValue(post.likes)
        }

        // Comment button logic
        holder.commentButton.setOnClickListener {
            val intent = Intent(holder.itemView.context, CommentsActivity::class.java)
            intent.putExtra("postId", post.postId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = posts.size
}