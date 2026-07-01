package com.wakeiq.presentation.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PolicyHeader() }
            item { HorizontalDivider() }
            item { ShortVersion() }
            item { HorizontalDivider() }
            item { LongVersion() }
            item { HorizontalDivider() }
            item { WhatTheAppUses() }
            item { HorizontalDivider() }
            item { PermissionsSection() }
            item { HorizontalDivider() }
            item { ContactSection() }
            item { HorizontalDivider() }
            item { ClosingSection() }
        }
    }
}

@Composable
private fun PolicyHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("WakeIQ Privacy Policy", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Last updated: July 1, 2026",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShortVersion() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("The short version", style = MaterialTheme.typography.titleMedium)
        Text(
            "WakeIQ knows nothing about you. Not your name. Not your location. Not your habits. " +
                "Not even which sounds you prefer to wake up to. That data lives on your phone " +
                "and never leaves it.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LongVersion() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("The long version (same answer, more words)", style = MaterialTheme.typography.titleMedium)

        PolicyPoint(
            title = "We collect zero personal data.",
            body = "No account. No sign-up. No email address. Nothing.",
        )
        PolicyPoint(
            title = "We store zero data remotely.",
            body = "Your alarms, your settings, your custom audio choices - they live in your " +
                "phone's local storage. There is no WakeIQ server. There is no WakeIQ database " +
                "in the cloud. There is no WakeIQ cloud.",
        )
        PolicyPoint(
            title = "We transmit zero data.",
            body = "WakeIQ has no internet permission because it doesn't need one. " +
                "It does not phone home, ever.",
        )
        PolicyPoint(
            title = "We share zero data.",
            body = "There is nothing to share. See above.",
        )
        PolicyPoint(
            title = "We sell zero data.",
            body = "See above.",
        )
        PolicyPoint(
            title = "We use zero analytics, trackers, or ad SDKs.",
            body = "No Firebase. No Mixpanel. No Facebook SDK. No Adjust. No hidden anything.",
        )
    }
}

@Composable
private fun PolicyPoint(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WhatTheAppUses() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("What the app does use", style = MaterialTheme.typography.titleMedium)

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                TableRow(
                    feature = "Permission / Feature",
                    why = "Why",
                    stays = "Stays on device?",
                    isHeader = true,
                )
                HorizontalDivider()
                TableRow("Alarm schedules you set", "To wake you up", "Yes")
                HorizontalDivider()
                TableRow("App settings (volume, ramp duration, etc.)", "To remember your preferences", "Yes")
                HorizontalDivider()
                TableRow("Accelerometer", "Smart Wake motion detection only", "Yes")
                HorizontalDivider()
                TableRow("Audio files you choose", "To play your alarm sound", "Yes")
                HorizontalDivider()
                TableRow("Notification permission", "To show the alarm notification", "Yes")
            }
        }

        Text(
            "None of the above is ever read, copied, or transmitted by us or anyone else.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TableRow(feature: String, why: String, stays: String, isHeader: Boolean = false) {
    val style = if (isHeader) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.bodySmall
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(feature, style = style, modifier = Modifier.weight(2f))
        Text(why, style = style, modifier = Modifier.weight(2f))
        Text(stays, style = style, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PermissionsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Permissions, explained plainly", style = MaterialTheme.typography.titleMedium)
        Text(
            "Every permission this app requests is listed in the app store and in the app itself. " +
                "Each one has a single purpose: making your alarm work reliably. No permission is " +
                "used for data collection.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ContactSection() {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Contact", style = MaterialTheme.typography.titleMedium)
        Text(
            "Questions? The source code is public. Read it.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "If you still want to reach a human, open an issue on GitHub:",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "https://github.com/rajeshsub/WakeIQ/issues".toUri()),
                )
            },
            modifier = Modifier.padding(start = 0.dp),
        ) {
            Text("github.com/rajeshsub/WakeIQ/issues")
        }
    }
}

@Composable
private fun ClosingSection() {
    Text(
        "That's it. No legalese, no carve-outs, no \"we may share with trusted partners.\" " +
            "We trust nobody that purchases the personal data of users for targeted advertising. " +
            "There is no sharing. There is no data. The cake was a lie, but this policy isn't.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 32.dp),
    )
}
