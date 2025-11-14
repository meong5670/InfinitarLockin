package com.track.infinitarlockin.ui.viewmodels

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.track.infinitarlockin.data.remote.RetrofitClient
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
import java.io.File

sealed class VerificationState {
    object Idle : VerificationState()
    object Verifying : VerificationState()
    data class Success(val message: String) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Submitting : SubmissionState()
    data class Success(val message: String) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}

class AttendanceViewModel : ViewModel() {

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState

    fun verifyAttendance(context: Context, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _verificationState.value = VerificationState.Verifying

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo.ssid.removeSurrounding("\"")
            val bssid = wifiInfo.bssid

            if (bssid == null) {
                _verificationState.value = VerificationState.Error("WiFi not connected.")
                return@launch
            }

            try {
                val request = VerifyRequest(
                    wifiSsid = ssid,
                    wifiBssid = bssid,// the bssid shoudl change, but Im not sure if it is
                    latitude = latitude,
                    longitude = longitude
                )
                val response = RetrofitClient.instance.verifyConditions(request)

                if (response.verified) {
                    _verificationState.value = VerificationState.Success("Verification successful!")
                } else {
                    _verificationState.value = VerificationState.Error(response.error ?: "Unknown verification error.")
                }
            } catch (e: Exception) {
                _verificationState.value = VerificationState.Error("Verification failed: ${e.message}")
            }
        }
    }

    fun submitAttendance(employeeId: Int, deviceId: String, photo: File) {
        viewModelScope.launch {
            _submissionState.value = SubmissionState.Submitting
            Log.d("AttendanceViewModel", "Starting submission for employeeId: $employeeId")

            try {
                withContext(Dispatchers.IO) {
                    Log.d("AttendanceViewModel", "Preparing request bodies on IO thread.")
                    val employeeIdBody = employeeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val deviceIdBody = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                    val timestampBody = System.currentTimeMillis().toString().toRequestBody("text/plain".toMediaTypeOrNull())

                    Log.d("AttendanceViewModel", "Photo file exists: ${photo.exists()}, size: ${photo.length()}")
                    val photoRequestBody = photo.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val photoPart = MultipartBody.Part.createFormData("photo", photo.name, photoRequestBody)
                    Log.d("AttendanceViewModel", "Photo part created. Making API call.")

                    val response = RetrofitClient.instance.submitAttendance(
                        employeeId = employeeIdBody,
                        deviceId = deviceIdBody,
                        timestamp = timestampBody,
                        photo = photoPart
                    )
//  thijngy for this shopuld be version with msg me thinks

                    Log.d("AttendanceViewModel", "API call successful. Response: ${response.success}")

                    if (response.success) {
                        _submissionState.value = SubmissionState.Success("Attendance submitted successfully!")
                    } else {
                        _submissionState.value = SubmissionState.Error(response.error ?: "Submission failed. Please try again.")
                    }
                }
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Submission failed with exception", e)
                _submissionState.value = SubmissionState.Error("Submission failed: ${e.message}")
            }
        }
    }

    fun resetVerificationState() {
        _verificationState.value = VerificationState.Idle
    }
}
