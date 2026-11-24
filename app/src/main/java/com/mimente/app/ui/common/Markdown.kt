package com.mimente.app.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Markdown sencillo a texto con estilo: # títulos, - viñetas, **negrita**, *cursiva*, `código`.
 * [onRef] hace tocables las referencias [#n] a elementos de la mente.
 */
fun markdown(src: String, accent: Color, onRef: ((Int) -> Unit)? = null): AnnotatedString = buildAnnotatedString {
    val lines = src.trimEnd().lines()
    lines.forEachIndexed { i, raw ->
        var line = raw.trimEnd()
        val heading = Regex("^(#{1,4})\\s+(.*)").find(line)
        when {
            heading != null -> {
                val level = heading.groupValues[1].length
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = if (level <= 2) 19.sp else 17.sp)) {
                    inline(heading.groupValues[2], accent, onRef)
                }
            }
            Regex("^\\s*[-*•]\\s+").containsMatchIn(line) -> {
                val indent = line.takeWhile { it == ' ' }.length / 2
                line = line.replaceFirst(Regex("^\\s*[-*•]\\s+"), "")
                append("   ".repeat(indent))
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append("•  ") }
                inline(line, accent, onRef)
            }
            Regex("^\\s*\\d+[.)]\\s+").containsMatchIn(line) -> {
                val n = Regex("^\\s*(\\d+[.)])\\s+").find(line)!!
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(n.groupValues[1] + " ") }
                inline(line.substring(n.range.last + 1), accent, onRef)
            }
            line.matches(Regex("^\\s*(---|\\*\\*\\*)\\s*$")) -> append("")
            else -> inline(line, accent, onRef)
        }
        if (i < lines.lastIndex) append('\n')
    }
}

private fun AnnotatedString.Builder.inline(text: String, accent: Color, onRef: ((Int) -> Unit)?) {
    val rx = Regex("\\*\\*(.+?)\\*\\*|(?<![*\\w])\\*(?!\\s)(.+?)\\*(?!\\w)|`([^`]+)`|\\[#(\\d+)]")
    var last = 0
    for (m in rx.findAll(text)) {
        append(text.substring(last, m.range.first))
        when {
            m.groups[1] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
            m.groups[2] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[2]) }
            m.groups[3] != null -> withStyle(SpanStyle(background = accent.copy(alpha = 0.14f))) { append(m.groupValues[3]) }
            m.groups[4] != null -> {
                val n = m.groupValues[4].toInt()
                if (onRef != null) {
                    withLink(LinkAnnotation.Clickable("ref$n", TextLinkStyles(SpanStyle(color = accent, fontWeight = FontWeight.Bold))) { onRef(n) }) {
                        append("[#$n]")
                    }
                } else append("[#$n]")
            }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

private val es = Locale("es")

/** "Ahora", "Hace 5 min", "Hoy 14:20", "Ayer", "12 nov 2026". */
fun friendlyDate(t: Long): String {
    val now = System.currentTimeMillis()
    val d = now - t
    if (d < 60_000) return "Ahora"
    if (d < 3_600_000) return "Hace ${d / 60_000} min"
    val c = Calendar.getInstance().apply { timeInMillis = t }
    val today = Calendar.getInstance()
    val sameDay = c.get(Calendar.YEAR) == today.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "Hoy " + SimpleDateFormat("HH:mm", es).format(Date(t))
    today.add(Calendar.DAY_OF_YEAR, -1)
    if (c.get(Calendar.YEAR) == today.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) return "Ayer"
    return SimpleDateFormat("d MMM yyyy", es).format(Date(t))
}

fun fileSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "%.0f KB".format(b / 1024f)
    b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / 1024f / 1024f)
    else -> "%.1f GB".format(b / 1024f / 1024f / 1024f)
}

fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Buenos días"
    in 12..19 -> "Buenas tardes"
    else -> "Buenas noches"
}
