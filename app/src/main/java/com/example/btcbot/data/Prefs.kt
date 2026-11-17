package com.example.btcbot.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class TradeRecord(
    val time: Long,
    val side: String,
    val entry: Double,
    val exit: Double,
    val qty: Double,
    val pnl: Double,
    val reason: String
)

data class DailyReport(
    val date: String,
    val startBalance: Double,
    val endBalance: Double,
    val pnl: Double,
    val tradesCount: Int,
    val trades: List<TradeRecord>
)

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("btcbot", Context.MODE_PRIVATE)
    private val gson = Gson()

    var balance: Double
        get() = p.getString("balance", "100.0")!!.toDouble()
        set(v) = p.edit().putString("balance", v.toString()).apply()

    var dayStartBalance: Double
        get() = p.getString("dayStart", "100.0")!!.toDouble()
        set(v) = p.edit().putString("dayStart", v.toString()).apply()

    var tradesToday: MutableList<TradeRecord>
        get() {
            val json = p.getString("tradesToday", "[]")!!
            return gson.fromJson(json, object : TypeToken<MutableList<TradeRecord>>() {}.type)
        }
        set(v) = p.edit().putString("tradesToday", gson.toJson(v)).apply()

    var reports: MutableList<DailyReport>
        get() {
            val json = p.getString("reports", "[]")!!
            return gson.fromJson(json, object : TypeToken<MutableList<DailyReport>>() {}.type)
        }
        set(v) = p.edit().putString("reports", gson.toJson(v)).apply()

    var openSide: String
        get() = p.getString("openSide", "")!!
        set(v) = p.edit().putString("openSide", v).apply()

    var openEntry: Double
        get() = p.getString("openEntry", "0")!!.toDouble()
        set(v) = p.edit().putString("openEntry", v.toString()).apply()

    var openStop: Double
        get() = p.getString("openStop", "0")!!.toDouble()
        set(v) = p.edit().putString("openStop", v.toString()).apply()

    var openTp: Double
        get() = p.getString("openTp", "0")!!.toDouble()
        set(v) = p.edit().putString("openTp", v.toString()).apply()

    var openQty: Double
        get() = p.getString("openQty", "0")!!.toDouble()
        set(v) = p.edit().putString("openQty", v.toString()).apply()

    var lastSignal: String
        get() = p.getString("lastSignal", "aguardando...")!!
        set(v) = p.edit().putString("lastSignal", v).apply()

    var candlesJson: String
        get() = p.getString("candles", "[]")!!
        set(v) = p.edit().putString("candles", v).apply()

    fun clearOpen() {
        openSide = ""; openEntry = 0.0; openStop = 0.0; openTp = 0.0; openQty = 0.0
    }
}
