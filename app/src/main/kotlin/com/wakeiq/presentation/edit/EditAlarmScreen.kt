package com.wakeiq.presentation.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeiq.R
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.MotionSensitivity
import com.wakeiq.domain.model.SoundCategory
import com.wakeiq.domain.model.SoundType
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditAlarmScreen(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditAlarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedOrDeleted) {
        if (uiState.savedOrDeleted) {
            if (uiState.isNew) onSaved() else onDeleted()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let { viewModel.setCustomSound(it) } }

    var showSmartWakeDialog by remember { mutableStateOf(false) }
    var showMotionDialog by remember { mutableStateOf(false) }

    if (showSmartWakeDialog) {
        AlertDialog(
            onDismissRequest = { showSmartWakeDialog = false },
            title = { Text(stringResource(R.string.smart_wake_tooltip_title)) },
            text = { Text(stringResource(R.string.smart_wake_tooltip_body)) },
            confirmButton = {
                TextButton(onClick = { showSmartWakeDialog = false }) {
                    Text(stringResource(R.string.tooltip_ok))
                }
            },
        )
    }

    if (showMotionDialog) {
        AlertDialog(
            onDismissRequest = { showMotionDialog = false },
            title = { Text(stringResource(R.string.motion_sensitivity_tooltip_title)) },
            text = { Text(stringResource(R.string.motion_sensitivity_tooltip_body)) },
            confirmButton = {
                TextButton(onClick = { showMotionDialog = false }) {
                    Text(stringResource(R.string.tooltip_ok))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isNew) {
                            stringResource(R.string.edit_alarm_title_new)
                        } else {
                            stringResource(R.string.edit_alarm_title_edit)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            key(uiState.isLoading) {
                val timePickerState = rememberTimePickerState(
                    initialHour = uiState.hour,
                    initialMinute = uiState.minute,
                    is24Hour = uiState.is24Hour,
                )
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.fillMaxWidth(),
                )
                LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                    viewModel.setTime(timePickerState.hour, timePickerState.minute)
                }
            }

            // Repeat / day selection
            Text(text = stringResource(R.string.every_day), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.daysOfWeek.size == 7,
                    onClick = viewModel::toggleAllDays,
                    label = { Text(stringResource(R.string.repeat_daily)) },
                )
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in uiState.daysOfWeek,
                        onClick = { viewModel.toggleDay(day) },
                        label = { Text(day.name.take(3)) },
                    )
                }
            }

            // Label
            OutlinedTextField(
                value = uiState.label,
                onValueChange = viewModel::setLabel,
                label = { Text(stringResource(R.string.label_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            HorizontalDivider()

            // Smart wake checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = uiState.useSmartWake,
                    onCheckedChange = viewModel::setUseSmartWake,
                )
                Text(
                    text = stringResource(R.string.smart_wake_label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { showSmartWakeDialog = true },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Motion sensitivity
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(R.string.motion_sensitivity_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { showMotionDialog = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MotionSensitivity.entries.forEach { s ->
                        FilterChip(
                            selected = s == uiState.motionSensitivity,
                            onClick = { viewModel.setSensitivity(s) },
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

            // Sound picker
            Text(stringResource(R.string.sound_picker_title), style = MaterialTheme.typography.titleMedium)

            SoundCategory.entries.forEach { category ->
                Text(
                    stringResource(category.displayNameRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BundledSound.entries
                        .filter { it.category == category }
                        .forEach { sound ->
                            key(sound) {
                                FilterChip(
                                    selected = uiState.soundConfig.type == SoundType.BUNDLED &&
                                        uiState.soundConfig.bundledSound == sound,
                                    onClick = { viewModel.setSound(sound) },
                                    label = { Text(stringResource(sound.displayNameRes)) },
                                )
                            }
                        }
                }
            }

            TextButton(
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.soundConfig.type == SoundType.CUSTOM) {
                        "✓ ${stringResource(R.string.sound_custom)}"
                    } else {
                        stringResource(R.string.sound_custom)
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
