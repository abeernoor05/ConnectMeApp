package com.abeernoor.i221122

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject

class SearchActivity : AppCompatActivity() {

    private lateinit var searchAdapter: SearchAdapter
    private val searchResults = mutableListOf<Search>()
    private val recentSearches = mutableListOf<String>()
    private var currentFilter = "All"
    private lateinit var recentSearchesText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val sessionManager = SessionManager(this)

        val recyclerView: RecyclerView = findViewById(R.id.recentSearchesRecyclerView)
        val searchBar: EditText = findViewById(R.id.searchBar)
        val filterAll: TextView = findViewById(R.id.filterAll)
        recentSearchesText = findViewById(R.id.recentSearchesText)
        val filterFollowers: TextView = findViewById(R.id.filterFollowers)
        val filterFollowing: TextView = findViewById(R.id.filterFollowing)

        searchAdapter = SearchAdapter(searchResults) { username: String ->
            Log.d("SearchActivity", "Clicked username: $username")
            if (username.isNotEmpty()) {
                openProfileView(username)
            } else {
                Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show()
            }
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
                    val recentSearchItems = recentSearches.map { Search(0, it, "") }
                    searchAdapter.updateData(recentSearchItems.toMutableList())
                } else {
                    recentSearchesText.text = "Search Results"
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
        val userId = SessionManager(this).getUserId()?.toIntOrNull() ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val requestQueue = Volley.newRequestQueue(this)
        val searchUrl = "http://192.168.1.11/ConnectMe/Profile/searchUsers.php"
        val searchJson = JSONObject().apply {
            put("user_id", userId)
            put("query", query)
            put("filter", currentFilter)
        }

        val searchRequest = JsonObjectRequest(
            Request.Method.POST, searchUrl, searchJson,
            { response ->
                Log.d("SearchActivity", "Search response: $response")
                if (response.getBoolean("success")) {
                    searchResults.clear()
                    val usersArray = response.getJSONArray("users")
                    for (i in 0 until usersArray.length()) {
                        val user = usersArray.getJSONObject(i)
                        val id = user.getInt("id")
                        val username = user.optString("username", "Unknown")
                        val profileImage = user.optString("profile_image", "")
                        searchResults.add(Search(id, username, profileImage))
                    }
                    searchAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, response.optString("message", "No users found"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("SearchActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error: ${error.message ?: "Failed to search"}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(searchRequest)
    }

    private fun openProfileView(username: String) {
        val intent = Intent(this, ProfileViewActivity::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
    }

    private fun loadRecentSearches() {
        val prefs = getSharedPreferences("RecentSearches", Context.MODE_PRIVATE)
        val searches = prefs.getStringSet("searches", emptySet())?.toList() ?: emptyList()
        recentSearches.clear()
        recentSearches.addAll(searches)
        val recentSearchItems = recentSearches.map { Search(0, it, "") }
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