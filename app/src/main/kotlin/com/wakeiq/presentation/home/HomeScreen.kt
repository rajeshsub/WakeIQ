package com.wakeiq.presentation.home

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeiq.R
import com.wakeiq.domain.model.Alarm
import com.wakeiq.presentation.AppStateViewModel
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

private val WarmHueColors = listOf(
    Color.Unspecified,
    Color(0xFFFFF3E0),
    Color(0xFFFFE0B2),
    Color(0xFFFFCDD2),
    Color(0xFFE8EAF6),
)

@Composable
fun HomeScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    appState: AppStateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val warmHueIndex by appState.warmHueIndex.collectAsStateWithLifecycle()
    val cardColor = WarmHueColors.getOrNull(warmHueIndex) ?: Color.Unspecified

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
                cardColor = cardColor,
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

@Composable
private fun AlarmList(
    alarms: List<Alarm>,
    padding: PaddingValues,
    cardColor: Color,
    onToggle: (Alarm, Boolean) -> Unit,
    onTap: (Alarm) -> Unit,
) {
    if (alarms.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.home_no_alarms),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                cardColor = cardColor,
                onToggle = { enabled -> onToggle(alarm, enabled) },
                onClick = { onTap(alarm) },
            )
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, cardColor: Color, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val colors = if (cardColor != Color.Unspecified) {
        CardDefaults.cardColors(containerColor = cardColor)
    } else {
        CardDefaults.cardColors()
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
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
                    text = alarm.timeString,
                    style = MaterialTheme.typography.displayLarge,
                )
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = alarm.scheduleDescription(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
