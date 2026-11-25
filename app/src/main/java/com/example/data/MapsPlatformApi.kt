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

// ---- OpenStreetMap Nominatim Geocoding & POI Search (100% Free, Zero API Key) ----

data class NominatimResult(
    @Json(name = "lat") val lat: String,
    @Json(name = "lon") val lon: String,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "osm_type") val osmType: String? = null,
    @Json(name = "osm_id") val osmId: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "address") val address: Map<String, String>? = null,
    @Json(name = "extratags") val extratags: Map<String, String>? = null
)

interface NominatimApi {
    @GET("search")
    suspend fun searchArea(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Header("User-Agent") userAgent: String = USER_AGENT
    ): List<NominatimResult>

    @GET("search")
    suspend fun searchPois(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("viewbox") viewbox: String,
        @Query("bounded") bounded: Int = 1,
        @Query("limit") limit: Int = 40,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("extratags") extratags: Int = 1,
        @Header("User-Agent") userAgent: String = USER_AGENT
    ): List<NominatimResult>

    companion object {
        const val USER_AGENT = "MasjidMapAndroid/1.0 (+https://openstreetmap.org)"
    }
}

// ---- Photon (Komoot) Free OSM POI & Geocoding Fallback (100% Free, Zero API Key) ----

data class PhotonGeometry(
    val coordinates: List<Double> = emptyList()
)

data class PhotonProperties(
    @Json(name = "osm_id") val osmId: Long? = null,
    @Json(name = "osm_type") val osmType: String? = null,
    @Json(name = "osm_key") val osmKey: String? = null,
    @Json(name = "osm_value") val osmValue: String? = null,
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val district: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null
)

data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties? = null
)

data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList()
)

interface PhotonApi {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("limit") limit: Int = 25
    ): PhotonResponse
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
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(22, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val req = if (original.header("User-Agent").isNullOrBlank()) {
                    original.newBuilder()
                        .header("User-Agent", NominatimApi.USER_AGENT)
                        .build()
                } else {
                    original
                }
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    private fun createOverpassApi(baseUrl: String): OverpassApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(OverpassApi::class.java)

    val overpassPrimaryApi: OverpassApi by lazy {
        createOverpassApi("https://overpass-api.de/")
    }

    val overpassMirrorApi: OverpassApi by lazy {
        createOverpassApi("https://overpass.kumi.systems/")
    }

    val overpassPrivateCoffeeApi: OverpassApi by lazy {
        createOverpassApi("https://overpass.private.coffee/")
    }

    val overpassMailRuApi: OverpassApi by lazy {
        createOverpassApi("https://maps.mail.ru/osm/tools/overpass/")
    }

    val overpassMirrors: List<OverpassApi> by lazy {
        listOf(
            overpassPrimaryApi,
            overpassMirrorApi,
            overpassPrivateCoffeeApi,
            overpassMailRuApi
        )
    }

    val nominatimApi: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(NominatimApi::class.java)
    }

    val photonApi: PhotonApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(PhotonApi::class.java)
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
