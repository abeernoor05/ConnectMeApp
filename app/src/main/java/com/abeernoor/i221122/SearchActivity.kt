package com.abeernoor.i221122

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchActivity : AppCompatActivity() {

    private lateinit var searchAdapter: SearchAdapter
    private val searchResults = mutableListOf<Search>()
    private val recentSearches = mutableListOf<String>()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var currentFilter = "All"
    private lateinit var recentSearchesText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val recyclerView: RecyclerView = findViewById(R.id.recentSearchesRecyclerView)
        val searchBar: EditText = findViewById(R.id.searchBar)
        val filterAll: TextView = findViewById(R.id.filterAll)
        recentSearchesText = findViewById(R.id.recentSearchesText)
        val filterFollowers: TextView = findViewById(R.id.filterFollowers)
        val filterFollowing: TextView = findViewById(R.id.filterFollowing)

        searchAdapter = SearchAdapter(searchResults) { username ->
            val intent = Intent(this, ProfileViewActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = searchAdapter
        loadRecentSearches()

        filterAll.setOnClickListener {
            currentFilter = "All"
            filterAll.setTextColor(resources.getColor(android.R.color.black))
            filterFollowers.setTextColor(resources.getColor(android.R.color.darker_gray))
            filterFollowing.setTextColor(resources.getColor(android.R.color.darker_gray))
            if (searchBar.text.isNotEmpty()) searchUsers(searchBar.text.toString())
        }

        filterFollowers.setOnClickListener {
            currentFilter = "Followers"
            filterAll.setTextColor(resources.getColor(android.R.color.darker_gray))
            filterFollowers.setTextColor(resources.getColor(android.R.color.black))
            filterFollowing.setTextColor(resources.getColor(android.R.color.darker_gray))
            if (searchBar.text.isNotEmpty()) searchUsers(searchBar.text.toString())
        }

        filterFollowing.setOnClickListener {
            currentFilter = "Following"
            filterAll.setTextColor(resources.getColor(android.R.color.darker_gray))
            filterFollowers.setTextColor(resources.getColor(android.R.color.darker_gray))
            filterFollowing.setTextColor(resources.getColor(android.R.color.black))
            if (searchBar.text.isNotEmpty()) searchUsers(searchBar.text.toString())
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    recentSearchesText.text = "Recent Searches"
                    val recentSearchItems = recentSearches.map { Search(it, "") }
                    searchAdapter.updateData(recentSearchItems.toMutableList())
                } else {
                    recentSearchesText.text = "Search Results"
                    searchAdapter.updateData(searchResults)
                    searchUsers(s.toString())
                }
            }
        })

        searchBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = searchBar.text.toString().trim()
                if (query.isNotEmpty()) {
                    saveRecentSearch(query)
                }
                true
            } else {
                false
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_post -> {
                    startActivity(Intent(this, PostActivity::class.java))
                    true
                }
                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun searchUsers(query: String) {
        searchResults.clear()
        val currentUserId = auth.currentUser?.uid ?: return

        when (currentFilter) {
            "All" -> {
                database.getReference("Users").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null && user.username.contains(query, ignoreCase = true)) {
                                searchResults.add(Search(user.username, user.image))
                            }
                        }
                        searchAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SearchActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            "Followers" -> {
                database.getReference("Followers").child(currentUserId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (followerSnapshot in snapshot.children) {
                                val followerId = followerSnapshot.key ?: continue
                                database.getReference("Users").child(followerId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(userSnapshot: DataSnapshot) {
                                            val user = userSnapshot.getValue(User::class.java)
                                            if (user != null && user.username.contains(query, ignoreCase = true)) {
                                                searchResults.add(Search(user.username, user.image))
                                                searchAdapter.notifyDataSetChanged()
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(this@SearchActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@SearchActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }

            "Following" -> {
                database.getReference("Following").child(currentUserId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (followingSnapshot in snapshot.children) {
                                val followingId = followingSnapshot.key ?: continue
                                database.getReference("Users").child(followingId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(userSnapshot: DataSnapshot) {
                                            val user = userSnapshot.getValue(User::class.java)
                                            if (user != null && user.username.contains(query, ignoreCase = true)) {
                                                searchResults.add(Search(user.username, user.image))
                                                searchAdapter.notifyDataSetChanged()
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(this@SearchActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@SearchActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
    }

    private fun loadRecentSearches() {
        val prefs = getSharedPreferences("RecentSearches", Context.MODE_PRIVATE)
        val searches = prefs.getStringSet("searches", emptySet())?.toList() ?: emptyList()
        recentSearches.clear()
        recentSearches.addAll(searches)
        val recentSearchItems = recentSearches.map { Search(it, "") }
        searchAdapter.updateData(recentSearchItems.toMutableList())
    }

    private fun saveRecentSearch(query: String) {
        val prefs = getSharedPreferences("RecentSearches", Context.MODE_PRIVATE)
        val searches = prefs.getStringSet("searches", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        searches.remove(query)
        searches.add(query)
        if (searches.size > 5) searches.remove(searches.first())
        prefs.edit().putStringSet("searches", searches).apply()

        loadRecentSearches()
    }
}