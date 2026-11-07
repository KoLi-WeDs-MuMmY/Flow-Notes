package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
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
            val studioViewModel: StudioViewModel = viewModel()
            val uiState by studioViewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeId = uiState.customization.themeId,
                cornerRadiusDp = uiState.customization.cornerRadiusDp.dp
            ) {
                AnimatedContent(
                    targetState = uiState.currentDestination,
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.96f))
                            .togetherWith(
                                fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                                    scaleOut(targetScale = 0.98f)
                            )
                    },
                    label = "root_destination_spring_transition"
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
