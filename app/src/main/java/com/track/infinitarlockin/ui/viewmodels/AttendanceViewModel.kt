package com.track.infinitarlockin.ui.viewmodels

import android.content.Context
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.track.infinitarlockin.data.remote.RetrofitClient
import com.track.infinitarlockin.data.remote.dto.ClockOutRequest
import com.track.infinitarlockin.data.remote.dto.ErrorResponse
import com.track.infinitarlockin.data.remote.dto.VerifyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String, val isVerification: Boolean = false) : UiState()
    data class Error(val message: String) : UiState()
}

class AttendanceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun verifyAttendance(context: Context, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val ssid = wifiInfo.ssid.removeSurrounding("\"")
                val bssid = wifiInfo.bssid
                if (bssid == null) {
                    _uiState.value = UiState.Error("WiFi not connected.")
                    return@launch
                }
                val request = VerifyRequest(ssid, bssid, latitude, longitude)
                val response = RetrofitClient.instance.verifyConditions(request)
                if (response.verified) {
                    _uiState.value = UiState.Success("Verified!", isVerification = true)
                } else {
                    _uiState.value = UiState.Error(response.error ?: "Verification failed.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(formatErrorMessage(e))
            }
        }
    }

    fun submitAttendance(employeeId: Int, deviceId: String, photo: File) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val employeeIdBody = employeeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val deviceIdBody = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                    val photoRequestBody = photo.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val photoPart = MultipartBody.Part.createFormData("photo", photo.name, photoRequestBody)
                    val response = RetrofitClient.instance.submitAttendance(employeeIdBody, deviceIdBody, photoPart)
                    if (response.success) {
                        _uiState.value = UiState.Success("Attendance submitted successfully!")
                    } else {
                        _uiState.value = UiState.Error(response.error ?: "Submission failed.")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(formatErrorMessage(e))
            }
        }
    }

    fun submitClockOut(employeeId: Int, deviceId: String, context: Context, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val ssid = wifiInfo.ssid.removeSurrounding("\"")
                val bssid = wifiInfo.bssid
                if (bssid == null) {
                    _uiState.value = UiState.Error("WiFi not connected.")
                    return@launch
                }
                val verifyRequest = VerifyRequest(ssid, bssid, latitude, longitude)
                val verifyResponse = RetrofitClient.instance.verifyConditions(verifyRequest)
                if (!verifyResponse.verified) {
                    _uiState.value = UiState.Error(verifyResponse.error ?: "Verification failed.")
                    return@launch
                }
                val request = ClockOutRequest(employeeId, deviceId)
                val response = RetrofitClient.instance.clockOut(request)
                if (response.success) {
                    _uiState.value = UiState.Success("Clocked out successfully!")
                } else {
                    _uiState.value = UiState.Error(response.error ?: "Clock-out failed.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(formatErrorMessage(e))
            }
        }
    }

    private fun formatErrorMessage(e: Exception): String {
        return when (e) {
            is HttpException -> {
                val errorBody = e.response()?.errorBody()?.string()
                try {
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.error ?: "An unknown server error occurred."
                } catch (jsonE: Exception) {
                    "An error occurred parsing the server response."
                }
            }
            else -> "An unexpected network error occurred."
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
