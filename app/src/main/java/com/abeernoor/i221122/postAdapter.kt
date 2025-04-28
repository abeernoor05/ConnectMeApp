package com.abeernoor.i221122

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
// import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.database.FirebaseDatabase

class postAdapter(private val posts: MutableList<Post>) : RecyclerView.Adapter<postAdapter.PostViewHolder>() {

    private val TAG = "postAdapter"
    // private val auth = FirebaseAuth.getInstance()
    // private val database = FirebaseDatabase.getInstance()

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
        Log.d(TAG, "Binding post $position: postId=${post.postId}, userId=${post.userId}, imageLength=${post.postImage.length}")

        holder.username.text = post.username
        holder.postUsername.text = post.username
        holder.caption.text = post.caption
        holder.likesCount.text = "${post.likes.size} likes"
        holder.commentsCount.text = "${post.comments.size} comments"

        // Load profile image
        try {
            if (post.profileImage.isNotEmpty()) {
                val decodedBytes = Base64.decode(post.profileImage, Base64.DEFAULT)
                Log.d(TAG, "Profile image bytes: ${decodedBytes.size} for post $position")
                val bitmap = decodeBitmap(decodedBytes)
                if (bitmap != null) {
                    holder.profileImage.setImageBitmap(bitmap)
                    Log.d(TAG, "Profile image loaded for post $position")
                } else {
                    holder.profileImage.setImageResource(R.drawable.profile_placeholder)
                    Log.e(TAG, "Failed to decode profile image for post $position")
                }
            } else {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder)
                Log.d(TAG, "No profile image for post $position")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding profile image for post $position: ${e.message}", e)
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        // Load post image
        try {
            if (post.postImage.isNotEmpty()) {
                val decodedBytes = Base64.decode(post.postImage, Base64.DEFAULT)
                Log.d(TAG, "Post image bytes: ${decodedBytes.size} for post $position")
                val bitmap = decodeBitmap(decodedBytes)
                if (bitmap != null) {
                    holder.postImage.setImageBitmap(bitmap)
                    holder.postImage.scaleType = ImageView.ScaleType.FIT_CENTER
                    Log.d(TAG, "Post image loaded for post $position: ${bitmap.width}x${bitmap.height}")
                } else {
                    holder.postImage.setImageResource(R.drawable.post_placeholder)
                    Log.e(TAG, "Failed to decode post image for post $position")
                }
            } else {
                holder.postImage.setImageResource(R.drawable.post_placeholder)
                Log.d(TAG, "No post image for post $position")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding post image for post $position: ${e.message}", e)
            holder.postImage.setImageResource(R.drawable.post_placeholder)
        }

        // Like button logic
        val sessionManager = SessionManager(holder.itemView.context)
        val currentUserId = sessionManager.getUserId() ?: return
        if (post.likes.contains(currentUserId)) {
            holder.likeButton.setImageResource(R.drawable.heart)
        } else {
            holder.likeButton.setImageResource(R.drawable.hollow_heart)
        }

        holder.likeButton.setOnClickListener {
            Log.d(TAG, "Like button clicked for post $position")
            val requestQueue = Volley.newRequestQueue(holder.itemView.context)
            val url = "http://192.168.1.11/ConnectMe/Profile/toggleLike.php"
            val jsonBody = JSONObject().apply {
                put("post_id", post.postId)
                put("user_id", currentUserId)
            }

            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    if (response.getBoolean("success")) {
                        val liked = response.getBoolean("liked")
                        val updatedLikes = post.likes.toMutableList()
                        if (liked) {
                            updatedLikes.add(currentUserId)
                            holder.likeButton.setImageResource(R.drawable.heart)
                        } else {
                            updatedLikes.remove(currentUserId)
                            holder.likeButton.setImageResource(R.drawable.hollow_heart)
                        }
                        post.likes.clear()
                        post.likes.addAll(updatedLikes)
                        holder.likesCount.text = "${post.likes.size} likes"
                        Log.d(TAG, "Like updated for post $position: liked=$liked")
                    } else {
                        Log.e(TAG, "Failed to toggle like: ${response.optString("message")}")
                    }
                },
                { error ->
                    Log.e(TAG, "Volley error toggling like: ${error.message}")
                    holder.likeButton.setImageResource(if (post.likes.contains(currentUserId)) R.drawable.heart else R.drawable.hollow_heart)
                }
            )
            requestQueue.add(request)
        }

        // Comment button logic
        holder.commentButton.setOnClickListener {
            Log.d(TAG, "Comment button clicked for post $position")
            val intent = Intent(holder.itemView.context, CommentsActivity::class.java)
            intent.putExtra("postId", post.postId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "Item count: ${posts.size}")
        return posts.size
    }

    private fun decodeBitmap(byteArray: ByteArray): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, this)
                inSampleSize = calculateInSampleSize(this, 1080, 1920) // Target screen resolution
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.ARGB_8888 // Ensure high quality
            }
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
            Log.d(TAG, "Decoded bitmap: ${bitmap?.width}x${bitmap?.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap: ${e.message}", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        Log.d(TAG, "Calculated inSampleSize: $inSampleSize for ${width}x${height}")
        return inSampleSize
    }
}