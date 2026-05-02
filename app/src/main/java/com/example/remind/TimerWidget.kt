package com.example.remind

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class TimerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = TimerRepository(context)
        
        provideContent {
            val size = LocalSize.current
            val timers by repository.timersFlow.collectAsState(initial = emptyList())
            
            WidgetContent(timers, size)
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(timers: List<Timer>, size: DpSize) {
        val isSmallHeight = size.height <= 110.dp
        val isSmallWidth = size.width <= 190.dp
        
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_glass_bg))
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            if (timers.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No active timers",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(timers) { timer ->
                        TimerItem(timer, isSmallHeight, isSmallWidth)
                        Spacer(modifier = GlanceModifier.height(12.dp))
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TimerItem(timer: Timer, isSmallHeight: Boolean, isSmallWidth: Boolean) {
        val now = System.currentTimeMillis()
        val progress = Utils.calculateProgress(timer.startDateTime, timer.endDateTime, now)
        val remaining = Utils.getRemainingTimeText(timer.endDateTime, now)

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            if (!isSmallHeight) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timer.name,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    if (!isSmallWidth) {
                        Text(
                            text = remaining,
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp)
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
            }

            // Custom Progress Bar using blocks for Glance compatibility
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.5f))
            ) {
                val blockCount = 50
                val filledBlocks = (progress * blockCount).toInt()
                
                repeat(blockCount) { index ->
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(if (index < filledBlocks) Color.White else Color.Transparent)
                    ) {}
                }
            }
        }
    }
}

class TimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimerWidget()
}
