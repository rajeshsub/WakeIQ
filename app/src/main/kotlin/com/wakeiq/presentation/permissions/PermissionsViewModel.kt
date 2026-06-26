package com.wakeiq.presentation.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.wakeiq.R
import com.wakeiq.core.InstrumentedOnly
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class PermissionType {
    NOTIFICATIONS,
    EXACT_ALARM,
    FULL_SCREEN_INTENT,
    BATTERY_OPTIMIZATION,
    DND_OVERRIDE,
}

data class AppPermission(
    val type: PermissionType,
    val title: String,
    val rationale: String,
    val isCritical: Boolean,
    val isGranted: Boolean,
)

@HiltViewModel
@InstrumentedOnly
class PermissionsViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {

    private val _permissions = MutableStateFlow(buildPermissionList())
    val permissions: StateFlow<List<AppPermission>> = _permissions.asStateFlow()

    val anyCriticalMissing: Boolean
        get() = _permissions.value.any { it.isCritical && !it.isGranted }

    fun refresh() {
        _permissions.value = buildPermissionList()
    }

    @Suppress("LongMethod")
    private fun buildPermissionList(): List<AppPermission> {
        val nm = context.getSystemService(NotificationManager::class.java)
        val pm = context.getSystemService(PowerManager::class.java)
        val am = context.getSystemService(AlarmManager::class.java)

        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    AppPermission(
                        type = PermissionType.NOTIFICATIONS,
                        title = context.getString(R.string.perm_setup_notif_title),
                        rationale = context.getString(R.string.perm_setup_notif_body),
                        isCritical = true,
                        isGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED,
                    ),
                )
            }

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2
            ) {
                add(
                    AppPermission(
                        type = PermissionType.EXACT_ALARM,
                        title = context.getString(R.string.perm_setup_exact_alarm_title),
                        rationale = context.getString(R.string.perm_setup_exact_alarm_body),
                        isCritical = true,
                        isGranted = am.canScheduleExactAlarms(),
                    ),
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(
                    AppPermission(
                        type = PermissionType.FULL_SCREEN_INTENT,
                        title = context.getString(R.string.perm_setup_fsi_title),
                        rationale = context.getString(R.string.perm_setup_fsi_body),
                        isCritical = true,
                        isGranted = nm.canUseFullScreenIntent(),
                    ),
                )
            }

            add(
                AppPermission(
                    type = PermissionType.BATTERY_OPTIMIZATION,
                    title = context.getString(R.string.perm_setup_battery_title),
                    rationale = context.getString(R.string.perm_setup_battery_body),
                    isCritical = true,
                    isGranted = pm.isIgnoringBatteryOptimizations(context.packageName),
                ),
            )

            add(
                AppPermission(
                    type = PermissionType.DND_OVERRIDE,
                    title = context.getString(R.string.perm_setup_dnd_title),
                    rationale = context.getString(R.string.perm_setup_dnd_body),
                    isCritical = true,
                    isGranted = nm.isNotificationPolicyAccessGranted(),
                ),
            )
        }
    }
}

@InstrumentedOnly
fun areCriticalPermissionsGranted(context: Context): Boolean {
    val nm = context.getSystemService(NotificationManager::class.java)
    val pm = context.getSystemService(PowerManager::class.java)
    val am = context.getSystemService(AlarmManager::class.java)

    val notifOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    val onApi31Or32 = Build.VERSION.SDK_INT == Build.VERSION_CODES.S ||
        Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2
    val exactAlarmOk = !onApi31Or32 || am.canScheduleExactAlarms()

    val fsiOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        nm.canUseFullScreenIntent()

    return notifOk && exactAlarmOk && fsiOk && pm.isIgnoringBatteryOptimizations(context.packageName)
}
