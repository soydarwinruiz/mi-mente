package com.mimente.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Qué es cada cosa guardada en la mente. */
enum class Kind(val label: String, val plural: String) {
    NOTE("Nota", "Notas"),
    TODO("Lista", "Listas"),
    PHOTO("Foto", "Fotos"),
    AUDIO("Audio", "Audios"),
    VIDEO("Video", "Videos"),
    FILE("Archivo", "Archivos"),
    LINK("Enlace", "Enlaces"),
}

data class Check(val text: String, val done: Boolean = false)

/** Un resultado de IA guardado junto al elemento. */
data class AiNote(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val text: String,
    val created: Long = System.currentTimeMillis(),
)

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val kind: Kind,
    val title: String = "",
    val text: String = "",
    val checks: List<Check> = emptyList(),
    /** Nombre del archivo dentro de filesDir/media. */
    val file: String? = null,
    val mime: String? = null,
    val fileName: String? = null,
    val size: Long = 0,
    val url: String? = null,
    val durationMs: Long = 0,
    val waveform: List<Float> = emptyList(),
    val tone: Int = 0,
    val pinned: Boolean = false,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
    val ai: List<AiNote> = emptyList(),
    /** Resumen corto generado al guardar (procesado automático). */
    val gist: String = "",
    /** Archivo subido a la File API de Gemini (caduca a las 48 h). */
    val remoteUri: String? = null,
    val remoteExpires: Long = 0,
) {
    val displayTitle: String
        get() = title.ifBlank {
            when (kind) {
                Kind.NOTE -> text.lineSequence().firstOrNull { it.isNotBlank() }?.take(60) ?: "Nota"
                Kind.LINK -> url?.let(::domainOf) ?: "Enlace"
                Kind.FILE -> fileName ?: "Archivo"
                else -> kind.label
            }
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("kind", kind.name); put("title", title); put("text", text)
        put("checks", JSONArray().apply { checks.forEach { put(JSONObject().put("t", it.text).put("d", it.done)) } })
        put("file", file); put("mime", mime); put("fileName", fileName); put("size", size)
        put("url", url); put("durationMs", durationMs)
        put("waveform", JSONArray().apply { waveform.forEach { put(it.toDouble()) } })
        put("tone", tone); put("pinned", pinned); put("created", created); put("updated", updated)
        put("ai", JSONArray().apply {
            ai.forEach { put(JSONObject().put("id", it.id).put("a", it.action).put("t", it.text).put("c", it.created)) }
        })
        put("gist", gist); put("remoteUri", remoteUri); put("remoteExpires", remoteExpires)
    }

    companion object {
        fun fromJson(o: JSONObject): Item = Item(
            id = o.getString("id"),
            kind = runCatching { Kind.valueOf(o.getString("kind")) }.getOrDefault(Kind.NOTE),
            title = o.optString("title"),
            text = o.optString("text"),
            checks = o.optJSONArray("checks").map { Check(it.optString("t"), it.optBoolean("d")) },
            file = o.optStringOrNull("file"),
            mime = o.optStringOrNull("mime"),
            fileName = o.optStringOrNull("fileName"),
            size = o.optLong("size"),
            url = o.optStringOrNull("url"),
            durationMs = o.optLong("durationMs"),
            waveform = o.optJSONArray("waveform")?.let { a -> List(a.length()) { a.getDouble(it).toFloat() } } ?: emptyList(),
            tone = o.optInt("tone"),
            pinned = o.optBoolean("pinned"),
            created = o.optLong("created"),
            updated = o.optLong("updated"),
            ai = o.optJSONArray("ai").map { AiNote(it.optString("id"), it.optString("a"), it.optString("t"), it.optLong("c")) },
            gist = o.optString("gist"),
            remoteUri = o.optStringOrNull("remoteUri"),
            remoteExpires = o.optLong("remoteExpires"),
        )
    }
}

private fun <T> JSONArray?.map(f: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else List(length()) { f(getJSONObject(it)) }

private fun JSONObject.optStringOrNull(k: String): String? =
    if (isNull(k) || !has(k)) null else optString(k).ifEmpty { null }

fun domainOf(url: String): String =
    runCatching { java.net.URI(url.trim()).host?.removePrefix("www.") }.getOrNull() ?: url

fun looksLikeUrl(s: String): Boolean {
    val t = s.trim()
    return !t.contains(' ') && !t.contains('\n') &&
        (t.startsWith("http://") || t.startsWith("https://") || Regex("^[\\w-]+(\\.[\\w-]+)+(/\\S*)?$").matches(t))
}

fun normalizeUrl(s: String): String = s.trim().let { if (it.startsWith("http")) it else "https://$it" }
