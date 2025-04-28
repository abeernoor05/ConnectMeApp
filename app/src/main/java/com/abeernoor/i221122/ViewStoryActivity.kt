package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
// import com.google.firebase.firestore.FirebaseFirestore

class ViewStoryActivity : AppCompatActivity() {

    private lateinit var storiesViewPager: ViewPager2
    private lateinit var stories: MutableList<story>
    // private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_story)

        storiesViewPager = findViewById(R.id.storiesViewPager)
        val closeButton = findViewById<ImageView>(R.id.closeButton)
        stories = mutableListOf()

        val storyKeys = intent.getStringArrayListExtra("storyKeys") ?: emptyList()
        val userId = intent.getStringExtra("userId") ?: ""

        if (storyKeys.isEmpty() || userId.isEmpty()) {
            finish()
            return
        }

        fetchStories(userId, storyKeys)

        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchStories(userId: String, storyKeys: List<String>) {
        val requestQueue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.11/ConnectMe/Story/getStories.php"
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    val storiesArray = response.getJSONArray("stories")
                    stories.clear()
                    for (i in 0 until storiesArray.length()) {
                        val storyJson = storiesArray.getJSONObject(i)
                        if (storyKeys.contains(storyJson.getString("story_id")) && storyJson.getString("user_id") == userId) {
                            val story = story(
                                userId = storyJson.getString("user_id"),
                                username = storyJson.getString("username"),
                                image = storyJson.getString("profile_image"),
                                storyContent = storyJson.getString("story_content"),
                                videoUrl = if (storyJson.getBoolean("is_video")) storyJson.getString("story_content") else "",
                                timestamp = storyJson.getLong("timestamp"),
                                isUserStory = false,
                                onlineStatus = storyJson.getString("status") == "online"
                            )
                            stories.add(story)
                        }
                    }
                    storiesViewPager.adapter = StoryViewAdapter(stories)
                } else {
                    Log.e("ViewStoryActivity", "Failed to fetch stories: ${response.optString("message")}")
                    finish()
                }
            },
            { error ->
                Log.e("ViewStoryActivity", "Volley error: ${error.message}")
                finish()
            }
        )
        requestQueue.add(request)

        /*
        // Firebase logic for fetching stories
        stories.clear()
        storyKeys.forEach { storyId ->
            db.collection("stories").document(storyId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val storyData = document.data
                        if (storyData != null && storyData["user_id"] == userId) {
                            db.collection("users").document(userId).get()
                                .addOnSuccessListener { userDoc ->
                                    stories.add(
                                        story(
                                            userId = userId,
                                            username = userDoc.getString("username") ?: "",
                                            image = userDoc.getString("profile_image") ?: "",
                                            storyContent = storyData["story_content"] as String,
                                            videoUrl = if (storyData["is_video"] == true) storyData["story_content"] as String else "",
                                            timestamp = storyData["timestamp"] as Long,
                                            isUserStory = false,
                                            onlineStatus = userDoc.getString("status") == "online"
                                        )
                                    )
                                    storiesViewPager.adapter = StoryViewAdapter(stories)
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ViewStoryActivity", "Error fetching Firebase story: ${e.message}")
                    finish()
                }
        }
        */
    }
}

class StoryViewAdapter(private val stories: List<story>) : RecyclerView.Adapter<StoryViewAdapter.StoryViewHolder>() {

    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyImage: ImageView = itemView.findViewById(R.id.storyImage)
        val storyVideo: VideoView = itemView.findViewById(R.id.storyVideo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.story_view_item, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        if (story.storyContent.isNotEmpty() && !story.videoUrl.isNotEmpty()) {
            holder.storyImage.visibility = View.VISIBLE
            holder.storyVideo.visibility = View.GONE
            try {
                val decodedBytes = Base64.decode(story.storyContent, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.storyImage.setImageBitmap(bitmap)
                Log.d("StoryViewAdapter", "Loaded story image")
            } catch (e: Exception) {
                holder.storyImage.setImageResource(R.drawable.profile_placeholder)
                Log.e("StoryViewAdapter", "Error loading story image: ${e.message}")
            }
        } else if (story.videoUrl.isNotEmpty()) {
            holder.storyImage.visibility = View.GONE
            holder.storyVideo.visibility = View.VISIBLE
            try {
                holder.storyVideo.setVideoURI(Uri.parse(story.videoUrl))
                holder.storyVideo.start()
                Log.d("StoryViewAdapter", "Playing story video")
            } catch (e: Exception) {
                Log.e("StoryViewAdapter", "Error playing story video: ${e.message}")
            }
        } else {
            holder.storyImage.visibility = View.VISIBLE
            holder.storyVideo.visibility = View.GONE
            holder.storyImage.setImageResource(R.drawable.profile_placeholder)
            Log.w("StoryViewAdapter", "No story content or video")
        }
    }

    override fun getItemCount(): Int = stories.size
}