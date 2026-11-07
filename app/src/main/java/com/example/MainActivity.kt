package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppDestination
import com.example.ui.HomeExplorerScreen
import com.example.ui.StudioWorkspaceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                val studioViewModel: StudioViewModel = viewModel()
                val uiState by studioViewModel.uiState.collectAsStateWithLifecycle()

                Crossfade(
                    targetState = uiState.currentDestination,
                    label = "root_destination_transition"
                ) { destination ->
                    when (destination) {
                        AppDestination.HOME_EXPLORER -> {
                            HomeExplorerScreen(viewModel = studioViewModel)
                        }
                        AppDestination.STUDIO_CANVAS -> {
                            StudioWorkspaceScreen(viewModel = studioViewModel)
                        }
                    }
                }
            }
        }
    }
}
