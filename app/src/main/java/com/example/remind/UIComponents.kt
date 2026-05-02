package com.example.remind

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.util.*

@Composable
fun MainScreen(viewModel: TimerViewModel) {
    val timers by viewModel.timers.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remind",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

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
            }

            if (timers.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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

    if (showAddDialog) {
        AddTimerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, endDateTime ->
                viewModel.addTimer(name, endDateTime)
                showAddDialog = false
            }
        )
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
                        text = String.format("%.1f%%", progress * 100),
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
