package com.example.remind

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.util.*

@Composable
fun MainScreen(viewModel: TimerViewModel) {
    val timers by viewModel.timers.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = if (isUpdating) 1 else if (isSettingsOpen) 2 else 0,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally { width -> width } + fadeIn(tween(300))).togetherWith(
                    slideOutHorizontally { width -> -width / 2 } + fadeOut(tween(300)))
            } else {
                (slideInHorizontally { width -> -width / 2 } + fadeIn(tween(300))).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut(tween(300)))
            }.using(SizeTransform(clip = false))
        },
        label = "ScreenTransition",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            1 -> UpdateScreen(viewModel)
            2 -> SettingsScreen(viewModel)
            else -> {
                Scaffold(
                    containerColor = Color(0xFFF1F1F1),
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(top = 20.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // New Timer Button
                            Box(
                                modifier = Modifier
                                    .shadow(20.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = 0.08f), ambientColor = Color.Transparent, clip = false)
                                    .background(Color.White, RoundedCornerShape(30.dp))
                                    .clickable { showAddDialog = true }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "New Timer",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            // Settings Button
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(20.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.08f), ambientColor = Color.Transparent, clip = false)
                                    .background(Color.White, CircleShape)
                                    .clickable { viewModel.openSettings() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "Settings", tint = Color.Black)
                            }
                        }

                        if (timers.isEmpty()) {
                            EmptyState()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(timers, key = { it.id }) { timer ->
                                    TimerCard(
                                        timer = timer,
                                        currentTime = currentTime,
                                        onDelete = { viewModel.deleteTimer(timer) },
                                        onPin = { viewModel.togglePin(timer) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, endDateTime ->
                viewModel.addTimer(name, endDateTime)
                showAddDialog = false
            }
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(
            onDismiss = { viewModel.dismissUpdateDialog() },
            onConfirm = { viewModel.startUpdate() }
        )
    }
}

@Composable
fun SettingsScreen(viewModel: TimerViewModel) {
    val settings by viewModel.notificationSettings.collectAsState()
    var showAddPercentageDialog by remember { mutableStateOf(false) }
    var showAddTimeDialog by remember { mutableStateOf(false) }
    var editingSetting by remember { mutableStateOf<NotificationSetting?>(null) }

    BackHandler {
        viewModel.closeSettings()
    }

    Scaffold(
        containerColor = Color(0xFFF1F1F1),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeSettings() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                NotificationGroup(
                    title = "Percentage Based",
                    settings = settings.filterIsInstance<NotificationSetting.PercentageSetting>(),
                    onAdd = { showAddPercentageDialog = true },
                    onEdit = { editingSetting = it },
                    onDelete = { viewModel.deleteNotificationSetting(it.id) }
                )
            }

            item {
                NotificationGroup(
                    title = "Time Based",
                    settings = settings.filterIsInstance<NotificationSetting.TimeSetting>(),
                    onAdd = { showAddTimeDialog = true },
                    onEdit = { editingSetting = it },
                    onDelete = { viewModel.deleteNotificationSetting(it.id) }
                )
            }
        }
    }

    if (showAddPercentageDialog) {
        AddEditPercentageDialog(
            onDismiss = { showAddPercentageDialog = false },
            onConfirm = { percentage ->
                viewModel.addNotificationSetting(NotificationSetting.PercentageSetting(percentage = percentage))
                showAddPercentageDialog = false
            }
        )
    }

    if (showAddTimeDialog) {
        AddEditTimeDialog(
            onDismiss = { showAddTimeDialog = false },
            onConfirm = { type, value, unit ->
                viewModel.addNotificationSetting(NotificationSetting.TimeSetting(type = type, value = value, unit = unit))
                showAddTimeDialog = false
            }
        )
    }

    editingSetting?.let { setting ->
        when (setting) {
            is NotificationSetting.PercentageSetting -> {
                AddEditPercentageDialog(
                    initialPercentage = setting.percentage,
                    onDismiss = { editingSetting = null },
                    onConfirm = { percentage ->
                        viewModel.updateNotificationSetting(setting.copy(percentage = percentage))
                        editingSetting = null
                    }
                )
            }
            is NotificationSetting.TimeSetting -> {
                AddEditTimeDialog(
                    initialType = setting.type,
                    initialValue = setting.value,
                    initialUnit = setting.unit,
                    onDismiss = { editingSetting = null },
                    onConfirm = { type, value, unit ->
                        viewModel.updateNotificationSetting(setting.copy(type = type, value = value, unit = unit))
                        editingSetting = null
                    }
                )
            }
        }
    }
}

@Composable
fun <T : NotificationSetting> NotificationGroup(
    title: String,
    settings: List<T>,
    onAdd: () -> Unit,
    onEdit: (T) -> Unit,
    onDelete: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.08f), ambientColor = Color.Transparent, clip = false)
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
            }
        }

        Spacer(Modifier.height(8.dp))

        settings.forEach { setting ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = when (setting) {
                    is NotificationSetting.PercentageSetting -> "${setting.percentage}%"
                    is NotificationSetting.TimeSetting -> {
                        val typeText = if (setting.type == TimeType.BEFORE) "Before" else "After"
                        "$typeText ${setting.value} ${setting.unit.name.lowercase()}"
                    }
                }

                Text(text = label, color = Color.Black)

                Row {
                    IconButton(onClick = { onEdit(setting) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                    }
                    IconButton(onClick = { onDelete(setting) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditPercentageDialog(
    initialPercentage: Int = 50,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var percentage by remember { mutableStateOf(initialPercentage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Percentage Notification") },
        text = {
            Column {
                OutlinedTextField(
                    value = percentage,
                    onValueChange = { percentage = it.filter { c -> c.isDigit() } },
                    label = { Text("Percentage") },
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("%") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { percentage.toIntOrNull()?.let { onConfirm(it) } },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Black)
            }
        }
    )
}

@Composable
fun AddEditTimeDialog(
    initialType: TimeType = TimeType.BEFORE,
    initialValue: Int = 1,
    initialUnit: TimeUnitType = TimeUnitType.DAYS,
    onDismiss: () -> Unit,
    onConfirm: (TimeType, Int, TimeUnitType) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var value by remember { mutableStateOf(initialValue.toString()) }
    var unit by remember { mutableStateOf(initialUnit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Time Based Notification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Type Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                // Value Input
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() } },
                    label = { Text("Value") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeUnitType.entries.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { value.toIntOrNull()?.let { onConfirm(type, it, unit) } },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Black)
            }
        }
    )
}

@Composable
fun UpdateDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("New Version Available", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text("A new update is available. Do you want to update now?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Update now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = Color.Gray)
            }
        }
    )
}

@Composable
fun UpdateScreen(viewModel: TimerViewModel) {
    val progress by viewModel.downloadProgress.collectAsState()
    val sizeInfo by viewModel.downloadSizeInfo.collectAsState()
    val isDone by viewModel.isUpdateDone.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F1F1))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isDone) "Done" else "Updating",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        if (!isDone) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = sizeInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        Spacer(Modifier.height(24.dp))
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFFECECEC), RoundedCornerShape(10.dp))
        ) {
            val gradient = if (isDone) {
                Brush.horizontalGradient(listOf(Color(0xFF00B9E0), Color(0xFF00C428)))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFFEC0065), Color(0xFFFFC501)))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(gradient, RoundedCornerShape(10.dp))
            )
        }
        
        if (isDone) {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.installUpdate() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No active timers",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun TimerCard(
    timer: Timer,
    currentTime: Long,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    val progress = Utils.calculateProgress(timer.startDateTime, timer.endDateTime, currentTime)
    val remainingText = Utils.getRemainingTimeText(timer.endDateTime, currentTime)
    val isCompleted = progress >= 1.0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.08f), ambientColor = Color.Transparent, clip = false)
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timer.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Ends: ${Utils.formatDateTime(timer.endDateTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Row {
                    IconButton(
                        onClick = onPin,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (timer.isPinned) Color(0xFFE3F2FD) else Color(0xFFEEEEEE),
                            contentColor = if (timer.isPinned) Color(0xFF2196F3) else Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (timer.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFEEEEEE),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFECECEC), RoundedCornerShape(10.dp))
            ) {
                val gradient = if (isCompleted) {
                    Brush.horizontalGradient(listOf(Color(0xFF00B9E0), Color(0xFF00C428)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFEC0065), Color(0xFFFFC501)))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(gradient, RoundedCornerShape(10.dp))
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", progress * 100),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.W500
                    )
                }
            }
        }
    }
}

@Composable
fun AddTimerDialog(onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedTime by remember { mutableStateOf<Calendar?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text("New Timer", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Timer Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        DatePickerDialog(context, { _, y, m, d ->
                            val c = Calendar.getInstance()
                            c.set(y, m, d)
                            selectedDate = c
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7F7F7), contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(selectedDate?.let { "${it.get(Calendar.DAY_OF_MONTH)}/${it.get(Calendar.MONTH) + 1}/${it.get(Calendar.YEAR)}" } ?: "Select Date")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            val c = Calendar.getInstance()
                            c.set(Calendar.HOUR_OF_DAY, h)
                            c.set(Calendar.MINUTE, m)
                            selectedTime = c
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7F7F7), contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(selectedTime?.let { 
                        val hour = it.get(Calendar.HOUR_OF_DAY)
                        val minute = it.get(Calendar.MINUTE)
                        val amPm = if (hour < 12) "AM" else "PM"
                        val displayHour = when {
                            hour == 0 -> 12
                            hour > 12 -> hour - 12
                            else -> hour
                        }
                        String.format("%02d:%02d %s", displayHour, minute, amPm)
                    } ?: "Select Time")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && selectedDate != null && selectedTime != null) {
                        val finalCalendar = Calendar.getInstance()
                        finalCalendar.set(
                            selectedDate!!.get(Calendar.YEAR),
                            selectedDate!!.get(Calendar.MONTH),
                            selectedDate!!.get(Calendar.DAY_OF_MONTH),
                            selectedTime!!.get(Calendar.HOUR_OF_DAY),
                            selectedTime!!.get(Calendar.MINUTE),
                            0
                        )
                        
                        if (finalCalendar.timeInMillis <= System.currentTimeMillis()) {
                            Toast.makeText(context, "Choose a valid date/time", Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirm(name, finalCalendar.timeInMillis)
                        }
                    }
                },
                enabled = name.isNotEmpty() && selectedDate != null && selectedTime != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
