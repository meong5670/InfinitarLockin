package com.track.infinitarlockin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.track.infinitarlockin.data.remote.dto.AttendanceRecord
import com.track.infinitarlockin.data.remote.dto.Employee
import com.track.infinitarlockin.ui.viewmodels.HistoryState
import com.track.infinitarlockin.ui.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    navController: NavController,
    employee: Employee,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val historyState by historyViewModel.historyState.collectAsState()

    LaunchedEffect(employee.id) {
        historyViewModel.fetchHistory(employee.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = historyState) {
                is HistoryState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HistoryState.Success -> {
                    if (state.records.isEmpty()) {
                        Text("No attendance records found.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.records) { record ->
                                RecordItem(record)
                            }
                        }
                    }
                }
                is HistoryState.Error -> {
                    Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun RecordItem(record: AttendanceRecord) {
    // Helper function to parse ISO 8601 timestamps
    fun parseDate(timestamp: String?): Date? {
        if (timestamp == null) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            parser.parse(timestamp)
        } catch (e: Exception) {
            null
        }
    }
    
    val clockInDate = parseDate(record.timestamp)
    val clockOutDate = parseDate(record.clockOutTimestamp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (clockInDate != null) {
                // --- DATE HEADER ---
                Text(
                    text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(clockInDate),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // --- CLOCK IN / CLOCK OUT ROW ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clock In Info
                    Column {
                        Text("Clock In:", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(clockInDate),
                            fontSize = 16.sp,
                            color = if (record.isLate) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                    
                    // Clock Out Info
                    Column(horizontalAlignment = Alignment.End) {
                         Text("Clock Out:", fontSize = 14.sp, color = Color.Gray)
                         if (clockOutDate != null) {
                             Text(
                                 text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(clockOutDate),
                                 fontSize = 16.sp
                             )
                         } else {
                             Text(
                                 text = "In Progress",
                                 fontSize = 16.sp,
                                 color = Color.Gray
                             )
                         }
                    }
                }
            } else {
                Text("Invalid date format: ${record.timestamp}")
            }
        }
    }
}
