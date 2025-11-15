package com.track.infinitarlockin.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- SHARED DTOs ---
data class ErrorResponse(
    val error: String?
)

// --- VERIFICATION ---
data class VerifyRequest(
    val wifiSsid: String,
    val wifiBssid: String,
    val latitude: Double,
    val longitude: Double
)

data class VerifyResponse(
    val verified: Boolean,
    val error: String?
)

// --- ATTENDANCE RECORD ---
data class AttendanceRecord(
    val id: Int,
    val timestamp: String,
    @SerializedName("is_late")
    val isLate: Boolean,
    @SerializedName("photo_path")
    val photoPath: String,
    @SerializedName("clock_out_timestamp")
    val clockOutTimestamp: String?
)

// --- CLOCK IN (SUBMIT) ---
data class SubmitResponse(
    val success: Boolean,
    val attendance: AttendanceRecord?,
    val error: String?
)

// --- CLOCK OUT ---
data class ClockOutRequest(
    val employeeId: Int,
    val deviceId: String
)

data class ClockOutResponse(
    val success: Boolean,
    val attendance: AttendanceRecord?,
    val error: String?
)

// --- HISTORY ---
data class HistoryResponse(
    val history: List<AttendanceRecord>?
)
