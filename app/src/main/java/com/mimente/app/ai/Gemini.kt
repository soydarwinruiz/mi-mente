package com.mimente.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiException(message: String) : Exception(message)

/** Cliente mínimo de la API de Gemini (Google AI Studio) por REST. */
object Gemini {
    private const val BASE = "https://generativelanguage.googleapis.com"

    sealed class Part {
        data class Text(val text: String) : Part()
        data class Inline(val mime: String, val base64: String) : Part()
        data class FileRef(val mime: String?, val uri: String) : Part()
    }

    data class Turn(val role: String, val parts: List<Part>)

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    private fun Part.toJson(): JSONObject = when (this) {
        is Part.Text -> JSONObject().put("text", text)
        is Part.Inline -> JSONObject().put("inline_data", JSONObject().put("mime_type", mime).put("data", base64))
        is Part.FileRef -> JSONObject().put("file_data", JSONObject().apply {
            if (mime != null) put("mime_type", mime)
            put("file_uri", uri)
        })
    }

    private fun body(
        turns: List<Turn>,
        system: String?,
        tools: Set<String>,
        jsonOutput: Boolean,
    ): String = JSONObject().apply {
        put("contents", JSONArray().apply {
            turns.forEach { t ->
                put(JSONObject().put("role", t.role).put("parts", JSONArray().apply { t.parts.forEach { put(it.toJson()) } }))
            }
        })
        if (system != null) put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
        if (tools.isNotEmpty()) put("tools", JSONArray().apply { tools.forEach { put(JSONObject().put(it, JSONObject())) } })
        if (jsonOutput) put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
    }.toString()

    private fun errorText(code: Int, raw: String): String {
        val msg = runCatching { JSONObject(raw).getJSONObject("error").getString("message") }.getOrNull()
        return when (code) {
            400 -> if (msg?.contains("API key", true) == true) "La clave de API no es válida. Revísala en Ajustes." else "Solicitud no válida: ${msg ?: raw.take(200)}"
            401, 403 -> "La clave de API fue rechazada (${msg ?: code}). Revísala en Ajustes."
            404 -> "El modelo no existe o no está disponible: ${msg ?: ""}"
            429 -> "Límite de uso alcanzado. Espera un momento e inténtalo de nuevo."
            in 500..599 -> "El servicio de Google no respondió bien ($code). Inténtalo otra vez."
            else -> msg ?: "Error $code"
        }
    }

    private fun textOf(chunk: JSONObject): String {
        val cand = chunk.optJSONArray("candidates")?.optJSONObject(0) ?: run {
            chunk.optJSONObject("promptFeedback")?.optString("blockReason")?.takeIf { it.isNotEmpty() }?.let {
                throw GeminiException("La IA bloqueó la solicitud ($it).")
            }
            return ""
        }
        val parts = cand.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        return buildString {
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                if (!p.optBoolean("thought")) append(p.optString("text"))
            }
        }
    }

    /** Respuesta en streaming: emite el texto acumulado cada vez que llega un trozo. */
    fun stream(
        apiKey: String,
        model: String,
        turns: List<Turn>,
        system: String? = null,
        tools: Set<String> = emptySet(),
    ): Flow<String> = flow {
        val req = Request.Builder()
            .url("$BASE/v1beta/models/$model:streamGenerateContent?alt=sse")
            .header("x-goog-api-key", apiKey)
            .post(body(turns, system, tools, false).toRequestBody(json))
            .build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw GeminiException(errorText(res.code, res.body?.string().orEmpty()))
            val src = res.body!!.source()
            val acc = StringBuilder()
            while (!src.exhausted()) {
                val line = src.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty()) continue
                val t = textOf(JSONObject(data))
                if (t.isNotEmpty()) {
                    acc.append(t)
                    emit(acc.toString())
                }
            }
            if (acc.isEmpty()) throw GeminiException("La IA no devolvió texto. Prueba con otra pregunta o modelo.")
        }
    }.flowOn(Dispatchers.IO)

    /** Respuesta completa de una vez (opcionalmente en JSON). */
    suspend fun generate(
        apiKey: String,
        model: String,
        turns: List<Turn>,
        system: String? = null,
        jsonOutput: Boolean = false,
    ): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$BASE/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body(turns, system, emptySet(), jsonOutput).toRequestBody(json))
            .build()
        http.newCall(req).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw GeminiException(errorText(res.code, raw))
            textOf(JSONObject(raw))
        }
    }

    /** Modelos disponibles para la clave que admiten generar contenido. */
    suspend fun listModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val out = mutableListOf<String>()
        var page: String? = null
        do {
            val url = "$BASE/v1beta/models?pageSize=200" + (page?.let { "&pageToken=$it" } ?: "")
            val req = Request.Builder().url(url).header("x-goog-api-key", apiKey).get().build()
            http.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (!res.isSuccessful) throw GeminiException(errorText(res.code, raw))
                val o = JSONObject(raw)
                val arr = o.optJSONArray("models") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val methods = m.optJSONArray("supportedGenerationMethods")?.toString().orEmpty()
                    val name = m.getString("name").removePrefix("models/")
                    if ("generateContent" in methods && name.startsWith("gemini") &&
                        listOf("tts", "image", "embedding", "live", "audio").none { it in name }
                    ) out += name
                }
                page = o.optString("nextPageToken").ifEmpty { null }
            }
        } while (page != null)
        out.distinct().sortedWith(compareByDescending<String> { it.contains("latest") || !it.contains("-0") }.thenByDescending { it })
    }

    /** Sube un archivo grande a la File API (protocolo resumable) y espera a que esté listo. */
    suspend fun upload(apiKey: String, file: File, mime: String, displayName: String): String = withContext(Dispatchers.IO) {
        val start = Request.Builder()
            .url("$BASE/upload/v1beta/files")
            .header("x-goog-api-key", apiKey)
            .header("X-Goog-Upload-Protocol", "resumable")
            .header("X-Goog-Upload-Command", "start")
            .header("X-Goog-Upload-Header-Content-Length", file.length().toString())
            .header("X-Goog-Upload-Header-Content-Type", mime)
            .post(JSONObject().put("file", JSONObject().put("display_name", displayName.take(120))).toString().toRequestBody(json))
            .build()
        val uploadUrl = http.newCall(start).execute().use { res ->
            if (!res.isSuccessful) throw GeminiException(errorText(res.code, res.body?.string().orEmpty()))
            res.header("x-goog-upload-url") ?: throw GeminiException("No se pudo iniciar la subida del archivo.")
        }
        val up = Request.Builder()
            .url(uploadUrl)
            .header("X-Goog-Upload-Offset", "0")
            .header("X-Goog-Upload-Command", "upload, finalize")
            .post(file.asRequestBody(mime.toMediaType()))
            .build()
        val fileObj = http.newCall(up).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw GeminiException(errorText(res.code, raw))
            JSONObject(raw).getJSONObject("file")
        }
        var state = fileObj.optString("state")
        val name = fileObj.getString("name")
        val uri = fileObj.getString("uri")
        var tries = 0
        while (state == "PROCESSING" && tries++ < 90) {
            delay(2000)
            val get = Request.Builder().url("$BASE/v1beta/$name").header("x-goog-api-key", apiKey).get().build()
            state = http.newCall(get).execute().use { JSONObject(it.body?.string().orEmpty()).optString("state") }
        }
        if (state == "FAILED") throw GeminiException("Google no pudo procesar el archivo.")
        uri
    }
}
