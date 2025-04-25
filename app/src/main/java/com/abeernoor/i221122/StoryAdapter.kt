package com.abeernoor.i221122

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class StoryAdapter(private val stories: MutableList<story>, private val context: MainActivity) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

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

        // Load profile picture from story.image (Firebase Storage URL)
        if (story.image.isNotEmpty()) {
            try {
                val storageRef = storage.getReferenceFromUrl(story.image)
                val tempFile = File.createTempFile("profile", "jpg")
                storageRef.getFile(tempFile).addOnSuccessListener {
                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                    holder.profileImage.setImageBitmap(bitmap)
                    Log.d("StoryAdapter", "Loaded profile picture for ${story.username}: ${story.image}")
                }.addOnFailureListener {
                    holder.profileImage.setImageResource(R.drawable.profile_placeholder)
                    Log.e("StoryAdapter", "Failed to load profile picture for ${story.username}: ${it.message}")
                }
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.profile_placeholder)
                Log.e("StoryAdapter", "Error loading profile picture for ${story.username}: ${e.message}")
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
            Log.w("StoryAdapter", "No profile picture for ${story.username}")
        }

        if (story.isUserStory && currentUserId.isNotEmpty()) {
            holder.plusIcon.visibility = View.VISIBLE
            // Load the most recent story thumbnail for the user
            database.getReference("Stories").child(currentUserId)
                .orderByChild("timestamp").limitToLast(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val mostRecentStory = snapshot.children.first()
                            val storyContent = mostRecentStory.child("image").getValue(String::class.java) ?: ""
                            if (storyContent.isNotEmpty()) {
                                try {
                                    val storageRef = storage.getReferenceFromUrl(storyContent)
                                    val tempFile = File.createTempFile("story", "jpg")
                                    storageRef.getFile(tempFile).addOnSuccessListener {
                                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                        holder.profileImage.setImageBitmap(bitmap)
                                        Log.d("StoryAdapter", "Loaded story thumbnail for user: $storyContent")
                                    }.addOnFailureListener {
                                        // Keep profile picture if story thumbnail fails
                                        Log.e("StoryAdapter", "Failed to load story thumbnail: ${it.message}")
                                    }
                                } catch (e: Exception) {
                                    // Keep profile picture
                                    Log.e("StoryAdapter", "Error loading story thumbnail: ${e.message}")
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("StoryAdapter", "Error fetching user stories: ${error.message}")
                    }
                })
        } else {
            holder.plusIcon.visibility = View.GONE
        }

        // Click to view story or add new one
        holder.itemView.setOnClickListener {
            if (currentUserId.isEmpty()) {
                Log.w("StoryAdapter", "No current user ID, cannot load stories")
                return@setOnClickListener
            }

            val targetUserId = if (story.isUserStory) currentUserId else story.userId
            database.getReference("Stories").child(targetUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val storyKeys = mutableListOf<String>()
                        for (storySnap in snapshot.children) {
                            val timestamp = storySnap.child("timestamp").getValue(Long::class.java) ?: 0L
                            if (System.currentTimeMillis() - timestamp < 24 * 60 * 60 * 1000) {
                                storyKeys.add(storySnap.key!!)
                            } else {
                                storySnap.ref.removeValue()
                            }
                        }
                        if (storyKeys.isNotEmpty()) {
                            val intent = Intent(context, ViewStoryActivity::class.java)
                            intent.putStringArrayListExtra("storyKeys", ArrayList(storyKeys))
                            intent.putExtra("userId", targetUserId)
                            context.startActivity(intent)
                        } else if (story.isUserStory) {
                            context.startActivity(Intent(context, AddStoryActivity::class.java))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (story.isUserStory) {
                            context.startActivity(Intent(context, AddStoryActivity::class.java))
                        }
                        Log.e("StoryAdapter", "Error fetching stories for ${targetUserId}: ${error.message}")
                    }
                })
        }
    }

    override fun getItemCount(): Int = stories.size
}