package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FollowingAdapter(private val followingList: MutableList<Following>) :
    RecyclerView.Adapter<FollowingAdapter.FollowingViewHolder>() {

    class FollowingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val followerName: TextView = view.findViewById(R.id.followerName)
        val messageIcon: ImageView = view.findViewById(R.id.messageIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.follower_item, parent, false)
        return FollowingViewHolder(view)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun onBindViewHolder(holder: FollowingViewHolder, position: Int) {
        val following = followingList[position]
        holder.followerName.text = following.name
        if (following.image.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(following.image)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder) // Fallback
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }
        holder.messageIcon.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Message ${following.name}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun getItemCount(): Int = followingList.size
}
