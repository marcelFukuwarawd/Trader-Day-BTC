package com.example.btcbot.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.btcbot.data.Prefs
import com.example.btcbot.domain.Engine

class DashboardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = Prefs(context)
        val engine = Engine(prefs)
        val pnl = engine.dailyPnl()
        val pos = if (prefs.openSide.isNotEmpty())
            "🟢 ${prefs.openSide} @ ${"%.2f".format(prefs.openEntry)}" else "⚪ SEM POSIÇÃO"
        val pnlColor = if (pnl >= 0) Color(0xFF0ECB81) else Color(0xFFF6465D)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0E11))
                    .cornerRadius(16.dp)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₿ BTC/USDT SIM",
                        style = TextStyle(color = ColorProvider(Color(0xFFF0B90B)),
                            fontWeight = FontWeight.Bold, fontSize = 17.sp))
                    Spacer(GlanceModifier.defaultWeight())
                    Text(pos, style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp))
                }
                Spacer(GlanceModifier.height(8.dp))
                Text("Saldo: $${"%.2f".format(prefs.balance)}",
                    style = TextStyle(color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold, fontSize = 26.sp))
                Spacer(GlanceModifier.height(4.dp))
                Text("Hoje: ${if (pnl >= 0) "+" else ""}$${"%.2f".format(pnl)}  •  ${prefs.tradesToday.size} trades",
                    style = TextStyle(color = ColorProvider(pnlColor),
                        fontWeight = FontWeight.Medium, fontSize = 15.sp))
                Spacer(GlanceModifier.height(6.dp))
                Text(prefs.lastSignal,
                    style = TextStyle(color = ColorProvider(Color(0xFF848E9C)), fontSize = 12.sp))
            }
        }
    }
}

class DashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DashboardWidget()
}

object WidgetUpdater {
    fun update(ctx: Context) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val mgr = GlanceAppWidgetManager(ctx)
            val ids = mgr.getGlanceIds(DashboardWidget::class.java)
            ids.forEach { DashboardWidget().update(ctx, it) }
        }
    }
}
