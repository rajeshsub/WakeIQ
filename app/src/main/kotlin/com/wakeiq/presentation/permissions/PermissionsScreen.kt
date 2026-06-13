package com.wakeiq.presentation.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeiq.R
import com.wakeiq.data.service.AlarmForegroundService

@Composable
fun PermissionsScreen(onDone: () -> Unit, viewModel: PermissionsViewModel = hiltViewModel()) {
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            ) {
                if (viewModel.anyCriticalMissing) {
                    Text(
                        text = stringResource(R.string.perm_setup_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (viewModel.anyCriticalMissing) {
                            stringResource(R.string.perm_setup_continue_anyway)
                        } else {
                            stringResource(R.string.perm_setup_continue)
                        },
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.perm_setup_heading),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.perm_setup_subheading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(permissions, key = { it.type.name }) { perm ->
                PermissionRow(
                    permission = perm,
                    onGrant = {
                        when (perm.type) {
                            PermissionType.NOTIFICATIONS ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            PermissionType.EXACT_ALARM ->
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
                                )
                            PermissionType.FULL_SCREEN_INTENT ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                            Uri.parse("package:${context.packageName}"),
                                        ),
                                    )
                                }
                            // Battery optimisation exemption is requestable here. OEM autostart
                            // managers (Xiaomi, Oppo, Vivo, Realme) cannot be toggled in code and
                            // must be documented for users in the README.
                            PermissionType.BATTERY_OPTIMIZATION ->
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            PermissionType.DND_OVERRIDE ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            putExtra(Settings.EXTRA_CHANNEL_ID, AlarmForegroundService.CHANNEL_ID)
                                        },
                                    )
                                }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: AppPermission, onGrant: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (permission.isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = when {
                    permission.isGranted -> MaterialTheme.colorScheme.primary
                    permission.isCritical -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = permission.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    if (permission.isCritical && !permission.isGranted) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = stringResource(R.string.perm_setup_required),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = permission.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (!permission.isGranted) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onGrant) {
                        Text(stringResource(R.string.perm_setup_grant))
                    }
                }
            }
        }
    }
}
