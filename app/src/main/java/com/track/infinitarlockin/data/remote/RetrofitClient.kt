package com.track.infinitarlockin.data.remote

import com.track.infinitarlockin.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // The BASE_URL is now securely loaded from your local.properties file at build time.
    private const val BASE_URL = BuildConfig.API_BASE_URL

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            var response = chain.proceed(request)
            var tryCount = 0
            val maxLimit = 3 // max retry count

            // Retry on 503 Service Unavailable
            while (!response.isSuccessful && response.code == 503 && tryCount < maxLimit) {
                tryCount++
                response.close() // Close the previous response body
                
                // Wait for a couple of seconds before retrying
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }

                response = chain.proceed(request)
            }
            response
        }
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
