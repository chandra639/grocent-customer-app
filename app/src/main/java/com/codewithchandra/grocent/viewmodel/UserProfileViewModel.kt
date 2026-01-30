package com.codewithchandra.grocent.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class UserProfileViewModel(private val context: Context? = null) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
    private val KEY_USER_NAME = "user_name"
    private val KEY_USER_PHOTO = "user_photo_uri"
    
    var userName by mutableStateOf(getSavedUserName() ?: "")
        private set
    
    var userPhotoUri by mutableStateOf<String?>(getSavedUserPhotoUri())
        private set
    
    private fun getSavedUserName(): String? {
        return prefs?.getString(KEY_USER_NAME, null)
    }
    
    private fun getSavedUserPhotoUri(): String? {
        return prefs?.getString(KEY_USER_PHOTO, null)
    }
    
    fun updateUserName(name: String) {
        userName = name
        prefs?.edit()?.apply {
            putString(KEY_USER_NAME, name)
            apply()
        }
    }
    
    fun updateUserPhoto(photoUri: String?) {
        userPhotoUri = photoUri
        prefs?.edit()?.apply {
            if (photoUri != null) {
                putString(KEY_USER_PHOTO, photoUri)
            } else {
                remove(KEY_USER_PHOTO)
            }
            apply()
        }
    }
}







































