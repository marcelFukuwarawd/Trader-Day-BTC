package com.example.btcbot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.btcbot.service.DailyReportReceiver
import com.example.btcbot.service.TradingService
import com.example.btcbot.ui.BtcTheme
import com.example.btcbot.ui.MainScreen
import com.example.btcbot.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // permissoes
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // inicia trading automatico + agenda relatorio 21h + atualiza widget
        TradingService.start(applicationContext)
        DailyReportReceiver.schedule(applicationContext)
        WidgetUpdater.update(applicationContext)

        setContent { BtcTheme { MainScreen(applicationContext) } }
    }
}
