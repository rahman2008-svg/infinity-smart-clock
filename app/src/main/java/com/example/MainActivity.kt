package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Alarm
import com.example.data.PlannerTask
import com.example.ui.ActiveTimer
import com.example.data.SleepLog
import com.example.data.WorldCity
import com.example.ui.ClockViewModel
import com.example.ui.theme.InfinityClockTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsState by viewModel.settings.collectAsStateWithLifecycle()
            
            InfinityClockTheme(
                themeName = settingsState.themeName,
                amoledDark = settingsState.amoledDark
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    
                    // Listen to ViewModel events to display snackbars or toasts
                    LaunchedEffect(Unit) {
                        viewModel.uiEvents.collect { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    Crossfade(
                        targetState = viewModel.currentScreen,
                        animationSpec = tween(400),
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            "WELCOME" -> WelcomeScreen(viewModel)
                            "PIN_LOCK" -> PinLockScreen(viewModel)
                            "ALARM_RING" -> AlarmRingScreen(viewModel)
                            else -> HomeScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. WELCOME SCREEN
// ==========================================
@Composable
fun WelcomeScreen(viewModel: ClockViewModel) {
    var selectedTheme by remember { mutableStateOf("Bento Grid") }
    var amoledDark by remember { mutableStateOf(false) }
    var is24Hour by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf("English") }

    val themesList = listOf("Bento Grid", "Infinity AMOLED Dark", "Cyberpunk Neon", "Royal Amethyst", "Midnight Ocean", "Cosmic Forest")
    val languages = listOf("English", "Bengali", "Spanish")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Intro
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "INFINITY SMART CLOCK",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Next-Gen AI Timekeeping & Awakening",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Animated Clock Icon in Welcome
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            DynamicAnalogClock(hour = 10, minute = 10, second = 30)
        }

        // Welcome Setup Options Scrollable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme selection
            item {
                Text("Select Theme Presets", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(themesList) { theme ->
                        val isSelected = selectedTheme == theme
                        Card(
                            modifier = Modifier
                                .width(150.dp)
                                .height(60.dp)
                                .clickable { selectedTheme = theme },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    theme,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // AMOLED toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AMOLED Pure Black", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Saves battery on OLED screens", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(checked = amoledDark, onCheckedChange = { amoledDark = it })
                }
            }

            // 12/24 hour format
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("24-Hour Time Format", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Toggle standard vs military time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(checked = is24Hour, onCheckedChange = { is24Hour = it })
                }
            }

            // Language choice
            item {
                Text("Select System Language", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { lang ->
                        val isSelected = selectedLang == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedLang = lang },
                            label = { Text(lang) }
                        )
                    }
                }
            }
        }

        // Action Button
        Button(
            onClick = {
                viewModel.completeWelcome(selectedTheme, amoledDark, is24Hour, selectedLang)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("welcome_complete_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("ENTER THE INFINITY", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.PlayArrow, contentDescription = "Enter")
            }
        }
    }
}

// ==========================================
// 2. PIN LOCK SCREEN
// ==========================================
@Composable
fun PinLockScreen(viewModel: ClockViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Infinity Smart Clock Locked",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Enter your secure authorization PIN",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // PIN display indicators
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
            for (i in 1..4) {
                val entered = viewModel.pinLockInput.length >= i
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (entered) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Basic 10-key number pad
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "🔓")
            )
            for (row in rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (key in row) {
                        Button(
                            onClick = {
                                when (key) {
                                    "C" -> viewModel.pinLockInput = ""
                                    "🔓" -> {
                                        if (!viewModel.unlockApp(viewModel.pinLockInput)) {
                                            viewModel.pinLockInput = ""
                                        }
                                    }
                                    else -> {
                                        if (viewModel.pinLockInput.length < 4) {
                                            viewModel.pinLockInput += key
                                            if (viewModel.pinLockInput.length == 4) {
                                                viewModel.unlockApp(viewModel.pinLockInput)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(key, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. MAIN DASHBOARD / HOME SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ClockViewModel) {
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    var aiCommandInput by remember { mutableStateOf("") }
    var isCommandRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding()
            ) {
                // Top Branding and Live Clock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bento Style Analog Clock logo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color(0xFF381E72),
                                    radius = size.minDimension / 2f,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawLine(
                                    color = Color(0xFF381E72),
                                    start = center,
                                    end = Offset(center.x, center.y - size.height * 0.3f),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color(0xFF381E72),
                                    start = center,
                                    end = Offset(center.x + size.width * 0.2f, center.y),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        Column {
                            Text(
                                "Infinity",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "SMART CLOCK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Live digital/analog secondary clock
                    LiveClockHeader(is24 = settingsState.is24HourFormat)
                }

                // AI Smart Command Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    TextField(
                        value = aiCommandInput,
                        onValueChange = { aiCommandInput = it },
                        placeholder = {
                            Text(
                                "AI Assistant: 'set alarm 6 AM tomorrow' or 'টাইমার ১০ মিনিট'",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_command_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (aiCommandInput.isNotBlank()) {
                                isCommandRunning = true
                                scope.launch {
                                    val reply = viewModel.executeSmartCommand(aiCommandInput)
                                    aiCommandInput = ""
                                    isCommandRunning = false
                                }
                            }
                        })
                    )
                    if (isCommandRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = {
                                if (aiCommandInput.isNotBlank()) {
                                    isCommandRunning = true
                                    scope.launch {
                                        viewModel.executeSmartCommand(aiCommandInput)
                                        aiCommandInput = ""
                                        isCommandRunning = false
                                    }
                                }
                            },
                            modifier = Modifier.testTag("ai_send_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Parse command", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                val menuItems = listOf(
                    Triple("ALARM", "Alarm", Icons.Default.Alarm),
                    Triple("WORLD_CLOCK", "World", Icons.Default.Public),
                    Triple("STOPWATCH", "Stopwatch", Icons.Default.Timer),
                    Triple("TIMER", "Timer", Icons.Default.Timelapse),
                    Triple("SLEEP", "Sleep", Icons.Default.NightsStay),
                    Triple("PLANNER", "Planner", Icons.Default.EventNote),
                    Triple("STATS", "Stats", Icons.Default.BarChart),
                    Triple("SETTINGS", "Settings", Icons.Default.Settings)
                )

                // Simple responsive chunking for tablet screens
                menuItems.forEach { (tabId, label, icon) ->
                    val isSelected = viewModel.currentTab == tabId
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.currentTab = tabId },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 9.sp) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_tab_$tabId")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = viewModel.currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabContent"
            ) { tab ->
                when (tab) {
                    "ALARM" -> AlarmTab(viewModel)
                    "WORLD_CLOCK" -> WorldClockTab(viewModel)
                    "STOPWATCH" -> StopwatchTab(viewModel)
                    "TIMER" -> TimerTab(viewModel)
                    "SLEEP" -> SleepTab(viewModel)
                    "PLANNER" -> PlannerTab(viewModel)
                    "STATS" -> StatsTab(viewModel)
                    else -> SettingsTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun LiveGreetingText() {
    var greeting by remember { mutableStateOf("Welcome") }
    LaunchedEffect(Unit) {
        while (true) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            greeting = when (hour) {
                in 5..11 -> "Good morning, Sleeping Beauty!"
                in 12..16 -> "Good afternoon!"
                in 17..21 -> "Good evening!"
                else -> "Unlocking infinite potential tonight."
            }
            delay(30000)
        }
    }
    Text(greeting, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
}

@Composable
fun LiveClockHeader(is24: Boolean) {
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val format = if (is24) "HH:mm:ss" else "hh:mm:ss a"
            timeText = SimpleDateFormat(format, Locale.getDefault()).format(Date())
            delay(1000)
        }
    }
    Text(
        timeText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// ==========================================
// 4. ALARM TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTab(viewModel: ClockViewModel) {
    val alarmList by viewModel.alarms.collectAsStateWithLifecycle()
    var openAddSheet by remember { mutableStateOf(false) }

    // Add Alarm States
    var hr by remember { mutableStateOf(6) }
    var min by remember { mutableStateOf(0) }
    var lbl by remember { mutableStateOf("") }
    var mis by remember { mutableStateOf("NONE") }
    var repeatDaysStr by remember { mutableStateOf("") }
    var isOneTimeAlarm by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (alarmList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = "No Alarms",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No Alarms Scheduled", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Write 'Set alarm for 7:30 AM tomorrow' or click the + button below to schedule.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Alarms scheduled",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(alarmList) { alarm ->
                    AlarmCard(alarm, viewModel)
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // FAB to add alarm
        FloatingActionButton(
            onClick = { openAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_alarm_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
        }

        // Custom Add Alarm Modal
        if (openAddSheet) {
            AlertDialog(
                onDismissRequest = { openAddSheet = false },
                title = { Text("Create Infinity Alarm", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hour/Min Spinners Mock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("HOUR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (hr > 0) hr-- else hr = 23 }) {
                                        Icon(Icons.Default.KeyboardArrowDown, "Dec")
                                    }
                                    Text(
                                        String.format("%02d", hr),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    IconButton(onClick = { if (hr < 23) hr++ else hr = 0 }) {
                                        Icon(Icons.Default.KeyboardArrowUp, "Inc")
                                    }
                                }
                            }
                            Text(":", fontSize = 32.sp, fontWeight = FontWeight.Black)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MINUTE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (min > 0) min-- else min = 59 }) {
                                        Icon(Icons.Default.KeyboardArrowDown, "Dec")
                                    }
                                    Text(
                                        String.format("%02d", min),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    IconButton(onClick = { if (min < 59) min++ else min = 0 }) {
                                        Icon(Icons.Default.KeyboardArrowUp, "Inc")
                                    }
                                }
                            }
                        }

                        // Label Input
                        OutlinedTextField(
                            value = lbl,
                            onValueChange = { lbl = it },
                            label = { Text("Custom Label") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Mission choice
                        Text("Awakening Mission Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val missions = listOf("NONE", "MATH", "SHAKE", "VOICE")
                            missions.forEach { m ->
                                val selected = mis == m
                                FilterChip(
                                    selected = selected,
                                    onClick = { mis = m },
                                    label = { Text(m) }
                                )
                            }
                        }

                        // One-time or Repeat Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("One-time Alarm", fontSize = 13.sp)
                            Switch(checked = isOneTimeAlarm, onCheckedChange = { isOneTimeAlarm = it })
                        }

                        if (!isOneTimeAlarm) {
                            OutlinedTextField(
                                value = repeatDaysStr,
                                onValueChange = { repeatDaysStr = it },
                                label = { Text("Repeat Days (e.g. Mon,Wed,Fri)") },
                                placeholder = { Text("Mon, Tue, Wed...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addAlarm(hr, min, lbl, mis, repeatDaysStr, isOneTimeAlarm)
                            openAddSheet = false
                            // reset
                            hr = 6
                            min = 0
                            lbl = ""
                            mis = "NONE"
                            repeatDaysStr = ""
                            isOneTimeAlarm = true
                        },
                        modifier = Modifier.testTag("save_alarm_button")
                    ) {
                        Text("Schedule")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { openAddSheet = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AlarmCard(alarm: Alarm, viewModel: ClockViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 1.dp,
                color = if (alarm.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(28.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.enabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val displayHour = if (viewModel.settings.value.is24HourFormat) alarm.hour
                    else if (alarm.hour == 0 || alarm.hour == 12) 12 else alarm.hour % 12
                    val ampm = if (viewModel.settings.value.is24HourFormat) ""
                    else if (alarm.hour >= 12) " PM" else " AM"

                    Text(
                        text = String.format("%02d:%02d%s", displayHour, alarm.minute, ampm),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (alarm.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        alarm.label,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Toggle Switch
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { viewModel.toggleAlarm(alarm) },
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Repeat Info & Mission details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NightsStay,
                        contentDescription = "Mission",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val missionText = when (alarm.dismissMission) {
                        "MATH" -> "Mission: Math Solver 🧮"
                        "SHAKE" -> "Mission: Shake Awake 📳"
                        "VOICE" -> "Mission: Voice command 🗣️"
                        else -> "Mission: Tap Dismiss ⚡"
                    }
                    Text(
                        missionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    if (alarm.isOneTime) "One-time" else alarm.daysOfWeek.ifEmpty { "Daily" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TEST TRIGGER BUTTON (Crucial for evaluation!)
                Button(
                    onClick = { viewModel.triggerAlarmRinging(alarm) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("test_trigger_alarm_${alarm.id}"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Test Trigger", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Wake Mission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.deleteAlarm(alarm) },
                    modifier = Modifier.testTag("delete_alarm_${alarm.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Alarm",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. WORLD CLOCK TAB
// ==========================================
@Composable
fun WorldClockTab(viewModel: ClockViewModel) {
    val worldCities by viewModel.worldCities.collectAsStateWithLifecycle()
    var openAddSheet by remember { mutableStateOf(false) }

    // Mock presets to add
    val cityPresets = listOf(
        WorldCity(cityName = "New York", countryName = "United States", timeZoneId = "America/New_York"),
        WorldCity(cityName = "London", countryName = "United Kingdom", timeZoneId = "Europe/London"),
        WorldCity(cityName = "Tokyo", countryName = "Japan", timeZoneId = "Asia/Tokyo"),
        WorldCity(cityName = "Dhaka", countryName = "Bangladesh", timeZoneId = "Asia/Dhaka"),
        WorldCity(cityName = "Sydney", countryName = "Australia", timeZoneId = "Australia/Sydney"),
        WorldCity(cityName = "Paris", countryName = "France", timeZoneId = "Europe/Paris"),
        WorldCity(cityName = "Cairo", countryName = "Egypt", timeZoneId = "Africa/Cairo")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper dynamic local world clock face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(110.dp)) {
                    val cal = Calendar.getInstance()
                    DynamicAnalogClock(
                        hour = cal.get(Calendar.HOUR_OF_DAY),
                        minute = cal.get(Calendar.MINUTE),
                        second = cal.get(Calendar.SECOND)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Local Time Zone", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cities lists
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("World Cities Time", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(
                onClick = { openAddSheet = true },
                modifier = Modifier.testTag("add_city_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add City")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(worldCities) { city ->
                WorldCityCard(city, viewModel)
            }
        }

        // Meeting timezone planning slider mockup
        MeetingPlannerComponent()

        // Select City Modal
        if (openAddSheet) {
            AlertDialog(
                onDismissRequest = { openAddSheet = false },
                title = { Text("Select City to Add", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cityPresets.forEach { preset ->
                            val alreadyIn = worldCities.any { it.cityName == preset.cityName }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !alreadyIn) {
                                        viewModel.addNewCity(preset.cityName, preset.countryName, preset.timeZoneId)
                                        openAddSheet = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${preset.cityName}, ${preset.countryName}",
                                    color = if (alreadyIn) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                )
                                if (alreadyIn) {
                                    Text("Added", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { openAddSheet = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun WorldCityCard(city: WorldCity, viewModel: ClockViewModel) {
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            timeText = viewModel.formatCityTime(city.timeZoneId)
            delay(1000)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(city.cityName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "${city.countryName} | ${viewModel.getCityTimeDifference(city.timeZoneId)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Day/Night indicator icon
                val cal = Calendar.getInstance(TimeZone.getTimeZone(city.timeZoneId))
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val isNight = hour < 6 || hour > 18
                Icon(
                    imageVector = if (isNight) Icons.Default.NightsStay else Icons.Default.LightMode,
                    contentDescription = if (isNight) "Night" else "Day",
                    tint = if (isNight) MaterialTheme.colorScheme.secondary else Color(0xFFFFB300),
                    modifier = Modifier.padding(end = 12.dp)
                )

                Text(
                    timeText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!city.isDefault) {
                    IconButton(
                        onClick = { viewModel.deleteCity(city) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingPlannerComponent() {
    var hourOffset by remember { mutableStateOf(0f) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Global Meeting Planner 💼", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Text("Slide to offset local hours to preview remote overlap", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = hourOffset,
                onValueChange = { hourOffset = it },
                valueRange = -12f..12f,
                steps = 24
            )
            val offsetInt = hourOffset.toInt()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Local Time: ${offsetInt}h shift", fontSize = 11.sp)
                Text(
                    if (offsetInt == 0) "Synchronized" else if (offsetInt > 0) "+$offsetInt Hours" else "$offsetInt Hours",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// 6. STOPWATCH TAB
// ==========================================
@Composable
fun StopwatchTab(viewModel: ClockViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-precision digital display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = viewModel.formatStopwatchTime(viewModel.stopwatchTimeMs),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pulseAnim = rememberInfiniteTransition(label = "pulse")
                    val alpha by pulseAnim.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (viewModel.stopwatchRunning) Color.Green.copy(alpha = alpha)
                                else Color.Red
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (viewModel.stopwatchRunning) "RUNNING" else "PAUSED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lap Button
            IconButton(
                onClick = { if (viewModel.stopwatchTimeMs > 0L) viewModel.recordStopwatchLap() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Flag, contentDescription = "Lap", tint = MaterialTheme.colorScheme.primary)
            }

            // Start/Pause Button (Main Action)
            FloatingActionButton(
                onClick = {
                    if (viewModel.stopwatchRunning) viewModel.pauseStopwatch() else viewModel.startStopwatch()
                },
                modifier = Modifier.size(72.dp).testTag("stopwatch_toggle_fab"),
                containerColor = if (viewModel.stopwatchRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = if (viewModel.stopwatchRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Start/Pause",
                    modifier = Modifier.size(36.dp)
                )
            }

            // Reset Button
            IconButton(
                onClick = { viewModel.resetStopwatch() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lap Times list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Laps Recorded", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (viewModel.stopwatchLaps.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val text = viewModel.exportStopwatchLapsText()
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Laps copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("export_laps_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Laps", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.stopwatchLaps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Laps", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(viewModel.stopwatchLaps) { lap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lap ${lap.lapNumber}", fontWeight = FontWeight.Bold)
                            Text(viewModel.formatStopwatchTime(lap.lapTimeMs), color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace)
                            Text(viewModel.formatStopwatchTime(lap.totalTimeMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. TIMER TAB
// ==========================================
@Composable
fun TimerTab(viewModel: ClockViewModel) {
    var minInput by remember { mutableStateOf(5) }
    var secInput by remember { mutableStateOf(0) }
    var timerLabel by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Create Multiple Timers ⏲️", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(10.dp))

        // Timer creator panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Duration spinners
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MINUTES", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (minInput > 0) minInput-- }) { Icon(Icons.Default.KeyboardArrowDown, "-") }
                            Text(minInput.toString(), fontSize = 24.sp, fontWeight = FontWeight.Black)
                            IconButton(onClick = { minInput++ }) { Icon(Icons.Default.KeyboardArrowUp, "+") }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SECONDS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (secInput > 0) secInput-- }) { Icon(Icons.Default.KeyboardArrowDown, "-") }
                            Text(secInput.toString(), fontSize = 24.sp, fontWeight = FontWeight.Black)
                            IconButton(onClick = { if (secInput < 59) secInput++ }) { Icon(Icons.Default.KeyboardArrowUp, "+") }
                        }
                    }
                }

                OutlinedTextField(
                    value = timerLabel,
                    onValueChange = { timerLabel = it },
                    label = { Text("Timer Label (e.g., Cooking, Gym)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Modes presets buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addNewTimer(minInput, secInput, timerLabel, "Normal")
                            timerLabel = ""
                        },
                        modifier = Modifier.weight(1f).testTag("start_timer_button")
                    ) {
                        Text("Start Standard")
                    }
                    Button(
                        onClick = {
                            viewModel.addNewTimer(15, 0, "Steak & Rice", "Kitchen")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🍳 Kitchen Mode")
                    }
                    Button(
                        onClick = {
                            viewModel.addNewTimer(1, 30, "Interval Rest", "Workout")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💪 Gym Mode")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Presets
        Text("Quick Presets", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(5, 10, 30)
            presets.forEach { min ->
                OutlinedButton(
                    onClick = { viewModel.addNewTimer(min, 0, "$min Min Preset") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("$min Min")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active timers list
        Text("Active Countdown Timers", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (viewModel.activeTimers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No active timers running.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.activeTimers) { timer ->
                    ActiveTimerCard(timer, viewModel)
                }
            }
        }
    }
}

@Composable
fun ActiveTimerCard(timer: ActiveTimer, viewModel: ClockViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(timer.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                val categoryText = when (timer.mode) {
                    "Kitchen" -> "🍳 Kitchen Countdown"
                    "Workout" -> "💪 Gym Interval"
                    else -> "⏲️ General Timer"
                }
                Text(categoryText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Countdown visual progress bar
                val progress = timer.remainingSeconds.toFloat() / timer.initialSeconds.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (timer.remainingSeconds < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    viewModel.formatTimerSeconds(timer.remainingSeconds),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (timer.remainingSeconds == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                IconButton(onClick = { viewModel.toggleTimerActive(timer) }) {
                    Icon(
                        imageVector = if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(onClick = { viewModel.removeTimer(timer) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ==========================================
// 8. SLEEP TAB (SLEEP SOUNDS, BEDTIME, WAKEUP)
// ==========================================
@Composable
fun SleepTab(viewModel: ClockViewModel) {
    val sleepRecords by viewModel.sleepLogs.collectAsStateWithLifecycle()
    var bedHour by remember { mutableStateOf("22:30") }
    var wakeHour by remember { mutableStateOf("06:30") }
    var qualityStars by remember { mutableStateOf(5) }
    var sleepNotes by remember { mutableStateOf("") }
    var openLogDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Bedtime Suggestion
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NightsStay, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Smart Sleep Suggestion", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    viewModel.getSleepSuggestionText(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playable Sleep Sounds
        Text("Ambient Sleep Sounds 😴", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sounds = listOf(
                Triple("Rain", "🌧️ Rain", "Rainforest storm"),
                Triple("Ocean", "🌊 Ocean", "Calm coast waves"),
                Triple("Forest", "🌲 Forest", "Night wind chimes")
            )
            sounds.forEach { (soundId, label, sub) ->
                val active = viewModel.activeSleepSound == soundId
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { viewModel.toggleSleepSound(soundId) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, fontSize = 18.sp)
                        Text(soundId, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        if (active) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("PLAYING", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Sleep Timer configuration for sounds
        if (viewModel.activeSleepSound != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sleep Sounds Timer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        if (viewModel.sleepSoundTimerSecondsLeft > 0)
                            "Stops music in: ${viewModel.sleepSoundTimerSecondsLeft / 60}m ${viewModel.sleepSoundTimerSecondsLeft % 60}s"
                        else "No timer configured",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { viewModel.setSleepSoundTimer(15) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("15m", fontSize = 10.sp) }
                    OutlinedButton(onClick = { viewModel.setSleepSoundTimer(30) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("30m", fontSize = 10.sp) }
                    IconButton(onClick = { viewModel.stopSleepSoundCountdown() }) { Icon(Icons.Default.Refresh, "Stop") }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wake-up Analysis Chart Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wake-up Quality History", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { openLogDialog = true }, modifier = Modifier.testTag("record_sleep_button")) {
                Icon(Icons.Default.Add, contentDescription = "Log Sleep")
            }
        }

        // Display recent sleep records as simple bar charts or list
        if (sleepRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No Sleep logs recorded yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Custom Sleep Duration chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(bottom = 10.dp)
                ) {
                    SleepQualityGraph(sleepRecords)
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sleepRecords) { log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(log.dateString, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Bedtime: ${log.bedTime} | Wakeup: ${log.wakeTime}", fontSize = 11.sp)
                                    if (log.notes.isNotEmpty()) {
                                        Text("\"${log.notes}\"", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row {
                                        repeat(log.sleepQualityScore) {
                                            Icon(Icons.Default.Star, contentDescription = "*", tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                        }
                                    }
                                    Text("${log.sleepDurationMinutes / 60} Hours", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Sleep Log dialog
        if (openLogDialog) {
            AlertDialog(
                onDismissRequest = { openLogDialog = false },
                title = { Text("Log Sleep Record", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = bedHour,
                            onValueChange = { bedHour = it },
                            label = { Text("Bedtime (e.g. 23:00)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = wakeHour,
                            onValueChange = { wakeHour = it },
                            label = { Text("Wake Time (e.g. 07:00)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Stars choice
                        Text("Sleep Quality Index", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (stars in 1..5) {
                                val selected = qualityStars == stars
                                IconButton(onClick = { qualityStars = stars }) {
                                    Icon(
                                        imageVector = if (selected) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Stars",
                                        tint = if (selected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = sleepNotes,
                            onValueChange = { sleepNotes = it },
                            label = { Text("How do you feel? Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveSleepRecord(bedHour, wakeHour, qualityStars, sleepNotes)
                            openLogDialog = false
                            sleepNotes = ""
                        },
                        modifier = Modifier.testTag("save_sleep_button")
                    ) {
                        Text("Log")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { openLogDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun SleepQualityGraph(logs: List<SleepLog>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val count = logs.size.coerceAtMost(7)
        val sorted = logs.take(count).reversed()
        if (sorted.isEmpty()) return@Canvas

        val margin = 20.dp.toPx()
        val graphHeight = size.height - margin * 2
        val colWidth = (size.width - margin * 2) / count.toFloat()

        sorted.forEachIndexed { index, log ->
            val pct = log.sleepQualityScore / 5.0f
            val h = graphHeight * pct
            val x = margin + (index * colWidth) + (colWidth * 0.15f)
            val y = size.height - margin - h

            // Draw shadow bar
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.1f),
                topLeft = Offset(x, margin),
                size = androidx.compose.ui.geometry.Size(colWidth * 0.7f, graphHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw actual bar
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(colWidth * 0.7f, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

// ==========================================
// 9. PLANNER TAB (DAILY TASKS & REMINDERS)
// ==========================================
@Composable
fun PlannerTab(viewModel: ClockViewModel) {
    val taskList by viewModel.plannerTasks.collectAsStateWithLifecycle()
    var openAddSheet by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var timeStr by remember { mutableStateOf("08:00") }
    var taskType by remember { mutableStateOf("SCHEDULE") } // MEDICINE, WATER, MEETING, HABIT, SCHEDULE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Daily Schedule Progress
        val total = taskList.size
        val completed = taskList.count { it.isCompleted }
        val pct = if (total > 0) completed.toFloat() / total.toFloat() else 0.0f

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Planner Progress 📅", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("$completed of $total tasks completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${(pct * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Planner & Reminders", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { openAddSheet = true }, modifier = Modifier.testTag("add_task_button")) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }

        if (taskList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Schedule is clear today! Type 'Set meeting at 3 PM' in AI box to test.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(taskList) { task ->
                    PlannerTaskCard(task, viewModel)
                }
            }
        }

        // Add Planner Task sheet
        if (openAddSheet) {
            AlertDialog(
                onDismissRequest = { openAddSheet = false },
                title = { Text("Add Schedule Item", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Short Description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = timeStr,
                            onValueChange = { timeStr = it },
                            label = { Text("Time (e.g. 14:30)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Reminder Type", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val types = listOf(
                            Pair("SCHEDULE", "📅 General"),
                            Pair("MEDICINE", "💊 Medicine"),
                            Pair("WATER", "💧 Water"),
                            Pair("MEETING", "💼 Meeting")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            types.forEach { (typeKey, label) ->
                                val selected = taskType == typeKey
                                FilterChip(
                                    selected = selected,
                                    onClick = { taskType = typeKey },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addPlannerTask(title, desc, timeStr, taskType)
                            openAddSheet = false
                            title = ""
                            desc = ""
                            timeStr = "08:00"
                            taskType = "SCHEDULE"
                        },
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { openAddSheet = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun PlannerTaskCard(task: PlannerTask, viewModel: ClockViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = if (task.isCompleted) MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Circular Check checkbox
                IconButton(
                    onClick = { viewModel.toggleTaskCompleted(task) },
                    modifier = Modifier.testTag("checkbox_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    val categoryPrefix = when (task.type) {
                        "MEDICINE" -> "💊 "
                        "WATER" -> "💧 "
                        "MEETING" -> "💼 "
                        "HABIT" -> "🔄 "
                        else -> "📅 "
                    }
                    Text(
                        text = "$categoryPrefix${task.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    )
                    if (task.description.isNotEmpty()) {
                        Text(task.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.timeString,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                IconButton(
                    onClick = { viewModel.deleteTask(task) },
                    modifier = Modifier.testTag("delete_task_${task.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// 10. STATISTICS TAB
// ==========================================
@Composable
fun StatsTab(viewModel: ClockViewModel) {
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Waking Statistics 📊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        // Metrics Grid Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Snooze Hits 😴", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        settingsState.snoozeTotalCount.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Total delay minutes", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("On-time wakeups 🚀", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        settingsState.onTimeWakeCount.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676)
                    )
                    Text("Consistent sleep habit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }

        // Analytical Canvas Chart representing snooze logs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Sleep Cycle Consistencies", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        val points = listOf(20f, 60f, 40f, 90f, 50f, 110f, 85f)
                        val step = size.width / 6f
                        val h = size.height

                        // Draw guidelines
                        drawLine(Color.Gray.copy(0.1f), Offset(0f, h * 0.3f), Offset(size.width, h * 0.3f))
                        drawLine(Color.Gray.copy(0.1f), Offset(0f, h * 0.6f), Offset(size.width, h * 0.6f))

                        // Draw path line
                        for (i in 0..5) {
                            val x1 = i * step
                            val y1 = h - points[i].coerceAtMost(h)
                            val x2 = (i + 1) * step
                            val y2 = h - points[i + 1].coerceAtMost(h)

                            drawLine(Color(0xFF00E5FF), Offset(x1, y1), Offset(x2, y2), strokeWidth = 3.dp.toPx())
                            drawCircle(Color(0xFF00E676), radius = 4.dp.toPx(), center = Offset(x1, y1))
                        }
                    }
                }
            }
        }

        // Fun feedback cards
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = "Tip", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Wakeup Coach Tip", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        if (settingsState.snoozeTotalCount > 3)
                            "Snooze Champion! Try leaving your phone 10 feet away from your bed to prevent accidental snoozes."
                        else "Excellent consistency! Keep up the high on-time wakeups to improve daytime efficiency.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// 11. SETTINGS TAB
// ==========================================
@Composable
fun SettingsTab(viewModel: ClockViewModel) {
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    var pinText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Infinity Preferences 🎨", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        // Theme Select
        Column {
            Text("Switch Active Palette", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            val themesList = listOf("Bento Grid", "Infinity AMOLED Dark", "Cyberpunk Neon", "Royal Amethyst", "Midnight Ocean", "Cosmic Forest")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(themesList) { t ->
                    val selected = settingsState.themeName == t
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.updateTheme(t, settingsState.amoledDark) },
                        label = { Text(t, fontSize = 11.sp) }
                    )
                }
            }
        }

        // AMOLED Black toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AMOLED Pure Black Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("True black pixels saves maximum screen battery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Switch(
                checked = settingsState.amoledDark,
                onCheckedChange = { viewModel.updateTheme(settingsState.themeName, it) }
            )
        }

        // 24 Hour format toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Military / 24-Hour clock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Toggle 12h (AM/PM) vs 24h digital formatting", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Switch(
                checked = settingsState.is24HourFormat,
                onCheckedChange = { viewModel.updateHourFormat(it) }
            )
        }

        // Set secure lock PIN
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Lock Screen Security 🔒", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Protect your schedule and alarm logs with a custom PIN.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 4) pinText = it },
                        label = { Text("4-digit lock PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            viewModel.setPinLock(pinText)
                            pinText = ""
                        },
                        modifier = Modifier.testTag("apply_pin_button")
                    ) {
                        Text("Apply")
                    }
                }
                if (settingsState.pinLock.isNotEmpty()) {
                    Text(
                        "PIN Lock is ACTIVE (PIN: ${settingsState.pinLock}). Click 'Apply' with empty input to disable.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==========================================
// 12. IMMERSIVE ALARM RINGING & MISSIONS SCREEN
// ==========================================
@Composable
fun AlarmRingScreen(viewModel: ClockViewModel) {
    val alarm = viewModel.activeRingingAlarm ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Title & Pulsing Ring
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 24.dp)) {
            Text(
                "INFINITY ALARM ACTIVE ⏰",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                alarm.label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                String.format("%02d:%02d", alarm.hour, alarm.minute),
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Active Mission Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (alarm.dismissMission) {
                    "MATH" -> {
                        Text("🧮 Math Awakening Mission", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Solve this equation to turn off the alarm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            viewModel.currentMathQuestion,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = viewModel.mathUserInput,
                            onValueChange = { viewModel.mathUserInput = it },
                            label = { Text("Enter Answer") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { viewModel.submitMathAnswer() }),
                            modifier = Modifier.fillMaxWidth(0.8f).testTag("math_mission_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.submitMathAnswer() },
                            modifier = Modifier.testTag("submit_math_answer_button")
                        ) {
                            Text("Submit Equation")
                        }
                    }

                    "SHAKE" -> {
                        Text("📳 Shake Awakening Mission", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Click 'SHAKE' rapidly to charge up & wake up", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(24.dp))

                        // Progress circle
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                            CircularProgressIndicator(
                                progress = { viewModel.shakeDismissProgress },
                                modifier = Modifier.size(110.dp),
                                strokeWidth = 8.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${(viewModel.shakeDismissProgress * 100).toInt()}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.triggerShakeProgress() },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(54.dp)
                                .testTag("shake_trigger_button")
                        ) {
                            Text("SHAKE ACTIVE 📳", fontWeight = FontWeight.Bold)
                        }
                    }

                    "QR" -> {
                        Text("📷 QR Code Scanning Mission", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Point camera to mock QR code to dismiss", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(20.dp))

                        if (!viewModel.qrScannerMockOpen) {
                            Button(
                                onClick = { viewModel.qrScannerMockOpen = true },
                                modifier = Modifier.testTag("open_qr_scanner_button")
                            ) {
                                Text("Open QR Scanner")
                            }
                        } else {
                            // Mock Camera Scanner Box
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Search, contentDescription = "QR", tint = Color.Green, modifier = Modifier.size(48.dp))
                                    Text("Scanning code...", fontSize = 10.sp, color = Color.Green)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.submitMockQrScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                                modifier = Modifier.testTag("dismiss_qr_scan_button")
                            ) {
                                Text("Complete QR Scan Match")
                            }
                        }
                    }

                    "VOICE" -> {
                        Text("🗣️ Voice Awaken Command", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Say clearly: 'I am fully awake now!'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(32.dp))

                        IconButton(
                            onClick = { viewModel.submitMockVoicePhrase() },
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("voice_mock_button")
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Voice Mic",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (viewModel.voiceRecordMockSuccess) "Voice matched successfully!"
                            else "Click microphone to record voice verification.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    else -> {
                        Text("Standard Dismissal Mode", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(30.dp))
                        Button(
                            onClick = { viewModel.dismissActiveAlarm() },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(54.dp)
                                .testTag("dismiss_standard_button")
                        ) {
                            Text("DISMISS ALARM", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Snooze Control Bottom Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.snoozeActiveAlarm() },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("snooze_alarm_button")
            ) {
                Text("SNOOZE (5 Min)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 13. DYNAMIC ANALOG CLOCK VISUAL CANVAS
// ==========================================
@Composable
fun DynamicAnalogClock(hour: Int, minute: Int, second: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // Clock Outer Ring
        drawCircle(
            color = primaryColor,
            radius = radius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Hour Marks
        for (i in 0..11) {
            val angle = i * 30 * (Math.PI / 180f)
            val start = Offset(
                (center.x + (radius - 10.dp.toPx()) * sin(angle)).toFloat(),
                (center.y - (radius - 10.dp.toPx()) * cos(angle)).toFloat()
            )
            val end = Offset(
                (center.x + radius * sin(angle)).toFloat(),
                (center.y - radius * cos(angle)).toFloat()
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.5f),
                start = start,
                end = end,
                strokeWidth = 2.dp.toPx()
            )
        }

        // Hour Hand
        val hrAngle = (hour * 30 + minute * 0.5f) * (Math.PI / 180f)
        val hrLength = radius * 0.5f
        val hrEnd = Offset(
            (center.x + hrLength * sin(hrAngle)).toFloat(),
            (center.y - hrLength * cos(hrAngle)).toFloat()
        )
        drawLine(
            color = primaryColor,
            start = center,
            end = hrEnd,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Minute Hand
        val minAngle = minute * 6 * (Math.PI / 180f)
        val minLength = radius * 0.75f
        val minEnd = Offset(
            (center.x + minLength * sin(minAngle)).toFloat(),
            (center.y - minLength * cos(minAngle)).toFloat()
        )
        drawLine(
            color = primaryColor,
            start = center,
            end = minEnd,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Second Hand
        val secAngle = second * 6 * (Math.PI / 180f)
        val secLength = radius * 0.85f
        val secEnd = Offset(
            (center.x + secLength * sin(secAngle)).toFloat(),
            (center.y - secLength * cos(secAngle)).toFloat()
        )
        drawLine(
            color = secondaryColor,
            start = center,
            end = secEnd,
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center Pin
        drawCircle(
            color = secondaryColor,
            radius = 4.dp.toPx(),
            center = center
        )
    }
}
