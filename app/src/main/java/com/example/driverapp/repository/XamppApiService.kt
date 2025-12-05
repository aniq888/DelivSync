package com.example.driverapp.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface XamppApiService {
    @Multipart
    @POST("upload_image.php")
    fun uploadImage(@Part image: MultipartBody.Part): Call<ResponseBody>
}