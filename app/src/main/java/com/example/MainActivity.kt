package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.CounterViewModel
import com.example.ui.CounterViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room DB & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = CounterRepository(database.counterDao())
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val viewModel: CounterViewModel = viewModel(
                    factory = CounterViewModelFactory(repository)
                )
                CounterAppScreen(viewModel = viewModel)
            }
        }
    }
}

// Helper to parse hex string safely to Compose Color
fun parseColorHex(hex: String, default: Color = Color(0xFF4F46E5)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}

// Predefined beautiful branding colors
val CuratedColors = listOf(
    "#4F46E5" to "Indigo",
    "#06B6D4" to "Cyan",
    "#10B981" to "Emerald",
    "#EF4444" to "Red",
    "#F59E0B" to "Amber",
    "#EC4899" to "Pink",
    "#8B5CF6" to "Purple",
    "#374151" to "Steel"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterAppScreen(viewModel: CounterViewModel) {
    val counters by viewModel.counters.collectAsStateWithLifecycle()
    val selectedCounter by viewModel.selectedCounter.collectAsStateWithLifecycle()
    val selectedCounterLogs by viewModel.selectedCounterLogs.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDirectEditDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { /* Decorative drawer menu matching design */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Tally Counter",
                        fontWeight = FontWeight.Normal,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showHistoryDialog = true
                        },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View all history logs"
                        )
                    }
                    if (selectedCounter != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEditDialog = true
                            },
                            modifier = Modifier.testTag("edit_counter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit current counter settings"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .testTag("add_counter_fab")
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new counter"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // 1. HORIZONTAL COUNTER LIST
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "My Counters",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                
                if (counters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(counters) { counter ->
                            val isSelected = selectedCounter?.id == counter.id
                            val counterColor = parseColorHex(counter.colorHex)
                            
                            Card(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectCounter(counter.id)
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        counterColor.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) counterColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .testTag("counter_tab_${counter.id}")
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Colored accent indicator
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(counterColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = counter.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "${counter.count}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) counterColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 2. MAIN CLICKER DISPLAY CONTAINER
            val currentCounter = selectedCounter
            if (currentCounter != null) {
                val counterColor = parseColorHex(currentCounter.colorHex)
                
                // Track scale animation for tapping
                var buttonPressedScale by remember { mutableStateOf(1f) }
                val scale by animateFloatAsState(
                    targetValue = buttonPressedScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                
                // Key-based animation state for when the number increases or decreases
                var countChangeKey by remember { mutableStateOf(currentCounter.count) }
                LaunchedEffect(currentCounter.count) {
                    countChangeKey = currentCounter.count
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    
                    // Large Counter click card (Sleek Theme style)
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else {
                                Color(0xFFF3EDF7)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .aspectRatio(1.1f)
                            .scale(scale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current
                            ) {
                                // Tapping the main card surface increments!
                                coroutineScope.launch {
                                    buttonPressedScale = 1.05f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.increment()
                                    delay(80)
                                    buttonPressedScale = 1f
                                }
                            }
                            .testTag("dial_clicker")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                // Sub title/label of counter
                                Text(
                                    text = "Current Count".uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = counterColor,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                
                                // Beautiful large animated ticker for count view
                                AnimatedContent(
                                    targetState = currentCounter.count,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            slideInVertically { height -> height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> -height } + fadeOut()
                                        } else {
                                            slideInVertically { height -> -height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> height } + fadeOut()
                                        }.using(
                                            SizeTransform(clip = false)
                                        )
                                    }
                                ) { targetCount ->
                                    Text(
                                        text = "$targetCount",
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = if (targetCount.toString().length > 4) 64.sp else 112.sp,
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = (-4).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showDirectEditDialog = true
                                            }
                                            .testTag("count_display")
                                    )
                                }
                                
                                // Target tracking metadata sub-row
                                val targetVal = 100
                                val percentage = if (targetVal > 0) (currentCounter.count.toFloat() / targetVal * 100).toInt() else 0
                                val reachedText = if (percentage >= 100) "Goal achieved!" else "$percentage% reached"
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text(
                                        text = "Target: $targetVal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = reachedText,
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // INCREMENT / DECREMENT / RESET Controls Bar (highly ergonomic)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // MINUS Button (Sleek Theme style)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    buttonPressedScale = 0.95f
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.decrement()
                                    delay(60)
                                    buttonPressedScale = 1f
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .testTag("decrement_button")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove, 
                                    contentDescription = "Subtract",
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "-${currentCounter.decrementStep}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        // RESET Button (Inline middle option)
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.reset()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(80.dp)
                                .width(68.dp)
                                .testTag("reset_button")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh, 
                                    contentDescription = "Reset Count",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        // PLUS Button (Sleek Theme style)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    buttonPressedScale = 1.05f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.increment()
                                    delay(60)
                                    buttonPressedScale = 1f
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = counterColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(80.dp)
                                .shadow(8.dp, shape = RoundedCornerShape(16.dp))
                                .testTag("increment_button")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add, 
                                    contentDescription = "Add",
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "+${currentCounter.incrementStep}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                // 3. STATS & RECENT HISTORY BLOCK
                Card(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Activity for ${currentCounter.name}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            
                            val lastUpText = if (currentCounter.lastUpdated > 0) {
                                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                                "Edited: " + sdf.format(Date(currentCounter.lastUpdated))
                            } else "No edits"
                            
                            Text(
                                text = lastUpText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        if (selectedCounterLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ready to count! Tap above to log activity.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(selectedCounterLogs.take(5)) { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val logDotColor = if (log.newValue > log.previousValue) {
                                                parseColorHex(currentCounter.colorHex)
                                            } else if (log.newValue < log.previousValue) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(logDotColor)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = log.label ?: "Value changed",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${log.previousValue} ➔ ${log.newValue}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = counterColor,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                            Text(
                                                text = formatRelativeTime(log.timestamp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty view loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    
    // --- DIALOGS IMPLEMENTATION ---
    
    // 1. DIALOG: CREATE NEW COUNTER
    if (showCreateDialog) {
        var nameInput by remember { mutableStateOf("") }
        var initialValInput by remember { mutableStateOf("0") }
        var incStepInput by remember { mutableStateOf("1") }
        var decStepInput by remember { mutableStateOf("1") }
        var selectedColorHex by remember { mutableStateOf(CuratedColors.first().first) }
        
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "New Counter",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Counter Name (e.g. Gym reps)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_counter_name")
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = initialValInput,
                            onValueChange = { initialValInput = it.filter { char -> char.isDigit() || char == '-' } },
                            label = { Text("Start Val") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_counter_initial")
                        )
                        OutlinedTextField(
                            value = incStepInput,
                            onValueChange = { incStepInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Inc Step (+)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_counter_inc_step")
                        )
                        OutlinedTextField(
                            value = decStepInput,
                            onValueChange = { decStepInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Dec Step (-)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                        )
                    }
                    
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    // Curated colors grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CuratedColors.take(4).forEach { (hex, name) ->
                            val color = parseColorHex(hex)
                            val isChosen = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isChosen) 3.dp else 0.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                                    .testTag("color_option_$name")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CuratedColors.drop(4).forEach { (hex, name) ->
                            val color = parseColorHex(hex)
                            val isChosen = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isChosen) 3.dp else 0.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                                    .testTag("color_option_$name")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val initVal = initialValInput.toIntOrNull() ?: 0
                        val incVal = incStepInput.toIntOrNull() ?: 1
                        val decVal = decStepInput.toIntOrNull() ?: 1
                        viewModel.createCounter(
                            name = nameInput,
                            initialValue = initVal,
                            incrementStep = incVal,
                            decrementStep = decVal,
                            colorHex = selectedColorHex
                        )
                        showCreateDialog = false
                    },
                    modifier = Modifier.testTag("save_new_counter")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // 2. DIALOG: EDIT SELECTED COUNTER
    if (showEditDialog && selectedCounter != null) {
        val editing = selectedCounter!!
        var nameInput by remember { mutableStateOf(editing.name) }
        var incStepInput by remember { mutableStateOf(editing.incrementStep.toString()) }
        var decStepInput by remember { mutableStateOf(editing.decrementStep.toString()) }
        var selectedColorHex by remember { mutableStateOf(editing.colorHex) }
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Settings",
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteSelectedCounter()
                            showEditDialog = false
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_counter_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete this counter"
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Counter Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_counter_name")
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = incStepInput,
                            onValueChange = { incStepInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Inc Step (+)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_counter_inc_step")
                        )
                        OutlinedTextField(
                            value = decStepInput,
                            onValueChange = { decStepInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Dec Step (-)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    // Curated colors grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CuratedColors.take(4).forEach { (hex, name) ->
                            val color = parseColorHex(hex)
                            val isChosen = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isChosen) 3.dp else 0.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                                    .testTag("edit_color_option_$name")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CuratedColors.drop(4).forEach { (hex, name) ->
                            val color = parseColorHex(hex)
                            val isChosen = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isChosen) 3.dp else 0.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                                    .testTag("edit_color_option_$name")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val incVal = incStepInput.toIntOrNull() ?: 1
                        val decVal = decStepInput.toIntOrNull() ?: 1
                        viewModel.updateSelectedCounterDetails(
                            name = nameInput,
                            incrementStep = incVal,
                            decrementStep = decVal,
                            colorHex = selectedColorHex
                        )
                        showEditDialog = false
                    },
                    modifier = Modifier.testTag("save_edit_counter")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // 3. DIALOG: DIRECT VALUE JUMP/EDIT
    if (showDirectEditDialog && selectedCounter != null) {
        val current = selectedCounter!!
        var valueInput by remember { mutableStateOf(current.count.toString()) }
        
        AlertDialog(
            onDismissRequest = { showDirectEditDialog = false },
            title = {
                Text(
                    text = "Jump to Value",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter a precise numeric count for ${current.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = valueInput,
                        onValueChange = { valueInput = it.filter { char -> char.isDigit() || char == '-' } },
                        label = { Text("Count Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_count_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newVal = valueInput.toIntOrNull() ?: current.count
                        viewModel.updateCountDirectly(newVal)
                        showDirectEditDialog = false
                    },
                    modifier = Modifier.testTag("save_direct_count")
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // 4. DIALOG: FULL LOG HISTORY REVIEW
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Tally Log History",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { showHistoryDialog = false }
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    if (allLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No log activity recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allLogs) { log ->
                                val corrCounter = counters.find { it.id == log.counterId }
                                val accentColor = if (corrCounter != null) parseColorHex(corrCounter.colorHex) else MaterialTheme.colorScheme.primary
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .border(
                                            width = 1.dp,
                                            color = accentColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = corrCounter?.name ?: "Unknown Counter",
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = formatRelativeTime(log.timestamp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.label ?: "Value updated",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${log.previousValue} ➔ ${log.newValue}",
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHistoryDialog = false }
                ) {
                    Text("Done")
                }
            }
        )
    }
}

// Relative time formatting function
fun formatRelativeTime(timestamp: Long): String {
    val duration = System.currentTimeMillis() - timestamp
    return when {
        duration < 1000 -> "now"
        duration < 60000 -> "${duration / 1000}s ago"
        duration < 3600000 -> "${duration / 60000}m ago"
        duration < 86400000 -> "${duration / 3600000}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
