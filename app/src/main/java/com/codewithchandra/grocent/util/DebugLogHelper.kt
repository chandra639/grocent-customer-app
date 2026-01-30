package com.codewithchandra.grocent.util

import android.content.Context
import java.io.File
import org.json.JSONObject

object DebugLogHelper {
    fun log(
        context: Context,
        location: String,
        message: String,
        hypothesisId: String,
        runId: String = "run1",
        data: Map<String, Any?> = emptyMap()
    ) {
        try {
            val logFile = File(context.getExternalFilesDir(null), "places_debug.log")
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", runId)
                put("hypothesisId", hypothesisId)
                put("location", location)
                put("message", message)
                put("data", JSONObject().apply {
                    data.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Number -> put(key, value)
                            is Boolean -> put(key, value)
                            null -> put(key, JSONObject.NULL)
                            else -> put(key, value.toString())
                        }
                    }
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.parentFile?.mkdirs()
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {
            // Silently fail to avoid breaking the app
        }
    }
}
