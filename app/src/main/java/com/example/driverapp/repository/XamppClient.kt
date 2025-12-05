package com.example.driverapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object XamppClient {
    // IMPORTANT: Use your PC's IP address if testing on a real device.
    // 10.0.2.2 works for the Android Emulator.
    private const val BASE_URL = "http://192.168.100.107/delivsync_api/"

    val instance: XamppApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(XamppApiService::class.java)
    }
}