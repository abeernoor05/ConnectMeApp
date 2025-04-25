package com.abeernoor.i221122

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import android.widget.Toast
import androidx.collection.emptyLongSet
import androidx.recyclerview.widget.RecyclerView
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FollowersAdapter(private val followersList: MutableList<Follower>) :
    RecyclerView.Adapter<FollowersAdapter.FollowerViewHolder>(){
    class FollowerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val followerName: TextView = view.findViewById(R.id.followerName)
        val messageIcon: ImageView = view.findViewById(R.id.messageIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.follower_item, parent, false)
        return FollowerViewHolder(view)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun onBindViewHolder(holder: FollowerViewHolder, position: Int) {
        val follower = followersList[position]
        holder.followerName.text = follower.name
        if(follower.image.isNotEmpty())
        {
            try{
                val decodedBytes = Base64.decode(follower.image)
                val bitmap= BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            }catch (e:Exception) {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder)
            }
        }
        else{
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }
        holder.messageIcon.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Message ${follower.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = followersList.size
}