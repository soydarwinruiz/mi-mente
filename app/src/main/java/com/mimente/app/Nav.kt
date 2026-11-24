package com.mimente.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mimente.app.data.Kind

sealed interface Screen {
    data object Home : Screen
    data class Detail(val id: String) : Screen
    data class Edit(val id: String?, val kind: Kind, val preset: String = "") : Screen
    data object Ask : Screen
    data object Settings : Screen
}

/** Pila de pantallas sencilla. */
object Nav {
    val stack = mutableStateListOf<Screen>(Screen.Home)

    /** Orden pendiente para el inicio: "voice" o "photo" (accesos directos). */
    var command by mutableStateOf<String?>(null)

    fun go(s: Screen) { if (stack.last() != s) stack.add(s) }
    fun back(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    fun home() { while (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replace(s: Screen) { stack[stack.lastIndex] = s }
}
