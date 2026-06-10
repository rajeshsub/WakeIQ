package com.wakeiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeiq.presentation.AppStateViewModel
import com.wakeiq.presentation.navigation.AlarmNavGraph
import com.wakeiq.presentation.theme.WakeIQTheme
import dagger.hilt.android.AndroidEntryPoint

private val BlueLightOverlayColor = Color(0x26FF8F00)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appState: AppStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakeIQTheme {
                val isBlueLightActive by appState.isBlueLightActive.collectAsStateWithLifecycle()
                Box(modifier = Modifier.fillMaxSize()) {
                    AlarmNavGraph()
                    if (isBlueLightActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BlueLightOverlayColor),
                        )
                    }
                }
            }
        }
    }
}
