package com.example.btcbot.domain

import com.example.btcbot.data.Candle
import com.example.btcbot.data.Prefs
import com.example.btcbot.data.TradeRecord
import kotlin.math.abs

class Engine(private val prefs: Prefs) {

    companion object {
        private const val FEE = 0.001        // 0,1% por lado (taker Binance)
        private const val SLIPPAGE = 0.0005  // 0,05%
        private const val MAX_TRADES_DAY = 3
        private const val MAX_DAILY_LOSS = 5.0   // para o dia se perder $5
    }

    private val balance: Double get() = prefs.balance
    private val trades get() = prefs.tradesToday

    val hasOpen: Boolean get() = prefs.openSide.isNotEmpty()

    fun dailyPnl(): Double = trades.sumOf { it.pnl }

    fun canTrade(): Boolean =
        trades.size < MAX_TRADES_DAY && dailyPnl() > -MAX_DAILY_LOSS && !hasOpen

    fun open(sig: Signal) {
        if (!canTrade() || sig.side == Side.NONE) return
        val riskUsd = balance * 0.01                 // 1% do capital = $1 com $100
        val stopDist = abs(sig.entry - sig.stop)
        if (stopDist <= 0) return
        val qty = riskUsd / stopDist                  // SEM alavancagem (spot 1x)
        val entry = if (sig.side == Side.LONG) sig.entry * (1 + SLIPPAGE)
                    else sig.entry * (1 - SLIPPAGE)
        prefs.openSide = sig.side.name
        prefs.openEntry = entry
        prefs.openStop = sig.stop
        prefs.openTp = sig.tp
        prefs.openQty = qty
        prefs.lastSignal = "${sig.label} @ ${"%.2f".format(entry)}"
    }

    /** Verifica posição com candle FECHADO. Retorna TradeRecord se fechou. */
    fun checkClose(c: Candle): TradeRecord? {
        if (!hasOpen) return null
        val side = Side.valueOf(prefs.openSide)
        val entry = prefs.openEntry; val stop = prefs.openStop
        val tp = prefs.openTp; val qty = prefs.openQty

        var exitPrice = 0.0; var reason = ""
        when (side) {
            Side.LONG -> {
                if (c.low <= stop)        { exitPrice = stop * (1 - SLIPPAGE); reason = "STOP" }
                else if (c.high >= tp)    { exitPrice = tp * (1 + SLIPPAGE);   reason = "TP" }
            }
            Side.SHORT -> {
                if (c.high >= stop)       { exitPrice = stop * (1 + SLIPPAGE); reason = "STOP" }
                else if (c.low <= tp)     { exitPrice = tp * (1 - SLIPPAGE);   reason = "TP" }
            }
            Side.NONE -> {}
        }
        if (exitPrice == 0.0) return null

        val gross = if (side == Side.LONG) (exitPrice - entry) * qty else (entry - exitPrice) * qty
        val fees = (entry * qty + exitPrice * qty) * FEE
        val net = gross - fees

        prefs.balance = balance + net
        val rec = TradeRecord(c.time, side.name, entry, exitPrice, qty, net, reason)
        val list = trades.toMutableList(); list.add(rec); prefs.tradesToday = list
        prefs.clearOpen()
        return rec
    }
}
