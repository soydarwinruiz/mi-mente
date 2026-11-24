package com.mimente.app.ai

import android.util.Base64
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Intake
import com.mimente.app.data.Store
import com.mimente.app.data.domainOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

/** Convierte un elemento de la mente en partes que Gemini entiende. */
object Content {
    private const val INLINE_LIMIT = 14L * 1024 * 1024
    private const val TEXT_LIMIT = 400_000

    private val geminiImages = setOf("image/png", "image/jpeg", "image/webp", "image/heic", "image/heif")

    private val textExtensions = setOf(
        "txt", "md", "markdown", "csv", "tsv", "json", "xml", "html", "htm", "yaml", "yml", "log", "ini", "kt", "java",
        "py", "js", "ts", "c", "cpp", "h", "cs", "go", "rs", "swift", "rb", "php", "sql", "sh", "css", "tex", "srt", "vtt",
    )

    fun isYouTube(url: String) = Regex("(youtube\\.com/(watch|shorts|live)|youtu\\.be/)").containsMatchIn(url)

    /** ¿Necesita la herramienta url_context? (enlaces normales) */
    fun needsUrlContext(item: Item) = item.kind == Kind.LINK && item.url != null && !isYouTube(item.url)

    private fun date(t: Long) = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es")).format(Date(t))

    /** Describe el elemento en texto (para el contexto de "Pregúntale a tu mente"). */
    fun describe(item: Item, index: Int, maxBody: Int = 4000): String = buildString {
        append("[#$index] ${item.kind.label} · «${item.displayTitle}» · ${date(item.created)}")
        if (item.pinned) append(" · fijado")
        append('\n')
        when (item.kind) {
            Kind.NOTE -> append(item.text.take(maxBody))
            Kind.TODO -> item.checks.forEach { append(if (it.done) "  [x] " else "  [ ] ").append(it.text).append('\n') }
            Kind.LINK -> { append("URL: ${item.url}\n"); if (item.text.isNotBlank()) append(item.text.take(maxBody)) }
            Kind.FILE -> {
                append("Archivo: ${item.fileName} (${item.mime ?: "?"})\n")
                if (item.text.isNotBlank()) append(item.text.take(maxBody))
            }
            else -> if (item.text.isNotBlank()) append(item.text.take(maxBody))
        }
        if (item.gist.isNotBlank()) append("\nResumen: ${item.gist}")
        item.ai.take(3).forEach { append("\n${it.action}: ${it.text.take(1500)}") }
        append("\n\n")
    }

    /** Partes completas de un elemento (incluye el archivo cuando lo hay). */
    suspend fun partsFor(item: Item, apiKey: String): List<Gemini.Part> {
        val parts = mutableListOf<Gemini.Part>()
        val header = buildString {
            append("Elemento «${item.displayTitle}» (${item.kind.label}), guardado el ${date(item.created)}.")
            if (item.fileName != null) append(" Archivo: ${item.fileName}.")
        }
        parts += Gemini.Part.Text(header)
        when (item.kind) {
            Kind.NOTE -> parts += Gemini.Part.Text(item.text.ifBlank { "(nota vacía)" })
            Kind.TODO -> parts += Gemini.Part.Text(
                item.checks.joinToString("\n") { (if (it.done) "[x] " else "[ ] ") + it.text }.ifBlank { "(lista vacía)" },
            )
            Kind.LINK -> {
                val url = item.url.orEmpty()
                if (isYouTube(url)) parts += Gemini.Part.FileRef(null, url)
                parts += Gemini.Part.Text("Enlace: $url (${domainOf(url)})" + if (item.text.isNotBlank()) "\nNota del usuario: ${item.text}" else "")
            }
            else -> {
                val f = Store.fileOf(item)
                if (f == null) parts += Gemini.Part.Text("(el archivo ya no está disponible)")
                else parts += fileParts(item, f, apiKey)
                if (item.text.isNotBlank()) parts += Gemini.Part.Text("Nota del usuario: ${item.text}")
            }
        }
        return parts
    }

    private suspend fun fileParts(item: Item, f: File, apiKey: String): List<Gemini.Part> {
        val mime = item.mime ?: "application/octet-stream"
        val ext = f.extension.lowercase()
        // Imágenes: siempre JPEG reducido (rápido y compatible con cualquier formato de origen).
        if (mime.startsWith("image/")) {
            val bytes = Intake.scaledJpeg(f)
            return if (bytes != null) listOf(Gemini.Part.Inline("image/jpeg", Base64.encodeToString(bytes, Base64.NO_WRAP)))
            else if (mime in geminiImages) listOf(binary(item, f, mime, apiKey))
            else listOf(Gemini.Part.Text("(no se pudo leer la imagen)"))
        }
        if (mime.startsWith("audio/") || mime.startsWith("video/") || mime == "application/pdf") {
            val m = when {
                mime == "audio/x-m4a" || mime == "audio/m4a" -> "audio/mp4"
                else -> mime
            }
            return listOf(binary(item, f, m, apiKey))
        }
        if (mime.startsWith("text/") || ext in textExtensions || mime == "application/json" || mime == "application/xml") {
            return listOf(Gemini.Part.Text("Contenido de ${item.fileName}:\n" + f.readText().take(TEXT_LIMIT)))
        }
        val office = extractOffice(f, ext)
        if (office != null) {
            return listOf(Gemini.Part.Text("Texto extraído de ${item.fileName}:\n" + office.take(TEXT_LIMIT)))
        }
        return listOf(Gemini.Part.Text("(Formato ${item.fileName} no legible directamente; usa el nombre y el contexto.)"))
    }

    /** Pequeño: va dentro de la petición. Grande: se sube a la File API y se reutiliza 47 h. */
    private suspend fun binary(item: Item, f: File, mime: String, apiKey: String): Gemini.Part {
        if (f.length() <= INLINE_LIMIT) {
            return Gemini.Part.Inline(mime, Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
        }
        val now = System.currentTimeMillis()
        val cached = item.remoteUri
        if (cached != null && item.remoteExpires > now) return Gemini.Part.FileRef(mime, cached)
        val uri = Gemini.upload(apiKey, f, mime, item.displayTitle)
        Store.update(item.id, touch = false) { it.copy(remoteUri = uri, remoteExpires = now + 47L * 3600 * 1000) }
        return Gemini.Part.FileRef(mime, uri)
    }

    /** Texto de Word, Excel, PowerPoint y OpenDocument (son ZIP con XML dentro). */
    fun extractOffice(f: File, ext: String): String? = runCatching {
        ZipFile(f).use { zip ->
            fun read(name: String) = zip.getEntry(name)?.let { e -> zip.getInputStream(e).use { it.readBytes().decodeToString() } }
            when (ext) {
                "docx", "docm", "dotx" -> read("word/document.xml")?.let { xmlText(it, paragraph = "</w:p>") }
                "pptx" -> zip.entries().toList()
                    .filter { it.name.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
                    .sortedBy { it.name.filter(Char::isDigit).toIntOrNull() ?: 0 }
                    .mapIndexed { i, e -> "— Diapositiva ${i + 1} —\n" + xmlText(zip.getInputStream(e).use { it.readBytes().decodeToString() }, "</a:p>") }
                    .joinToString("\n\n")
                "xlsx", "xlsm" -> {
                    val shared = read("xl/sharedStrings.xml")?.let { s ->
                        Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(s).map { xmlText(it.groupValues[1], "") }.toList()
                    } ?: emptyList()
                    zip.entries().toList().filter { it.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }
                        .sortedBy { it.name.filter(Char::isDigit).toIntOrNull() ?: 0 }
                        .mapIndexed { i, e ->
                            val xml = zip.getInputStream(e).use { it.readBytes().decodeToString() }
                            val rows = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { row ->
                                Regex("<c ([^>]*?)(/>|>(.*?)</c>)", RegexOption.DOT_MATCHES_ALL).findAll(row.groupValues[1]).map { c ->
                                    val attrs = c.groupValues[1]
                                    val v = Regex("<v>(.*?)</v>").find(c.groupValues[3])?.groupValues?.get(1)
                                        ?: Regex("<t[^>]*>(.*?)</t>").find(c.groupValues[3])?.groupValues?.get(1) ?: ""
                                    if ("t=\"s\"" in attrs) shared.getOrNull(v.toIntOrNull() ?: -1) ?: v else unescape(v)
                                }.joinToString("\t")
                            }.joinToString("\n")
                            "— Hoja ${i + 1} —\n$rows"
                        }.joinToString("\n\n")
                }
                "odt", "ods", "odp" -> read("content.xml")?.let { xmlText(it, "</text:p>") }
                else -> null
            }
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun xmlText(xml: String, paragraph: String): String {
        var s = xml
        if (paragraph.isNotEmpty()) s = s.replace(paragraph, "\n")
        s = s.replace(Regex("<w:tab/>|<w:br/>"), " ").replace(Regex("<[^>]+>"), "")
        return unescape(s).lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    private fun unescape(s: String) = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
        .replace("&apos;", "'").replace("&amp;", "&")
}
