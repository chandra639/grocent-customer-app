package com.codewithchandra.grocent.util

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DebugLogger {
    private var logFile: File? = null
    private val sessionId = "debug-session-${System.currentTimeMillis()}"
    // Background coroutine scope for async file writes (non-blocking)
    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun initialize(context: Context) {
        if (logFile == null) {
            // Write to external files directory so we can pull via ADB
            val externalFile = File(context.getExternalFilesDir(null), "debug.log")
            // Also write to cache directory (more accessible)
            val cacheFile = File(context.cacheDir, "debug.log")
            // Use external file if available, otherwise use cache
            logFile = if (externalFile.parentFile?.exists() == true) externalFile else cacheFile
            // Also log to logcat for visibility
            android.util.Log.d("DebugLogger", "Debug logs will be written to: ${logFile?.absolutePath}")
        }
    }
    
    fun log(
        location: String,
        message: String,
        hypothesisId: String? = null,
        runId: String = "run1",
        data: Map<String, Any?> = emptyMap()
    ) {
        // CRITICAL: Log to logcat immediately (fast, non-blocking)
        // This ensures we can see logs even if file write fails or is slow
        android.util.Log.d("DEBUG_LOG", "$location: $message ${if (data.isNotEmpty()) "data=$data" else ""}")
        
        // Write to file asynchronously in background (non-blocking)
        // This prevents blocking the main thread during startup
        logScope.launch {
            try {
                val logEntry = JSONObject().apply {
                    put("id", "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}")
                    put("timestamp", System.currentTimeMillis())
                    put("location", location)
                    put("message", message)
                    put("sessionId", sessionId)
                    put("runId", runId)
                    hypothesisId?.let { put("hypothesisId", it) }
                    if (data.isNotEmpty()) {
                        val dataObj = JSONObject()
                        data.forEach { (key, value) ->
                            when (value) {
                                null -> dataObj.put(key, JSONObject.NULL)
                                is Number -> dataObj.put(key, value)
                                is Boolean -> dataObj.put(key, value)
                                else -> dataObj.put(key, value.toString())
                            }
                        }
                        put("data", dataObj)
                    }
                }
                
                val entryString = logEntry.toString()
                
                // Write to file if initialized (in background thread)
                logFile?.let { file ->
                    BufferedWriter(FileWriter(file, true)).use { writer ->
                        writer.write(entryString)
                        writer.newLine()
                    }
                }
            } catch (e: Exception) {
                // Silent fail - don't break app
                android.util.Log.e("DebugLogger", "Failed to write log: ${e.message}")
            }
        }
    }
}

