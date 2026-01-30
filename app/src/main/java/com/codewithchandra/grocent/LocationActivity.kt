package com.codewithchandra.grocent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import java.util.Locale

/**
 * LocationActivity - Handles GPS location fetching with FusedLocationProviderClient
 * 
 * Updated Flow:
 * 1. On app open, if location enabled → automatically fetch location
 * 2. If location not enabled → show "Enable device location" dialog
 * 3. If user chooses "Enable device location" → show "Location Accuracy" popup (No thanks / Turn on)
 * 4. If "Turn on" → automatically enable and fetch location (no device screen navigation)
 * 5. If "No thanks" → show address selection (saved addresses or current location)
 */
class LocationActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvAddress: TextView
    private lateinit var btnRefreshLocation: Button

    // Location Services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted - check if location is enabled
            if (isLocationEnabled()) {
                // Location enabled - fetch automatically
                fetchCurrentLocation()
            } else {
                // Location not enabled - show "Enable device location" dialog
                showEnableLocationDialog()
            }
        } else {
            // Permission denied - show dialog with options
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeLocationServices()
        
        // On app open: Check if location is already enabled
        checkInitialLocationStatus()
    }

    private fun initializeViews() {
        tvAddress = findViewById(R.id.tvAddress)
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)

        btnRefreshLocation.setOnClickListener {
            checkInitialLocationStatus()
        }
    }

    private fun initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMaxUpdateDelayMillis(5000)
            .build()
    }

    /**
     * Check initial location status on app open
     * If location enabled → automatically fetch location
     * If not enabled → show enable location dialog
     */
    private fun checkInitialLocationStatus() {
        // Check permissions first
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            // Permissions granted - check if location is enabled
            val locationEnabled = isLocationEnabled()
            android.util.Log.d("LocationActivity", "Permissions granted. Location enabled: $locationEnabled")
            
            if (locationEnabled) {
                // Location enabled - automatically fetch location from backend
                android.util.Log.d("LocationActivity", "Location enabled - fetching automatically")
                fetchCurrentLocation()
            } else {
                // Location not enabled - show "Enable device location" dialog
                android.util.Log.d("LocationActivity", "Location not enabled - showing dialog")
                showEnableLocationDialog()
            }
        } else {
            // Permissions not granted - request permissions
            android.util.Log.d("LocationActivity", "Permissions not granted - requesting")
            requestLocationPermissions()
        }
    }

    /**
     * Check if location services are enabled on device
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Request location permissions
     */
    private fun requestLocationPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Show "Enable device location" dialog (first dialog)
     * When user clicks "Enable device location", show Location Accuracy popup
     */
    private fun showEnableLocationDialog() {
        android.util.Log.d("LocationActivity", "Showing Enable device location dialog")
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_permission_needed, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val btnEnableLocation = dialogView.findViewById<Button>(R.id.btnEnableLocation)
        val btnSelectManually = dialogView.findViewById<Button>(R.id.btnSelectManually)

        // Button 1: Enable device location - show Location Accuracy popup
        btnEnableLocation.setOnClickListener {
            android.util.Log.d("LocationActivity", "Enable device location clicked")
            dialog.dismiss()
            // Show Location Accuracy popup (No thanks / Turn on)
            showLocationAccuracyDialog()
        }

        // Button 2: Select location manually - show address selection
        btnSelectManually.setOnClickListener {
            android.util.Log.d("LocationActivity", "Select manually clicked")
            dialog.dismiss()
            showAddressSelectionDialog()
        }

        // Ensure dialog is shown
        dialog.show()
        android.util.Log.d("LocationActivity", "Dialog shown: ${dialog.isShowing}")
    }

    /**
     * Show Location Accuracy dialog (second dialog with "No thanks" and "Turn on")
     * Reference: Blinkit/Zomato style popup
     */
    private fun showLocationAccuracyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_location_accuracy, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val btnNoThanks = dialogView.findViewById<Button>(R.id.btnNoThanks)
        val btnTurnOn = dialogView.findViewById<Button>(R.id.btnTurnOn)
        val tvManageSettings = dialogView.findViewById<TextView>(R.id.tvManageSettings)
        val tvLearnMore = dialogView.findViewById<TextView>(R.id.tvLearnMore)

        // Button: No thanks - show address selection
        btnNoThanks.setOnClickListener {
            dialog.dismiss()
            showAddressSelectionDialog()
        }

        // Button: Turn on - automatically enable location and fetch
        btnTurnOn.setOnClickListener {
            dialog.dismiss()
            // Automatically enable location settings and fetch location
            enableLocationAndFetch()
        }

        // Manage settings link
        tvManageSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        // Learn more link (optional - can open help page)
        tvLearnMore.setOnClickListener {
            Toast.makeText(this, "Learn more about location accuracy", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    /**
     * When user clicks "Turn on" - try to fetch location directly from backend
     * If location is disabled, it will fail gracefully and show address selection
     * NO navigation to device settings
     */
    private fun enableLocationAndFetch() {
        android.util.Log.d("LocationActivity", "Turn on clicked - attempting to fetch location")
        
        // Try to fetch location directly - if location is enabled, it will work
        // If location is disabled, fetchCurrentLocation() will handle the error gracefully
        fetchCurrentLocation()
        
        // Note: We can't programmatically enable location without system dialogs
        // So we try to fetch, and if it fails, the error handler will show address selection
    }

    /**
     * Show address selection dialog (saved addresses or current location)
     * Shown when user chooses "No thanks" or manual selection
     */
    private fun showAddressSelectionDialog() {
        // TODO: Implement RecyclerView with saved addresses
        // For now, show simple dialog with options
        val options = arrayOf(
            "Use Current Location",
            "Select from Saved Addresses",
            "Enter Address Manually"
        )

        AlertDialog.Builder(this)
            .setTitle("Select Location")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Use Current Location - try to fetch
                        if (isLocationEnabled() && hasLocationPermission()) {
                            fetchCurrentLocation()
                        } else {
                            Toast.makeText(this, "Please enable location first", Toast.LENGTH_SHORT).show()
                            showEnableLocationDialog()
                        }
                    }
                    1 -> {
                        // Select from Saved Addresses
                        // TODO: Open address list screen
                        Toast.makeText(this, "Open saved addresses list", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        // Enter Address Manually
                        // TODO: Open manual address entry screen
                        Toast.makeText(this, "Open manual address entry", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Show permission denied dialog
     */
    private fun showPermissionDeniedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_permission_needed, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val btnEnableLocation = dialogView.findViewById<Button>(R.id.btnEnableLocation)
        val btnSelectManually = dialogView.findViewById<Button>(R.id.btnSelectManually)

        btnEnableLocation.setOnClickListener {
            dialog.dismiss()
            showLocationAccuracyDialog()
        }

        btnSelectManually.setOnClickListener {
            dialog.dismiss()
            showAddressSelectionDialog()
        }

        dialog.show()
    }

    /**
     * Fetch current location using FusedLocationProviderClient
     */
    private fun fetchCurrentLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermissions()
            return
        }

        // Check if location is enabled before attempting to fetch
        if (!isLocationEnabled()) {
            android.util.Log.d("LocationActivity", "Location is disabled - showing address selection")
            Toast.makeText(
                this,
                "Location is disabled. Please enable it or select address manually",
                Toast.LENGTH_LONG
            ).show()
            showAddressSelectionDialog()
            return
        }

        tvAddress.text = "Fetching location..."
        android.util.Log.d("LocationActivity", "Fetching location from backend")

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                android.util.Log.d("LocationActivity", "Location fetched successfully")
                convertLocationToAddress(location)
            } else {
                android.util.Log.d("LocationActivity", "Last location is null - requesting updates")
                requestLocationUpdates()
            }
        }.addOnFailureListener { exception ->
            android.util.Log.e("LocationActivity", "Failed to get location: ${exception.message}")
            tvAddress.text = "Unable to get location. Location may be disabled."
            // Show address selection as fallback
            showAddressSelectionDialog()
        }
    }

    private fun requestLocationUpdates() {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = locationResult.lastLocation
                if (location != null) {
                    convertLocationToAddress(location)
                } else {
                    tvAddress.text = "Unable to get location"
                }
            }
        }

        if (!hasLocationPermission()) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun convertLocationToAddress(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressText = formatAddress(address)
                tvAddress.text = addressText
            } else {
                tvAddress.text = "Address not found\nLat: ${location.latitude}, Lng: ${location.longitude}"
            }
        } catch (e: Exception) {
            tvAddress.text = "Geocoding failed\nLat: ${location.latitude}, Lng: ${location.longitude}"
            Toast.makeText(this, "Geocoding error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatAddress(address: Address): String {
        val addressParts = mutableListOf<String>()
        address.getAddressLine(0)?.let { addressParts.add(it) }
        address.locality?.let { addressParts.add(it) }
        address.adminArea?.let { addressParts.add(it) }
        address.countryName?.let { addressParts.add(it) }
        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            "Address: ${address.latitude}, ${address.longitude}"
        }
    }

    // Note: onActivityResult removed since we're not navigating to device settings
    // Location is checked directly without system dialogs
}
