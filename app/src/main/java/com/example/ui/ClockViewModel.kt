package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AppSettings
import com.example.data.ClockDatabase
import com.example.data.ClockRepository
import com.example.data.PlannerTask
import com.example.data.SleepLog
import com.example.data.SmartAction
import com.example.data.SmartClockParser
import com.example.data.WorldCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Stopwatch lap item
data class StopwatchLap(val lapNumber: Int, val lapTimeMs: Long, val totalTimeMs: Long)

// Active Timer state
data class ActiveTimer(
    val id: String,
    val initialSeconds: Int,
    var remainingSeconds: Int,
    var isRunning: Boolean,
    val label: String,
    val mode: String = "Normal" // Kitchen, Workout, Normal
)

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClockRepository
    private val TAG = "ClockViewModel"

    // DB States
    val alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val worldCities = MutableStateFlow<List<WorldCity>>(emptyList())
    val plannerTasks = MutableStateFlow<List<PlannerTask>>(emptyList())
    val sleepLogs = MutableStateFlow<List<SleepLog>>(emptyList())
    val settings = MutableStateFlow<AppSettings>(AppSettings())

    // UI Navigation State
    var currentScreen by mutableStateOf("WELCOME") // WELCOME, HOME, ALARM_RING, PIN_LOCK
    var currentTab by mutableStateOf("ALARM") // ALARM, WORLD_CLOCK, STOPWATCH, TIMER, SLEEP, PLANNER, STATS, SETTINGS

    // Active Alarm firing context
    var activeRingingAlarm by mutableStateOf<Alarm?>(null)
    var currentMathQuestion by mutableStateOf("")
    var correctMathAnswer by mutableStateOf(0)
    var mathUserInput by mutableStateOf("")
    var shakeCountRemaining by mutableStateOf(15)
    var shakeDismissProgress by mutableStateOf(0.0f)
    var qrScannerMockOpen by mutableStateOf(false)
    var voiceRecordMockSuccess by mutableStateOf(false)

    // Stopwatch State
    var stopwatchRunning by mutableStateOf(false)
    var stopwatchTimeMs by mutableStateOf(0L)
    val stopwatchLaps = mutableStateListOf<StopwatchLap>()
    private var stopwatchJob: Job? = null

    // Multiple Timer State
    val activeTimers = mutableStateListOf<ActiveTimer>()
    private var timerTickJob: Job? = null

    // Sleep Sound States
    var activeSleepSound by mutableStateOf<String?>(null) // Rain, Ocean, Forest
    var sleepSoundTimerMinutes by mutableStateOf<Int?>(null) // Countdown in minutes
    var sleepSoundTimerSecondsLeft by mutableStateOf(0)
    private var sleepSoundJob: Job? = null

    // UI Event messages
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    // PIN lock entry
    var pinLockInput by mutableStateOf("")

    init {
        val database = ClockDatabase.getDatabase(application)
        repository = ClockRepository(database)

        // Listen to flows
        viewModelScope.launch {
            repository.alarmsFlow.collectLatest { alarms.value = it }
        }
        viewModelScope.launch {
            repository.citiesFlow.collectLatest { worldCities.value = it }
        }
        viewModelScope.launch {
            repository.tasksFlow.collectLatest { plannerTasks.value = it }
        }
        viewModelScope.launch {
            repository.sleepLogsFlow.collectLatest { sleepLogs.value = it }
        }
        viewModelScope.launch {
            repository.settingsFlow.collectLatest { appSettings ->
                appSettings?.let {
                    settings.value = it
                    if (it.hasCompletedWelcome) {
                        currentScreen = if (it.pinLock.isNotEmpty()) "PIN_LOCK" else "HOME"
                    } else {
                        currentScreen = "WELCOME"
                    }
                }
            }
        }

        // Seeds
        viewModelScope.launch(Dispatchers.IO) {
            seedInitialDataIfNeeded()
        }

        // Start central Timer Tick loop for countdowns
        startTimerTickLoop()
    }

    private suspend fun seedInitialDataIfNeeded() {
        // 1. Initial settings
        val currentSettings = repository.getSettingsDirect()

        if (!currentSettings.hasCompletedWelcome) {
            repository.insertCity(WorldCity(cityName = "Dhaka", countryName = "Bangladesh", timeZoneId = "Asia/Dhaka", isDefault = true))
            repository.insertCity(WorldCity(cityName = "New York", countryName = "United States", timeZoneId = "America/New_York"))
            repository.insertCity(WorldCity(cityName = "London", countryName = "United Kingdom", timeZoneId = "Europe/London"))
            repository.insertCity(WorldCity(cityName = "Tokyo", countryName = "Japan", timeZoneId = "Asia/Tokyo"))
            repository.insertCity(WorldCity(cityName = "Sydney", countryName = "Australia", timeZoneId = "Australia/Sydney"))

            repository.insertAlarm(Alarm(hour = 6, minute = 0, label = "Morning Math Mission", dismissMission = "MATH", daysOfWeek = ""))
            repository.insertAlarm(Alarm(hour = 7, minute = 30, label = "Work Shake Mission", dismissMission = "SHAKE", daysOfWeek = ""))
            repository.insertAlarm(Alarm(hour = 9, minute = 0, label = "Routine Quick Dismiss", dismissMission = "NONE", daysOfWeek = ""))

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertTask(PlannerTask(title = "Morning Medicine Check", description = "Take vitamins", timeString = "08:00", type = "MEDICINE", dateString = today))
            repository.insertTask(PlannerTask(title = "Hydrate Now", description = "Drink 500ml water", timeString = "11:00", type = "WATER", dateString = today))
            repository.insertTask(PlannerTask(title = "Project Sync", description = "Infinity Clock alignment", timeString = "15:00", type = "MEETING", dateString = today))

            repository.insertSleepLog(SleepLog(dateString = "2026-07-10", bedTime = "22:30", wakeTime = "06:30", sleepDurationMinutes = 480, sleepQualityScore = 5, notes = "Woke up fully refreshed"))
            repository.insertSleepLog(SleepLog(dateString = "2026-07-11", bedTime = "23:15", wakeTime = "07:15", sleepDurationMinutes = 480, sleepQualityScore = 4, notes = "Slept late but clean rest"))
        }
    }

    // ==========================================
    // WELCOME FLOW
    // ==========================================
    fun completeWelcome(themeName: String, amoled: Boolean, format24: Boolean, lang: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getSettingsDirect().copy(
                themeName = themeName,
                amoledDark = amoled,
                is24HourFormat = format24,
                language = lang,
                hasCompletedWelcome = true
            )
            repository.updateSettings(s)
            withContext(Dispatchers.Main) {
                currentScreen = "HOME"
            }
        }
    }

    // ==========================================
    // SETTINGS & CUSTOMIZATION
    // ==========================================
    fun updateTheme(themeName: String, amoled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = settings.value.copy(themeName = themeName, amoledDark = amoled)
            repository.updateSettings(s)
        }
    }

    fun updateHourFormat(is24: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = settings.value.copy(is24HourFormat = is24)
            repository.updateSettings(s)
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = settings.value.copy(language = lang)
            repository.updateSettings(s)
        }
    }

    fun setPinLock(pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = settings.value.copy(pinLock = pin)
            repository.updateSettings(s)
            _uiEvents.emit(if (pin.isEmpty()) "PIN lock disabled" else "PIN lock enabled")
        }
    }

    fun unlockApp(pin: String): Boolean {
        if (settings.value.pinLock == pin) {
            currentScreen = "HOME"
            pinLockInput = ""
            return true
        }
        pinLockInput = ""
        return false
    }

    // ==========================================
    // ALARMS
    // ==========================================
    fun addAlarm(hour: Int, minute: Int, label: String, mission: String, repeatDays: String, isOneTime: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val alarm = Alarm(
                hour = hour,
                minute = minute,
                label = label.ifEmpty { "Alarm" },
                dismissMission = mission,
                daysOfWeek = repeatDays,
                isOneTime = isOneTime
            )
            repository.insertAlarm(alarm)
            _uiEvents.emit("Alarm set for ${formatTime(hour, minute)}")
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarm.copy(enabled = !alarm.enabled)
            repository.updateAlarm(updated)
            _uiEvents.emit(if (updated.enabled) "Alarm enabled" else "Alarm disabled")
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlarm(alarm.id)
            _uiEvents.emit("Alarm deleted")
        }
    }

    // ==========================================
    // ALARM DISMISS MISSIONS
    // ==========================================
    fun triggerAlarmRinging(alarm: Alarm) {
        activeRingingAlarm = alarm
        currentScreen = "ALARM_RING"

        // Initialize missions
        when (alarm.dismissMission) {
            "MATH" -> {
                generateMathQuestion()
            }
            "SHAKE" -> {
                shakeCountRemaining = 15
                shakeDismissProgress = 0.0f
            }
            "QR" -> {
                qrScannerMockOpen = false
            }
            "VOICE" -> {
                voiceRecordMockSuccess = false
            }
        }
    }

    private fun generateMathQuestion() {
        val op = listOf("+", "-", "*").random()
        val n1 = (5..30).random()
        val n2 = (3..12).random()
        currentMathQuestion = when (op) {
            "+" -> {
                correctMathAnswer = n1 + n2
                "$n1 + $n2"
            }
            "-" -> {
                correctMathAnswer = n1 - n2
                "$n1 - $n2"
            }
            else -> {
                correctMathAnswer = n1 * n2
                "$n1 × $n2"
            }
        }
        mathUserInput = ""
    }

    fun submitMathAnswer() {
        val ans = mathUserInput.toIntOrNull()
        if (ans == correctMathAnswer) {
            dismissActiveAlarm()
        } else {
            generateMathQuestion()
            viewModelScope.launch {
                _uiEvents.emit("Wrong Answer! New question generated.")
            }
        }
    }

    fun triggerShakeProgress() {
        if (shakeCountRemaining > 0) {
            shakeCountRemaining--
            shakeDismissProgress = (15 - shakeCountRemaining) / 15.0f
            if (shakeCountRemaining == 0) {
                dismissActiveAlarm()
            }
        }
    }

    fun submitMockQrScan() {
        dismissActiveAlarm()
    }

    fun submitMockVoicePhrase() {
        voiceRecordMockSuccess = true
        viewModelScope.launch {
            delay(1000)
            dismissActiveAlarm()
        }
    }

    fun snoozeActiveAlarm() {
        val alarm = activeRingingAlarm ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Update stats
            val currentSettings = repository.getSettingsDirect()
            repository.updateSettings(currentSettings.copy(snoozeTotalCount = currentSettings.snoozeTotalCount + 1))

            _uiEvents.emit("Snoozed for 5 minutes")
            withContext(Dispatchers.Main) {
                currentScreen = "HOME"
                activeRingingAlarm = null
            }
        }
    }

    fun dismissActiveAlarm() {
        val alarm = activeRingingAlarm ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Update stats
            val currentSettings = repository.getSettingsDirect()
            repository.updateSettings(currentSettings.copy(onTimeWakeCount = currentSettings.onTimeWakeCount + 1))

            // If one-time, disable alarm
            if (alarm.isOneTime || alarm.daysOfWeek.isEmpty()) {
                repository.updateAlarm(alarm.copy(enabled = false))
            }

            _uiEvents.emit("Alarm dismissed! Great morning!")
            withContext(Dispatchers.Main) {
                currentScreen = "HOME"
                activeRingingAlarm = null
            }
        }
    }

    // ==========================================
    // WORLD CLOCK
    // ==========================================
    fun addNewCity(cityName: String, countryName: String, timeZoneId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val city = WorldCity(cityName = cityName, countryName = countryName, timeZoneId = timeZoneId)
            repository.insertCity(city)
            _uiEvents.emit("Added $cityName to World Clock")
        }
    }

    fun deleteCity(city: WorldCity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCity(city.id)
            _uiEvents.emit("Removed ${city.cityName}")
        }
    }

    fun formatCityTime(timeZoneId: String): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
        val format = if (settings.value.is24HourFormat) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        sdf.timeZone = cal.timeZone
        return sdf.format(cal.time)
    }

    fun getCityTimeDifference(timeZoneId: String): String {
        val currentTz = TimeZone.getDefault()
        val targetTz = TimeZone.getTimeZone(timeZoneId)
        val diffMs = targetTz.rawOffset - currentTz.rawOffset
        val diffHours = diffMs / (1000 * 60 * 60)
        return if (diffHours == 0) "Same time as local"
        else if (diffHours > 0) "+${diffHours}h ahead"
        else "${diffHours}h behind"
    }

    // ==========================================
    // STOPWATCH
    // ==========================================
    fun startStopwatch() {
        if (stopwatchRunning) return
        stopwatchRunning = true
        val startTime = System.currentTimeMillis() - stopwatchTimeMs
        stopwatchJob = viewModelScope.launch(Dispatchers.Default) {
            while (stopwatchRunning) {
                stopwatchTimeMs = System.currentTimeMillis() - startTime
                delay(11) // High-precision loop
            }
        }
    }

    fun pauseStopwatch() {
        stopwatchRunning = false
        stopwatchJob?.cancel()
    }

    fun resetStopwatch() {
        pauseStopwatch()
        stopwatchTimeMs = 0L
        stopwatchLaps.clear()
    }

    fun recordStopwatchLap() {
        val currentTotal = stopwatchTimeMs
        val lastLapTotal = stopwatchLaps.lastOrNull()?.totalTimeMs ?: 0L
        val lapTime = currentTotal - lastLapTotal
        val nextLapNum = stopwatchLaps.size + 1
        stopwatchLaps.add(0, StopwatchLap(nextLapNum, lapTime, currentTotal)) // Add to top
    }

    fun formatStopwatchTime(timeMs: Long): String {
        val mins = (timeMs / 60000) % 60
        val secs = (timeMs / 1000) % 60
        val millis = (timeMs / 10) % 100
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", mins, secs, millis)
    }

    fun exportStopwatchLapsText(): String {
        val sb = java.lang.StringBuilder()
        sb.append("Infinity Smart Clock - Stopwatch Lap Times\n")
        sb.append("Total Elapsed: ${formatStopwatchTime(stopwatchTimeMs)}\n\n")
        stopwatchLaps.sortedBy { it.lapNumber }.forEach {
            sb.append("Lap ${it.lapNumber}: Lap Time ${formatStopwatchTime(it.lapTimeMs)} | Total ${formatStopwatchTime(it.totalTimeMs)}\n")
        }
        return sb.toString()
    }

    // ==========================================
    // TIMERS (MULTIPLE ACTIVE TIMERS)
    // ==========================================
    fun addNewTimer(minutes: Int, seconds: Int, label: String, mode: String = "Normal") {
        val totalSecs = (minutes * 60) + seconds
        if (totalSecs <= 0) return
        val newTimer = ActiveTimer(
            id = java.util.UUID.randomUUID().toString(),
            initialSeconds = totalSecs,
            remainingSeconds = totalSecs,
            isRunning = true,
            label = label.ifEmpty { "Timer" },
            mode = mode
        )
        activeTimers.add(newTimer)
        viewModelScope.launch {
            _uiEvents.emit("Timer started for $minutes m $seconds s")
        }
    }

    fun toggleTimerActive(timer: ActiveTimer) {
        val index = activeTimers.indexOfFirst { it.id == timer.id }
        if (index != -1) {
            val t = activeTimers[index]
            activeTimers[index] = t.copy(isRunning = !t.isRunning)
        }
    }

    fun resetTimer(timer: ActiveTimer) {
        val index = activeTimers.indexOfFirst { it.id == timer.id }
        if (index != -1) {
            val t = activeTimers[index]
            activeTimers[index] = t.copy(remainingSeconds = t.initialSeconds, isRunning = false)
        }
    }

    fun removeTimer(timer: ActiveTimer) {
        activeTimers.removeAll { it.id == timer.id }
    }

    fun formatTimerSeconds(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    private fun startTimerTickLoop() {
        timerTickJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000)
                for (i in activeTimers.indices) {
                    val t = activeTimers[i]
                    if (t.isRunning && t.remainingSeconds > 0) {
                        t.remainingSeconds--
                        if (t.remainingSeconds == 0) {
                            t.isRunning = false
                            _uiEvents.emit("Timer '${t.label}' finished!")
                        }
                        // Refresh state trigger
                        activeTimers[i] = t.copy()
                    }
                }
            }
        }
    }

    // ==========================================
    // SLEEP & SLEEP SOUNDS
    // ==========================================
    fun toggleSleepSound(soundName: String) {
        if (activeSleepSound == soundName) {
            activeSleepSound = null
            stopSleepSoundCountdown()
        } else {
            activeSleepSound = soundName
            // Set a default sleep timer of 30 minutes if none is active
            if (sleepSoundTimerMinutes == null) {
                setSleepSoundTimer(30)
            }
        }
    }

    fun setSleepSoundTimer(minutes: Int) {
        sleepSoundTimerMinutes = minutes
        sleepSoundTimerSecondsLeft = minutes * 60
        startSleepSoundTimerCountdown()
    }

    fun stopSleepSoundCountdown() {
        sleepSoundJob?.cancel()
        sleepSoundTimerMinutes = null
        sleepSoundTimerSecondsLeft = 0
    }

    private fun startSleepSoundTimerCountdown() {
        sleepSoundJob?.cancel()
        sleepSoundJob = viewModelScope.launch(Dispatchers.Default) {
            while (sleepSoundTimerSecondsLeft > 0 && activeSleepSound != null) {
                delay(1000)
                sleepSoundTimerSecondsLeft--
                if (sleepSoundTimerSecondsLeft <= 0) {
                    activeSleepSound = null
                    sleepSoundTimerMinutes = null
                    _uiEvents.emit("Sleep Sounds Timer finished. Rest well!")
                }
            }
        }
    }

    fun saveSleepRecord(bedTime: String, wakeTime: String, quality: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            // Calculate sleep duration
            var durationMinutes = 480 // 8 hours default
            try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val d1 = sdf.parse(bedTime)
                val d2 = sdf.parse(wakeTime)
                if (d1 != null && d2 != null) {
                    var diff = d2.time - d1.time
                    if (diff < 0) {
                        diff += 24 * 60 * 60 * 1000 // crossed midnight
                    }
                    durationMinutes = (diff / (1000 * 60)).toInt()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating sleep duration", e)
            }

            val log = SleepLog(
                dateString = today,
                bedTime = bedTime,
                wakeTime = wakeTime,
                sleepDurationMinutes = durationMinutes,
                sleepQualityScore = quality,
                notes = notes
            )
            repository.insertSleepLog(log)
            _uiEvents.emit("Sleep recorded! Daily Wakeup Analysis complete.")
        }
    }

    fun getSleepSuggestionText(): String {
        val records = sleepLogs.value
        if (records.isEmpty()) {
            return "Try going to bed by 10:30 PM to maintain a healthy 8-hour sleep cycle."
        }
        val averageScore = records.map { it.sleepQualityScore }.average()
        val averageDuration = records.map { it.sleepDurationMinutes }.average() / 60.0

        return when {
            averageScore >= 4.5 -> "Your sleep cycle is optimal! Maintain your consistent Bedtime routine."
            averageDuration < 7.0 -> "Smart Suggestion: You average ${String.format("%.1f", averageDuration)} hours. Try sleeping 45 minutes earlier."
            else -> "Use 'Rain' sleep sounds for 20 mins to improve sleep induction efficiency."
        }
    }

    // ==========================================
    // PLANNER / REMINDERS
    // ==========================================
    fun addPlannerTask(title: String, description: String, timeString: String, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val task = PlannerTask(
                title = title,
                description = description,
                timeString = timeString,
                type = type,
                dateString = today
            )
            repository.insertTask(task)
            _uiEvents.emit("$title task added for $timeString")
        }
    }

    fun toggleTaskCompleted(task: PlannerTask) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: PlannerTask) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task.id)
            _uiEvents.emit("Task deleted")
        }
    }

    // ==========================================
    // SMART COMMAND PARSER (AI INTERACTION)
    // ==========================================
    suspend fun executeSmartCommand(command: String): String {
        if (command.isBlank()) return "Please type or speak a command."
        val action = SmartClockParser.parseCommand(command)

        return withContext(Dispatchers.Main) {
            when (action) {
                is SmartAction.CreateAlarm -> {
                    addAlarm(
                        hour = action.hour,
                        minute = action.minute,
                        label = action.label,
                        mission = "NONE",
                        repeatDays = "",
                        isOneTime = true
                    )
                    "Successfully created alarm for ${formatTime(action.hour, action.minute)}"
                }
                is SmartAction.CreateTimer -> {
                    addNewTimer(
                        minutes = action.durationSeconds / 60,
                        seconds = action.durationSeconds % 60,
                        label = action.label
                    )
                    "Timer started for ${action.durationSeconds / 60}m ${action.durationSeconds % 60}s"
                }
                is SmartAction.CreateTask -> {
                    addPlannerTask(
                        title = action.title,
                        description = "Created via Smart Assistant",
                        timeString = action.timeString,
                        type = action.type
                    )
                    "Task '${action.title}' scheduled for ${action.timeString}"
                }
                is SmartAction.Error -> {
                    action.message
                }
            }
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================
    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        val format = if (settings.value.is24HourFormat) "HH:mm" else "hh:mm a"
        return SimpleDateFormat(format, Locale.getDefault()).format(cal.time)
    }

    override fun onCleared() {
        super.onCleared()
        stopwatchRunning = false
        stopwatchJob?.cancel()
        timerTickJob?.cancel()
        sleepSoundJob?.cancel()
    }
}
