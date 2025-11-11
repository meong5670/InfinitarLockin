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
    @SerializedName("registered_at")
    val registeredAt: String,
    // This new field will be provided by the server
    val hasClockedInToday: Boolean? = false
) : Parcelable

data class RegisterRequest(
    val name: String,
    val deviceId: String
)

data class RegisterResponse(
    val success: Boolean,
    val employee: Employee
)

data class CheckEmployeeResponse(
    val registered: Boolean,
    val employee: Employee?
)
