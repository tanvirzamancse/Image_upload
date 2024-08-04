package com.tzp.image_upload

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RetrofitApiService {
    // google-login-php/
    @Multipart
    @POST("uploadV.php")
    fun uploadImage(
        @Part image: MultipartBody.Part?
    ): Call<JsonObject>

}