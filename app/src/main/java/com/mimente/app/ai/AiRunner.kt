package com.mimente.app.ai

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.mimente.app.data.AiNote
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Prefs
import com.mimente.app.data.Store
import com.mimente.app.ui.common.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Ejecuta la IA sobre los elementos y guarda los resultados. Sobrevive a cambiar de pantalla. */
object AiRunner {
    data class Live(val action: String, val text: String)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Resultado que se está escribiendo ahora mismo, por id de elemento. */
    val live = mutableStateMapOf<String, Live>()
    private val jobs = mutableMapOf<String, Job>()

    /** Elementos que se están procesando automáticamente al guardarlos. */
    val processing = mutableStateListOf<String>()

    fun system(extra: String = "") =
        "Eres «Mi Mente», el segundo cerebro personal del usuario: le ayudas a entender, resumir y organizar " +
            "lo que guarda (notas, fotos, audios, videos, documentos y enlaces). Sé claro, útil y conciso. " +
            "Usa markdown sencillo: títulos con #, viñetas con -, **negrita**. ${Prefs.languageInstruction} $extra"

    fun requireKey(): String? {
        val k = Prefs.apiKey
        if (k.isBlank()) Toaster.show("Añade tu clave de Google AI Studio en Ajustes", Toaster.Action.OpenSettings)
        return k.ifBlank { null }
    }

    fun run(item: Item, action: AiAction, question: String? = null) {
        val key = requireKey() ?: return
        if (jobs[item.id]?.isActive == true) return
        val label = if (question != null) question.take(80) else action.label
        val prompt = question ?: action.prompt
        live[item.id] = Live(label, "")
        jobs[item.id] = scope.launch {
            try {
                val parts = Content.partsFor(item, key) + Gemini.Part.Text("\nInstrucción: $prompt")
                val tools = buildSet {
                    if (Content.needsUrlContext(item)) add("url_context")
                    if (question != null && Prefs.webSearch) add("google_search")
                }
                var last = ""
                Models.withWorkingModel(key, ::announceSwitch) { model ->
                    Gemini.stream(key, model, listOf(Gemini.Turn("user", parts)), system(), tools).collect {
                        last = it
                        live[item.id] = Live(label, it)
                    }
                }
                Store.update(item.id) { it.copy(ai = listOf(AiNote(action = label, text = last.trim())) + it.ai) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toaster.show(e.message ?: "No se pudo completar")
            } finally {
                live.remove(item.id)
                jobs.remove(item.id)
            }
        }
    }

    fun announceSwitch(model: String) = Toaster.show("Tu modelo anterior no está disponible; ahora uso $model")

    fun cancel(itemId: String) { jobs[itemId]?.cancel() }

    /** Al guardar algo nuevo: título, resumen de una línea y, si aplica, transcripción o texto visible. */
    fun autoProcess(item: Item) {
        if (!Prefs.autoProcess || Prefs.apiKey.isBlank()) return
        if (item.kind == Kind.NOTE || item.kind == Kind.TODO) return
        val key = Prefs.apiKey
        processing += item.id
        scope.launch {
            try {
                val extraField = when (item.kind) {
                    Kind.AUDIO, Kind.VIDEO -> "\"texto\": transcripción completa de lo que se dice (cadena vacía si no hay voz)"
                    Kind.PHOTO -> "\"texto\": el texto visible en la imagen, tal cual (cadena vacía si no hay)"
                    else -> "\"texto\": cadena vacía"
                }
                val ask = "Analiza el elemento y responde SOLO con un objeto JSON válido, sin nada más, con estas claves: " +
                    "\"titulo\": título descriptivo de máximo 6 palabras; " +
                    "\"resumen\": una o dos frases con lo esencial; $extraField."
                val parts = Content.partsFor(item, key) + Gemini.Part.Text(ask)
                val raw = Models.withWorkingModel(key, ::announceSwitch) { model ->
                    if (Content.needsUrlContext(item)) {
                        // url_context no admite salida JSON forzada: se pide en texto y se extrae.
                        var out = ""
                        Gemini.stream(key, model, listOf(Gemini.Turn("user", parts)), system(), setOf("url_context")).collect { out = it }
                        out
                    } else Gemini.generate(key, model, listOf(Gemini.Turn("user", parts)), system(), jsonOutput = true)
                }
                val o = JSONObject(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1))
                val title = o.optString("titulo").trim().trim('"', '.')
                val gist = o.optString("resumen").trim()
                val text = o.optString("texto").trim()
                Store.update(item.id, touch = false) { cur ->
                    cur.copy(
                        title = cur.title.ifBlank { title },
                        gist = gist,
                        ai = if (text.length > 3) listOf(
                            AiNote(action = if (item.kind == Kind.PHOTO) "Texto visible" else "Transcripción", text = text),
                        ) + cur.ai else cur.ai,
                    )
                }
            } catch (e: Exception) {
                Toaster.show("No se pudo procesar con IA: ${e.message ?: ""}".trim())
            } finally {
                processing -= item.id
            }
        }
    }
}
