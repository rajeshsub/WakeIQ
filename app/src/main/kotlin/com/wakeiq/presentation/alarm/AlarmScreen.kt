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

// Text and controls start white on the near-black opening and shift to a dark warm tone as the
// background brightens, so they always contrast against the sunrise.
private val DarkContent = Color(0xFF2A1A00)
private const val CONTENT_FLIP_START = 0.45f

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

private fun contentColorFor(progress: Float): Color {
    val f = ((progress - CONTENT_FLIP_START) / (1f - CONTENT_FLIP_START)).coerceIn(0f, 1f)
    return lerp(Color.White, DarkContent, f)
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
    val contentColor = contentColorFor(brightness)

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
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                color = contentColor.copy(alpha = 0.9f),
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
                color = contentColor,
            )

            if (uiState.label.isNotEmpty()) {
                Text(
                    text = uiState.label,
                    style = MaterialTheme.typography.headlineLarge,
                    color = contentColor.copy(alpha = 0.85f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.5.dp, contentColor.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
                ) {
                    Text(stringResource(R.string.alarm_snooze, uiState.snoozeMinutes))
                }
                Button(
                    onClick = { showDismissDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        contentColor = backgroundColor,
                    ),
                ) {
                    Text(stringResource(R.string.alarm_dismiss))
                }
            }
        }
    }
}
