package com.mimente.app.ai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mimente.app.data.Kind
import com.mimente.app.ui.theme.Candy

/** Acciones de IA de un toque. [kinds] = null significa que valen para todo. */
data class AiAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val prompt: String,
    val kinds: Set<Kind>? = null,
    /** El resultado son tareas, una por línea, que se pueden convertir en lista. */
    val makesTasks: Boolean = false,
)

object Actions {
    private val media = setOf(Kind.AUDIO, Kind.VIDEO)
    private val visual = setOf(Kind.PHOTO, Kind.FILE, Kind.VIDEO)

    val all = listOf(
        AiAction("Resumir", Icons.Rounded.Notes, Candy.Sky,
            "Resume este contenido: primero un párrafo breve con la idea principal y después de 3 a 6 viñetas con lo esencial."),
        AiAction("Transcribir", Icons.Rounded.RecordVoiceOver, Candy.Grape,
            "Transcribe el audio completo, palabra por palabra, separando por párrafos. Si hay varias personas, indica quién habla (Persona 1, Persona 2…).",
            kinds = media),
        AiAction("Describir", Icons.Rounded.Image, Candy.Bubblegum,
            "Describe con detalle lo que se ve: objetos, personas, lugar, texto visible y cualquier dato útil.",
            kinds = setOf(Kind.PHOTO, Kind.VIDEO)),
        AiAction("Extraer texto", Icons.Rounded.TextFields, Candy.Tangerine,
            "Extrae todo el texto visible tal cual aparece, respetando el orden y los saltos de línea. No añadas comentarios.",
            kinds = visual),
        AiAction("Puntos clave", Icons.Rounded.FormatListBulleted, Candy.Mint,
            "Enumera los puntos clave, datos, fechas, cifras y nombres importantes como viñetas cortas."),
        AiAction("Tareas", Icons.Rounded.Checklist, Candy.Lime,
            "Extrae las tareas, pendientes o próximos pasos que se desprenden de este contenido. Responde SOLO con la lista, una tarea por línea, cada línea empezando por \"- \". Si no hay ninguna, propone 3 pasos útiles.",
            makesTasks = true),
        AiAction("Explicar fácil", Icons.Rounded.ChildCare, Candy.Lemon,
            "Explícalo de forma muy sencilla, como a alguien que no sabe nada del tema, con un ejemplo cotidiano."),
        AiAction("Traducir", Icons.Rounded.Translate, Candy.Sky,
            "Traduce el contenido completo. Si está en español, tradúcelo al inglés; si está en otro idioma, tradúcelo al español. Devuelve solo la traducción."),
        AiAction("Ideas", Icons.Rounded.Lightbulb, Candy.Tangerine,
            "Desarrolla este contenido: propone ideas, conexiones, preguntas abiertas y cómo podría aprovecharlo.",
            kinds = setOf(Kind.NOTE, Kind.TODO, Kind.LINK, Kind.PHOTO, Kind.AUDIO)),
        AiAction("Cuestionario", Icons.Rounded.Quiz, Candy.Grape,
            "Crea 5 preguntas de repaso sobre este contenido, cada una con su respuesta debajo, para estudiarlo.",
            kinds = setOf(Kind.NOTE, Kind.FILE, Kind.LINK, Kind.AUDIO, Kind.VIDEO)),
        AiAction("Redactar", Icons.Rounded.Description, Candy.Bubblegum,
            "Convierte este contenido en un texto bien redactado, ordenado y listo para compartir.",
            kinds = setOf(Kind.NOTE, Kind.TODO, Kind.AUDIO)),
    )

    fun forKind(kind: Kind) = all.filter { it.kinds == null || kind in it.kinds }

    val custom = AiAction("Pregunta", Icons.Rounded.AutoAwesome, Candy.Sky, "")

    /** Líneas de tarea de una respuesta ("- algo", "* algo", "1. algo", "[ ] algo"). */
    fun parseTasks(text: String): List<String> = text.lines()
        .map { it.trim() }
        .mapNotNull { l ->
            Regex("^(?:[-*•]|\\d+[.)]|\\[ ?\\])\\s*(?:\\[ ?[xX ]?\\]\\s*)?(.+)$").find(l)?.groupValues?.get(1)
                ?.replace("**", "")?.trim()
        }
        .filter { it.isNotEmpty() }
}
