package com.prtlabs.eventtower.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.ui.components.EventCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizonScreen(
    upcomingEvents: List<Event>,
    onEventClick: (Event) -> Unit
) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Horizon", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .pointerInput(Unit) {
                    detectTapGestures { 
                        selectedEvent = null // Close detail when clicking background
                    }
                }
        ) {
            RadarMap(
                events = upcomingEvents,
                today = today,
                onEventSelect = { selectedEvent = it }
            )

            selectedEvent?.let { event ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                ) {
                    Box {
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event) },
                            onDelete = { /* Logic handled in list view */ }
                        )
                        IconButton(
                            onClick = { selectedEvent = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Close",
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadarMap(
    events: List<Event>,
    today: LocalDate,
    onEventSelect: (Event) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val textMeasurer = rememberTextMeasurer()
    val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    // Calculate dot positions
    val eventPositions = remember(events) {
        events.map { event ->
            val days = ChronoUnit.DAYS.between(today, event.date).coerceAtLeast(0)
            val radius = when {
                days == 0L -> 100f
                days <= 10L -> 200f
                days <= 30L -> 300f
                days <= 90L -> 400f
                else -> 500f
            }
            val angle = (event.id.hashCode().absoluteValue % 360).toFloat()
            event to Pair(radius, angle)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(events) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    eventPositions.find { (_, pos) ->
                        val (r, a) = pos
                        val dotX = centerX + r * cos(a * PI / 180).toFloat()
                        val dotY = centerY + r * sin(a * PI / 180).toFloat()
                        val dist = sqrt((offset.x - dotX).pow(2) + (offset.y - dotY).pow(2))
                        dist < 40f
                    }?.let { (event, _) ->
                        onEventSelect(event)
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)

        // Today Circle
        drawCircle(
            color = Color(0xFFFBC02D),
            radius = 100f,
            center = center,
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )
        
        // Next 10 Days
        drawCircle(
            color = Color(0xFF81C784),
            radius = 200f,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // Month Labels and further circles
        val circles = listOf(
            Triple(300f, today.plusMonths(1), 0.3f),
            Triple(400f, today.plusMonths(2), 0.2f),
            Triple(500f, today.plusMonths(3), 0.1f)
        )

        circles.forEach { (radius, date, alpha) ->
            drawCircle(
                color = Color.Gray.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 1f)
            )
            
            // Draw Month Text on the circle line
            val textLayoutResult = textMeasurer.measure(
                text = date.format(monthFormatter),
                style = TextStyle(color = Color.Gray.copy(alpha = 0.6f), fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(center.x - textLayoutResult.size.width / 2, center.y + radius + 4f)
            )
        }

        // Spinning Light
        rotate(rotation, center) {
            val sweepGradient = Brush.sweepGradient(
                0f to Color.Transparent,
                0.15f to Color(0xFFFBC02D).copy(alpha = 0.3f),
                0.3f to Color.Transparent,
                center = center
            )
            drawArc(
                brush = sweepGradient,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = true,
                size = Size(1000f, 1000f),
                topLeft = Offset(center.x - 500f, center.y - 500f)
            )
        }

        // Event Dots
        eventPositions.forEach { (event, pos) ->
            val (r, a) = pos
            val dotX = center.x + r * cos(a * PI / 180).toFloat()
            val dotY = center.y + r * sin(a * PI / 180).toFloat()
            
            val days = ChronoUnit.DAYS.between(today, event.date)
            val color = when {
                days == 0L -> Color(0xFFFBC02D)
                days <= 10L -> Color(0xFF81C784)
                else -> Color.White.copy(alpha = 0.6f)
            }

            drawCircle(color = color, radius = 14f, center = Offset(dotX, dotY))
            if (days <= 10) {
                drawCircle(color = color.copy(alpha = 0.3f), radius = 22f, center = Offset(dotX, dotY))
            }
        }

        // Tower Silhouette
        val towerWidth = 30f
        val towerHeight = 80f
        drawRect(
            color = Color(0xFF1A1C1E),
            topLeft = Offset(center.x - towerWidth/2, center.y - towerHeight/2 + 10f),
            size = Size(towerWidth, towerHeight - 10f)
        )
        drawRect(
            color = Color(0xFFFBC02D),
            topLeft = Offset(center.x - towerWidth/2 - 5f, center.y - towerHeight/2),
            size = Size(towerWidth + 10f, 15f)
        )
    }
}
