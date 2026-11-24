package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---- OpenStreetMap Overpass API (100% Free, Zero API Key) ----

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

data class OverpassElement(
    val type: String? = null,
    val id: Long = 0L,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
)

data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

interface OverpassApi {
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun queryMasjids(
        @Field("data") query: String
    ): OverpassResponse
}

// ---- OpenStreetMap Nominatim Geocoding (100% Free, Zero API Key) ----

data class NominatimResult(
    @Json(name = "lat") val lat: String,
    @Json(name = "lon") val lon: String,
    @Json(name = "display_name") val displayName: String? = null
)

interface NominatimApi {
    @GET("search")
    suspend fun searchArea(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Header("User-Agent") userAgent: String = "MasjidMapAndroid/1.0"
    ): List<NominatimResult>
}

// ---- OSRM Routing API (100% Free, Zero API Key) ----

data class OsrmManeuver(
    val type: String? = null,
    val modifier: String? = null
)

data class OsrmStep(
    val name: String? = null,
    val distance: Double? = null,
    val maneuver: OsrmManeuver? = null
)

data class OsrmLeg(
    val steps: List<OsrmStep> = emptyList()
)

data class OsrmRoute(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: String? = null,
    val legs: List<OsrmLeg> = emptyList()
)

data class OsrmResponse(
    val code: String? = null,
    val routes: List<OsrmRoute> = emptyList()
)

data class OsrmTableResponse(
    val code: String? = null,
    val distances: List<List<Double?>> = emptyList(),
    val durations: List<List<Double?>> = emptyList()
)

interface OsrmApi {
    @GET("route/v1/{profile}/{coordinates}")
    suspend fun getRoute(
        @Path("profile") profile: String,
        @Path("coordinates") coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("steps") steps: Boolean = true
    ): OsrmResponse

    @GET("table/v1/{profile}/{coordinates}")
    suspend fun getTable(
        @Path("profile") profile: String,
        @Path("coordinates") coordinates: String,
        @Query("sources") sources: String = "0",
        @Query("annotations") annotations: String = "distance,duration"
    ): OsrmTableResponse
}

object NetworkClients {
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val moshiConverterFactory: MoshiConverterFactory by lazy {
        MoshiConverterFactory.create(moshi)
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val overpassPrimaryApi: OverpassApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(OverpassApi::class.java)
    }

    val overpassMirrorApi: OverpassApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://overpass.kumi.systems/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(OverpassApi::class.java)
    }

    val nominatimApi: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(NominatimApi::class.java)
    }

    val osrmApi: OsrmApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(OsrmApi::class.java)
    }
}
