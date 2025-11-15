package com.track.infinitarlockin.data.remote

import com.track.infinitarlockin.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST("api/employees/register")
    suspend fun registerEmployee(@Body request: RegisterRequest): RegisterResponse

    @GET("api/employees/check/{deviceId}")
    suspend fun checkEmployee(@Path("deviceId") deviceId: String): CheckEmployeeResponse

    @POST("api/attendance/verify")
    suspend fun verifyConditions(@Body request: VerifyRequest): VerifyResponse

    @Multipart
    @POST("api/attendance/submit")
    suspend fun submitAttendance(
        @Part("employeeId") employeeId: RequestBody,
        @Part("deviceId") deviceId: RequestBody,
        @Part photo: MultipartBody.Part
    ): SubmitResponse

    @POST("api/attendance/clock-out")
    suspend fun clockOut(@Body request: ClockOutRequest): ClockOutResponse

    @GET("api/attendance/history/{employeeId}")
    suspend fun getAttendanceHistory(
        @Path("employeeId") employeeId: Int,
        @Query("limit") limit: Int = 30
    ): HistoryResponse
}
