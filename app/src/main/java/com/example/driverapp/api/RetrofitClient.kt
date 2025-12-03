package com.example.driverapp.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client singleton
 */
object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    /**
     * Get Retrofit instance
     */
    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            // Create logging interceptor
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Create OkHttp client with logging
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            // Create Gson instance
            val gson = GsonBuilder()
                .setLenient()
                .create()

            // Build Retrofit instance
            retrofit = Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!
    }

    /**
     * Get API Service instance
     */
    fun getApiService(): ApiService {
        if (apiService == null) {
            apiService = getRetrofit().create(ApiService::class.java)
        }
        return apiService!!
    }

    /**
     * Update base URL (useful for switching between environments)
     */
    fun updateBaseUrl(newBaseUrl: String) {
        retrofit = null
        apiService = null
        ApiConfig::class.java.getDeclaredField("BASE_URL").apply {
            isAccessible = true
            set(null, newBaseUrl)
        }
    }
}

