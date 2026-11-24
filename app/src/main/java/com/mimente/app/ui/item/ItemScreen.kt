package com.mimente.app.ui.item

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mimente.app.Nav
import com.mimente.app.Screen
import com.mimente.app.ai.Actions
import com.mimente.app.ai.AiRunner
import com.mimente.app.data.AiNote
import com.mimente.app.data.Check
import com.mimente.app.data.Intake
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Store
import com.mimente.app.data.domainOf
import com.mimente.app.media.formatDuration
import com.mimente.app.media.rememberPlayer
import com.mimente.app.ui.common.Ico
import com.mimente.app.ui.common.Sharing
import com.mimente.app.ui.common.Toaster
import com.mimente.app.ui.common.fileSize
import com.mimente.app.ui.common.friendlyDate
import com.mimente.app.ui.common.markdown
import com.mimente.app.ui.glass.Capsule
import com.mimente.app.ui.glass.Feel
import com.mimente.app.ui.glass.GlassCard
import com.mimente.app.ui.glass.GlassChip
import com.mimente.app.ui.glass.GlassCircle
import com.mimente.app.ui.glass.GlassDialog
import com.mimente.app.ui.glass.GlassPill
import com.mimente.app.ui.glass.GlassScreen
import com.mimente.app.ui.glass.GlassSheet
import com.mimente.app.ui.glass.GlassTextField
import com.mimente.app.ui.glass.TopChrome
import com.mimente.app.ui.glass.pressable
import com.mimente.app.ui.glass.rememberFeel
import com.mimente.app.ui.home.CheckPill
import com.mimente.app.ui.home.ItemOptions
import com.mimente.app.ui.home.Waveform
import com.mimente.app.ui.home.cardPaint
import com.mimente.app.ui.theme.Candy
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.Tone
import com.mimente.app.ui.theme.Type

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemScreen(id: String) {
    val all by Store.items.collectAsState()
    val item = all.firstOrNull { it.id == id }
    if (item == null) {
        // Se borró: volver
        androidx.compose.runtime.LaunchedEffect(Unit) { Nav.back() }
        return
    }
    val context = LocalContext.current
    val p = LocalPalette.current
    val feel = rememberFeel()
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val live = AiRunner.live[item.id]
    val processing = item.id in AiRunner.processing
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    fun ask() {
        val q = question.trim()
        if (q.isEmpty()) return
        AiRunner.run(item, Actions.custom, q)
        question = ""
    }

    GlassScreen(
        content = {
            LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = top + 76.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "title") {
                    Column {
                        Text(
                            "${item.kind.label.uppercase()} · ${friendlyDate(item.created).uppercase()}",
                            style = Type.Eyebrow, color = p.heroInk.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            item.displayTitle, style = Type.Display, color = p.heroInk,
                            modifier = Modifier.pressable(0.98f, null) { Nav.go(Screen.Edit(item.id, item.kind)) },
                        )
                    }
                }
                item(key = "hero") { Hero(item) }
                if (item.text.isNotBlank() && item.kind != Kind.NOTE) {
                    item(key = "note") {
                        GlassCard {
                            Text("TU NOTA", style = Type.Eyebrow, color = p.inkSoft)
                            Spacer(Modifier.height(6.dp))
                            SelectionContainer { Text(item.text, style = Type.Body, color = p.ink) }
                        }
                    }
                }
                if (item.gist.isNotBlank() || processing) {
                    item(key = "gist") {
                        GlassCard(tint = Candy.Grape.copy(alpha = 0.22f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Ico.Spark, null, tint = Candy.Grape, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("EN POCAS PALABRAS", style = Type.Eyebrow, color = Candy.Grape)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(if (processing) "Analizando con Gemini…" else item.gist, style = Type.BodyStrong, color = p.ink)
                        }
                    }
                }
                item(key = "actions") {
                    Column {
                        Text("PROCESAR CON IA", style = Type.Eyebrow, color = p.heroInk.copy(alpha = 0.85f), modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Actions.forKind(item.kind).forEach { a ->
                                GlassChip(a.label, false, icon = a.icon, accent = a.color) { AiRunner.run(item, a) }
                            }
                        }
                    }
                }
                if (live != null) {
                    item(key = "live") {
                        ResultCard(
                            AiNote(id = "live", action = live.action, text = live.text.ifEmpty { "Pensando…" }),
                            streaming = true,
                            onCancel = { AiRunner.cancel(item.id) },
                        )
                    }
                }
                items(item.ai, key = { it.id }) { note ->
                    ResultCard(
                        note,
                        onDelete = { Store.update(item.id, touch = false) { it.copy(ai = it.ai.filterNot { n -> n.id == note.id }) }; feel(Feel.Fall) },
                        onSaveNote = {
                            Intake.note(note.text, "${note.action}: ${item.displayTitle}".take(70))
                            Toaster.show("Guardado como nota")
                        },
                        onMakeList = Actions.parseTasks(note.text).takeIf { it.size >= 2 }?.let { tasks ->
                            {
                                Store.add(Item(kind = Kind.TODO, title = "Tareas: ${item.displayTitle}".take(50), checks = tasks.map { Check(it) }, tone = 2))
                                feel(Feel.Rise)
                                Toaster.show("Lista creada con ${tasks.size} tareas")
                            }
                        },
                    )
                }
            }
        },
        chrome = {
            TopChrome(onBack = { Nav.back() }) {
                GlassCircle(Ico.Pin, "Fijar", tint = if (item.pinned) Candy.Tangerine else null) {
                    Store.update(item.id, touch = false) { it.copy(pinned = !it.pinned) }
                }
                GlassCircle(Ico.Share, "Compartir") { Sharing.share(context, item) }
                GlassCircle(Ico.More, "Más") { options = true }
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassTextField(
                    question, { question = it }, "Pregunta algo sobre esto…",
                    leading = Ico.Spark, modifier = Modifier.weight(1f), shape = Capsule,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { ask() }),
                )
                GlassCircle(Ico.Send, "Enviar", size = 54.dp, solid = Candy.Sky) { ask() }
            }
            GlassSheet(options, onDismiss = { options = false }) {
                ItemOptions(item, onDone = { options = false }, onDelete = { options = false; deleting = true })
            }
            GlassDialog(
                deleting, "¿Eliminar?", "«${item.displayTitle}» se borrará de tu mente.", "Eliminar", destructive = true,
                onConfirm = { deleting = false; Store.delete(item.id); feel(Feel.Fall) },
                onDismiss = { deleting = false },
            )
        },
    )
}

@Composable
private fun Hero(item: Item) {
    val context = LocalContext.current
    val p = LocalPalette.current
    val tone = p.tone(item.tone)
    when (item.kind) {
        Kind.NOTE -> Column(Modifier.fillMaxWidth().cardPaint(tone, p.dark).padding(22.dp)) {
            SelectionContainer { Text(item.text.ifBlank { "Nota vacía" }, style = Type.Body.copy(fontSize = 17.sp, lineHeight = 27.sp), color = tone.ink) }
            Spacer(Modifier.height(14.dp))
            GlassPill("Editar", icon = Ico.Edit, onClick = { Nav.go(Screen.Edit(item.id, item.kind)) })
        }
        Kind.TODO -> TodoHero(item, if (item.tone == 0) p.tone(2) else tone)
        Kind.PHOTO -> AsyncImage(
            Store.fileOf(item), item.displayTitle, contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).cardPaint(tone, p.dark).pressable(0.98f) { Sharing.open(context, item) },
        )
        Kind.VIDEO -> Box(Modifier.fillMaxWidth().cardPaint(tone, p.dark).pressable(0.98f) { Sharing.open(context, item) }) {
            AsyncImage(Store.fileOf(item), item.displayTitle, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp))
            Box(Modifier.align(Alignment.Center).size(70.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                Icon(Ico.Play, "Reproducir", tint = Color.White, modifier = Modifier.size(42.dp))
            }
            if (item.durationMs > 0) Text(
                formatDuration(item.durationMs), style = Type.Caption, color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
            )
        }
        Kind.AUDIO -> AudioHero(item, p.tone(1))
        Kind.FILE -> Column(Modifier.fillMaxWidth().cardPaint(p.tone(5), p.dark).padding(22.dp)) {
            val t = p.tone(5)
            Text(item.fileName?.substringAfterLast('.', "")?.uppercase()?.ifEmpty { null } ?: "ARCHIVO", style = Type.Headline, color = t.accent)
            Text(item.fileName.orEmpty(), style = Type.BodyStrong, color = t.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("${fileSize(item.size)} · ${item.mime ?: "desconocido"}", style = Type.Caption, color = t.ink.copy(alpha = 0.65f))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPill("Abrir", icon = Ico.Open, tint = Candy.Sky) { Sharing.open(context, item) }
                GlassPill("Compartir", icon = Ico.Share) { Sharing.share(context, item) }
            }
        }
        Kind.LINK -> Column(Modifier.fillMaxWidth().cardPaint(p.tone(3), p.dark).padding(22.dp)) {
            val t = p.tone(3)
            Text(domainOf(item.url.orEmpty()).uppercase(), style = Type.Eyebrow, color = t.accent)
            Spacer(Modifier.height(6.dp))
            SelectionContainer { Text(item.url.orEmpty(), style = Type.Label, color = t.ink) }
            Spacer(Modifier.height(14.dp))
            GlassPill("Abrir enlace", icon = Ico.Open, tint = Candy.Sky) { Sharing.open(context, item) }
        }
    }
}

@Composable
private fun TodoHero(item: Item, tone: Tone) {
    var adding by remember { mutableStateOf("") }
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().cardPaint(tone, p.dark).padding(14.dp)) {
        val done = item.checks.count { it.done }
        Text(
            "$done de ${item.checks.size} hechas", style = Type.Label, color = tone.ink.copy(alpha = 0.75f),
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
        )
        item.checks.forEachIndexed { i, c ->
            CheckPill(c.text, c.done, tone) {
                Store.update(item.id) { it.copy(checks = it.checks.toMutableList().also { l -> l[i] = c.copy(done = !c.done) }) }
            }
            Spacer(Modifier.height(8.dp))
        }
        fun add() {
            val t = adding.trim()
            if (t.isNotEmpty()) Store.update(item.id) { it.copy(checks = it.checks + Check(t)) }
            adding = ""
        }
        GlassTextField(
            adding, { adding = it }, "Añadir elemento", leading = Ico.Add, shape = Capsule,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
        )
        if (done > 0) {
            Spacer(Modifier.height(10.dp))
            GlassPill("Quitar las hechas", icon = Ico.Delete) {
                Store.update(item.id) { it.copy(checks = it.checks.filterNot { c -> c.done }) }
            }
        }
    }
}

@Composable
private fun AudioHero(item: Item, tone: Tone) {
    val p = LocalPalette.current
    val player = rememberPlayer(Store.fileOf(item))
    Column(Modifier.fillMaxWidth().cardPaint(tone, p.dark).padding(20.dp)) {
        Waveform(
            item.waveform, player.progress,
            Modifier.fillMaxWidth().height(110.dp).pointerInput(Unit) {
                detectTapGestures { o -> player.seek((o.x / size.width).coerceIn(0f, 1f)) }
            },
            glow = true,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassCircle(if (player.playing) Ico.Pause else Ico.Play, "Reproducir", size = 64.dp, solid = Candy.Sky) { player.toggle() }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    formatDuration((player.progress * item.durationMs).toLong()) + " / " + formatDuration(item.durationMs),
                    style = Type.Title, color = Color.White,
                )
                Text(fileSize(item.size), style = Type.Caption, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun ResultCard(
    note: AiNote,
    streaming: Boolean = false,
    onCancel: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onSaveNote: (() -> Unit)? = null,
    onMakeList: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val p = LocalPalette.current
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ico.Spark, null, tint = if (streaming) Candy.Grape else Candy.Sky, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(note.action, style = Type.BodyStrong, color = p.ink, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!streaming) Text(friendlyDate(note.created), style = Type.Caption, color = p.inkFaint)
        }
        Spacer(Modifier.height(10.dp))
        SelectionContainer {
            Text(markdown(note.text, Candy.Sky), style = Type.Body.copy(fontSize = 15.sp, lineHeight = 23.sp), color = p.ink)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (streaming) {
                onCancel?.let { GlassPill("Detener", icon = Ico.Stop, onClick = it) }
            } else {
                SmallAction(Ico.Copy, "Copiar") { Sharing.copy(context, note.text) }
                onSaveNote?.let { SmallAction(Ico.of(Kind.NOTE), "Guardar como nota", onClick = it) }
                onMakeList?.let { SmallAction(Ico.of(Kind.TODO), "Crear lista", onClick = it) }
                SmallAction(Ico.Share, "Compartir") { Sharing.shareText(context, note.text) }
                Spacer(Modifier.weight(1f))
                onDelete?.let { SmallAction(Ico.Delete, "Borrar", Candy.Tomato, it) }
            }
        }
    }
}

@Composable
private fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.size(40.dp).pressable(0.85f, onClick = onClick).clip(RoundedCornerShape(14.dp)).background(p.ink.copy(alpha = 0.07f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = color ?: p.inkSoft, modifier = Modifier.size(20.dp)) }
}
