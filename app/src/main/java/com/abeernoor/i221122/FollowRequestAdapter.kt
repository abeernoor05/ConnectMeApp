package com.abeernoor.i221122

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FollowRequestAdapter(
    private val requestList: MutableList<FollowRequest>,
    private val onRequestAction: (FollowRequest, Boolean) -> Unit
) : RecyclerView.Adapter<FollowRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val userName: TextView = view.findViewById(R.id.requestUserName)
        val acceptButton: ImageView = view.findViewById(R.id.acceptRequest)
        val rejectButton: ImageView = view.findViewById(R.id.rejectRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.follow_request_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requestList[position]
        holder.userName.text = request.username

        // Placeholder image (fetch from database if needed)
        holder.profileImage.setImageResource(R.drawable.profile_placeholder)

        holder.acceptButton.setOnClickListener {
            onRequestAction(request, true)
            requestList.removeAt(position)
            notifyItemRemoved(position)
        }

        holder.rejectButton.setOnClickListener {
            onRequestAction(request, false)
            requestList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount(): Int = requestList.size
}