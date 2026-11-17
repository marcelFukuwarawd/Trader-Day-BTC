package com.example.btcbot.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface BinanceApi {

    @GET("api/v3/klines")
    suspend fun klines(
        @Query("symbol") symbol: String = "BTCUSDT",
        @Query("interval") interval: String = "5m",
        @Query("limit") limit: Int = 120
    ): List<List<Any>>

    @GET("api/v3/ticker/price")
    suspend fun price(@Query("symbol") symbol: String = "BTCUSDT"): Map<String, String>

    companion object {
        fun create(): BinanceApi {
            val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder()
                .addInterceptor(log)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl("https://api.binance.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BinanceApi::class.java)
        }
    }
}

/** Candle já convertido para Double */
data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

fun List<List<Any>>.toCandles(): List<Candle> = map {
    Candle(
        time = (it[0] as Number).toLong(),
        open = (it[1] as String).toDouble(),
        high = (it[2] as String).toDouble(),
        low  = (it[3] as String).toDouble(),
        close= (it[4] as String).toDouble()
    )
}
