package com.example.btcbot.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.btcbot.R
import com.example.btcbot.data.BinanceApi
import com.example.btcbot.data.Prefs
import com.example.btcbot.data.toCandles
import com.example.btcbot.domain.Engine
import com.example.btcbot.domain.Strategy
import com.example.btcbot.widget.WidgetUpdater
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class TradingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefs: Prefs
    private lateinit var engine: Engine
    private val api = BinanceApi.create()

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(applicationContext)
        engine = Engine(prefs)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotif("Trader ativo • Saldo $%s".format("%.2f".format(prefs.balance))))
        scope.launch {
            while (isActive) {
                try { tick() } catch (e: Exception) { /* rede caiu, tenta de novo */ }
                delay(30_000) // verifica a cada 30s
            }
        }
        return START_STICKY
    }

    private suspend fun tick() {
        val raw = api.klines(limit = 120)
        val all = raw.toCandles()
        val closed = all.dropLast(1) // ignora candle em formação
        if (closed.size < 30) return

        // salva candles p/ grafico do app
        val show = closed.takeLast(80)
        prefs.candlesJson = Gson().toJson(show)

        // 1) se tem posição aberta, verifica stop/tp no candle fechado
        val closedTrade = engine.checkClose(closed.last())
        if (closedTrade != null) {
            notifyTrade(closedTrade)
            prefs.lastSignal = "Fechou ${closedTrade.side} ${closedTrade.reason} " +
                "${if (closedTrade.pnl >= 0) "+" else ""}$" + "%.2f".format(closedTrade.pnl)
        }

        // 2) se não tem posição, procura sinal
        if (!engine.hasOpen) {
            val sig = Strategy.analyze(closed)
            if (sig.side != com.example.btcbot.domain.Side.NONE) engine.open(sig)
            else prefs.lastSignal = sig.label
        }

        WidgetUpdater.update(applicationContext)
        startForeground(1, buildNotif(
            "Saldo $%s • Hoje %s$%s".format(
                "%.2f".format(prefs.balance),
                if (engine.dailyPnl() >= 0) "+" else "",
                "%.2f".format(engine.dailyPnl())
            )
        ))
    }

    private fun notifyTrade(t: com.example.btcbot.data.TradeRecord) {
        val nm = getSystemService(NotificationManager::class.java)
        val n = NotificationCompat.Builder(this, CH_TRADES)
            .setSmallIcon(R.drawable.ic_bot)
            .setContentTitle("Trade fechado: ${t.side} ${t.reason}")
            .setContentText("P&L: ${if (t.pnl >= 0) "+" else ""}$" + "%.2f".format(t.pnl) +
                " • Saldo: $" + "%.2f".format(prefs.balance))
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), n)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_MAIN, "Trader BTC", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(CH_TRADES, getString(R.string.channel_trades), NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel(CH_REPORTS, getString(R.string.channel_reports), NotificationManager.IMPORTANCE_HIGH))
        }
    }

    private fun buildNotif(text: String): Notification =
        NotificationCompat.Builder(this, CH_MAIN)
            .setSmallIcon(R.drawable.ic_bot)
            .setContentTitle("BTC/USDT • Simulação $100")
            .setContentText(text)
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        const val CH_MAIN = "main"
        const val CH_TRADES = "trades"
        const val CH_REPORTS = "reports"

        fun start(ctx: Context) {
            val i = Intent(ctx, TradingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, TradingService::class.java)) }
    }
}
