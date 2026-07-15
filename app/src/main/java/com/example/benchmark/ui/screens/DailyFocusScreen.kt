package com.example.benchmark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benchmark.TaskViewModel
import com.example.benchmark.sortedByStartTime
import com.example.benchmark.ui.components.TimelineTaskItem
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.DarkAccent
import com.example.benchmark.ui.theme.PrimaryText
import com.example.benchmark.ui.theme.SecondaryText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** "Important" tab: every starred task, today and beyond, grouped by day. */
@Composable
fun DailyFocusScreen(viewModel: TaskViewModel = viewModel()) {
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val context = LocalContext.current

    val dbFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayString = remember { dbFmt.format(Calendar.getInstance().time) }
    val tomorrowString = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        dbFmt.format(cal.time)
    }

    // Starred tasks from today onward, grouped by day.
    // "yyyy-MM-dd" strings sort chronologically, so a sorted map keeps day order.
    val groupedByDay = allTasks
        .filter { it.isImportant && it.day >= todayString }
        .groupBy { it.day }
        .toSortedMap()

    fun dayLabel(day: String): String = when (day) {
        todayString -> "Today"
        tomorrowString -> "Tomorrow"
        else -> try {
            SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(dbFmt.parse(day)!!)
        } catch (e: Exception) {
            day
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Important",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Your ⭐ starred tasks — today and upcoming days.",
            fontSize = 14.sp,
            color = SecondaryText,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (groupedByDay.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nothing important yet", color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap the ☆ star on any task in Home,\nor say \"mark gym as important\".",
                        color = SecondaryText, fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedByDay.forEach { (day, dayTasks) ->
                    item(key = "header_$day") {
                        Text(
                            text = dayLabel(day),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkAccent,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(dayTasks.sortedByStartTime(), key = { it.id }) { task ->
                        TimelineTaskItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleComplete(context, it) },
                            onToggleImportant = { viewModel.toggleImportant(it) },
                            onDelete = { viewModel.deleteTask(context, it) }
                        )
                    }
                }
            }
        }
    }
}
