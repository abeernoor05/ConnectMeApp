package com.abeernoor.i221122

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(
    private val searchResults: MutableList<Search>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val deleteIcon: ImageView = view.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_item, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val result = searchResults[position]
        holder.usernameText.text = result.username

        if (result.image.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(result.image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        // Show delete icon for recent searches, hide for search results
        if (result.userId == 0) {
            holder.deleteIcon.visibility = View.VISIBLE
            holder.deleteIcon.setOnClickListener {
                searchResults.removeAt(position)
                notifyItemRemoved(position)
                val prefs = holder.itemView.context.getSharedPreferences("RecentSearches", Context.MODE_PRIVATE)
                val searches = prefs.getStringSet("searches", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                searches.remove(result.username)
                prefs.edit().putStringSet("searches", searches).apply()
            }
        } else {
            holder.deleteIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(result.username)
        }
    }

    override fun getItemCount(): Int = searchResults.size

    fun updateData(newData: MutableList<Search>) {
        searchResults.clear()
        searchResults.addAll(newData)
        notifyDataSetChanged()
    }
}