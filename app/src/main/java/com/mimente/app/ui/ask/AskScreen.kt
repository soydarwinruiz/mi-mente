package com.mimente.app.ui.ask

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimente.app.Nav
import com.mimente.app.Screen
import com.mimente.app.ai.AiRunner
import com.mimente.app.ai.Content
import com.mimente.app.ai.Gemini
import com.mimente.app.data.Intake
import com.mimente.app.data.Item
import com.mimente.app.data.Prefs
import com.mimente.app.data.Store
import com.mimente.app.ui.common.Ico
import com.mimente.app.ui.common.Sharing
import com.mimente.app.ui.common.Toaster
import com.mimente.app.ui.common.markdown
import com.mimente.app.ui.glass.Capsule
import com.mimente.app.ui.glass.GlassCard
import com.mimente.app.ui.glass.GlassChip
import com.mimente.app.ui.glass.GlassCircle
import com.mimente.app.ui.glass.GlassScreen
import com.mimente.app.ui.glass.GlassSheet
import com.mimente.app.ui.glass.GlassTextField
import com.mimente.app.ui.glass.TopChrome
import com.mimente.app.ui.glass.glass
import com.mimente.app.ui.glass.pressable
import com.mimente.app.ui.theme.Candy
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** La conversación vive aquí para no perderse al cambiar de pantalla. */
object AskSession {
    data class Msg(val user: Boolean, val text: String, val attached: List<String> = emptyList(), val error: Boolean = false)

    val messages = mutableStateListOf<Msg>()
    var live by mutableStateOf<String?>(null)
    var job: Job? = null
    /** Número [#n] → id del elemento, según el contexto enviado en la última pregunta. */
    var refs: Map<Int, String> = emptyMap()

    fun clear() { job?.cancel(); messages.clear(); live = null }
}

private const val CONTEXT_LIMIT = 300_000

private val suggestions = listOf(
    "¿Qué tengo pendiente?",
    "Resume lo que guardé esta semana",
    "¿Qué ideas se repiten en mis notas?",
    "Hazme un plan para mañana con mis tareas",
    "¿Qué dije en mis últimas notas de voz?",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskScreen() {
    val context = LocalContext.current
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val all by Store.items.collectAsState()
    var input by remember { mutableStateOf("") }
    val attached = remember { mutableStateListOf<String>() }
    var picking by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val busy = AskSession.live != null

    val fromPhone = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        scope.launch {
            val items = withContext(Dispatchers.IO) { uris.mapNotNull { Intake.importUri(context, it) } }
            items.forEach { attached += it.id; AiRunner.autoProcess(it) }
            picking = false
        }
    }

    LaunchedEffect(AskSession.messages.size, AskSession.live?.length) {
        val n = AskSession.messages.size + (if (AskSession.live != null) 1 else 0)
        if (n > 0) list.animateScrollToItem(n)
    }

    fun send(textIn: String = input) {
        val text = textIn.trim()
        if (text.isEmpty() || busy) return
        val key = AiRunner.requireKey() ?: return
        val attachIds = attached.toList()
        val history = AskSession.messages.filterNot { it.error }.toList()
        AskSession.messages += AskSession.Msg(true, text, attachIds.mapNotNull { Store.get(it)?.displayTitle })
        input = ""
        attached.clear()
        AskSession.live = ""
        AskSession.job = scope.launch {
            try {
                // Contexto: todo lo guardado, de lo más reciente a lo más antiguo, numerado.
                val sorted = Store.items.value.sortedWith(compareByDescending<Item> { it.pinned }.thenByDescending { it.created })
                val refs = mutableMapOf<Int, String>()
                val memory = StringBuilder()
                for ((i, it) in sorted.withIndex()) {
                    val d = Content.describe(it, i + 1)
                    if (memory.length + d.length > CONTEXT_LIMIT) break
                    memory.append(d); refs[i + 1] = it.id
                }
                AskSession.refs = refs
                val system = AiRunner.system(
                    "Hoy es ${java.text.SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy, HH:mm", java.util.Locale("es")).format(java.util.Date())}. " +
                        "Abajo está TODO lo que el usuario ha guardado en su mente, numerado. Úsalo para responder; cuando uses un elemento, " +
                        "cítalo como [#n]. Si la respuesta no está en su información, dilo y responde con tu conocimiento general.\n\n" +
                        "=== MI MENTE (${refs.size} elementos) ===\n$memory",
                )
                val turns = history.map { Gemini.Turn(if (it.user) "user" else "model", listOf(Gemini.Part.Text(it.text))) }.toMutableList()
                val parts = mutableListOf<Gemini.Part>()
                for (id in attachIds) {
                    val item = Store.get(id) ?: continue
                    val n = refs.entries.firstOrNull { it.value == id }?.key
                    parts += Gemini.Part.Text("Adjunto${n?.let { " [#$it]" } ?: ""}:")
                    parts += Content.partsFor(item, key)
                }
                parts += Gemini.Part.Text(text)
                turns += Gemini.Turn("user", parts)
                val tools = buildSet {
                    if (Prefs.webSearch) add("google_search")
                    if (attachIds.any { Store.get(it)?.let(Content::needsUrlContext) == true }) add("url_context")
                }
                var last = ""
                com.mimente.app.ai.Models.withWorkingModel(key, AiRunner::announceSwitch) { model ->
                    Gemini.stream(key, model, turns, system, tools).collect { last = it; AskSession.live = it }
                }
                AskSession.messages += AskSession.Msg(false, last.trim())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AskSession.messages += AskSession.Msg(false, e.message ?: "Algo salió mal", error = true)
            } finally {
                AskSession.live = null
            }
        }
    }

    GlassScreen(
        content = {
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 80.dp, bottom = 170.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "intro") {
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Text("Pregúntale\na tu mente", style = Type.Headline, color = p.heroInk)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Gemini lee tus ${all.size} recuerdos para responderte. Adjunta fotos, audios o documentos para que los analice a fondo.",
                            style = Type.Label, color = p.heroInk.copy(alpha = 0.85f),
                        )
                        if (AskSession.messages.isEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                suggestions.forEach { s -> GlassChip(s, false, icon = Ico.Spark, accent = Candy.Grape) { send(s) } }
                            }
                        }
                    }
                }
                itemsIndexed(AskSession.messages) { _, m -> Bubble(m) }
                if (AskSession.live != null) item(key = "live") {
                    Bubble(AskSession.Msg(false, AskSession.live!!.ifEmpty { "Pensando…" }), streaming = true)
                }
            }
        },
        chrome = {
            TopChrome(onBack = { Nav.back() }, center = { m ->
                Text("Mi Mente · ${Prefs.model}", style = Type.Caption, color = p.heroInk.copy(alpha = 0.85f), modifier = m, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }) {
                GlassCircle(Ico.Refresh, "Nueva conversación") { AskSession.clear() }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    item {
                        GlassChip("Web", Prefs.webSearch, icon = Ico.Web, accent = Candy.Sky) { Prefs.setWebSearch(!Prefs.webSearch) }
                    }
                    items(attached.toList()) { id ->
                        val it = Store.get(id)
                        if (it != null) GlassChip(it.displayTitle.take(22), false, icon = Ico.of(it.kind), accent = Candy.Grape) { attached.remove(id) }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCircle(Ico.Attach, "Adjuntar", size = 52.dp) { picking = true }
                    GlassTextField(
                        input, { input = it }, "Pregunta lo que quieras…",
                        modifier = Modifier.weight(1f), singleLine = false, maxLines = 5, shape = RoundedCornerShape(26.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    if (busy) GlassCircle(Ico.Stop, "Detener", size = 52.dp, solid = Candy.Tomato) { AskSession.job?.cancel() }
                    else GlassCircle(Ico.Send, "Enviar", size = 52.dp, solid = Candy.Sky) { send() }
                }
            }
            GlassSheet(picking, onDismiss = { picking = false }) {
                Text("Adjuntar", style = Type.Title, color = p.ink)
                Spacer(Modifier.height(10.dp))
                com.mimente.app.ui.home.OptionRow(Ico.Attach, "Desde el teléfono (se guarda en tu mente)", Candy.Sky) { fromPhone.launch(arrayOf("*/*")) }
                Spacer(Modifier.height(6.dp))
                Text("DE TU MENTE", style = Type.Eyebrow, color = p.inkSoft)
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(all.take(60), key = { it.id }) { it ->
                        val on = it.id in attached
                        Row(
                            Modifier.fillMaxWidth().pressable(0.97f) { if (on) attached.remove(it.id) else attached += it.id }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Candy.Sky else p.ink.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(if (on) Ico.Check else Ico.of(it.kind), null, tint = if (on) Color.White else p.inkSoft, modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Text(it.displayTitle, style = Type.BodyStrong, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                com.mimente.app.ui.glass.GlassPill("Listo", tint = Candy.Sky, modifier = Modifier.fillMaxWidth()) { picking = false }
            }
        },
    )
}

@Composable
private fun Bubble(m: AskSession.Msg, streaming: Boolean = false) {
    val context = LocalContext.current
    val p = LocalPalette.current
    if (m.user) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Box(
                Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp))
                    .background(Candy.Sky).padding(horizontal = 16.dp, vertical = 12.dp),
            ) { Text(m.text, style = Type.Body, color = Color.White) }
            if (m.attached.isNotEmpty()) Text(
                "📎 " + m.attached.joinToString(", "), style = Type.Caption, color = p.heroInk.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp, end = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp),
            tint = if (m.error) Candy.Tomato.copy(alpha = 0.25f) else null,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Ico.Spark, null, tint = if (streaming) Candy.Grape else Candy.Sky, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (m.error) "No se pudo responder" else "Mi Mente", style = Type.Caption, color = p.inkSoft)
            }
            Spacer(Modifier.height(6.dp))
            SelectionContainer {
                Text(
                    markdown(m.text, Candy.Sky) { n -> AskSession.refs[n]?.let { id -> if (Store.get(id) != null) Nav.go(Screen.Detail(id)) } },
                    style = Type.Body.copy(fontSize = 15.sp, lineHeight = 23.sp), color = p.ink,
                )
            }
            if (!streaming && !m.error) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniPill(Ico.Copy, "Copiar") { Sharing.copy(context, m.text) }
                    MiniPill(Ico.of(com.mimente.app.data.Kind.NOTE), "Guardar nota") {
                        Intake.note(m.text, "Respuesta de Mi Mente"); Toaster.show("Guardado como nota")
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.pressable(0.9f, onClick = onClick).clip(Capsule).background(p.ink.copy(alpha = 0.07f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = p.inkSoft, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = Type.Caption, color = p.inkSoft)
    }
}
