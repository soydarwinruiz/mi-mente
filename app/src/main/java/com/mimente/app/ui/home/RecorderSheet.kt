package com.mimente.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mimente.app.data.Intake
import com.mimente.app.data.Item
import com.mimente.app.media.Recorder
import com.mimente.app.media.formatDuration
import com.mimente.app.ui.common.Ico
import com.mimente.app.ui.common.Toaster
import com.mimente.app.ui.glass.Feel
import com.mimente.app.ui.glass.GlassCircle
import com.mimente.app.ui.glass.GlassSheet
import com.mimente.app.ui.glass.rememberFeel
import com.mimente.app.ui.theme.Candy
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.Type
import kotlinx.coroutines.delay

/** Grabadora rápida de ideas: onda en vivo, cancelar o guardar. */
@Composable
fun RecorderSheet(visible: Boolean, onDone: (Item?) -> Unit) {
    val context = LocalContext.current
    val p = LocalPalette.current
    val feel = rememberFeel()
    val recorder = remember { Recorder(context) }
    val levels = remember { mutableStateListOf<Float>() }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        levels.clear(); elapsed = 0
        val ok = runCatching { recorder.start() }.isSuccess
        if (!ok) { Toaster.show("No se pudo usar el micrófono"); onDone(null); return@LaunchedEffect }
        feel(Feel.ToggleOn)
        while (true) {
            delay(70)
            levels += recorder.level().coerceAtLeast(0.08f)
            if (levels.size > 44) levels.removeAt(0)
            elapsed = System.currentTimeMillis() - recorder.startedAt
        }
    }

    fun cancel() { if (visible) { recorder.cancel(); feel(Feel.Fall); onDone(null) } }

    GlassSheet(visible, onDismiss = ::cancel) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Grabando idea", style = Type.Title, color = p.ink)
            Text(formatDuration(elapsed), style = Type.Display, color = Candy.Tomato, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Waveform(
                List(44 - levels.size) { 0.08f } + levels, 0f,
                Modifier.fillMaxWidth().height(90.dp),
                played = Candy.Grape,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassCircle(Ico.Close, "Cancelar", size = 56.dp, onClick = ::cancel)
                GlassCircle(Ico.Stop, "Guardar", size = 76.dp, solid = Candy.Tomato) {
                    val res = recorder.stop()
                    if (res == null) { Toaster.show("La grabación es demasiado corta"); onDone(null) }
                    else onDone(Intake.fromFile(context, res.first, "audio/aac", "Nota de voz.aac", res.second))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("La IA la transcribirá y le pondrá título al guardarla.", style = Type.Caption, color = p.inkSoft)
            Spacer(Modifier.height(8.dp))
        }
    }
}
