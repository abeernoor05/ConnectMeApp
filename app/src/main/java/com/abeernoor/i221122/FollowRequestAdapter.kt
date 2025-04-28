package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FollowRequestAdapter(
    private val requests: MutableList<FollowRequest>,
    private val onAction: (FollowRequest, Boolean) -> Unit
) : RecyclerView.Adapter<FollowRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.requestUserName)
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val acceptButton: ImageView = view.findViewById(R.id.acceptRequest)
        val rejectButton: ImageView = view.findViewById(R.id.rejectRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.follow_request_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.usernameText.text = request.username

        if (request.profileImage.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(request.profileImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        holder.acceptButton.setOnClickListener {
            onAction(request, true)
        }
        holder.rejectButton.setOnClickListener {
            onAction(request, false)
        }
    }

    override fun getItemCount(): Int = requests.size
}