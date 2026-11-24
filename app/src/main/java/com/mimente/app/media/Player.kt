package com.mimente.app.media

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.io.File

@Stable
class PlayerState(private val file: File?) {
    var playing by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var duration by mutableStateOf(0)
        private set
    private var mp: MediaPlayer? = null

    private fun ensure(): MediaPlayer? {
        if (mp != null) return mp
        val f = file ?: return null
        return runCatching {
            MediaPlayer().apply {
                setDataSource(f.absolutePath)
                prepare()
                setOnCompletionListener { this@PlayerState.playing = false; this@PlayerState.progress = 0f }
            }
        }.getOrNull()?.also { mp = it; duration = it.duration }
    }

    fun toggle() {
        val p = ensure() ?: return
        if (p.isPlaying) { p.pause(); playing = false } else { p.start(); playing = true }
    }

    fun seek(fraction: Float) {
        val p = ensure() ?: return
        p.seekTo((fraction * p.duration).toInt())
        progress = fraction
    }

    fun tick() {
        val p = mp ?: return
        if (p.isPlaying && p.duration > 0) progress = p.currentPosition.toFloat() / p.duration
    }

    fun position(): Int = mp?.currentPosition ?: 0

    fun release() { mp?.release(); mp = null; playing = false }
}

@Composable
fun rememberPlayer(file: File?): PlayerState {
    val state = remember(file) { PlayerState(file) }
    DisposableEffect(state) { onDispose { state.release() } }
    LaunchedEffect(state, state.playing) {
        while (state.playing) { state.tick(); delay(50) }
    }
    return state
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
