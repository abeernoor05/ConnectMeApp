package com.abeernoor.i221122

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    companion object {
        const val USER_ID = "user_id"
        const val USERNAME = "username"
        const val IS_LOGGED_IN = "is_logged_in"
        const val IS_PROFILE_SETUP = "is_profile_setup"
    }

    fun saveUserSession(userId: String, username: String) {
        val editor = prefs.edit()
        editor.putString(USER_ID, userId)
        editor.putString(USERNAME, username)
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.putBoolean(IS_PROFILE_SETUP, false)
        editor.apply()
        Log.d("SessionManager", "Session saved: userId=$userId, username=$username, isLoggedIn=true, isProfileSetup=false")
    }

    fun markProfileSetupComplete() {
        val editor = prefs.edit()
        editor.putBoolean(IS_PROFILE_SETUP, true)
        editor.apply()
        Log.d("SessionManager", "Profile setup marked complete")
    }

    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(IS_LOGGED_IN, false)
        Log.d("SessionManager", "Checking isLoggedIn: $isLoggedIn")
        return isLoggedIn
    }

    fun isProfileSetupComplete(): Boolean {
        val isProfileSetup = prefs.getBoolean(IS_PROFILE_SETUP, false)
        Log.d("SessionManager", "Checking isProfileSetup: $isProfileSetup")
        return isProfileSetup
    }

    fun getUserId(): String? {
        val userId = prefs.getString(USER_ID, null)
        Log.d("SessionManager", "Getting userId: $userId")
        return userId
    }

    fun getUsername(): String? {
        val username = prefs.getString(USERNAME, null)
        Log.d("SessionManager", "Getting username: $username")
        return username
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
        Log.d("SessionManager", "Session cleared")
    }
}