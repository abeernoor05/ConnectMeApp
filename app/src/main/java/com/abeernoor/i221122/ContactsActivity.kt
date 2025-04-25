package com.abeernoor.i221122

import InviteFriendsAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class ContactsActivity : AppCompatActivity() {

    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var inviteRecyclerView: RecyclerView
    private lateinit var contactsAdapter: ContactAdapter
    private lateinit var inviteFriendsAdapter: InviteFriendsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        contactsRecyclerView = findViewById(R.id.contactsRecyclerView)
        inviteRecyclerView = findViewById(R.id.inviteRecyclerView)
        val backbtn=findViewById<ImageView>(R.id.backIcon)

        contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        inviteRecyclerView.layoutManager = LinearLayoutManager(this)

        backbtn.setOnClickListener {
            finish()
        }

        val contactsList = listOf(
            Contact("Alice", R.drawable.post1),
            Contact("Bob", R.drawable.post2),
            Contact("Charlie", R.drawable.post3)
        )

        val inviteList = listOf(
            InviteFriend("David", R.drawable.post4),
            InviteFriend("Emma", R.drawable.post5),
            InviteFriend("Liam", R.drawable.post6)
        )

        contactsAdapter = ContactAdapter(contactsList)
        inviteFriendsAdapter = InviteFriendsAdapter(inviteList)

        contactsRecyclerView.adapter = contactsAdapter
        inviteRecyclerView.adapter = inviteFriendsAdapter

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_contacts -> return@setOnItemSelectedListener true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    return@setOnItemSelectedListener true
                }

                R.id.nav_post -> {
                    startActivity(Intent(this, PostActivity::class.java))
                    return@setOnItemSelectedListener true
                }

                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    return@setOnItemSelectedListener true
                }

                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    return@setOnItemSelectedListener true
                }

                else -> false
            }
        }
    }
}
