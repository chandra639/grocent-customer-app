package com.codewithchandra.grocent.util

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONObject

data class AddressSuggestion(
    val placeId: String,
    val fullText: String,
    val primaryText: String,
    val secondaryText: String
)

class PlacesAutocompleteHelper(private val context: Context) {
    private var placesClient: PlacesClient? = null
    private var currentSessionToken: AutocompleteSessionToken? = null
    
    init {
        // #region agent log
        try {
            val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
            val apiKey = getApiKey()
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "A")
                put("location", "PlacesAutocompleteHelper.kt:init")
                put("message", "PlacesAutocompleteHelper initialization")
                put("data", JSONObject().apply {
                    put("apiKeyLength", apiKey.length)
                    put("apiKeyPrefix", if (apiKey.length > 10) apiKey.substring(0, 10) + "..." else apiKey)
                    put("placesInitialized", Places.isInitialized())
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.parentFile?.mkdirs()
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {}
        // #endregion
        // Initialize Places SDK (API key should be in AndroidManifest.xml)
        if (!Places.isInitialized()) {
            try {
                Places.initialize(context, getApiKey())
                // #region agent log
                try {
                    val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "B")
                        put("location", "PlacesAutocompleteHelper.kt:init")
                        put("message", "Places.initialize succeeded")
                        put("data", JSONObject().apply {
                            put("placesInitialized", Places.isInitialized())
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
            } catch (e: Exception) {
                // #region agent log
                try {
                    val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "B")
                        put("location", "PlacesAutocompleteHelper.kt:init")
                        put("message", "Places.initialize failed")
                        put("data", JSONObject().apply {
                            put("error", e.message ?: "unknown")
                            put("errorType", e.javaClass.simpleName)
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e2: Exception) {}
                // #endregion
            }
        }
        placesClient = Places.createClient(context)
        // #region agent log
        try {
            val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "B")
                put("location", "PlacesAutocompleteHelper.kt:init")
                put("message", "PlacesClient created")
                put("data", JSONObject().apply {
                    put("placesClientNull", placesClient == null)
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {}
        // #endregion
    }
    
    /**
     * Get API key from AndroidManifest or return empty string
     * User needs to add: <meta-data android:name="com.google.android.geo.API_KEY" android:value="YOUR_KEY"/>
     */
    private fun getApiKey(): String {
        // Try to get from manifest
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
        return apiKey ?: ""
    }
    
    /**
     * Get autocomplete suggestions as user types
     * Uses session token for cost optimization
     */
    suspend fun getAutocompleteSuggestions(
        query: String,
        sessionToken: AutocompleteSessionToken? = null
    ): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        // #region agent log
        try {
            val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "C")
                put("location", "PlacesAutocompleteHelper.kt:getAutocompleteSuggestions")
                put("message", "getAutocompleteSuggestions called")
                put("data", JSONObject().apply {
                    put("query", query)
                    put("queryLength", query.length)
                    put("sessionTokenNull", sessionToken == null)
                    put("placesClientNull", placesClient == null)
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.parentFile?.mkdirs()
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {}
        // #endregion
        if (query.length < 2) return@withContext emptyList()
        
        try {
            val token = sessionToken ?: AutocompleteSessionToken.newInstance().also {
                currentSessionToken = it
            }
            
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(token)
                .setCountries("IN") // Restrict to India only
                .build()
            
            // #region agent log
            android.util.Log.d("PlacesAutocompleteHelper", "Request built: query='$query', sessionToken=${token != null}")
            // #endregion
            
            // #region agent log
            try {
                val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "C")
                    put("location", "PlacesAutocompleteHelper.kt:getAutocompleteSuggestions")
                    put("message", "Before API call")
                    put("data", JSONObject().apply {
                        put("query", query)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {}
            // #endregion
            
            val response = placesClient?.findAutocompletePredictions(request)?.await()
            
            // #region agent log
            android.util.Log.d("PlacesAutocompleteHelper", "API response: responseNull=${response == null}, predictionsCount=${response?.autocompletePredictions?.size ?: 0}, placesClientNull=${placesClient == null}")
            try {
                val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "C")
                    put("location", "PlacesAutocompleteHelper.kt:getAutocompleteSuggestions")
                    put("message", "API call completed")
                    put("data", JSONObject().apply {
                        put("responseNull", response == null)
                        put("predictionsCount", response?.autocompletePredictions?.size ?: 0)
                        put("placesClientNull", placesClient == null)
                        put("query", query)
                        if (response != null && response.autocompletePredictions != null) {
                            put("predictionsListNull", false)
                            put("predictionsListSize", response.autocompletePredictions.size)
                        } else {
                            put("predictionsListNull", true)
                        }
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {
                android.util.Log.e("PlacesAutocompleteHelper", "Log write failed: ${e.message}")
            }
            // #endregion
            
            val predictionsList = response?.autocompletePredictions
            android.util.Log.d("PlacesAutocompleteHelper", "Predictions list: null=${predictionsList == null}, size=${predictionsList?.size ?: 0}")
            
            val results = predictionsList?.map { prediction ->
                AddressSuggestion(
                    placeId = prediction.placeId,
                    fullText = prediction.getFullText(null).toString(),
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString()
                )
            } ?: emptyList()
            
            // #region agent log
            android.util.Log.d("PlacesAutocompleteHelper", "Mapped results count: ${results.size}")
            if (results.isEmpty() && response != null) {
                android.util.Log.w("PlacesAutocompleteHelper", "⚠️ API returned 0 results for query='$query'. This may indicate: 1) Places API (New) not enabled, 2) API key restrictions, or 3) No matches found.")
            }
            // #endregion
            
            // #region agent log
            try {
                val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "C")
                    put("location", "PlacesAutocompleteHelper.kt:getAutocompleteSuggestions")
                    put("message", "Returning results")
                    put("data", JSONObject().apply {
                        put("resultsCount", results.size)
                        if (results.isNotEmpty()) {
                            put("firstResult", results[0].fullText)
                        }
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {}
            // #endregion
            
            return@withContext results
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Re-throw cancellation - this is expected when user types quickly
            // Don't log as error, just re-throw to let caller handle it
            throw e
        } catch (e: Exception) {
            // Check if this is an API key authorization error
            val errorMessage = e.message ?: ""
            val isApiKeyError = errorMessage.contains("9011") || 
                               errorMessage.contains("API key") || 
                               errorMessage.contains("not authorized") ||
                               (e is ApiException && e.statusCode == 9011)
            
            // #region agent log
            try {
                val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "C")
                    put("location", "PlacesAutocompleteHelper.kt:getAutocompleteSuggestions")
                    put("message", if (isApiKeyError) "API KEY ERROR" else "Exception in getAutocompleteSuggestions")
                    put("data", JSONObject().apply {
                        put("error", errorMessage)
                        put("errorType", e.javaClass.simpleName)
                        put("isApiKeyError", isApiKeyError)
                        if (e is ApiException) {
                            put("apiExceptionStatusCode", e.statusCode)
                        }
                        put("stackTrace", e.stackTraceToString())
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e2: Exception) {}
            // #endregion
            
            if (isApiKeyError) {
                android.util.Log.e("PlacesAutocompleteHelper", "⚠️ API KEY NOT CONFIGURED: ${e.message}. Please enable Places API (New) in Google Cloud Console. See API_KEY_FIX_REQUIRED.md", e)
            } else {
                android.util.Log.e("PlacesAutocompleteHelper", "Error getting autocomplete: ${e.message}", e)
            }
            emptyList()
        }
    }
    
    /**
     * Fetch place details from place ID
     * This completes the session and charges once
     */
    suspend fun getPlaceDetails(placeId: String): Place? = withContext(Dispatchers.IO) {
        // #region agent log
        try {
            val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "F")
                put("location", "PlacesAutocompleteHelper.kt:getPlaceDetails")
                put("message", "getPlaceDetails called")
                put("data", JSONObject().apply {
                    put("placeId", placeId)
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.parentFile?.mkdirs()
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {}
        // #endregion
        try {
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS_COMPONENTS
            )
            
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            val response = placesClient?.fetchPlace(request)?.await()
            val place = response?.place
            
            // #region agent log
            try {
                val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "F")
                    put("location", "PlacesAutocompleteHelper.kt:getPlaceDetails")
                    put("message", "getPlaceDetails completed")
                    put("data", JSONObject().apply {
                        put("placeNull", place == null)
                        put("address", place?.address ?: "null")
                        put("hasLatLng", place?.latLng != null)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {}
            // #endregion
            
            return@withContext place
        } catch (e: Exception) {
            // #region agent log
            try {
                val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "F")
                    put("location", "PlacesAutocompleteHelper.kt:getPlaceDetails")
                    put("message", "Exception in getPlaceDetails")
                    put("data", JSONObject().apply {
                        put("error", e.message ?: "unknown")
                        put("errorType", e.javaClass.simpleName)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e2: Exception) {}
            // #endregion
            android.util.Log.e("PlacesAutocompleteHelper", "Error fetching place: ${e.message}", e)
            null
        }
    }
    
    /**
     * Create new session token (for new address entry)
     */
    fun createNewSessionToken(): AutocompleteSessionToken {
        return AutocompleteSessionToken.newInstance().also {
            currentSessionToken = it
        }
    }
    
    /**
     * Get current session token
     */
    fun getCurrentSessionToken(): AutocompleteSessionToken? = currentSessionToken
}
