package com.mimente.app.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimente.app.Nav
import com.mimente.app.ai.AiRunner
import com.mimente.app.data.Check
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Store
import com.mimente.app.data.looksLikeUrl
import com.mimente.app.data.normalizeUrl
import com.mimente.app.ui.common.Ico
import com.mimente.app.ui.common.Toaster
import com.mimente.app.ui.glass.Feel
import com.mimente.app.ui.glass.GlassCircle
import com.mimente.app.ui.glass.GlassScreen
import com.mimente.app.ui.glass.GlassTextField
import com.mimente.app.ui.glass.TopChrome
import com.mimente.app.ui.glass.pressable
import com.mimente.app.ui.glass.rememberFeel
import com.mimente.app.ui.home.cardPaint
import com.mimente.app.ui.theme.Candy
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.Type

/** Crear o editar: notas, listas, enlaces, y título y nota de cualquier elemento. */
@Composable
fun EditorScreen(id: String?, kind: Kind, preset: String) {
    val p = LocalPalette.current
    val feel = rememberFeel()
    val existing = remember(id) { id?.let(Store::get) }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var text by rememberSaveable { mutableStateOf(existing?.text.orEmpty()) }
    var url by rememberSaveable { mutableStateOf(existing?.url ?: preset) }
    var tone by rememberSaveable { mutableIntStateOf(existing?.tone ?: if (kind == Kind.TODO) 2 else (0..7).random()) }
    val checks = remember { mutableStateListOf<Check>().apply { addAll(existing?.checks ?: if (kind == Kind.TODO) listOf(Check("")) else emptyList()) } }
    var focusIndex by remember { mutableIntStateOf(if (existing == null && kind == Kind.TODO) -2 else -1) }
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }
    val rowFocus = remember { mutableMapOf<Int, FocusRequester>() }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val t = p.tone(tone)

    LaunchedEffect(Unit) {
        if (existing == null) runCatching {
            when (kind) {
                Kind.NOTE -> bodyFocus.requestFocus()
                Kind.LINK -> if (url.isBlank()) urlFocus.requestFocus() else titleFocus.requestFocus()
                else -> titleFocus.requestFocus()
            }
        }
    }

    fun save() {
        val cleanChecks = checks.filter { it.text.isNotBlank() }
        if (kind == Kind.LINK && url.isNotBlank() && !looksLikeUrl(url)) { Toaster.show("Ese enlace no parece válido"); feel(Feel.Reject); return }
        if (existing == null) {
            val empty = title.isBlank() && text.isBlank() && cleanChecks.isEmpty() && url.isBlank()
            if (!empty) {
                val item = Store.add(
                    Item(
                        kind = kind, title = title.trim(), text = text.trim(), checks = cleanChecks,
                        url = url.takeIf { it.isNotBlank() }?.let(::normalizeUrl), tone = tone,
                    ),
                )
                feel(Feel.Rise)
                if (kind == Kind.LINK) AiRunner.autoProcess(item)
            }
        } else {
            Store.update(existing.id) {
                it.copy(
                    title = title.trim(), text = text.trim(), tone = tone,
                    checks = if (kind == Kind.TODO) cleanChecks else it.checks,
                    url = if (kind == Kind.LINK) url.takeIf { u -> u.isNotBlank() }?.let(::normalizeUrl) else it.url,
                )
            }
            feel(Feel.Confirm)
        }
        Nav.back()
    }

    val fieldStyle = Type.Body.copy(color = t.ink, fontSize = 17.sp, lineHeight = 27.sp)

    GlassScreen(
        content = {
            LazyColumn(
                Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 76.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "head") {
                    Text(
                        (if (existing == null) "NUEVA ${kind.label}" else "EDITAR ${kind.label}").uppercase(),
                        style = Type.Eyebrow, color = p.heroInk, modifier = Modifier.padding(start = 6.dp),
                    )
                }
                if (kind == Kind.LINK) item(key = "url") {
                    GlassTextField(
                        url, { url = it }, "https://…", leading = Ico.Web,
                        modifier = Modifier.fillMaxWidth().focusRequester(urlFocus),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { runCatching { titleFocus.requestFocus() } }),
                    )
                }
                item(key = "paper") {
                    Column(Modifier.fillMaxWidth().cardPaint(t, p.dark).padding(22.dp)) {
                        Field(
                            title, { title = it }, if (kind == Kind.LINK) "Título (opcional, la IA lo pone)" else "Título",
                            Type.Serif.copy(color = t.accent), Modifier.focusRequester(titleFocus), singleLine = true,
                            ime = ImeAction.Next,
                            onIme = { runCatching { if (kind == Kind.TODO) { if (checks.isEmpty()) checks += Check(""); focusIndex = 0 } else bodyFocus.requestFocus() } },
                        )
                        Spacer(Modifier.height(14.dp))
                        if (kind == Kind.TODO) {
                            checks.forEachIndexed { i, c ->
                                val fr = rowFocus.getOrPut(i) { FocusRequester() }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Box(
                                        Modifier.size(28.dp).pressable(0.8f, Feel.Tick) { checks[i] = c.copy(done = !c.done) }
                                            .clip(CircleShape).background(if (c.done) t.accent else Color.Transparent)
                                            .border(2.dp, t.accent, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) { if (c.done) Icon(Ico.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                                    Spacer(Modifier.width(12.dp))
                                    Field(
                                        c.text, { checks[i] = c.copy(text = it.replace("\n", "")) }, "Elemento",
                                        fieldStyle.copy(textDecoration = if (c.done) TextDecoration.LineThrough else null),
                                        Modifier.weight(1f).focusRequester(fr), singleLine = true, ime = ImeAction.Next,
                                        onIme = { checks.add(i + 1, Check("")); focusIndex = i + 1 },
                                    )
                                    Icon(
                                        Ico.Close, "Quitar", tint = t.ink.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp).padding(8.dp).pressable(0.8f, Feel.Fall) { checks.removeAt(i); rowFocus.remove(checks.size) },
                                    )
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().pressable(0.97f) { checks += Check(""); focusIndex = checks.lastIndex }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ico.Add, null, tint = t.accent, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Añadir elemento", style = Type.BodyStrong, color = t.accent)
                            }
                        } else {
                            Field(
                                text, { text = it },
                                when (kind) { Kind.NOTE -> "Escribe lo que tengas en mente…"; Kind.LINK -> "¿Por qué lo guardas? (opcional)"; else -> "Añade una nota (opcional)" },
                                fieldStyle, Modifier.fillMaxWidth().heightIn(min = 220.dp).focusRequester(bodyFocus), singleLine = false,
                            )
                        }
                    }
                }
                if (kind == Kind.NOTE || kind == Kind.TODO) item(key = "tones") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(start = 4.dp)) {
                        p.tones.forEachIndexed { i, tn ->
                            Box(
                                Modifier.size(34.dp).pressable(0.8f, Feel.Tick) { tone = i }.clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(tn.top.copy(alpha = 1f), tn.bottom.copy(alpha = 1f))))
                                    .border(if (i == tone) 3.dp else 1.dp, if (i == tone) Candy.Sky else Color.White.copy(alpha = 0.7f), CircleShape),
                            )
                        }
                    }
                }
            }
        },
        chrome = {
            TopChrome(onBack = { Nav.back() }) {
                GlassCircle(Ico.Check, "Guardar", size = 52.dp, solid = Candy.Sky) { save() }
            }
        },
    )

    LaunchedEffect(focusIndex, checks.size) {
        if (focusIndex == -2) { runCatching { titleFocus.requestFocus() }; focusIndex = -1 }
        else if (focusIndex >= 0) {
            kotlinx.coroutines.delay(30)
            runCatching { rowFocus[focusIndex]?.requestFocus() }
            focusIndex = -1
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean,
    ime: ImeAction = ImeAction.Default,
    onIme: (() -> Unit)? = null,
) {
    Box(modifier) {
        if (value.isEmpty()) Text(placeholder, style = style.copy(color = style.color.copy(alpha = 0.4f)))
        BasicTextField(
            value, onChange, textStyle = style, singleLine = singleLine,
            cursorBrush = SolidColor(Candy.Sky),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ime),
            keyboardActions = KeyboardActions(onNext = { onIme?.invoke() }, onDone = { onIme?.invoke() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
