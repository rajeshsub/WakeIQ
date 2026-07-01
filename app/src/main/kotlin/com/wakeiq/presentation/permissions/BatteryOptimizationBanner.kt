package com.wakeiq.presentation.permissions

import android.content.ActivityNotFoundException
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wakeiq.data.util.BatteryOptimizationUtil
import timber.log.Timber

/**
 * Persistent banner shown when WakeIQ is running on a known-aggressive OEM ROM and has not been
 * exempted from battery optimization. Refreshes automatically when the user returns from settings.
 *
 * Visibility conditions (all must be true):
 *   - Device manufacturer is in the known-problematic OEM list
 *   - App is NOT already ignoring battery optimizations
 *   - User has not dismissed the banner in this session
 *
 * Drop this composable near the top of HomeScreen (or PermissionsScreen) content:
 *
 *     BatteryOptimizationBanner(modifier = Modifier.padding(horizontal = 16.dp))
 */
@Composable
fun BatteryOptimizationBanner(modifier: Modifier = Modifier) {
    if (!BatteryOptimizationUtil.isKnownProblematicOem()) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isOptimized by rememberSaveable {
        mutableStateOf(!BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context))
    }
    var dismissedByUser by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOptimized = !BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AnimatedVisibility(
        visible = isOptimized && !dismissedByUser,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${Build.MANUFACTURER.replaceFirstChar { it.uppercaseChar() }} battery manager detected",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = "Aggressive power management on this device may prevent alarms from " +
                            "firing. Exempt WakeIQ to guarantee reliable wake-ups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            val intent = BatteryOptimizationUtil.buildBatterySettingsIntent(context)
                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Timber.e(e, "Vendor battery settings not available, trying standard dialog")
                                try {
                                    context.startActivity(
                                        BatteryOptimizationUtil.buildStandardIgnoreIntent(context),
                                    )
                                } catch (fallback: ActivityNotFoundException) {
                                    Timber.e(fallback, "Standard battery intent also unavailable")
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text("Fix Now")
                    }
                }

                IconButton(onClick = { dismissedByUser = true }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}
