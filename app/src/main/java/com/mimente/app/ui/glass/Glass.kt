package com.mimente.app.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimente.app.data.Prefs
import com.mimente.app.ui.theme.Candy
import com.mimente.app.ui.theme.LocalPalette
import com.mimente.app.ui.theme.Type
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay

/** El fondo que hay detrás del vidrio de esta pantalla. */
val LocalHaze = compositionLocalOf<HazeState?> { null }

val Capsule = RoundedCornerShape(percent = 50)

/** Cielo de fondo: degradado con manchas de color suaves (estático: no respira ni brilla solo). */
@Composable
fun Sky(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.groundTop, p.groundBottom)))
            .drawBehind {
                val w = size.width; val h = size.height
                val spots = listOf(Offset(w * 0.9f, h * 0.08f), Offset(w * 0.05f, h * 0.42f), Offset(w * 0.85f, h * 0.7f), Offset(w * 0.2f, h * 0.95f))
                spots.forEachIndexed { i, c ->
                    drawCircle(Brush.radialGradient(listOf(p.blobs[i].copy(alpha = 0.85f), Color.Transparent), c, w * 0.75f), w * 0.75f, c)
                }
            },
    )
}

/**
 * Una pantalla con vidrio: [content] es lo que hay detrás (cielo + contenido) y [chrome] lo que flota
 * encima como vidrio (barras, botones, hojas).
 */
@Composable
fun GlassScreen(
    content: @Composable BoxScope.() -> Unit,
    chrome: @Composable BoxScope.() -> Unit,
) {
    val haze = rememberHazeState()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            Sky()
            content()
        }
        CompositionLocalProvider(LocalHaze provides haze) { chrome() }
    }
}

/** Vidrio líquido: desenfoca y tiñe lo que hay detrás, con un borde de luz. */
fun Modifier.glass(shape: Shape, tint: Color? = null, elevation: Dp = 10.dp): Modifier = composed {
    val p = LocalPalette.current
    val haze = LocalHaze.current
    val tintColor = tint?.let { if (it.alpha < 1f) it else it.copy(alpha = 0.82f) } ?: p.glassTint
    val shadowColor = if (p.dark) Color.Black.copy(alpha = 0.5f) else Color(0xFF3A3A8A).copy(alpha = 0.28f)
    this
        .shadow(elevation, shape, ambientColor = shadowColor, spotColor = shadowColor)
        .clip(shape)
        .then(
            if (haze != null) Modifier.hazeEffect(
                haze,
                HazeStyle(
                    backgroundColor = p.glassBase,
                    tints = listOf(HazeTint(p.glassTint)),
                    blurRadius = Prefs.glassBlur.dp,
                    noiseFactor = 0.035f,
                    fallbackTint = HazeTint(p.glassBase.copy(alpha = 0.94f)),
                ),
            ) else Modifier.background(p.glassBase.copy(alpha = 0.9f)),
        )
        // el color propio del vidrio (botón principal, tarjeta de IA) va encima del desenfoque
        .then(if (tint != null) Modifier.background(tintColor) else Modifier)
        .drawWithContent {
            drawContent()
            // brillo especular arriba, como una lente
            drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = if (p.dark) 0.10f else 0.28f), 0.45f to Color.Transparent))
        }
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(p.rim.copy(alpha = 0.9f), p.rim.copy(alpha = 0.12f), p.rim.copy(alpha = 0.55f)),
                start = Offset.Zero, end = Offset.Infinite,
            ),
            shape,
        )
}

/** Tinta legible sobre un color: blanca sobre colores vivos, casi negra sobre Lemon. */
fun inkOn(c: Color): Color = if (c.luminance() > 0.6f) Color(0xFF1E2030) else Color.White

@Composable
fun GlassCircle(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color? = null,
    solid: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val ink = when {
        solid != null -> inkOn(solid)
        tint != null -> inkOn(tint)
        else -> p.ink
    }
    Box(
        modifier
            .size(size)
            .pressable(0.86f, onLongClick = onLongClick, onClick = onClick)
            .then(
                if (solid != null) Modifier.shadow(12.dp, CircleShape, spotColor = Color(0x553A3A8A)).clip(CircleShape)
                    .background(solid).border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                else Modifier.glass(CircleShape, tint),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = ink, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val ink = if (tint != null) inkOn(tint) else p.ink
    Row(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .pressable(0.93f) { if (enabled) onClick() }
            .glass(Capsule, tint)
            .heightIn(min = 48.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Type.BodyStrong, color = ink, maxLines = 1)
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color? = null,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val ink = if (selected) Color(0xFF26283A) else p.ink
    Row(
        modifier
            .pressable(0.9f, Feel.Tick, onClick = onClick)
            .then(
                if (selected) Modifier
                    .shadow(8.dp, Capsule, spotColor = Color(0x553A3A8A))
                    .clip(Capsule)
                    .background(Brush.horizontalGradient(listOf(Color(0xFFD9EE7C), Candy.Lemon, Color(0xFFF4F5A6))))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), Capsule)
                else Modifier.glass(Capsule, elevation = 4.dp),
            )
            .heightIn(min = 46.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) ink else accent ?: ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = Type.Label.copy(fontSize = Type.BodyStrong.fontSize), color = ink, maxLines = 1)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.glass(shape, tint, elevation = 6.dp).padding(padding), content = content)
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    style: TextStyle = Type.Body,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    shape: Shape = RoundedCornerShape(24.dp),
    maxLines: Int = if (singleLine) 1 else 6,
) {
    val p = LocalPalette.current
    Row(
        modifier.glass(shape, elevation = 4.dp).heightIn(min = 52.dp).padding(start = if (leading != null) 14.dp else 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = p.inkSoft, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
            if (value.isEmpty()) Text(placeholder, style = style, color = p.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = style.copy(color = p.ink),
                singleLine = singleLine,
                maxLines = maxLines,
                cursorBrush = SolidColor(Candy.Sky),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailing != null) trailing() else Spacer(Modifier.width(12.dp))
    }
}

/** Interruptor con pulgar que salta con resorte. */
@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val feel = rememberFeel()
    val x by animateDpAsState(if (checked) 22.dp else 0.dp, Motion.pop(), label = "thumb")
    val stretch by animateFloatAsState(if (checked) 1f else 0f, Motion.quick(), label = "fill")
    val p = LocalPalette.current
    Box(
        Modifier
            .size(52.dp, 30.dp)
            .clip(Capsule)
            .background(lerpColor(if (p.dark) Color(0x33FFFFFF) else Color(0x33252850), Candy.Mint, stretch))
            .clickable(remember { MutableInteractionSource() }, null) {
                feel(if (checked) Feel.ToggleOff else Feel.ToggleOn); onChange(!checked)
            }
            .padding(3.dp),
    ) {
        Box(Modifier.offset(x = x).size(24.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White))
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

/** Hoja inferior de vidrio con fondo atenuado. */
@Composable
fun GlassSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(Motion.quick()), exit = fadeOut(Motion.quick())) {
            Box(Modifier.fillMaxSize().background(p.scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(Motion.unfold()) { it } + fadeIn(Motion.quick()),
            exit = slideOutVertically(Motion.fold()) { it } + fadeOut(Motion.quick()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(10.dp)
                    .glass(RoundedCornerShape(34.dp), elevation = 24.dp)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 5.dp).clip(Capsule).background(p.inkFaint.copy(alpha = 0.5f)))
                Spacer(Modifier.size(12.dp))
                content()
            }
        }
    }
}

/** Diálogo que aparece con un pequeño salto. */
@Composable
fun GlassDialog(
    visible: Boolean,
    title: String,
    text: String,
    confirm: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(Motion.quick()), exit = fadeOut(Motion.quick())) {
            Box(Modifier.fillMaxSize().background(p.scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(Motion.pop(), initialScale = 0.8f) + fadeIn(Motion.quick()),
            exit = scaleOut(Motion.fold(), targetScale = 0.9f) + fadeOut(Motion.quick()),
        ) {
            Column(
                Modifier.padding(28.dp).widthIn(max = 380.dp).glass(RoundedCornerShape(30.dp), elevation = 24.dp).padding(24.dp),
            ) {
                Text(title, style = Type.Title, color = p.ink)
                Spacer(Modifier.size(8.dp))
                Text(text, style = Type.Body, color = p.inkSoft)
                Spacer(Modifier.size(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    GlassPill("Cancelar", onClick = onDismiss)
                    GlassPill(confirm, tint = if (destructive) Candy.Tomato else Candy.Sky, onClick = onConfirm)
                }
            }
        }
    }
}

/** Burbuja de mensaje arriba de la pantalla. */
@Composable
fun GlassToastHost(onAction: (com.mimente.app.ui.common.Toaster.Action) -> Unit) {
    val toast = com.mimente.app.ui.common.Toaster.current
    val p = LocalPalette.current
    LaunchedEffect(toast?.id) {
        if (toast != null) { delay(if (toast.text.length > 60) 5200 else 3200); com.mimente.app.ui.common.Toaster.dismiss(toast.id) }
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            toast != null,
            enter = slideInVertically(Motion.pop()) { -it * 2 } + fadeIn(Motion.quick()),
            exit = slideOutVertically(Motion.fold()) { -it * 2 } + fadeOut(Motion.quick()),
        ) {
            val t = remember(toast?.id) { toast } ?: return@AnimatedVisibility
            Row(
                Modifier
                    .padding(horizontal = 18.dp)
                    .pressable(0.95f) {
                        onAction(t.action); com.mimente.app.ui.common.Toaster.dismiss(t.id)
                    }
                    .glass(RoundedCornerShape(24.dp), elevation = 16.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(t.text, style = Type.Label, color = p.ink, modifier = Modifier.weight(1f, fill = false))
                if (t.action == com.mimente.app.ui.common.Toaster.Action.OpenSettings) {
                    Spacer(Modifier.width(12.dp))
                    Text("Ajustes", style = Type.BodyStrong, color = Candy.Sky)
                }
            }
        }
    }
}

/** Barra superior estándar: atrás a la izquierda, acciones a la derecha. */
@Composable
fun TopChrome(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    center: @Composable (Modifier) -> Unit = { Spacer(it) },
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) GlassCircle(com.mimente.app.ui.common.Ico.Back, "Atrás", onClick = onBack)
        center(Modifier.weight(1f))
        actions()
    }
}
