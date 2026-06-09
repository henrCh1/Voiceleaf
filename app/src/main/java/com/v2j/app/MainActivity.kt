package com.v2j.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.v2j.app.ui.HistoryScreen
import com.v2j.app.ui.MainScreen
import com.v2j.app.ui.SettingsScreen
import com.v2j.app.ui.theme.VoiceleafTheme

private enum class Screen { Main, Settings, History }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as V2JApp
        setContent {
            VoiceleafTheme {
                // One shared ViewModel for Main + History.
                val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
                var screen by rememberSaveable { mutableStateOf(Screen.Main) }

                // System back from a sub-screen returns to Main; from Main it exits the app.
                BackHandler(enabled = screen != Screen.Main) { screen = Screen.Main }

                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "screen",
                ) { current ->
                    when (current) {
                        Screen.Settings -> SettingsScreen(
                            settings = app.container.settings,
                            repo = app.container.repository,
                            onBack = { screen = Screen.Main },
                        )
                        Screen.History -> HistoryScreen(
                            vm = vm,
                            onBack = { screen = Screen.Main },
                        )
                        Screen.Main -> MainScreen(
                            vm = vm,
                            onOpenSettings = { screen = Screen.Settings },
                            onOpenHistory = { screen = Screen.History },
                        )
                    }
                }
            }
        }
    }
}
