package com.mimente.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mimente.app.ai.AiRunner
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Store
import com.mimente.app.media.formatDuration
import com.mimente.app.media.rememberPlayer
import com.mimente.app.ui.common.Ico
import com.mimente.app.ui.common.fileSize
import com.mimente.app.ui.common.friendlyDate
import com.mimente.app.ui.glass.Feel
import com.mimente.app.ui.glass.Motion
import com.mimente.app.ui.glass.pressable
import com.mimente.app.ui.theme.Candy
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.Tone
import com.mimente.app.ui.theme.Type

val CardShape = RoundedCornerShape(30.dp)

/** La pintura de una tarjeta del escritorio: degradado del tono y borde de luz. */
fun Modifier.cardPaint(tone: Tone, dark: Boolean): Modifier = this
    .shadow(10.dp, CardShape, ambientColor = Color(0x333A3A8A), spotColor = if (dark) Color(0x88000000) else Color(0x443A3A8A))
    .clip(CardShape)
    .background(Brush.verticalGradient(listOf(tone.top, tone.bottom)))
    .border(
        1.dp,
        Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.35f else 0.9f), Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.45f))),
        CardShape,
    )

@Composable
fun DeskCard(item: Item, onOpen: () -> Unit, onMenu: () -> Unit) {
    val p = LocalPalette.current
    val tone = p.tone(item.tone)
    val base = Modifier.fillMaxWidth().pressable(0.95f, onLongClick = onMenu, onClick = onOpen)
    when (item.kind) {
        Kind.NOTE -> NoteCard(item, tone, base.cardPaint(tone, p.dark))
        Kind.TODO -> TodoCard(item, p.tone(2).takeIf { item.tone == 0 } ?: tone, base.cardPaint(p.tone(2).takeIf { item.tone == 0 } ?: tone, p.dark))
        Kind.PHOTO -> PhotoCard(item, base.cardPaint(tone, p.dark))
        Kind.VIDEO -> PhotoCard(item, base.cardPaint(tone, p.dark), video = true)
        Kind.AUDIO -> AudioCard(item, p.tone(1), base.cardPaint(p.tone(1), p.dark))
        Kind.FILE -> FileCard(item, p.tone(5), base.cardPaint(p.tone(5), p.dark))
        Kind.LINK -> LinkCard(item, p.tone(3), base.cardPaint(p.tone(3), p.dark))
    }
}

@Composable
private fun Footer(item: Item, ink: Color, modifier: Modifier = Modifier) {
    val processing = item.id in AiRunner.processing || AiRunner.live.containsKey(item.id)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (item.pinned) { Icon(Ico.Pin, null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
        if (processing || item.ai.isNotEmpty() || item.gist.isNotBlank()) {
            Icon(Ico.Spark, null, tint = if (processing) Candy.Grape else ink.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            if (processing) Text(" Pensando…", style = Type.Caption, color = Candy.Grape)
        }
        Spacer(Modifier.weight(1f))
        Text(friendlyDate(item.created), style = Type.Caption, color = ink.copy(alpha = 0.6f))
    }
}

@Composable
private fun NoteCard(item: Item, tone: Tone, modifier: Modifier) {
    val body = if (item.title.isBlank()) item.text.lines().dropWhile { it.isBlank() }.drop(1).joinToString("\n").trim() else item.text
    Column(modifier.padding(start = 20.dp, end = 18.dp, top = 22.dp, bottom = 14.dp)) {
        Text(item.displayTitle, style = Type.Serif, color = tone.accent, maxLines = 4, overflow = TextOverflow.Ellipsis)
        if (body.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "“${body.take(220)}”".takeIf { body.length < 90 } ?: body,
                style = Type.Body.copy(fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 21.sp),
                color = tone.ink.copy(alpha = 0.85f), maxLines = 7, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Footer(item, tone.ink)
    }
}

@Composable
private fun TodoCard(item: Item, tone: Tone, modifier: Modifier) {
    Column(modifier.padding(14.dp)) {
        Text(
            item.displayTitle, style = Type.Dot, color = tone.ink.copy(alpha = 0.85f), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        val shown = item.checks.take(5)
        shown.forEachIndexed { i, c ->
            CheckPill(c.text, c.done, tone) {
                Store.update(item.id) { it.copy(checks = it.checks.toMutableList().also { l -> l[i] = c.copy(done = !c.done) }) }
            }
            Spacer(Modifier.height(7.dp))
        }
        if (item.checks.size > 5) Text("+${item.checks.size - 5} más", style = Type.Caption, color = tone.ink.copy(alpha = 0.7f), modifier = Modifier.padding(start = 8.dp))
        if (item.checks.isEmpty()) Text("Lista vacía", style = Type.Label, color = tone.ink.copy(alpha = 0.6f), modifier = Modifier.padding(8.dp))
        Footer(item, tone.ink, Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun CheckPill(text: String, done: Boolean, tone: Tone, onToggle: () -> Unit) {
    val fill by animateFloatAsState(if (done) 1f else 0f, Motion.pop(), label = "check")
    val pill = androidx.compose.ui.graphics.lerp(tone.accent, tone.accent.copy(alpha = 0.55f), fill)
    Row(
        Modifier.fillMaxWidth().pressable(0.94f, if (done) Feel.ToggleOff else Feel.ToggleOn, onClick = onToggle)
            .clip(RoundedCornerShape(50)).background(pill).padding(6.dp).heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
            if (fill > 0.05f) Icon(Ico.Check, null, tint = tone.accent, modifier = Modifier.size(18.dp * fill))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text, style = Type.BodyStrong, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
    }
}

@Composable
private fun PhotoCard(item: Item, modifier: Modifier, video: Boolean = false) {
    val f = Store.fileOf(item)
    Box(modifier.aspectRatio(if (video) 0.9f else 0.78f)) {
        AsyncImage(model = f, contentDescription = item.displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xB3141428))))
        if (video) {
            Box(
                Modifier.align(Alignment.Center).size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Ico.Play, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            if (item.title.isNotBlank() || item.gist.isNotBlank()) {
                Text(item.displayTitle, style = Type.BodyStrong, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (video && item.durationMs > 0) Text(formatDuration(item.durationMs), style = Type.Caption, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Footer(item, Color.White)
        }
    }
}

@Composable
private fun AudioCard(item: Item, tone: Tone, modifier: Modifier) {
    val player = rememberPlayer(Store.fileOf(item))
    Column(modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (player.playing) Ico.Pause else Ico.Play, "Reproducir", tint = Color.White,
                modifier = Modifier.size(34.dp).pressable(0.8f) { player.toggle() },
            )
            Spacer(Modifier.width(6.dp))
            Text(item.displayTitle, style = Type.BodyStrong, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(formatDuration(item.durationMs), style = Type.Caption, color = Color.White.copy(alpha = 0.85f))
        }
        Waveform(item.waveform, player.progress, Modifier.fillMaxWidth().height(70.dp).padding(vertical = 8.dp), glow = true)
        if (item.gist.isNotBlank()) Text(item.gist, style = Type.Caption, color = tone.ink.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Footer(item, tone.ink)
    }
}

/** Barras de onda centradas; las ya reproducidas brillan. */
@Composable
fun Waveform(levels: List<Float>, progress: Float, modifier: Modifier, glow: Boolean = false, played: Color = Color.White, rest: Color = Color.White.copy(alpha = 0.55f)) {
    val bars = levels.ifEmpty { List(36) { 0.4f } }
    Canvas(modifier) {
        val n = bars.size
        val gap = size.width / n
        val w = (gap * 0.55f).coerceAtMost(8.dp.toPx())
        bars.forEachIndexed { i, v ->
            val h = (size.height * v).coerceAtLeast(w)
            val x = i * gap + (gap - w) / 2
            val c = if (progress > 0f && i.toFloat() / n <= progress) played else if (progress > 0f) rest else played.copy(alpha = 0.95f)
            if (glow) drawRoundRect(c.copy(alpha = 0.25f), Offset(x - 2, (size.height - h) / 2 - 2), Size(w + 4, h + 4), CornerRadius(w))
            drawRoundRect(c, Offset(x, (size.height - h) / 2), Size(w, h), CornerRadius(w / 2))
        }
    }
}

@Composable
private fun FileCard(item: Item, tone: Tone, modifier: Modifier) {
    val ext = item.fileName?.substringAfterLast('.', "")?.uppercase()?.take(5)?.ifEmpty { null } ?: "DOC"
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(96.dp)) { Papers(Modifier.align(Alignment.Center).offset(y = 8.dp)) }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp)) {
            Text(
                ext, style = Type.Headline.copy(fontSize = 46.sp, lineHeight = 48.sp, shadow = Shadow(Color(0x22000000), Offset(0f, 3f), 6f)),
                color = tone.accent, maxLines = 1,
            )
            Text(item.displayTitle, style = Type.BodyStrong, color = tone.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(fileSize(item.size), style = Type.Caption, color = tone.ink.copy(alpha = 0.6f))
            if (item.gist.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.gist, style = Type.Caption, color = tone.ink.copy(alpha = 0.8f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Footer(item, tone.ink)
        }
    }
}

/** Dos hojas de papel ladeadas, como en un escritorio. */
@Composable
private fun Papers(modifier: Modifier) {
    Box(modifier.size(120.dp, 80.dp)) {
        Sheet(Modifier.align(Alignment.Center).offset(x = (-22).dp).rotate(-10f))
        Sheet(Modifier.align(Alignment.Center).offset(x = 22.dp).rotate(8f))
    }
}

@Composable
private fun BoxScope.Sheet(modifier: Modifier) {
    Column(
        modifier.size(58.dp, 72.dp).shadow(6.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(4) { i -> Box(Modifier.fillMaxWidth(if (i == 3) 0.6f else 1f).height(5.dp).clip(CircleShape).background(Color(0xFFCFCFD8))) }
    }
}

@Composable
private fun LinkCard(item: Item, tone: Tone, modifier: Modifier) {
    Column(modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Icon(Ico.Web, null, tint = tone.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(com.mimente.app.data.domainOf(item.url.orEmpty()).uppercase(), style = Type.Eyebrow, color = tone.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Text(item.displayTitle, style = Type.Title, color = tone.ink, maxLines = 4, overflow = TextOverflow.Ellipsis)
        if (item.gist.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(item.gist, style = Type.Caption, color = tone.ink.copy(alpha = 0.75f), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Footer(item, tone.ink)
    }
}
