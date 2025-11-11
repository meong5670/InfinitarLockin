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
import androidx.compose.ui.unit.dp
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
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
    val date = try {
        // The incoming format from PostgreSQL/node-postgres is ISO 8601, e.g., "2024-05-23T09:30:00.123Z"
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        parser.parse(record.timestamp)
    } catch (e: Exception) {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (date != null) {
                Column {
                    Text("Date: ${SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date)}")
                    Text("Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)}")
                }
                Text(
                    text = if (record.isLate) "Late" else "On Time",
                    color = if (record.isLate) Color.Red else Color.Green
                )
            } else {
                Text("Invalid date format: ${record.timestamp}")
            }
        }
    }
}
