package com.example.btcbot.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.btcbot.data.Candle
import com.example.btcbot.data.Prefs
import com.example.btcbot.domain.Engine
import com.example.btcbot.domain.Strategy
import com.example.btcbot.service.TradingService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val Green = Color(0xFF0ECB81)
private val Red = Color(0xFFF6465D)
private val Yellow = Color(0xFFF0B90B)
private val Gray = Color(0xFF848E9C)

@Composable
fun MainScreen(ctx: Context) {
    val prefs = remember { Prefs(ctx) }
    val engine = remember { Engine(prefs) }
    var candles by remember { mutableStateOf(loadCandles(prefs)) }
    var running by remember { mutableStateOf(true) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            candles = loadCandles(prefs)
            tick++
        }
    }

    val pnl = engine.dailyPnl()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0E11)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("₿ BTC/USDT — Simulador (sem alavancagem)",
                color = Yellow, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
        }
        item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF1E2329))) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo", color = Gray, fontSize = 13.sp)
                    Text("$${"%.2f".format(prefs.balance)}", color = Color.White,
                        fontSize = 24.sp, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("P&L hoje ($tick)", color = Gray, fontSize = 13.sp)
                    Text("${if (pnl >= 0) "+" else ""}$${"%.2f".format(pnl)}  (${prefs.tradesToday.size} trades)",
                        color = if (pnl >= 0) Green else Red, fontSize = 16.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(if (prefs.openSide.isNotEmpty())
                        "Posição: ${prefs.openSide} @ ${"%.2f".format(prefs.openEntry)} | SL ${"%.2f".format(prefs.openStop)} | TP ${"%.2f".format(prefs.openTp)}"
                     else "Posição: nenhuma",
                    color = if (prefs.openSide.isNotEmpty()) Yellow else Gray, fontSize = 12.sp)
                Text("Último sinal: ${prefs.lastSignal}", color = Gray, fontSize = 12.sp)
            }
        }}
        item {
            OutlinedButton(
                onClick = {
                    running = !running
                    if (running) TradingService.start(ctx) else TradingService.stop(ctx)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (running) Red else Green)
            ) { Text(if (running) "⏸ PARAR TRADING AUTOMÁTICO" else "▶ INICIAR TRADING AUTOMÁTICO") }
        }
        item {
            Text("Gráfico 5m (últimos candles fechados)", color = Gray, fontSize = 13.sp)
            CandleChart(candles, Modifier.fillMaxWidth().height(260.dp))
        }
        item { Text("Trades de hoje", color = Gray, fontSize = 13.sp) }
        items(prefs.tradesToday.reversed()) { t ->
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t.time))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF1E2329))) {
                Row(Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("$time  ${t.side}  ${t.reason}", color = Color.White, fontSize = 13.sp)
                        Text("${"%.2f".format(t.entry)} → ${"%.2f".format(t.exit)}  qty ${"%.5f".format(t.qty)}",
                            color = Gray, fontSize = 11.sp)
                    }
                    Text("${if (t.pnl >= 0) "+" else ""}$${"%.2f".format(t.pnl)}",
                        color = if (t.pnl >= 0) Green else Red, fontSize = 15.sp)
                }
            }
        }
    }
}

private fun loadCandles(prefs: Prefs): List<Candle> {
    return try {
        Gson().fromJson(prefs.candlesJson, object : TypeToken<List<Candle>>() {}.type) ?: emptyList()
    } catch (e: Exception) { emptyList() }
}

@Composable
private fun CandleChart(candles: List<Candle>, modifier: Modifier) {
    val ema9 = remember(candles) { Strategy.ema(candles.map { it.close }, 9) }
    val ema21 = remember(candles) { Strategy.ema(candles.map { it.close }, 21) }
    Canvas(modifier.background(Color(0xFF1E2329), RoundedCornerShape(12.dp))) {
        if (candles.isEmpty()) return@Canvas
        val n = candles.size
        val w = size.width / n
        val min = candles.minOf { it.low }
        val max = candles.maxOf { it.high }
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        fun y(v: Double) = size.height - ((v - min) / range * size.height * 0.9 + size.height * 0.05)

        // grid
        for (i in 1..3) {
            val gy = size.height * i / 4
            drawLine(Gray.copy(alpha = 0.15f), Offset(0f, gy), Offset(size.width, gy), 1f)
        }
        // candles
        candles.forEachIndexed { i, c ->
            val x = i * w + w / 2
            val up = c.close >= c.open
            val col = if (up) Green else Red
            drawLine(col, Offset(x, y(c.high)), Offset(x, y(c.low)), 1.5f)
            val top = y(maxOf(c.open, c.close)); val bot = y(minOf(c.open, c.close))
            drawRect(col, Offset(x - w * 0.3f, top), androidx.compose.ui.geometry.Size(w * 0.6f, (bot - top).coerceAtLeast(1f)))
        }
        // EMAs
        fun line(data: List<Double>, color: Color) {
            val path = androidx.compose.ui.graphics.Path()
            data.forEachIndexed { i, v ->
                val x = i * w + w / 2; val yy = y(v)
                if (i == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
            }
            drawPath(path, color, style = Stroke(2f))
        }
        line(ema9, Yellow); line(ema21, Color(0xFF2EBD85).copy(alpha = 0.7f))
    }
}
