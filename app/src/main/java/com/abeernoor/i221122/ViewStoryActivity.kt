package com.abeernoor.i221122

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class ViewStoryActivity : AppCompatActivity() {

    private lateinit var storiesViewPager: ViewPager2
    private lateinit var stories: MutableList<story>
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_story)

        storiesViewPager = findViewById(R.id.storiesViewPager)
        val closeButton = findViewById<ImageView>(R.id.closeButton)
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        val storyKeys = intent.getStringArrayListExtra("storyKeys") ?: emptyList()
        val userId = intent.getStringExtra("userId") ?: ""

        if (storyKeys.isEmpty() || userId.isEmpty()) {
            finish()
            return
        }

        stories = mutableListOf()
        fetchStories(userId, storyKeys)

        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchStories(userId: String, storyKeys: List<String>) {
        val storiesRef = database.getReference("Stories").child(userId)
        // Fetch username and profile picture
        database.getReference("Users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val username = userSnapshot.child("username").getValue(String::class.java) ?: "Unknown"
                val profileImage = userSnapshot.child("image").getValue(String::class.java) ?: ""
                Log.d("ViewStoryActivity", "Fetched profile picture for $username: $profileImage")

                stories.clear()
                for (key in storyKeys) {
                    storiesRef.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val storyImage = snapshot.child("image").getValue(String::class.java) ?: ""
                            val videoUrl = snapshot.child("videoUrl").getValue(String::class.java) ?: ""
                            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                            val story = story(
                                userId = userId,
                                username = username,
                                image = profileImage, // Profile picture from Users
                                storyContent = storyImage, // Story image
                                videoUrl = videoUrl,
                                timestamp = timestamp,
                                isUserStory = false,
                                onlineStatus = false
                            )
                            stories.add(story)

                            // Update the adapter once all stories are fetched
                            if (stories.size == storyKeys.size) {
                                storiesViewPager.adapter = StoryViewAdapter(stories)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("ViewStoryActivity", "Error fetching story $key: ${error.message}")
                            if (stories.size == storyKeys.size) {
                                storiesViewPager.adapter = StoryViewAdapter(stories)
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ViewStoryActivity", "Error fetching user $userId: ${error.message}")
                finish()
            }
        })
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

        if (story.storyContent.isNotEmpty()) {
            holder.storyImage.visibility = View.VISIBLE
            holder.storyVideo.visibility = View.GONE
            try {
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(story.storyContent)
                val tempFile = File.createTempFile("story", "jpg")
                storageRef.getFile(tempFile).addOnSuccessListener {
                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                    holder.storyImage.setImageBitmap(bitmap)
                    Log.d("StoryViewAdapter", "Loaded story image: ${story.storyContent}")
                }.addOnFailureListener {
                    holder.storyImage.setImageResource(R.drawable.profile_placeholder)
                    Log.e("StoryViewAdapter", "Failed to load story image: ${it.message}")
                }
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
                Log.d("StoryViewAdapter", "Playing story video: ${story.videoUrl}")
            } catch (e: Exception) {
                Log.e("StoryViewAdapter", "Error playing story video: ${e.message}")
            }
        } else {
            holder.storyImage.visibility = View.VISIBLE
            holder.storyVideo.visibility = View.GONE
            holder.storyImage.setImageResource(R.drawable.profile_placeholder)
            Log.w("StoryViewAdapter", "No story content or video for story")
        }
    }

    override fun getItemCount(): Int = stories.size
}