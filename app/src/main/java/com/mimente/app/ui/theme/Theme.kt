@file:OptIn(ExperimentalTextApi::class)

package com.mimente.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mimente.app.R
import com.mimente.app.data.Prefs

/** Acentos: iguales en tema claro y oscuro. */
object Candy {
    val Sky = Color(0xFF2F7BF5)        // acción principal
    val Tomato = Color(0xFFFF4F45)     // borrar, errores
    val Lemon = Color(0xFFE8F07A)      // chip seleccionado, valor cambiado
    val Mint = Color(0xFF2FC79A)
    val Lime = Color(0xFF7DD34F)
    val Grape = Color(0xFF8D6CF2)
    val Bubblegum = Color(0xFFF070AE)
    val Tangerine = Color(0xFFF7923A)
}

/** El color de una tarjeta del escritorio: degradado suave, tinta para escribir y su acento. */
@Immutable
data class Tone(val top: Color, val bottom: Color, val ink: Color, val accent: Color, val name: String)

private val lightTones = listOf(
    Tone(Color(0xF2FBFAFF), Color(0xE6EEEFFB), Color(0xFF2A2D45), Color(0xFF2D5FA8), "Papel"),
    Tone(Color(0xE6C9C3F7), Color(0xD9B7B5F0), Color(0xFF2E2A5C), Color(0xFF5B49C9), "Lavanda"),
    Tone(Color(0xE6BEE8DA), Color(0xD99FD7C6), Color(0xFF1F4A3E), Color(0xFF1E6B54), "Salvia"),
    Tone(Color(0xE6C7DFFA), Color(0xD9AACDF5), Color(0xFF1E3557), Color(0xFF1F63C6), "Cielo"),
    Tone(Color(0xE6FBD5C0), Color(0xD9F8C1A6), Color(0xFF55301E), Color(0xFFD5501E), "Melocotón"),
    Tone(Color(0xF2FBF2C9), Color(0xE6F3E3A6), Color(0xFF4B3F12), Color(0xFFA9821B), "Mantequilla"),
    Tone(Color(0xE6F9CBDC), Color(0xD9F3AFC6), Color(0xFF55203A), Color(0xFFD23D6E), "Rosa"),
    Tone(Color(0xE6D9C9F2), Color(0xD9C4B0EB), Color(0xFF3B2557), Color(0xFF8A3FC8), "Lila"),
)

private val darkTones = listOf(
    Tone(Color(0xCC2B2C44), Color(0xCC222338), Color(0xFFE9E9F7), Color(0xFF8DB6FF), "Papel"),
    Tone(Color(0xCC3A3375), Color(0xCC2C2760), Color(0xFFEDEBFF), Color(0xFFB7A9FF), "Lavanda"),
    Tone(Color(0xCC1F4B43), Color(0xCC173A34), Color(0xFFE3F7F0), Color(0xFF7BE0BE), "Salvia"),
    Tone(Color(0xCC1E3D69), Color(0xCC172F52), Color(0xFFE5F0FF), Color(0xFF8CC0FF), "Cielo"),
    Tone(Color(0xCC5A3324), Color(0xCC47281C), Color(0xFFFFEDE4), Color(0xFFFFA27A), "Melocotón"),
    Tone(Color(0xCC4E4419), Color(0xCC3D3513), Color(0xFFFFF7DA), Color(0xFFF2D06A), "Mantequilla"),
    Tone(Color(0xCC5A2340), Color(0xCC461B32), Color(0xFFFFE8F1), Color(0xFFFF8DB6), "Rosa"),
    Tone(Color(0xCC452A69), Color(0xCC362153), Color(0xFFF3E9FF), Color(0xFFD1A3FF), "Lila"),
)

@Immutable
data class Palette(
    val dark: Boolean,
    val groundTop: Color,
    val groundBottom: Color,
    val blobs: List<Color>,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val heroInk: Color,
    val glassTint: Color,
    val glassBase: Color,
    val rim: Color,
    val scrim: Color,
    val tones: List<Tone>,
) {
    fun tone(i: Int) = tones[((i % tones.size) + tones.size) % tones.size]
}

val LightPalette = Palette(
    dark = false,
    groundTop = Color(0xFFB9CBEE),
    groundBottom = Color(0xFFC6BFEA),
    blobs = listOf(Color(0xFFE3C6F0), Color(0xFFA9D3F5), Color(0xFFD6D0FA), Color(0xFFF5CFE1)),
    ink = Color(0xFF23253A),
    inkSoft = Color(0xFF4E5170),
    inkFaint = Color(0xFF7E81A0),
    heroInk = Color.White,
    glassTint = Color(0x8CE9E9FF),
    glassBase = Color(0xFFCFD2F0),
    rim = Color.White,
    scrim = Color(0x40323060),
    tones = lightTones,
)

val DarkPalette = Palette(
    dark = true,
    groundTop = Color(0xFF151632),
    groundBottom = Color(0xFF1E1638),
    blobs = listOf(Color(0xFF3B2A6E), Color(0xFF173E6B), Color(0xFF2C2366), Color(0xFF55204A)),
    ink = Color(0xFFF1F1FB),
    inkSoft = Color(0xFFC0C1DA),
    inkFaint = Color(0xFF8A8CAA),
    heroInk = Color.White,
    glassTint = Color(0x66302F55),
    glassBase = Color(0xFF1C1C38),
    rim = Color(0xFFBFC3FF),
    scrim = Color(0x80000010),
    tones = darkTones,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

private fun jakarta(w: Int) = Font(R.font.jakarta, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
private fun fraunces(w: Int, italic: Boolean = false) = Font(
    R.font.fraunces, FontWeight(w), if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.Setting("SOFT", 50f), FontVariation.Setting("opsz", 72f)),
)

val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))
val Fraunces = FontFamily(fraunces(600), fraunces(700), fraunces(800), fraunces(900))

/** Estilos de texto: usa el estilo, no un tamaño suelto. */
object Type {
    val Hero = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 54.sp, lineHeight = 56.sp, letterSpacing = (-0.04).em)
    val Display = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em)
    val Headline = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.02).em)
    val Serif = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em)
    val Title = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp)
    val BodyStrong = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val Body = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val Label = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp)
    val Caption = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val Eyebrow = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.12.em)
    val Dot = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.sp, letterSpacing = (-0.02).em)
}

@Composable
fun MiMenteTheme(content: @Composable () -> Unit) {
    val dark = when (Prefs.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(primary = Candy.Sky) else lightColorScheme(primary = Candy.Sky)
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
