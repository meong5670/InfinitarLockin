package com.track.infinitarlockin.ui.viewmodels

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.track.infinitarlockin.data.remote.RetrofitClient
import com.track.infinitarlockin.data.remote.dto.Employee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val employee: Employee) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class MainViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    fun checkDeviceRegistration(context: Context, isRetry: Boolean = false) {
        // Show loading indicator only on the first load or an explicit retry
        if (_authState.value !is AuthState.Authenticated || isRetry) {
            _authState.value = AuthState.Loading
        }
        
        viewModelScope.launch {
            try {
                val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                val response = RetrofitClient.instance.checkEmployee(deviceId)
                if (response.registered && response.employee != null) {
                    _authState.value = AuthState.Authenticated(response.employee)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network error: ${e.message}")
            }
        }
    }
}
