package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppUpdateViewModel {
    var isUpdateAvailable by mutableStateOf(false)
        private set
    
    var updateVersion by mutableStateOf<String?>(null)
        private set
    
    // Simulate checking for updates
    // In a real app, this would check with your backend or Play Store API
    fun checkForUpdates() {
        // Simulate: randomly show update (for demo purposes)
        // In production, replace this with actual update check logic
        isUpdateAvailable = false // Set to false by default, change to true when update is available
        updateVersion = "2.1.0" // Example version
    }
    
    fun setUpdateAvailable(available: Boolean, version: String? = null) {
        isUpdateAvailable = available
        updateVersion = version
    }
}

