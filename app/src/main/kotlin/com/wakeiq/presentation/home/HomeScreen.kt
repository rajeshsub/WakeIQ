package com.wakeiq.presentation.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeiq.R
import com.wakeiq.domain.model.Alarm
import com.wakeiq.presentation.AppStateViewModel
import com.wakeiq.presentation.paletteForIndex
import com.wakeiq.presentation.permissions.BatteryOptimizationBanner
import com.wakeiq.presentation.permissions.PermissionsViewModel
import java.time.DayOfWeek

private const val DAYS_IN_WEEK = 7

private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

@Composable
fun HomeScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    appState: AppStateViewModel = hiltViewModel(),
    permissionsViewModel: PermissionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val is24Hour by appState.use24HourClock.collectAsStateWithLifecycle()
    val permissions by permissionsViewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionsViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.openExactAlarmSettings.collect { showExactAlarmDialog = true }
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text(stringResource(R.string.perm_rationale_exact_alarm_title)) },
            text = { Text(stringResource(R.string.perm_rationale_exact_alarm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }) { Text(stringResource(R.string.perm_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text(stringResource(R.string.perm_skip))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            HomeTopBar(onOpenSettings = onOpenSettings)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        },
    ) { padding ->
        when (val state = uiState) {
            HomeUiState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is HomeUiState.Error -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text(state.message) }

            is HomeUiState.Success -> AlarmList(
                alarms = state.alarms,
                padding = padding,
                is24Hour = is24Hour,
                showPermissionWarning = permissionsViewModel.anyCriticalMissing,
                onGrantPermissions = onOpenPermissions,
                onToggle = { alarm, enabled -> viewModel.toggle(alarm, enabled) },
                onTap = { alarm -> onEditAlarm(alarm.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onOpenSettings: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.home_title)) },
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        },
    )
}

@Suppress("LongParameterList")
@Composable
private fun AlarmList(
    alarms: List<Alarm>,
    padding: PaddingValues,
    is24Hour: Boolean,
    showPermissionWarning: Boolean,
    onGrantPermissions: () -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onTap: (Alarm) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "oem_battery_warning") {
            BatteryOptimizationBanner()
        }
        if (showPermissionWarning) {
            item(key = "perm_warning") {
                PermissionWarningBanner(onClick = onGrantPermissions)
            }
        }
        if (alarms.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(bottom = if (showPermissionWarning) 80.dp else 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.home_no_alarms),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    is24Hour = is24Hour,
                    onToggle = { enabled -> onToggle(alarm, enabled) },
                    onClick = { onTap(alarm) },
                )
            }
        }
    }
}

@Composable
private fun PermissionWarningBanner(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.perm_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = stringResource(R.string.perm_banner_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            TextButton(onClick = onClick) {
                Text(
                    text = stringResource(R.string.perm_banner_fix),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, is24Hour: Boolean, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val palette = paletteForIndex(alarm.colorIndex)
    val cardColors = if (palette.isCustom) {
        CardDefaults.cardColors(containerColor = palette.background)
    } else {
        CardDefaults.cardColors()
    }
    val primaryText = if (palette.isCustom) palette.onBackground else MaterialTheme.colorScheme.onSurface
    val secondaryText = if (palette.isCustom) {
        palette.onBackground.copy(
            alpha = 0.7f,
        )
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = alarm.formattedTime(is24Hour),
                    style = MaterialTheme.typography.displayLarge,
                    color = primaryText,
                )
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = secondaryText,
                    )
                }
                Text(
                    text = alarm.scheduleDescription(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryText,
                )
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

private fun Alarm.scheduleDescription(): String = when {
    daysOfWeek.size == DAYS_IN_WEEK -> "Every day"
    daysOfWeek.isEmpty() -> "Once"
    daysOfWeek == WEEKDAYS -> "Weekdays"
    daysOfWeek == WEEKEND -> "Weekends"
    else -> daysOfWeek.sortedBy { it.value }.joinToString(", ") { it.name.take(3) }
}
