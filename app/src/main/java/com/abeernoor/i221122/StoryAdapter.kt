package com.abeernoor.i221122

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
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
// import com.google.firebase.firestore.FirebaseFirestore

class StoryAdapter(private val stories: MutableList<story>, private val context: MainActivity) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private val sessionManager = SessionManager(context)
    private val currentUserId = sessionManager.getUserId() ?: ""
    // private val db = FirebaseFirestore.getInstance()

    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.story_image)
        val username: TextView = itemView.findViewById(R.id.storyUsername)
        val plusIcon: ImageView = itemView.findViewById(R.id.plusIcon)
        val statusDot: View = itemView.findViewById(R.id.statusDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.story_item, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.username.text = story.username

        // Set status dot color
        val drawable = holder.statusDot.background as GradientDrawable
        drawable.setColor(if (story.onlineStatus) 0xFF00FF00.toInt() else 0xFFFF0000.toInt())

        // Load profile picture
        if (story.image.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(story.image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
                Log.d("StoryAdapter", "Loaded profile picture for ${story.username}")
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder)
                Log.e("StoryAdapter", "Error loading profile picture for ${story.username}: ${e.message}")
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        // For current user's story, load latest story thumbnail
        if (story.isUserStory && currentUserId.isNotEmpty()) {
            holder.plusIcon.visibility = View.VISIBLE
            fetchLatestStoryThumbnail(currentUserId, holder.profileImage)
        } else {
            holder.plusIcon.visibility = View.GONE
        }

        // Click to view story or add new one
        holder.itemView.setOnClickListener {
            if (currentUserId.isEmpty()) {
                Log.w("StoryAdapter", "No current user ID")
                return@setOnClickListener
            }

            val targetUserId = if (story.isUserStory) currentUserId else story.userId
            fetchStories(targetUserId) { storyKeys ->
                if (storyKeys.isNotEmpty()) {
                    val intent = Intent(context, ViewStoryActivity::class.java)
                    intent.putStringArrayListExtra("storyKeys", ArrayList(storyKeys))
                    intent.putExtra("userId", targetUserId)
                    context.startActivity(intent)
                } else if (story.isUserStory) {
                    context.startActivity(Intent(context, AddStoryActivity::class.java))
                }
            }
        }
    }

    private fun fetchLatestStoryThumbnail(userId: String, imageView: ImageView) {
        val requestQueue = Volley.newRequestQueue(context)
        val url = "http://192.168.1.11/ConnectMe/Story/getStories.php"
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    val storiesArray = response.getJSONArray("stories")
                    var latestStory: JSONObject? = null
                    var latestTimestamp = 0L
                    for (i in 0 until storiesArray.length()) {
                        val storyJson = storiesArray.getJSONObject(i)
                        if (storyJson.getString("user_id") == userId && storyJson.getLong("timestamp") > latestTimestamp) {
                            latestStory = storyJson
                            latestTimestamp = storyJson.getLong("timestamp")
                        }
                    }
                    if (latestStory != null && latestStory.getString("story_content").isNotEmpty()) {
                        try {
                            val content = latestStory.getString("story_content")
                            if (!latestStory.getBoolean("is_video")) {
                                val decodedBytes = Base64.decode(content, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                imageView.setImageBitmap(bitmap)
                                Log.d("StoryAdapter", "Loaded story thumbnail for user $userId")
                            }
                        } catch (e: Exception) {
                            Log.e("StoryAdapter", "Error loading story thumbnail: ${e.message}")
                        }
                    }
                }
            },
            { error ->
                Log.e("StoryAdapter", "Volley error fetching stories: ${error.message}")
            }
        )
        requestQueue.add(request)

        /*
        // Firebase logic for fetching latest story thumbnail
        db.collection("stories")
            .whereEqualTo("user_id", userId)
            .whereGreaterThan("timestamp", System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val storyData = documents.documents[0].data
                    if (storyData != null && storyData["story_content"] != null && storyData["is_video"] == false) {
                        try {
                            val content = storyData["story_content"] as String
                            val decodedBytes = Base64.decode(content, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            imageView.setImageBitmap(bitmap)
                            Log.d("StoryAdapter", "Loaded Firebase story thumbnail for user $userId")
                        } catch (e: Exception) {
                            Log.e("StoryAdapter", "Error loading Firebase story thumbnail: ${e.message}")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("StoryAdapter", "Error fetching Firebase story: ${e.message}")
            }
        */
    }

    private fun fetchStories(userId: String, callback: (List<String>) -> Unit) {
        val requestQueue = Volley.newRequestQueue(context)
        val url = "http://192.168.1.11/ConnectMe/Story/getStories.php"
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    val storiesArray = response.getJSONArray("stories")
                    val storyKeys = mutableListOf<String>()
                    for (i in 0 until storiesArray.length()) {
                        val storyJson = storiesArray.getJSONObject(i)
                        if (storyJson.getString("user_id") == userId && storyJson.getLong("timestamp") > 0) {
                            storyKeys.add(storyJson.getString("story_id"))
                        }
                    }
                    callback(storyKeys)
                } else {
                    callback(emptyList())
                }
            },
            { error ->
                Log.e("StoryAdapter", "Volley error fetching stories: ${error.message}")
                callback(emptyList())
            }
        )
        requestQueue.add(request)

        /*
        // Firebase logic for fetching stories
        db.collection("stories")
            .whereEqualTo("user_id", userId)
            .whereGreaterThan("timestamp", System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            .get()
            .addOnSuccessListener { documents ->
                val storyKeys = documents.documents.map { it.id }
                callback(storyKeys)
            }
            .addOnFailureListener { e ->
                Log.e("StoryAdapter", "Error fetching Firebase stories: ${e.message}")
                callback(emptyList())
            }
        */
    }

    override fun getItemCount(): Int = stories.size
}