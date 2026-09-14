package com.example.data.remote

import com.example.data.model.NominatimLocation
import com.example.data.model.OsrmRouteResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface NominatimApiService {

    @GET("search")
    suspend fun searchLocations(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 8,
        @Query("countrycodes") countryCodes: String = "tr",
        @Query("accept-language") language: String = "tr"
    ): List<NominatimLocation>

    companion object {
        private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"

        fun create(): NominatimApiService {
            val userAgentInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RadarUyariApp/1.0 (Android Auto Radar Navigation)")
                    .build()
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(userAgentInterceptor)
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                .build()

            return Retrofit.Builder()
                .baseUrl(NOMINATIM_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(NominatimApiService::class.java)
        }
    }
}

interface OsrmApiService {

    @GET("route/v1/driving/{coordinates}")
    suspend fun calculateRoute(
        @Path(value = "coordinates", encoded = true) coordinates: String, // lon1,lat1;lon2,lat2
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = true
    ): OsrmRouteResponse

    companion object {
        private const val OSRM_BASE_URL = "https://router.project-osrm.org/"

        fun create(): OsrmApiService {
            val userAgentInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RadarUyariApp/1.0 (OSRM Navigation)")
                    .build()
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(userAgentInterceptor)
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                .build()

            return Retrofit.Builder()
                .baseUrl(OSRM_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OsrmApiService::class.java)
        }
    }
}
