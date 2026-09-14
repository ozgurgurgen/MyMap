package com.example.data.remote

import com.example.data.model.OverpassResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface OverpassApiService {

    @POST("api/interpreter")
    @FormUrlEncoded
    suspend fun getSpeedCamerasPost(
        @Field("data") query: String
    ): OverpassResponse

    @GET("api/interpreter")
    suspend fun getSpeedCamerasGet(
        @Query("data") query: String
    ): OverpassResponse

    companion object {
        const val PRIMARY_BASE_URL = "https://overpass-api.de/"
        const val MIRROR_BASE_URL = "https://overpass.kumi.systems/"
        const val TURKEY_SPEED_CAMERA_QUERY =
            """[out:json][timeout:90];node["highway"="speed_camera"](35.8,25.6,42.3,44.9);out body;"""

        fun create(baseUrl: String): OverpassApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OverpassApiService::class.java)
        }
    }
}
