package com.track.infinitarlockin.data.remote.dto

import com.google.gson.annotations.SerializedName

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

data class AttendanceRecord(
    val id: Int,
    val timestamp: String,
    @SerializedName("is_late")
    val isLate: Boolean,
    @SerializedName("photo_path")
    val photoPath: String
)

data class SubmitResponse(
    val success: Boolean,
    val attendance: AttendanceRecord?,
    val error: String?
)

data class HistoryResponse(
    val history: List<AttendanceRecord>
)
