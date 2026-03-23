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
                        selectedEvent = null
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

    // Configuration
    val baseRadius = 100f
    val pixelsPerDay = 6.0f // Increased for better spacing

    val eventPositions = remember(events) {
        events.map { event ->
            val days = ChronoUnit.DAYS.between(today, event.date).coerceAtLeast(0)
            val radius = baseRadius + (days * pixelsPerDay)
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

        // 1. Today Circle (Solid Yellow)
        drawCircle(
            color = Color(0xFFFBC02D),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 2f)
        )
        val todayTextResult = textMeasurer.measure(
            text = "TODAY",
            style = TextStyle(color = Color(0xFFFBC02D).copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        drawText(todayTextResult, topLeft = Offset(center.x - todayTextResult.size.width / 2, center.y + baseRadius + 4f))

        // 2. Next 10 Days Circle (Solid Green, slightly larger radius)
        val next10Radius = baseRadius + (15 * pixelsPerDay) // Increased offset from Today
        drawCircle(
            color = Color(0xFF81C784),
            radius = next10Radius,
            center = center,
            style = Stroke(width = 2f)
        )
        val next10TextResult = textMeasurer.measure(
            text = "NEXT 10 DAYS",
            style = TextStyle(color = Color(0xFF81C784).copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        drawText(next10TextResult, topLeft = Offset(center.x - next10TextResult.size.width / 2, center.y + next10Radius + 4f))

        // 3. Sequential Month Circles
        val circles = listOf(
            Triple(next10Radius + 80f, today.plusMonths(1), "solid_white"),
            Triple(next10Radius + 160f, today.plusMonths(2), "solid_80"),
            Triple(next10Radius + 240f, today.plusMonths(3), "dotted_60"),
            Triple(next10Radius + 320f, today.plusMonths(4), "dotted_40")
        )

        circles.forEach { (radius, date, style) ->
            val color: Color
            val alpha: Float
            val pathEffect: PathEffect?

            when (style) {
                "solid_white" -> {
                    color = Color.White
                    alpha = 1.0f
                    pathEffect = null
                }
                "solid_80" -> {
                    color = Color.White
                    alpha = 0.8f
                    pathEffect = null
                }
                "dotted_60" -> {
                    color = Color.White
                    alpha = 0.6f
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f)
                }
                else -> { // dotted_40
                    color = Color.White
                    alpha = 0.4f
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f)
                }
            }

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 1f, pathEffect = pathEffect)
            )
            
            val monthTextResult = textMeasurer.measure(
                text = date.format(monthFormatter).uppercase(),
                style = TextStyle(color = color.copy(alpha = alpha * 0.7f), fontSize = 10.sp)
            )
            drawText(monthTextResult, topLeft = Offset(center.x - monthTextResult.size.width / 2, center.y + radius + 4f))
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
                size = Size(2000f, 2000f),
                topLeft = Offset(center.x - 1000f, center.y - 1000f)
            )
        }

        // Event Dots
        eventPositions.forEach { (event, pos) ->
            val (r, a) = pos
            val dotX = center.x + r * cos(a * PI / 180).toFloat()
            val dotY = center.y + r * sin(a * PI / 180).toFloat()
            
            val days = ChronoUnit.DAYS.between(today, event.date).coerceAtLeast(0)
            val baseOpacity = (1f - (days / 200f)).coerceIn(0.2f, 1f)
            
            val color = when {
                days == 0L -> Color(0xFFFBC02D)
                days <= 10L -> Color(0xFF81C784)
                else -> Color.White
            }.copy(alpha = baseOpacity)

            drawCircle(color = color, radius = 14f, center = Offset(dotX, dotY))
            
            if (days <= 10) {
                drawCircle(color = color.copy(alpha = 0.3f * baseOpacity), radius = 22f, center = Offset(dotX, dotY))
            }
        }

        // Watchtower Silhouette
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
