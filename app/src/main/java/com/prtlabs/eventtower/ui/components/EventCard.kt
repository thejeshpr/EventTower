package com.prtlabs.eventtower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.data.Recurring
import com.prtlabs.eventtower.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isPast = event.date.isBefore(today)
    val isToday = event.date.isEqual(today)
    val daysRemaining = ChronoUnit.DAYS.between(today, event.date)
    
    // Determining background and content color
    // Today: Light Yellow with Black text
    // Next 10 Days: Light Green with Black text
    // standard/Upcoming: Dark with White text
    val cardBg = when {
        isToday -> Color(0xFFFFF9C4)
        daysRemaining in 1..10 && !isPast -> Color(0xFFE8F5E9)
        isPast -> Color(0xFF2D2F31)
        else -> Color(0xFF1E1F22)
    }
    
    val contentColor = when {
        isToday || (daysRemaining in 1..10 && !isPast) -> Color.Black
        else -> Color.White.copy(alpha = 0.9f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayTitle = event.title.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event.recurring != Recurring.NO) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            modifier = Modifier.size(18.dp),
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = contentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = event.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = contentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = contentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = event.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                if (!event.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = event.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End, 
                modifier = Modifier.padding(start = 16.dp)
            ) {
                val durationText = DateUtils.formatDuration(event.date)
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 36.sp,
                        letterSpacing = (-1).sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    textAlign = TextAlign.End
                )
                
                Text(
                    text = if (isPast) "SINCE THEN" else if (isToday) "TODAY" else "REMAINING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = contentColor.copy(alpha = 0.5f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
