package com.mimente.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Ajustes, observables desde Compose. */
object Prefs {
    private lateinit var sp: SharedPreferences

    var apiKey by mutableStateOf("")
        private set
    var model by mutableStateOf(DEFAULT_MODEL)
        private set
    /** 0 automático, 1 español, 2 inglés */
    var language by mutableIntStateOf(1)
        private set
    var autoProcess by mutableStateOf(true)
        private set
    var webSearch by mutableStateOf(false)
        private set
    /** 0 sistema, 1 claro, 2 oscuro */
    var theme by mutableIntStateOf(0)
        private set
    var haptics by mutableStateOf(true)
        private set
    var glassBlur by mutableFloatStateOf(24f)
        private set

    const val DEFAULT_MODEL = "gemini-flash-latest"

    fun init(context: Context) {
        sp = context.getSharedPreferences("mimente", Context.MODE_PRIVATE)
        apiKey = sp.getString("apiKey", "") ?: ""
        model = sp.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        // Los modelos 2.5 Flash/Pro están retirados para cuentas nuevas: pasar al alias que Google mantiene.
        if (model == "gemini-2.5-flash" || model == "gemini-2.5-pro") setModel(DEFAULT_MODEL)
        language = sp.getInt("language", 1)
        autoProcess = sp.getBoolean("autoProcess", true)
        webSearch = sp.getBoolean("webSearch", false)
        theme = sp.getInt("theme", 0)
        haptics = sp.getBoolean("haptics", true)
        glassBlur = sp.getFloat("glassBlur", 24f)
    }

    @JvmName("putApiKey") fun setApiKey(v: String) { apiKey = v.trim(); sp.edit().putString("apiKey", apiKey).apply() }
    @JvmName("putModel") fun setModel(v: String) { model = v.trim().removePrefix("models/").ifBlank { DEFAULT_MODEL }; sp.edit().putString("model", model).apply() }
    @JvmName("putLanguage") fun setLanguage(v: Int) { language = v; sp.edit().putInt("language", v).apply() }
    @JvmName("putAutoProcess") fun setAutoProcess(v: Boolean) { autoProcess = v; sp.edit().putBoolean("autoProcess", v).apply() }
    @JvmName("putWebSearch") fun setWebSearch(v: Boolean) { webSearch = v; sp.edit().putBoolean("webSearch", v).apply() }
    @JvmName("putTheme") fun setTheme(v: Int) { theme = v; sp.edit().putInt("theme", v).apply() }
    @JvmName("putHaptics") fun setHaptics(v: Boolean) { haptics = v; sp.edit().putBoolean("haptics", v).apply() }
    @JvmName("putGlassBlur") fun setGlassBlur(v: Float) { glassBlur = v; sp.edit().putFloat("glassBlur", v).apply() }

    val languageInstruction: String
        get() = when (language) {
            1 -> "Responde siempre en español."
            2 -> "Always answer in English."
            else -> "Responde en el mismo idioma que el usuario o el contenido."
        }
}
