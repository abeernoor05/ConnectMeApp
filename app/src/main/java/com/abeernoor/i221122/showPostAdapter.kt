package com.abeernoor.i221122

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class showPostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<showPostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        try {
            if (post.postImage.isNotEmpty()) {
                val decodedBytes = Base64.decode(post.postImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } else {
                holder.postImage.setImageResource(R.drawable.post_placeholder)
            }
        } catch (e: Exception) {
            holder.postImage.setImageResource(R.drawable.post_placeholder)
        }

        // Optional: Click to view post details
//        holder.postImage.setOnClickListener {
//            val context = holder.itemView.context
//            val intent = Intent(context, PostDetailActivity::class.java).apply {
//                putExtra("post_id", post.postId)
//            }
//            context.startActivity(intent)
//        }
    }

    override fun getItemCount(): Int = posts.size
}