package com.example.btcbot.domain

import com.example.btcbot.data.Candle
import kotlin.math.abs

enum class Side { LONG, SHORT, NONE }

data class Signal(
    val side: Side,
    val entry: Double,
    val stop: Double,
    val tp: Double,
    val label: String
)

object Strategy {

    fun ema(values: List<Double>, period: Int): List<Double> {
        val k = 2.0 / (period + 1)
        val out = mutableListOf<Double>()
        var e = values.first()
        values.forEach { v -> e = v * k + e * (1 - k); out.add(e) }
        return out
    }

    fun rsi(closes: List<Double>, period: Int = 14): Double {
        if (closes.size < period + 1) return 50.0
        var gains = 0.0; var losses = 0.0
        for (i in closes.size - period until closes.size) {
            val d = closes[i] - closes[i - 1]
            if (d > 0) gains += d else losses -= d
        }
        if (losses == 0.0) return 100.0
        val rs = gains / losses
        return 100 - (100 / (1 + rs))
    }

    fun atr(c: List<Candle>, period: Int = 14): Double {
        if (c.size < period + 1) return c.last().high - c.last().low
        val trs = mutableListOf<Double>()
        for (i in c.size - period until c.size) {
            val cur = c[i]; val prev = c[i - 1]
            trs.add(maxOf(cur.high - cur.low, abs(cur.high - prev.close), abs(cur.low - prev.close)))
        }
        return trs.average()
    }

    /** Analisa APENAS candles fechados (dropLast do atual) */
    fun analyze(closed: List<Candle>): Signal {
        if (closed.size < 30) return Signal(Side.NONE, 0.0, 0.0, 0.0, "dados insuficientes")
        val closes = closed.map { it.close }
        val ema9 = ema(closes, 9).last()
        val ema21 = ema(closes, 21).last()
        val r = rsi(closes)
        val a = atr(closed)
        val last = closed.last()
        val risk = a * 1.5
        val maxStop = last.close * 0.02

        return when {
            ema9 > ema21 && r in 40.0..70.0 && last.close > ema9 && risk < maxStop -> {
                val stop = last.low - (risk - (last.close - last.low)).coerceAtLeast(0.0)
                Signal(Side.LONG, last.close, stop, last.close + risk * 2, "LONG Pullback EMA9")
            }
            ema9 < ema21 && r in 30.0..60.0 && last.close < ema9 && risk < maxStop -> {
                val stop = last.high + (risk - (last.high - last.close)).coerceAtLeast(0.0)
                Signal(Side.SHORT, last.close, stop, last.close - risk * 2, "SHORT Pullback EMA9")
            }
            else -> Signal(Side.NONE, 0.0, 0.0, 0.0, "sem sinal (RSI ${"%.0f".format(r)})")
        }
    }
}
