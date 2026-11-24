package com.mimente.app.ai

import com.mimente.app.data.Prefs

/**
 * Elegir un modelo que funcione con la clave del usuario. Google retira modelos con el tiempo
 * (y a veces solo los deja a cuentas que ya los usaban), así que no se confía en un nombre fijo:
 * se prueba y, si falla, se busca otro entre los que ofrece la cuenta.
 */
object Models {
    /** Alias que Google mantiene apuntando al modelo más reciente de cada familia. */
    val aliases = listOf("gemini-flash-latest", "gemini-flash-lite-latest", "gemini-pro-latest")

    private fun version(name: String): Double =
        Regex("^gemini-(\\d+(?:\\.\\d+)?)").find(name)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

    /** Orden de preferencia: alias → Flash estable más nuevo → Flash-Lite → Pro → previews. */
    fun rank(available: List<String>): List<String> {
        val pool = available.ifEmpty { aliases }
        fun score(n: String): Int = when {
            n == "gemini-flash-latest" -> 0
            n == "gemini-flash-lite-latest" -> 1
            "preview" in n || "exp" in n -> 6
            Regex("^gemini-[\\d.]+-flash$").matches(n) -> 2
            Regex("^gemini-[\\d.]+-flash-lite$").matches(n) -> 3
            n == "gemini-pro-latest" -> 4
            "flash" in n -> 4
            else -> 5
        }
        return pool.distinct().sortedWith(compareBy<String> { score(it) }.thenByDescending { version(it) })
    }

    /** Una petición mínima para comprobar que el modelo responde. */
    suspend fun probe(key: String, model: String) {
        Gemini.generate(key, model, listOf(Gemini.Turn("user", listOf(Gemini.Part.Text("Responde solo: OK")))))
    }

    /**
     * Prueba el modelo actual y, si no está disponible, recorre los de la cuenta hasta dar con uno
     * que responda. Lo guarda en Ajustes y lo devuelve. Los errores de clave o cuota se propagan.
     */
    suspend fun ensureWorking(key: String, available: List<String>? = null): String {
        val current = Prefs.model
        try {
            probe(key, current); return current
        } catch (e: GeminiException) {
            if (!e.modelProblem) throw e
        }
        val list = available ?: runCatching { Gemini.listModels(key) }.getOrDefault(emptyList())
        var last: GeminiException? = null
        for (m in rank(list + aliases).filter { it != current }.take(8)) {
            try {
                probe(key, m)
                Prefs.setModel(m)
                return m
            } catch (e: GeminiException) {
                if (!e.modelProblem) throw e
                last = e
            }
        }
        throw last ?: GeminiException("Ningún modelo de Gemini respondió con esta clave.", modelProblem = true)
    }

    /**
     * Ejecuta [block] con el modelo actual; si Google dice que el modelo no está disponible,
     * cambia a uno que funcione y lo reintenta una vez.
     */
    suspend fun <T> withWorkingModel(key: String, onSwitch: (String) -> Unit = {}, block: suspend (String) -> T): T {
        val first = Prefs.model
        return try {
            block(first)
        } catch (e: GeminiException) {
            if (!e.modelProblem) throw e
            val m = ensureWorking(key)
            if (m != first) onSwitch(m)
            block(m)
        }
    }
}
