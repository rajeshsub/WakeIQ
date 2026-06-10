package com.wakeiq

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.wakeiq.data.service.AlarmForegroundService
import dagger.hilt.android.HiltAndroidApp
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.notification
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import timber.log.Timber

@HiltAndroidApp
class WakeIQApp : Application() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        if (!BuildConfig.DEBUG) {
            initAcra {
                buildConfigClass = BuildConfig::class.java
                reportFormat = StringFormat.JSON
                reportContent = listOf(
                    ReportField.APP_VERSION_NAME,
                    ReportField.ANDROID_VERSION,
                    ReportField.PHONE_MODEL,
                    ReportField.STACK_TRACE,
                    ReportField.LOGCAT,
                )
                notification {
                    title = getString(R.string.acra_notification_title)
                    text = getString(R.string.acra_notification_text)
                    sendButtonText = getString(R.string.acra_notification_send)
                    discardButtonText = getString(R.string.acra_notification_dismiss)
                    channelName = getString(R.string.notif_channel_alarm_name)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(AcraTree())
        }
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AlarmForegroundService.CHANNEL_ID,
                getString(R.string.notif_channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notif_channel_alarm_desc)
                setShowBadge(false)
                enableVibration(true)
                setBypassDnd(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private inner class AcraTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= android.util.Log.WARN) {
                ACRA.errorReporter.handleSilentException(t ?: Exception(message))
            }
        }
    }
}
