package com.wakeiq.presentation.alarm

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wakeiq.data.service.AlarmForegroundService
import com.wakeiq.presentation.theme.WakeIQTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        }

        val rampDurationMs = intent.getLongExtra(EXTRA_RAMP_DURATION_MS, DEFAULT_RAMP_MS)

        setContent {
            WakeIQTheme {
                AlarmScreen(
                    rampDurationMs = rampDurationMs,
                    onDismiss = {
                        AlarmForegroundService.dismiss(this)
                        finish()
                    },
                    onSnooze = {
                        AlarmForegroundService.snooze(this)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_RAMP_DURATION_MS = "extra_ramp_duration_ms"
        private const val DEFAULT_RAMP_MS = 5L * 60_000L
    }
}
