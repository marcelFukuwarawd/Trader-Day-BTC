package com.example.btcbot.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.btcbot.R
import com.example.btcbot.data.DailyReport
import com.example.btcbot.data.Prefs
import com.example.btcbot.widget.WidgetUpdater
import java.text.SimpleDateFormat
import java.util.*

class DailyReportReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val prefs = Prefs(ctx)
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val trades = prefs.tradesToday
        val pnl = trades.sumOf { it.pnl }
        val report = DailyReport(
            date = date,
            startBalance = prefs.dayStartBalance,
            endBalance = prefs.balance,
            pnl = pnl,
            tradesCount = trades.size,
            trades = trades
        )
        val all = prefs.reports.toMutableList()
        all.add(0, report)
        prefs.reports = all

        // novo dia: zera trades e marca saldo inicial
        prefs.tradesToday = mutableListOf()
        prefs.dayStartBalance = prefs.balance

        // notifica
        val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
        val det = trades.joinToString("\n") {
            "• ${it.side} ${it.reason} → ${if (it.pnl >= 0) "+" else ""}$" + "%.2f".format(it.pnl)
        }.ifEmpty { "Sem trades hoje" }
        val n = NotificationCompat.Builder(ctx, TradingService.CH_REPORTS)
            .setSmallIcon(R.drawable.ic_bot)
            .setContentTitle("📊 Relatório $date")
            .setContentText("P&L: ${if (pnl >= 0) "+" else ""}$" + "%.2f".format(pnl) +
                " • Saldo: $" + "%.2f".format(prefs.balance) + " • Trades: ${trades.size}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Saldo inicial: $" + "%.2f".format(report.startBalance) + "\n" +
                "Saldo final:   $" + "%.2f".format(report.endBalance) + "\n" +
                "P&L do dia:    ${if (pnl >= 0) "+" else ""}$" + "%.2f".format(pnl) + "\n\n$det"))
            .setAutoCancel(true)
            .build()
        nm.notify(9001, n)

        WidgetUpdater.update(ctx)
        schedule(ctx) // reagenda 21h do próximo dia
    }

    companion object {
        fun schedule(ctx: Context) {
            val am = ctx.getSystemService(AlarmManager::class.java)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 21)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            val pi = PendingIntent.getBroadcast(
                ctx, 4001, Intent(ctx, DailyReportReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
}
