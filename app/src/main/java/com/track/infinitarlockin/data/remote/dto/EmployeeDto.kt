package com.track.infinitarlockin.data.remote.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Employee(
    val id: Int,
    val name: String,
    @SerializedName("device_id")
    val deviceId: String,
    // This new field will be one of: "NONE", "CLOCKED_IN", "COMPLETED"
    val attendanceStatus: String?
) : Parcelable

data class RegisterRequest(
    val name: String,
    val deviceId: String
)

data class RegisterResponse(
    val success: Boolean,
    val employee: Employee?,
    val error: String?
)

data class CheckEmployeeResponse(
    val registered: Boolean,
    val employee: Employee?,
    val error: String?
)
