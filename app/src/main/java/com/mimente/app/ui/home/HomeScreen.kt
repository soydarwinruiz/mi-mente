package com.mimente.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import androidx.core.content.ContextCompat
import com.mimente.app.Nav
import com.mimente.app.Screen
import com.mimente.app.ai.AiRunner
import com.mimente.app.data.Intake
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Store
import com.mimente.app.data.looksLikeUrl
import com.mimente.app.ui.common.Ico
import com.mimente.app.ui.common.Sharing
import com.mimente.app.ui.common.Toaster
import com.mimente.app.ui.common.greeting
import com.mimente.app.ui.glass.Capsule
import com.mimente.app.ui.glass.Feel
import com.mimente.app.ui.glass.GlassChip
import com.mimente.app.ui.glass.GlassCircle
import com.mimente.app.ui.glass.GlassDialog
import com.mimente.app.ui.glass.GlassScreen
import com.mimente.app.ui.glass.GlassSheet
import com.mimente.app.ui.glass.GlassTextField
import com.mimente.app.ui.glass.Motion
import com.mimente.app.ui.glass.glass
import com.mimente.app.ui.glass.pressable
import com.mimente.app.ui.glass.rememberFeel
import com.mimente.app.ui.theme.Candy
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

private val filters: List<Pair<String, String>> = listOf("all" to "Todo", "pinned" to "Fijados") +
    Kind.entries.map { it.name to it.plural }

private fun matches(item: Item, q: String): Boolean {
    if (q.isBlank()) return true
    val needle = q.trim().lowercase()
    return listOfNotNull(item.title, item.text, item.url, item.fileName, item.gist).any { needle in it.lowercase() } ||
        item.checks.any { needle in it.text.lowercase() } || item.ai.any { needle in it.text.lowercase() }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val p = LocalPalette.current
    val feel = rememberFeel()
    val scope = rememberCoroutineScope()
    val all by Store.items.collectAsState()
    var filter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<Item?>(null) }
    var deleting by remember { mutableStateOf<Item?>(null) }
    var pendingPhoto by remember { mutableStateOf<java.io.File?>(null) }

    val shown = all.filter {
        when (filter) {
            "all" -> true
            "pinned" -> it.pinned
            else -> it.kind.name == filter
        } && matches(it, query)
    }.sortedWith(compareByDescending<Item> { it.pinned }.thenByDescending { it.created })

    fun saved(items: List<Item>) {
        if (items.isEmpty()) { Toaster.show("No se pudo guardar"); return }
        feel(Feel.Rise)
        items.forEach(AiRunner::autoProcess)
        Toaster.show(if (items.size == 1) "Guardado" else "${items.size} elementos guardados")
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val items = withContext(Dispatchers.IO) { uris.mapNotNull { Intake.importUri(context, it) } }
            saved(items)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingPhoto
        pendingPhoto = null
        if (ok && f != null && f.length() > 0) saved(listOf(Intake.fromFile(context, f, "image/jpeg", "Foto")))
        else f?.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { importUris(it) }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { importUris(it) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recording = true else Toaster.show("Necesito el micrófono para grabar")
    }

    fun takePhoto() {
        val f = Intake.newMediaFile("jpg")
        pendingPhoto = f
        runCatching { camera.launch(Sharing.uriFor(context, f)) }.onFailure { Toaster.show("No hay app de cámara") }
    }

    fun record() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) recording = true
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun paste() {
        val clip = Sharing.clipboard(context) ?: run { Toaster.show("El portapapeles está vacío"); return }
        saved(listOf(if (looksLikeUrl(clip)) Intake.link(clip) else Intake.note(clip)))
    }

    LaunchedEffect(Nav.command) {
        when (Nav.command) {
            "voice" -> record()
            "photo" -> takePhoto()
        }
        Nav.command = null
    }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    GlassScreen(
        content = {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 12.dp, bottom = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalItemSpacing = 14.dp,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = StaggeredGridItemSpan.FullLine, key = "head") {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "m", style = Type.Headline.copy(fontSize = 54.sp, shadow = Shadow(Color(0x33000000), Offset(0f, 4f), 12f)),
                                color = p.heroInk, modifier = Modifier.padding(start = 6.dp).rotate(-6f),
                            )
                            Spacer(Modifier.weight(1f))
                            GlassCircle(Ico.Search, "Buscar", tint = if (searching) Candy.Lemon else null) {
                                searching = !searching
                                if (!searching) query = ""
                            }
                            Spacer(Modifier.width(10.dp))
                            GlassCircle(Ico.Settings, "Ajustes") { Nav.go(Screen.Settings) }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Mi Mente",
                            style = Type.Hero.copy(shadow = Shadow(Color(0x2A1D1B4A), Offset(0f, 6f), 18f)),
                            color = p.heroInk, modifier = Modifier.padding(start = 4.dp),
                        )
                        Text(
                            "${greeting()} · ${all.size} ${if (all.size == 1) "recuerdo" else "recuerdos"}",
                            style = Type.BodyStrong, color = p.heroInk.copy(alpha = 0.85f), modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                        )
                        AnimatedVisibility(searching, enter = expandVertically(Motion.unfold()) + fadeIn(), exit = shrinkVertically(Motion.fold()) + fadeOut()) {
                            GlassTextField(
                                query, { query = it }, "Buscar en todo lo que guardaste",
                                leading = Ico.Search, modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                shape = Capsule,
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
                item(span = StaggeredGridItemSpan.FullLine, key = "chips") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.bleed(16.dp),
                    ) {
                        items(filters, key = { it.first }) { (key, label) ->
                            GlassChip(label, filter == key) { filter = key }
                        }
                    }
                }
                item(span = StaggeredGridItemSpan.FullLine, key = "ask") { AskHero() }
                if (shown.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "empty") { Empty(all.isEmpty(), query) }
                }
                items(shown, key = { it.id }) { item ->
                    Box(Modifier.animateItem()) {
                        DeskCard(item, onOpen = { Nav.go(Screen.Detail(item.id)) }, onMenu = { optionsFor = item })
                    }
                }
            }
        },
        chrome = {
            // Atenúa el fondo mientras el menú está abierto
            AnimatedVisibility(menuOpen, enter = fadeIn(Motion.quick()), exit = fadeOut(Motion.quick())) {
                Box(Modifier.fillMaxSize().background(p.scrim).clickable(remember { MutableInteractionSource() }, null) { menuOpen = false })
            }
            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptureMenu(menuOpen) { choice ->
                    menuOpen = false
                    when (choice) {
                        "note" -> Nav.go(Screen.Edit(null, Kind.NOTE))
                        "todo" -> Nav.go(Screen.Edit(null, Kind.TODO))
                        "camera" -> takePhoto()
                        "gallery" -> gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        "file" -> files.launch(arrayOf("*/*"))
                        "link" -> Nav.go(Screen.Edit(null, Kind.LINK, Sharing.clipboard(context)?.takeIf(::looksLikeUrl).orEmpty()))
                        "paste" -> paste()
                        "voice" -> record()
                    }
                }
                Spacer(Modifier.height(14.dp))
                Dock(
                    menuOpen = menuOpen,
                    onPlus = { menuOpen = !menuOpen },
                    onMic = { menuOpen = false; record() },
                    onAsk = { menuOpen = false; Nav.go(Screen.Ask) },
                )
            }
            RecorderSheet(recording, onDone = { item -> recording = false; if (item != null) saved(listOf(item)) })
            GlassSheet(optionsFor != null, onDismiss = { optionsFor = null }) {
                val it = optionsFor
                if (it != null) ItemOptions(it, onDone = { optionsFor = null }, onDelete = { optionsFor = null; deleting = it })
            }
            GlassDialog(
                deleting != null, "¿Eliminar?", "«${deleting?.displayTitle}» se borrará de tu mente.", "Eliminar", destructive = true,
                onConfirm = { deleting?.let { Store.delete(it.id); feel(Feel.Fall) }; deleting = null },
                onDismiss = { deleting = null },
            )
        },
    )
}

/** Deja que una fila se salga del margen hasta el borde de la pantalla. */
private fun Modifier.bleed(h: androidx.compose.ui.unit.Dp) = layout { m, c ->
    val px = h.roundToPx()
    val pl = m.measure(c.copy(minWidth = c.minWidth + 2 * px, maxWidth = c.maxWidth + 2 * px))
    layout(pl.width - 2 * px, pl.height) { pl.place(-px, 0) }
}

@Composable
private fun AskHero() {
    Box(
        Modifier.fillMaxWidth()
            .pressable(0.97f) { Nav.go(Screen.Ask) }
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF7C6CF2), Color(0xFF5B8CF7), Color(0xFF7FC4F5))))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(30.dp))
            .padding(20.dp),
    ) {
        Column(Modifier.padding(end = 70.dp)) {
            Text("Pregúntale a tu mente", style = Type.Serif, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("Busca, resume y conecta todo lo que guardaste con Gemini.", style = Type.Label, color = Color.White.copy(alpha = 0.9f))
        }
        Box(
            Modifier.align(Alignment.CenterEnd).size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))
                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Ico.Spark, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun Empty(nothing: Boolean, query: String) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (nothing) "Tu mente está despejada" else if (query.isNotBlank()) "Nada coincide con «$query»" else "Aún no hay nada aquí",
            style = Type.Display, color = p.heroInk, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (nothing) "Toca ＋ para guardar una nota, foto, audio, archivo o enlace. También puedes compartir cosas desde cualquier app hacia Mi Mente."
            else "Prueba con otro filtro o búsqueda.",
            style = Type.Body, color = p.heroInk.copy(alpha = 0.85f), textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Dock(menuOpen: Boolean, onPlus: () -> Unit, onMic: () -> Unit, onAsk: () -> Unit) {
    val turn by animateFloatAsState(if (menuOpen) 45f else 0f, Motion.pop(), label = "plus")
    Row(
        Modifier.glass(Capsule, elevation = 18.dp).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircle(Ico.Add, "Añadir", size = 62.dp, solid = Color.White, modifier = Modifier.rotate(turn), onClick = onPlus)
        GlassCircle(Ico.Mic, "Grabar", size = 62.dp, tint = Candy.Mint.copy(alpha = 0.55f), onClick = onMic)
        GlassCircle(Ico.Spark, "Preguntar", size = 62.dp, tint = Candy.Grape.copy(alpha = 0.55f), onClick = onAsk)
    }
}

private data class Capture(val key: String, val label: String, val icon: ImageVector, val color: Color)

private val captures = listOf(
    Capture("note", "Nota", Ico.of(Kind.NOTE), Candy.Grape),
    Capture("todo", "Lista", Ico.of(Kind.TODO), Candy.Mint),
    Capture("camera", "Cámara", Ico.Camera, Candy.Bubblegum),
    Capture("gallery", "Galería", Ico.Gallery, Candy.Tangerine),
    Capture("file", "Archivo", Ico.of(Kind.FILE), Candy.Sky),
    Capture("link", "Enlace", Ico.of(Kind.LINK), Candy.Lime),
    Capture("paste", "Pegar", Ico.Paste, Color(0xFFC9B23A)),
    Capture("voice", "Voz", Ico.Mic, Candy.Tomato),
)

/** El menú de captura se despliega desde el botón ＋ con un resorte. */
@Composable
private fun CaptureMenu(open: Boolean, onPick: (String) -> Unit) {
    val p = LocalPalette.current
    AnimatedVisibility(
        open,
        enter = androidx.compose.animation.scaleIn(Motion.unfold(), 0.3f, TransformOrigin(0.5f, 1f)) + fadeIn(Motion.quick()),
        exit = androidx.compose.animation.scaleOut(Motion.fold(), 0.3f, TransformOrigin(0.5f, 1f)) + fadeOut(Motion.quick()),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp).glass(RoundedCornerShape(34.dp), elevation = 24.dp).padding(16.dp),
        ) {
            Text("GUARDAR", style = Type.Eyebrow, color = p.inkSoft, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
            captures.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { c ->
                        Column(
                            Modifier.width(74.dp).pressable(0.88f) { onPick(c.key) }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(52.dp).clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(c.color.copy(alpha = 0.85f), c.color)))
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Icon(c.icon, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                            Spacer(Modifier.height(6.dp))
                            Text(c.label, style = Type.Caption, color = p.ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemOptions(item: Item, onDone: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val p = LocalPalette.current
    Text(item.displayTitle, style = Type.Title, color = p.ink, maxLines = 1)
    Spacer(Modifier.height(12.dp))
    OptionRow(Ico.Pin, if (item.pinned) "Desfijar" else "Fijar arriba", Candy.Tangerine) {
        Store.update(item.id, touch = false) { it.copy(pinned = !it.pinned) }; onDone()
    }
    OptionRow(Ico.Edit, "Editar", Candy.Grape) { onDone(); Nav.go(Screen.Edit(item.id, item.kind)) }
    OptionRow(Ico.Share, "Compartir", Candy.Sky) { onDone(); Sharing.share(context, item) }
    if (item.kind != Kind.NOTE && item.kind != Kind.TODO) OptionRow(Ico.Open, "Abrir con…", Candy.Mint) { onDone(); Sharing.open(context, item) }
    OptionRow(Ico.Delete, "Eliminar", Candy.Tomato, onClick = onDelete)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun OptionRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().pressable(0.97f, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, style = Type.BodyStrong, color = if (color == Candy.Tomato) Candy.Tomato else p.ink)
    }
}
