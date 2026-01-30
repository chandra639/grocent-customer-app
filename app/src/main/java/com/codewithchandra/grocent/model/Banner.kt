package com.codewithchandra.grocent.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

enum class BannerMediaType {
    IMAGE,
    VIDEO
}

data class Banner(
    val id: String = "",
    val mediaType: BannerMediaType = BannerMediaType.IMAGE,  // IMAGE or VIDEO
    val imageUrl: String = "",
    val videoUrl: String = "",  // URL for video files (MP4, etc.)
    val title: String = "",
    val deepLink: String = "",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    @PropertyName("active") val isActive: Boolean = true,  // Maps to "active" field in Firestore
    val priority: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    // Duration settings (in milliseconds)
    val imageDisplayDuration: Long = 5000,  // Default 5 seconds for images
    val videoPlayDuration: Long = 0,  // 0 = play full video, >0 = custom duration in milliseconds
    val playFullVideo: Boolean = true  // If true, play full video; if false, use videoPlayDuration
)







































