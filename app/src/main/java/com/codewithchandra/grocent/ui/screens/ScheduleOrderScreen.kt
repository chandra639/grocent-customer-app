package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.TextGray
import com.codewithchandra.grocent.ui.theme.PrimaryOrange
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Builds time slots for a given day.
 * @param forToday if true, only slots after current time (next full hour); if false, 7:00–22:00.
 * @return List of pairs: (display "11 – 12 PM", stored "11:00 AM")
 */
private fun buildSlots(forToday: Boolean): List<Pair<String, String>> {
    val cal = Calendar.getInstance()
    val nowHour = cal.get(Calendar.HOUR_OF_DAY)
    val startHour = if (forToday) {
        val nextHour = nowHour + 1
        if (nextHour > 22) return emptyList()
        nextHour.coerceIn(7, 22)
    } else {
        7
    }
    val slots = mutableListOf<Pair<String, String>>()
    for (h in startHour..21) {
        val startAmPm = if (h < 12) "AM" else "PM"
        val startHour12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        val endH = h + 1
        val endAmPm = if (endH < 12) "AM" else "PM"
        val endHour12 = if (endH == 0) 12 else if (endH > 12) endH - 12 else endH
        val display = "$startHour12 – $endHour12 $endAmPm"
        val stored = String.format("%d:00 %s", startHour12, startAmPm)
        slots.add(display to stored)
    }
    return slots
}

@Composable
fun ScheduleOrderScreen(
    locationViewModel: LocationViewModel,
    onBackClick: () -> Unit,
    onConfirm: () -> Unit
) {
    val cal = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())

    val todayLabel = "Today, ${dateFormat.format(cal.time)}"
    cal.add(Calendar.DAY_OF_MONTH, 1)
    val tomorrowLabel = "Tomorrow, ${dateFormat.format(cal.time)}"

    var selectedDateTab by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Tomorrow
    var selectedTimeSlot by remember { mutableStateOf<String?>(locationViewModel.selectedDeliveryTimeSlot) }

    val todaySlots = remember { buildSlots(forToday = true) }
    val tomorrowSlots = remember { buildSlots(forToday = false) }
    val currentSlots = if (selectedDateTab == 0) todaySlots else tomorrowSlots

    // Sync initial selection from VM when coming back to refine
    val vmDate = locationViewModel.selectedDeliveryDate
    val vmSlot = locationViewModel.selectedDeliveryTimeSlot
    LaunchedEffect(vmDate, vmSlot) {
        if (vmDate == "TOMORROW") selectedDateTab = 1
        else selectedDateTab = 0
        if (vmSlot != null) selectedTimeSlot = vmSlot
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextBlack)
            }
            Text(
                text = "Schedule your order",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
        }

        // Date tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val todayCount = todaySlots.size
            val tomorrowCount = tomorrowSlots.size
            listOf(
                todayLabel to todayCount to 0,
                tomorrowLabel to tomorrowCount to 1
            ).forEach { (labelCount, tabIndex) ->
                val (label, count) = labelCount
                val selected = selectedDateTab == tabIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) PrimaryOrange else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedDateTab = tabIndex }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextBlack
                        )
                        Text(
                            text = "$count Slots",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time slots
        Text(
            text = "Select time slot",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextBlack,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentSlots) { (display, stored) ->
                val isSelected = selectedTimeSlot == stored
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFFFFF8E1) else Color.White
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) PrimaryOrange else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTimeSlot = stored }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBlack,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Confirm button (extra bottom padding so it stays above system nav bar and fully tappable)
        Button(
            onClick = {
                val date = if (selectedDateTab == 0) "TODAY" else "TOMORROW"
                locationViewModel.setDeliveryPreferences("SCHEDULE", date, selectedTimeSlot)
                onConfirm()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedTimeSlot != null
        ) {
            Text("Confirm", fontWeight = FontWeight.SemiBold)
        }
    }
}
