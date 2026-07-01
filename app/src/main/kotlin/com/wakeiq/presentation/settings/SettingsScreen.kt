package com.wakeiq.presentation.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeiq.R
import com.wakeiq.domain.model.MotionSensitivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBlueLightDialog by remember { mutableStateOf(false) }
    var showSmartWindowInfo by remember { mutableStateOf(false) }
    var showRampInfo by remember { mutableStateOf(false) }

    if (showBlueLightDialog) {
        InfoDialog(
            title = stringResource(R.string.blue_light_tooltip_title),
            body = stringResource(R.string.blue_light_tooltip_body),
            onDismiss = { showBlueLightDialog = false },
        )
    }
    if (showSmartWindowInfo) {
        InfoDialog(
            title = stringResource(R.string.smart_window_info_title),
            body = stringResource(R.string.smart_window_info_body),
            onDismiss = { showSmartWindowInfo = false },
        )
    }
    if (showRampInfo) {
        InfoDialog(
            title = stringResource(R.string.ramp_duration_info_title),
            body = stringResource(R.string.ramp_duration_info_body),
            onDismiss = { showRampInfo = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Smart wake defaults
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingTitleRow(
                    title = stringResource(R.string.smart_window_title),
                    onInfo = { showSmartWindowInfo = true },
                )
                Text(
                    stringResource(R.string.smart_window_desc, uiState.defaultSmartWindowMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = uiState.defaultSmartWindowMinutes.toFloat(),
                    onValueChange = { viewModel.setDefaultSmartWindow(it.toInt()) },
                    valueRange = 0f..30f,
                    steps = 5,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingTitleRow(
                    title = stringResource(R.string.ramp_duration_title),
                    onInfo = { showRampInfo = true },
                )
                Text(
                    stringResource(R.string.ramp_duration_desc, uiState.defaultRampDurationMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = uiState.defaultRampDurationMinutes.toFloat(),
                    onValueChange = { viewModel.setDefaultRampDuration(it.toInt()) },
                    valueRange = 1f..15f,
                    steps = 13,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.motion_sensitivity_title), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MotionSensitivity.entries.forEach { s ->
                        FilterChip(
                            selected = s == uiState.defaultMotionSensitivity,
                            onClick = { viewModel.setDefaultSensitivity(s) },
                            label = {
                                Text(
                                    when (s) {
                                        MotionSensitivity.LOW -> stringResource(R.string.motion_sensitivity_low)
                                        MotionSensitivity.MEDIUM -> stringResource(R.string.motion_sensitivity_medium)
                                        MotionSensitivity.HIGH -> stringResource(R.string.motion_sensitivity_high)
                                    },
                                )
                            },
                        )
                    }
                }
            }

            HorizontalDivider()

            // Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.blue_light_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.blue_light_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showBlueLightDialog = true }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.blueLightReductionEnabled,
                    onCheckedChange = viewModel::setBlueLightReduction,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_24_hour_clock),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = uiState.use24HourClock,
                    onCheckedChange = viewModel::setUse24HourClock,
                )
            }

            HorizontalDivider()

            // About
            Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleMedium)

            Text(
                stringResource(R.string.about_science_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.about_science_body), style = MaterialTheme.typography.bodyMedium)

            Text(
                stringResource(R.string.about_gradual_wake_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.about_gradual_wake_body), style = MaterialTheme.typography.bodyMedium)

            Text(
                stringResource(R.string.about_privacy_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.about_privacy_body), style = MaterialTheme.typography.bodyMedium)

            Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium)

            Text(
                stringResource(R.string.about_mit_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/rajeshsub/WakeIQ".toUri(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.about_github_button))
            }

            OutlinedButton(
                onClick = onOpenPrivacyPolicy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Privacy Policy")
            }
        }
    }
}

@Composable
private fun SettingTitleRow(title: String, onInfo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onInfo, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tooltip_ok))
            }
        },
    )
}
