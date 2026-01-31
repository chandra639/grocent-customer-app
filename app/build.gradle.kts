plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    kotlin("kapt")
}

android {
    namespace = "com.codewithchandra.grocent"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.codewithchandra.grocent"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.media3.common.util.UnstableApi"
        )
    }
    buildFeatures {
        compose = true
    }
    
    // Enable kapt for Room
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // AppCompat for AppCompatActivity and AlertDialog
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    
    // Splash Screen API (Android 12+)
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Video player (ExoPlayer - Media3)
    val media3Version = "1.4.0"  // Stable version of Media3
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-datasource:$media3Version")  // For data sources
    
    // Icons Extended (use BOM for version compatibility)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Pager for image carousel
    implementation("androidx.compose.foundation:foundation:1.7.5")
    
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Google Places API (New) for address autocomplete
    // Note: Only use the new Places SDK, not the legacy one
    implementation("com.google.android.libraries.places:places:3.4.0")
    
    // Kotlin Coroutines for Play Services
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Firebase BOM (Bill of Materials) - manages all Firebase library versions
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    // Firebase Analytics (optional but recommended)
    implementation("com.google.firebase:firebase-analytics-ktx")
    
    // Firebase Cloud Messaging (push notifications)
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    // Browser support for Firebase reCAPTCHA (required for phone OTP)
    implementation("androidx.browser:browser:1.3.0")
    
    // OpenStreetMap (free, no API key needed)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    
    // OkHttp for better SSL/TLS handling and HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Room Database (Local Storage)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // Razorpay Payment Gateway
    implementation("com.razorpay:checkout:1.6.33")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}