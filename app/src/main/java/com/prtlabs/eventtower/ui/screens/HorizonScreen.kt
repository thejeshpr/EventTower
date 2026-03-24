package com.prtlabs.eventtower.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
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
    onEventClick: (Event) -> Unit,
    onBackToUpcoming: () -> Unit
) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val today = LocalDate.now()

    BackHandler {
        onBackToUpcoming()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Horizon", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0D1117))
        ) {
            RadarMap(
                events = upcomingEvents,
                today = today,
                onEventSelect = { selectedEvent = it },
                onBackgroundClick = { selectedEvent = null }
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
    onEventSelect: (Event) -> Unit,
    onBackgroundClick: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarRotation")
    val scanningRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val textMeasurer = rememberTextMeasurer()
    val monthFormatter = DateTimeFormatter.ofPattern("MMM")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

    val baseRadius = 80f
    val pixelsPerDay = 8.0f 

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
            .transformable(state = state)
            .pointerInput(events, scale, offset) {
                detectTapGestures { tapOffset ->
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    val transformedTap = Offset(
                        x = (tapOffset.x - centerX - offset.x) / scale + centerX,
                        y = (tapOffset.y - centerY - offset.y) / scale + centerY
                    )

                    var clickedEvent: Event? = null
                    eventPositions.find { (_, pos) ->
                        val (r, a) = pos
                        val dotX = centerX + r * cos(a * PI / 180).toFloat()
                        val dotY = centerY + r * sin(a * PI / 180).toFloat()
                        val dist = sqrt((transformedTap.x - dotX).pow(2) + (transformedTap.y - dotY).pow(2))
                        dist < 40f / scale
                    }?.let { (event, _) ->
                        clickedEvent = event
                        onEventSelect(event)
                    }
                    
                    if (clickedEvent == null) {
                        onBackgroundClick()
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)

        withTransform({
            translate(offset.x, offset.y)
            scale(scale, scale, center)
        }) {
            // 1. Today Circle
            drawCircle(
                color = Color(0xFFFBC02D),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2f / scale)
            )
            val todayTextResult = textMeasurer.measure(
                text = "TODAY",
                style = TextStyle(color = Color(0xFFFBC02D).copy(alpha = 0.8f), fontSize = (10 / scale).sp, fontWeight = FontWeight.Bold)
            )
            drawText(todayTextResult, topLeft = Offset(center.x - todayTextResult.size.width / 2, center.y + baseRadius + 4f))

            // 2. 10 Days Circle
            val radius10Days = baseRadius + (10 * pixelsPerDay)
            drawCircle(
                color = Color(0xFF81C784),
                radius = radius10Days,
                center = center,
                style = Stroke(width = 2f / scale)
            )
            val text10DaysResult = textMeasurer.measure(
                text = "10DAYS",
                style = TextStyle(color = Color(0xFF81C784).copy(alpha = 0.8f), fontSize = (10 / scale).sp, fontWeight = FontWeight.Bold)
            )
            drawText(text10DaysResult, topLeft = Offset(center.x - text10DaysResult.size.width / 2, center.y + radius10Days + 4f))

            // 3. Month Rings
            val circles = listOf(
                Triple(baseRadius + (30 * pixelsPerDay), today.plusMonths(1), "solid_white"),
                Triple(baseRadius + (60 * pixelsPerDay), today.plusMonths(2), "solid_80"),
                Triple(baseRadius + (90 * pixelsPerDay), today.plusMonths(3), "dotted_60"),
                Triple(baseRadius + (120 * pixelsPerDay), today.plusMonths(4), "dotted_40")
            )

            circles.forEach { (radius, date, style) ->
                val color: Color
                val alpha: Float
                val pathEffect: PathEffect?

                when (style) {
                    "solid_white" -> { color = Color.White; alpha = 1.0f; pathEffect = null }
                    "solid_80" -> { color = Color.White; alpha = 0.8f; pathEffect = null }
                    "dotted_60" -> { color = Color.White; alpha = 0.6f; pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f / scale, 10f / scale), 0f) }
                    else -> { color = Color.White; alpha = 0.4f; pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f / scale, 10f / scale), 0f) }
                }

                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1f / scale, pathEffect = pathEffect)
                )
                
                val monthTextResult = textMeasurer.measure(
                    text = date.format(monthFormatter).uppercase(),
                    style = TextStyle(color = color.copy(alpha = alpha * 0.7f), fontSize = (10 / scale).sp)
                )
                drawText(monthTextResult, topLeft = Offset(center.x - monthTextResult.size.width / 2, center.y + radius + 4f))
            }

            // Spinning scanning light
            rotate(scanningRotation, center) {
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

                // Reduced dot size from 18f to 12f
                drawCircle(color = color, radius = 12f / scale, center = Offset(dotX, dotY))
                
                if (days <= 10) {
                    drawCircle(color = color.copy(alpha = 0.3f * baseOpacity), radius = 20f / scale, center = Offset(dotX, dotY))
                }

                // Draw Event Title Text + Date
                val displayText = "${event.title} (${event.date.format(dateFormatter)})"
                val titleTextResult = textMeasurer.measure(
                    text = displayText,
                    style = TextStyle(
                        color = color.copy(alpha = (baseOpacity + 0.2f).coerceAtMost(1f)),
                        fontSize = (10 / scale).sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                drawText(
                    textLayoutResult = titleTextResult,
                    topLeft = Offset(dotX + (16f / scale), dotY - titleTextResult.size.height / 2)
                )
            }

            // Watchtower Silhouette
            val towerWidth = 30f / scale
            val towerHeight = 80f / scale
            drawRect(
                color = Color(0xFF1A1C1E),
                topLeft = Offset(center.x - towerWidth/2, center.y - towerHeight/2 + (10f/scale)),
                size = Size(towerWidth, towerHeight - (10f/scale))
            )
            drawRect(
                color = Color(0xFFFBC02D),
                topLeft = Offset(center.x - towerWidth/2 - (5f/scale), center.y - towerHeight/2),
                size = Size(towerWidth + (10f/scale), 15f / scale)
            )
        }
    }
}
