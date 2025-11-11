package com.track.infinitarlockin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.track.infinitarlockin.data.remote.RetrofitClient
import com.track.infinitarlockin.data.remote.dto.AttendanceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HistoryState {
    object Loading : HistoryState()
    data class Success(val records: List<AttendanceRecord>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

class HistoryViewModel : ViewModel() {

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState: StateFlow<HistoryState> = _historyState

    fun fetchHistory(employeeId: Int) {
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading
            try {
                val response = RetrofitClient.instance.getAttendanceHistory(employeeId)
                _historyState.value = HistoryState.Success(response.history)
            } catch (e: Exception) {
                _historyState.value = HistoryState.Error("Failed to load history: ${e.message}")
            }
        }
    }
}
