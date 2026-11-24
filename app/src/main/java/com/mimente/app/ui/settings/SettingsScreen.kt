package com.mimente.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mimente.app.BuildConfig
import com.mimente.app.Nav
import com.mimente.app.ai.Gemini
import com.mimente.app.data.Backup
import com.mimente.app.data.Prefs
import com.mimente.app.ui.common.Ico
import com.mimente.app.ui.common.Toaster
import com.mimente.app.ui.glass.Feel
import com.mimente.app.ui.glass.GlassCard
import com.mimente.app.ui.glass.GlassChip
import com.mimente.app.ui.glass.GlassCircle
import com.mimente.app.ui.glass.GlassPill
import com.mimente.app.ui.glass.GlassScreen
import com.mimente.app.ui.glass.GlassSwitch
import com.mimente.app.ui.glass.GlassTextField
import com.mimente.app.ui.glass.TopChrome
import com.mimente.app.ui.glass.rememberFeel
import com.mimente.app.ui.theme.Candy
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val feel = rememberFeel()
    var key by remember { mutableStateOf(Prefs.apiKey) }
    var showKey by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf(Prefs.model) }
    var models by remember { mutableStateOf(listOf(Prefs.DEFAULT_MODEL, "gemini-2.5-pro", "gemini-2.5-flash-lite")) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { Backup.export(context, uri) } }
                .onSuccess { Toaster.show("Copia guardada") }.onFailure { Toaster.show("No se pudo exportar: ${it.message}") }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { Backup.import(context, uri) } }
                .onSuccess { Toaster.show("$it elementos recuperados") }.onFailure { Toaster.show("No se pudo importar: ${it.message}") }
        }
    }

    fun test() {
        Prefs.setApiKey(key)
        if (key.isBlank()) { status = "Escribe tu clave primero."; return }
        testing = true; status = null
        scope.launch {
            runCatching { Gemini.listModels(key.trim()) }
                .onSuccess { list ->
                    if (list.isNotEmpty()) models = list
                    status = "✓ Conectado. ${list.size} modelos disponibles."
                    feel(Feel.Confirm)
                }
                .onFailure { status = "✗ ${it.message}"; feel(Feel.Reject) }
            testing = false
        }
    }

    GlassScreen(
        content = {
            LazyColumn(
                Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 76.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Text("Ajustes", style = Type.Headline, color = p.heroInk, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) }

                item {
                    Section(Ico.Key, "Google AI Studio", Candy.Sky) {
                        Text(
                            "Mi Mente usa Gemini con tu propia clave gratuita de Google AI Studio. Se guarda solo en este teléfono.",
                            style = Type.Label, color = p.inkSoft,
                        )
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(
                            key, { key = it; status = null }, "Pega tu clave (AIza…)",
                            visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailing = { GlassCircle(if (showKey) Ico.EyeOff else Ico.Eye, "Mostrar", size = 40.dp) { showKey = !showKey } },
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPill(if (testing) "Probando…" else "Guardar y probar", tint = Candy.Sky, enabled = !testing) { test() }
                            GlassPill("Obtener clave", icon = Ico.Open) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
                            }
                        }
                        status?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(it, style = Type.Label, color = if (it.startsWith("✓")) Candy.Mint else Candy.Tomato)
                        }
                    }
                }

                item {
                    Section(Ico.Spark, "Modelo", Candy.Grape) {
                        Text("Pulsa «Guardar y probar» para cargar todos los modelos de tu cuenta.", style = Type.Label, color = p.inkSoft)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            models.take(14).forEach { m ->
                                GlassChip(m, Prefs.model == m) { Prefs.setModel(m); model = m }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(model, { model = it }, "Otro modelo", trailing = {
                            GlassCircle(Ico.Check, "Usar", size = 40.dp, tint = Candy.Sky) { Prefs.setModel(model); Toaster.show("Modelo: ${Prefs.model}") }
                        })
                    }
                }

                item {
                    Section(Ico.Settings, "Inteligencia", Candy.Mint) {
                        Text("Idioma de las respuestas", style = Type.BodyStrong, color = p.ink)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Automático", "Español", "English").forEachIndexed { i, l -> GlassChip(l, Prefs.language == i) { Prefs.setLanguage(i) } }
                        }
                        Spacer(Modifier.height(14.dp))
                        ToggleRow("Procesar al guardar", "Título, resumen y transcripción automáticos para fotos, audios, archivos y enlaces.", Prefs.autoProcess, Prefs::setAutoProcess)
                        ToggleRow("Buscar en la web", "Permite a Gemini usar la Búsqueda de Google al responder preguntas.", Prefs.webSearch, Prefs::setWebSearch)
                    }
                }

                item {
                    Section(Ico.Eye, "Apariencia", Candy.Bubblegum) {
                        Text("Tema", style = Type.BodyStrong, color = p.ink)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Sistema", "Claro", "Oscuro").forEachIndexed { i, l -> GlassChip(l, Prefs.theme == i) { Prefs.setTheme(i) } }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vidrio", style = Type.BodyStrong, color = p.ink, modifier = Modifier.weight(1f))
                            Text(
                                when { Prefs.glassBlur < 14 -> "Cristalino"; Prefs.glassBlur < 30 -> "Equilibrado"; else -> "Esmerilado" },
                                style = Type.Label, color = if (Prefs.glassBlur != 24f) Candy.Tangerine else p.inkSoft,
                            )
                        }
                        var lastStep by remember { mutableStateOf(Prefs.glassBlur.toInt() / 4) }
                        Slider(
                            value = Prefs.glassBlur, valueRange = 6f..44f,
                            onValueChange = {
                                Prefs.setGlassBlur(it)
                                val step = it.toInt() / 4
                                if (step != lastStep) { lastStep = step; feel(Feel.Tick) }
                            },
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Candy.Sky, inactiveTrackColor = p.ink.copy(alpha = 0.15f)),
                        )
                        ToggleRow("Vibración", "Respuesta háptica al tocar, marcar y guardar.", Prefs.haptics, Prefs::setHaptics)
                    }
                }

                item {
                    Section(Ico.Export, "Tus datos", Candy.Tangerine) {
                        Text(
                            "Todo se guarda en este teléfono. Solo se envía a Google lo que procesas con IA.",
                            style = Type.Label, color = p.inkSoft,
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPill("Exportar copia", icon = Ico.Export) {
                                exporter.launch("mi-mente-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.zip")
                            }
                            GlassPill("Importar", icon = Ico.Import) { importer.launch(arrayOf("application/zip", "application/octet-stream")) }
                        }
                    }
                }

                item {
                    Text(
                        "Mi Mente ${BuildConfig.VERSION_NAME}\nDiseño de vidrio líquido inspirado en Artefact (MIT / Apache-2.0).\nTipografías Plus Jakarta Sans y Fraunces (SIL OFL).",
                        style = Type.Caption, color = p.heroInk.copy(alpha = 0.8f), modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        },
        chrome = { TopChrome(onBack = { Nav.back() }) },
    )
}

@Composable
private fun Section(icon: ImageVector, title: String, color: Color, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    GlassCard(padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(color), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = Type.Title, color = p.ink)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Type.BodyStrong, color = p.ink)
            Text(subtitle, style = Type.Caption, color = p.inkSoft)
        }
        GlassSwitch(checked, onChange)
    }
}
