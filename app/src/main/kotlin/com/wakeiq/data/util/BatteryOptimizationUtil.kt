package com.wakeiq.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import timber.log.Timber

/**
 * Detects OEM manufacturers whose custom battery managers can silently kill scheduled alarms
 * and provides the most targeted settings intent available on the current device.
 *
 * Priority order per OEM:
 *   1. Vendor-specific autostart / background manager screen (deepest, most actionable)
 *   2. Vendor-specific battery settings screen
 *   3. AOSP ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS (universal fallback)
 *
 * resolveActivity() is called on each candidate so we never hand the caller an intent that
 * would throw ActivityNotFoundException on devices where a component was renamed or removed.
 */
object BatteryOptimizationUtil {

    private val KNOWN_PROBLEMATIC_OEMS = setOf(
        "samsung", "xiaomi", "redmi", "poco",
        "oneplus", "oppo", "realme", "vivo",
        "huawei", "honor",
    )

    fun isIgnoringBatteryOptimizations(context: Context): Boolean = context.getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(context.packageName)

    fun isKnownProblematicOem(): Boolean = Build.MANUFACTURER.lowercase() in KNOWN_PROBLEMATIC_OEMS

    /**
     * Returns the best available intent to open battery/autostart settings for WakeIQ.
     * Always safe to call - falls back to the standard system dialog if OEM screens are absent.
     */
    @Suppress("DEPRECATION")
    fun buildBatterySettingsIntent(context: Context): Intent {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val pm = context.packageManager
        val resolved = oemCandidates(manufacturer).firstOrNull { it.resolveActivity(pm) != null }
        return if (resolved != null) {
            Timber.d("BatteryOptimizationUtil: resolved vendor intent for $manufacturer")
            resolved
        } else {
            Timber.d("BatteryOptimizationUtil: falling back to AOSP battery exemption dialog")
            buildStandardIgnoreIntent(context)
        }
    }

    /** The standard AOSP dialog that requests the battery optimization exemption directly. */
    fun buildStandardIgnoreIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    @Suppress("LongMethod")
    private fun oemCandidates(manufacturer: String): List<Intent> = when {
        manufacturer == "xiaomi" || manufacturer == "redmi" || manufacturer == "poco" ->
            listOf(
                vendorIntent(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HideAppsContainerManagementActivity",
                ),
                vendorIntent(
                    "com.miui.securitycenter",
                    "com.miui.securitycenter.PowerSettings",
                ),
            )
        manufacturer == "huawei" || manufacturer == "honor" ->
            listOf(
                vendorIntent(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity",
                ),
                vendorIntent(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
                ),
            )
        manufacturer == "oneplus" ->
            listOf(
                vendorIntent(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
                ),
            )
        manufacturer == "oppo" ->
            listOf(
                vendorIntent(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                ),
                vendorIntent(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity",
                ),
            )
        manufacturer == "realme" ->
            listOf(
                vendorIntent(
                    "com.realme.safecenter",
                    "com.realme.safecenter.permission.startup.StartupAppListActivity",
                ),
                vendorIntent(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                ),
            )
        manufacturer == "vivo" ->
            listOf(
                vendorIntent(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                ),
                vendorIntent(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
                ),
            )
        manufacturer == "samsung" ->
            listOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        else -> emptyList()
    }

    private fun vendorIntent(pkg: String, cls: String): Intent = Intent().apply {
        setClassName(pkg, cls)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
