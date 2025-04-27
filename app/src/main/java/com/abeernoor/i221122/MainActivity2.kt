package com.abeernoor.i221122

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Adjust to R.layout.activity_main if correct

        val sessionManager = SessionManager(this)

        // Log session state for debugging
        Log.d("MainActivity2", "isLoggedIn: ${sessionManager.isLoggedIn()}")
        Log.d("MainActivity2", "userId: ${sessionManager.getUserId()}")
        Log.d("MainActivity2", "username: ${sessionManager.getUsername()}")

        // Clear invalid session (e.g., logged in but no userId)
        if (sessionManager.isLoggedIn() && sessionManager.getUserId() == null) {
            Log.d("MainActivity2", "Invalid session detected, clearing session")
            sessionManager.clearSession()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (sessionManager.isLoggedIn()) {
                Log.d("MainActivity2", "Navigating to MainActivity (logged in)")
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                Log.d("MainActivity2", "Navigating to LoginActivity (not logged in or no user)")
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 5000) // 5-second delay for splash
    }
}