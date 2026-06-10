package com.wakeiq.presentation.alarm

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun AlarmScreen(
    rampDurationMs: Long,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var brightness by remember { mutableFloatStateOf(0f) }

    // Ramp screen brightness
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
        val window = (windowView.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val finalParams = window.attributes
        finalParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = finalParams
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            // Current time
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            Text(
                text = currentTime,
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
            )

            if (uiState.label.isNotEmpty()) {
                Text(
                    text = uiState.label,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.alarm_snooze, uiState.snoozeMinutes))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.alarm_dismiss))
                }
            }
        }
    }
}
