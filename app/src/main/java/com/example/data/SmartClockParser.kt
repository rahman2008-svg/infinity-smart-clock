package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class SmartAction {
    data class CreateAlarm(val hour: Int, val minute: Int, val label: String, val daysOfWeek: String) : SmartAction()
    data class CreateTimer(val durationSeconds: Int, val label: String) : SmartAction()
    data class CreateTask(val title: String, val timeString: String, val type: String) : SmartAction()
    data class Error(val message: String) : SmartAction()
}

object SmartClockParser {
    private const val TAG = "SmartClockParser"

    // Local client with robust timeouts for Gemini API
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Entry point to parse user command.
     * Tries Gemini API if a key is present and returns a valid parsing.
     * Otherwise, falls back gracefully to the highly robust local parser.
     */
    suspend fun parseCommand(input: String): SmartAction = withContext(Dispatchers.IO) {
        val sanitizedInput = sanitizeBengaliInput(input).trim()
        Log.d(TAG, "Parsing input: '$input' -> Sanitized: '$sanitizedInput'")

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "GEMINI_API_KEY") {
            try {
                val geminiResult = callGeminiAPI(sanitizedInput, apiKey)
                if (geminiResult != null) {
                    return@withContext geminiResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini parsing failed, using local parser", e)
            }
        }

        // Fallback to local parser
        return@withContext parseLocally(sanitizedInput)
    }

    /**
     * Standardizes Bengali digits and normalizes common Bengali words.
     */
    private fun sanitizeBengaliInput(input: String): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var result = input
        for (i in 0..9) {
            result = result.replace(banglaDigits[i], englishDigits[i])
        }
        return result
    }

    /**
     * Local rule-based parser for Bengali and English.
     */
    private fun parseLocally(input: String): SmartAction {
        val lower = input.lowercase()

        // 1. Check for Timer
        if (lower.contains("timer") || lower.contains("টাইমার") || lower.contains("কাউন্টডাউন")) {
            // Find minutes or seconds
            val numberPattern = Pattern.compile("(\\d+)")
            val matcher = numberPattern.matcher(lower)
            if (matcher.find()) {
                val num = matcher.group(1)?.toIntOrNull() ?: 5
                val isSeconds = lower.contains("sec") || lower.contains("সেকেন্ড")
                val duration = if (isSeconds) num else num * 60
                val label = if (lower.contains("kitchen") || lower.contains("রান্না")) "Kitchen Timer"
                            else if (lower.contains("workout") || lower.contains("ব্যায়াম")) "Workout Timer"
                            else "Smart Timer"
                return SmartAction.CreateTimer(duration, label)
            }
            return SmartAction.CreateTimer(300, "Timer")
        }

        // 2. Check for Task/Planner Reminder
        val isReminder = lower.contains("remind") || lower.contains("task") || lower.contains("রিমাইন্ডার") ||
                lower.contains("টাস্ক") || lower.contains("পানি") || lower.contains("ঔষধ") || lower.contains("মেডিসিন") ||
                lower.contains("meeting") || lower.contains("মিটিং")

        if (isReminder) {
            var type = "SCHEDULE"
            var title = "Smart Reminder"
            if (lower.contains("water") || lower.contains("পানি")) {
                type = "WATER"
                title = "Drink Water"
            } else if (lower.contains("medicine") || lower.contains("ঔষধ") || lower.contains("মেডিসিন") || lower.contains("ট্যাবলেট")) {
                type = "MEDICINE"
                title = "Take Medicine"
            } else if (lower.contains("meeting") || lower.contains("মিটিং")) {
                type = "MEETING"
                title = "Meeting Reminder"
            } else if (lower.contains("habit") || lower.contains("অভ্যাস")) {
                type = "HABIT"
                title = "Habit Check-in"
            }

            // Find time (e.g. 14:00 or 2 PM or বিকাল ৩টা)
            val timeString = extractTimeString(lower)
            return SmartAction.CreateTask(title, timeString, type)
        }

        // 3. Default fallback to Alarm parsing
        // e.g. "আগামীকাল সকাল ৬টায় অ্যালার্ম সেট করো" or "alarm at 7:30 am"
        var hour = 6
        var minute = 0
        var label = "AI Smart Alarm"

        // Extract numbers from string
        val numbers = mutableListOf<Int>()
        val matcher = Pattern.compile("(\\d+)").matcher(lower)
        while (matcher.find()) {
            matcher.group(1)?.toIntOrNull()?.let { numbers.add(it) }
        }

        if (numbers.isNotEmpty()) {
            if (numbers.size >= 2) {
                hour = numbers[0]
                minute = numbers[1]
            } else {
                hour = numbers[0]
                minute = 0
            }
        }

        // Adjust for AM/PM in Bengali and English
        val isPM = lower.contains("pm") || lower.contains("রাত") || lower.contains("সন্ধ্যা") ||
                lower.contains("বিকাল") || lower.contains("দুপুর") || lower.contains("afternoon") ||
                lower.contains("evening") || lower.contains("night")

        val isAM = lower.contains("am") || lower.contains("সকাল") || lower.contains("ভোর") || lower.contains("morning")

        if (isPM && hour < 12) {
            hour += 12
        } else if (isAM && hour == 12) {
            hour = 0
        }

        // Bound checks
        hour = hour.coerceIn(0, 23)
        minute = minute.coerceIn(0, 59)

        if (lower.contains("wake") || lower.contains("ঘুম")) {
            label = "Wake Up Alarm"
        }

        return SmartAction.CreateAlarm(hour, minute, label, "")
    }

    private fun extractTimeString(input: String): String {
        // Extract time like "15:30" or "3:00" or simple hours
        val matcher = Pattern.compile("(\\d{1,2})[:.](\\d{2})").matcher(input)
        if (matcher.find()) {
            val h = matcher.group(1)?.toIntOrNull() ?: 12
            val m = matcher.group(2)?.toIntOrNull() ?: 0
            return String.format("%02d:%02d", h, m)
        }
        val singleMatcher = Pattern.compile("(\\d+)").matcher(input)
        if (singleMatcher.find()) {
            val h = singleMatcher.group(1)?.toIntOrNull() ?: 12
            return String.format("%02d:00", h)
        }
        return "08:00"
    }

    /**
     * Invokes Gemini model directly to parse the structured instructions in JSON format.
     */
    private suspend fun callGeminiAPI(input: String, apiKey: String): SmartAction? {
        val systemPrompt = """
            You are the AI engine of "Infinity Smart Clock". Parse the user's natural language command into a structured JSON action.
            Supported actions:
            1. CREATE_ALARM: { "action": "CREATE_ALARM", "hour": Int, "minute": Int, "label": String, "daysOfWeek": String }
            2. CREATE_TIMER: { "action": "CREATE_TIMER", "durationSeconds": Int, "label": String }
            3. CREATE_TASK: { "action": "CREATE_TASK", "title": String, "timeString": String, "type": "MEDICINE"|"WATER"|"MEETING"|"HABIT"|"SCHEDULE" }

            Output ONLY the raw JSON object, without any markdown enclosing backticks. Make sure hour is 0-23 and minute is 0-59.
            For Bengali commands:
            - "আগামীকাল সকাল ৬টায় অ্যালার্ম সেট করো" -> { "action": "CREATE_ALARM", "hour": 6, "minute": 0, "label": "Morning Alarm", "daysOfWeek": "" }
            - "টাইমার ১০ মিনিট" -> { "action": "CREATE_TIMER", "durationSeconds": 600, "label": "Timer" }
            - "দুপুর ২টায় ঔষধ খাওয়ার রিমাইন্ডার" -> { "action": "CREATE_TASK", "title": "Take Medicine", "timeString": "14:00", "type": "MEDICINE" }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArray = org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", input)
                        })
                    })
                })
            }
            put("contents", contentsArray)

            // Inject system instruction if supported, or pass inside user content
            put("systemInstruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "API call unsuccessful: ${response.code} ${response.message}")
            return null
        }

        val bodyString = response.body?.string() ?: return null
        Log.d(TAG, "Gemini Response: $bodyString")

        val jsonResponse = JSONObject(bodyString)
        val candidates = jsonResponse.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val text = firstCandidate.getJSONObject("content")
            .getJSONArray("parts").getJSONObject(0).getString("text")

        val cleanJson = text.trim().removeSurrounding("```json", "```").trim()
        val parsedJson = JSONObject(cleanJson)

        return when (val actionType = parsedJson.optString("action")) {
            "CREATE_ALARM" -> {
                SmartAction.CreateAlarm(
                    hour = parsedJson.optInt("hour", 6),
                    minute = parsedJson.optInt("minute", 0),
                    label = parsedJson.optString("label", "AI Smart Alarm"),
                    daysOfWeek = parsedJson.optString("daysOfWeek", "")
                )
            }
            "CREATE_TIMER" -> {
                SmartAction.CreateTimer(
                    durationSeconds = parsedJson.optInt("durationSeconds", 300),
                    label = parsedJson.optString("label", "Smart Timer")
                )
            }
            "CREATE_TASK" -> {
                SmartAction.CreateTask(
                    title = parsedJson.optString("title", "Smart Task"),
                    timeString = parsedJson.optString("timeString", "08:00"),
                    type = parsedJson.optString("type", "SCHEDULE")
                )
            }
            else -> {
                Log.e(TAG, "Unknown action type parsed: $actionType")
                null
            }
        }
    }
}
