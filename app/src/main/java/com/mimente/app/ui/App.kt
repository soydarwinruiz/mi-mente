package com.mimente.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mimente.app.Nav
import com.mimente.app.Screen
import com.mimente.app.ui.ask.AskScreen
import com.mimente.app.ui.common.Toaster
import com.mimente.app.ui.edit.EditorScreen
import com.mimente.app.ui.glass.GlassToastHost
import com.mimente.app.ui.glass.Motion
import com.mimente.app.ui.home.HomeScreen
import com.mimente.app.ui.item.ItemScreen
import com.mimente.app.ui.settings.SettingsScreen
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.MiMenteTheme

private data class Entry(val screen: Screen, val depth: Int)

@Composable
fun AppRoot() {
    MiMenteTheme {
        val palette = LocalPalette.current
        val view = LocalView.current
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.dark
                isAppearanceLightNavigationBars = !palette.dark
            }
        }
        BackHandler(Nav.stack.size > 1) { Nav.back() }
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = Entry(Nav.stack.last(), Nav.stack.size),
                transitionSpec = {
                    if (targetState.depth >= initialState.depth) {
                        (slideInHorizontally(Motion.slide) { it } + fadeIn(Motion.quick())) togetherWith
                            (slideOutHorizontally(Motion.slide) { -it / 4 } + fadeOut(Motion.ease()))
                    } else {
                        // salir es más rápido que llegar, y no rebota
                        (slideInHorizontally(Motion.slide) { -it / 4 } + fadeIn(Motion.quick())) togetherWith
                            (slideOutHorizontally(Motion.slide) { it } + scaleOut(Motion.fold(), 0.96f) + fadeOut(Motion.quick()))
                    }
                },
                contentKey = { it.screen },
                label = "nav",
            ) { entry ->
                when (val s = entry.screen) {
                    Screen.Home -> HomeScreen()
                    is Screen.Detail -> ItemScreen(s.id)
                    is Screen.Edit -> EditorScreen(s.id, s.kind, s.preset)
                    Screen.Ask -> AskScreen()
                    Screen.Settings -> SettingsScreen()
                }
            }
            GlassToastHost { action -> if (action == Toaster.Action.OpenSettings) Nav.go(Screen.Settings) }
        }
    }
}
