package com.mimente.app.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Mensajes cortos en una burbuja de vidrio. */
object Toaster {
    enum class Action { None, OpenSettings }
    data class Toast(val text: String, val action: Action, val id: Long)

    var current by mutableStateOf<Toast?>(null)
        private set
    private var counter by mutableLongStateOf(0L)

    fun show(text: String, action: Action = Action.None) {
        counter++
        current = Toast(text, action, counter)
    }

    fun dismiss(id: Long) { if (current?.id == id) current = null }
}
