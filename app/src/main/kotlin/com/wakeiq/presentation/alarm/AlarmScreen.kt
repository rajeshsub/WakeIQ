package com.wakeiq.presentation.alarm

import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeiq.R
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val BRIGHTNESS_RAMP_STEPS = 300

// Sunrise gradient: the background climbs from black through ember and deep orange to a warm
// bright yellow as the alarm ramp progresses, mirroring the volume ramp.
private val SunriseStops = listOf(
    0f to Color(0xFF000000),
    0.30f to Color(0xFF2A0E00),
    0.55f to Color(0xFF7A2D00),
    0.78f to Color(0xFFD96A1A),
    1f to Color(0xFFFFCF66),
)

// Text and controls are always white. A fixed dark scrim (see ContentScrimColor) sits behind them
// so contrast against the sunrise background stays >= WCAG AA (4.5:1) at every ramp step, instead
// of crossfading content color to track the background: measured contrast for a crossfade dipped
// as low as ~1.04:1 around progress 0.75, because the gradient's mid-ramp orange tones are close
// in lightness to both white and dark text and no two-color crossfade clears AA through that band.
private val ContentColor = Color.White

// Semi-opaque black backing behind the clock/label/buttons. alpha=0.55 keeps white-on-scrim
// contrast >= ~6.3:1 across the full ramp (worst case, calculated against the lightest sunrise
// stop, 0xFFCF66); alpha=0.45 is the minimum that clears the 4.5:1 AA floor, so this keeps margin.
private val ContentScrimColor = Color.Black.copy(alpha = 0.55f)

private fun sunriseColor(progress: Float): Color {
    val t = progress.coerceIn(0f, 1f)
    for (i in 0 until SunriseStops.size - 1) {
        val (t0, c0) = SunriseStops[i]
        val (t1, c1) = SunriseStops[i + 1]
        if (t in t0..t1) {
            val f = if (t1 == t0) 0f else (t - t0) / (t1 - t0)
            return lerp(c0, c1, f)
        }
    }
    return SunriseStops.last().second
}

@Composable
fun AlarmScreen(
    rampDurationMs: Long,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var brightness by remember { mutableFloatStateOf(0f) }
    var showDismissDialog by remember { mutableStateOf(false) }

    val windowView = LocalView.current
    LaunchedEffect(rampDurationMs) {
        val steps = BRIGHTNESS_RAMP_STEPS
        val stepDelayMs = rampDurationMs / steps
        repeat(steps) { step ->
            brightness = step.toFloat() / steps
            val window = (windowView.context as? android.app.Activity)?.window ?: return@LaunchedEffect
            val params = window.attributes
            params.screenBrightness = brightness
            window.attributes = params
            delay(stepDelayMs)
        }
        brightness = 1f
        val window = (windowView.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val finalParams = window.attributes
        finalParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = finalParams
    }

    val backgroundColor = sunriseColor(brightness)

    if (showDismissDialog) {
        AlertDialog(
            onDismissRequest = { showDismissDialog = false },
            title = { Text(stringResource(R.string.alarm_dismiss_confirm_title)) },
            text = { Text(stringResource(R.string.alarm_dismiss_confirm_body)) },
            confirmButton = {
                Button(onClick = {
                    showDismissDialog = false
                    onDismiss()
                }) {
                    Text(stringResource(R.string.alarm_dismiss_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDismissDialog = false }) {
                    Text(stringResource(R.string.alarm_dismiss_confirm_no))
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            // Scrollable so the clock/buttons never clip off-screen at large system font scale or
            // in landscape on a short screen - the ramp/dismiss controls must stay reachable.
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .background(ContentScrimColor, RoundedCornerShape(28.dp))
                .padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                color = ContentColor.copy(alpha = 0.9f),
            )

            val formatter = if (uiState.is24Hour) {
                DateTimeFormatter.ofPattern("HH:mm")
            } else {
                DateTimeFormatter.ofPattern("h:mm a")
            }
            Text(
                text = LocalTime.now().format(formatter),
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                color = ContentColor,
            )

            if (uiState.label.isNotEmpty()) {
                Text(
                    text = uiState.label,
                    style = MaterialTheme.typography.headlineLarge,
                    color = ContentColor.copy(alpha = 0.85f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.5.dp, ContentColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ContentColor),
                ) {
                    Text(stringResource(R.string.alarm_snooze, uiState.snoozeMinutes))
                }
                Button(
                    onClick = { showDismissDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ContentColor,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(stringResource(R.string.alarm_dismiss))
                }
            }
        }
    }
}
