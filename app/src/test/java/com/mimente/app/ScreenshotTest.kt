package com.mimente.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.mimente.app.data.AiNote
import com.mimente.app.data.Check
import com.mimente.app.data.Intake
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Prefs
import com.mimente.app.data.Store
import com.mimente.app.ui.AppRoot
import com.mimente.app.ui.ask.AskSession
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val out = "../screenshots"

    @Before
    fun seed() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        Store.init(ctx)
        Prefs.init(ctx)
        Nav.stack.clear(); Nav.stack.add(Screen.Home)
        if (Store.items.value.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val photo = Intake.newMediaFile("jpg").also { f ->
            val b = Bitmap.createBitmap(600, 760, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 600f, 760f, 0xFFF6B26B.toInt(), 0xFF6D5BD0.toInt(), Shader.TileMode.CLAMP) })
            c.drawCircle(420f, 220f, 110f, Paint().apply { color = 0xFFFFE9A8.toInt() })
            c.drawRect(0f, 540f, 600f, 760f, Paint().apply { color = 0xFF2C3E66.toInt() })
            f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }
        Store.add(Item(kind = Kind.LINK, url = "https://www.nature.com/articles/sueno-memoria", title = "Cómo el sueño consolida la memoria",
            gist = "Dormir 7–9 horas fortalece los recuerdos del día.", created = now - 86_400_000L * 3, tone = 3))
        Store.add(Item(kind = Kind.FILE, file = "x.pdf", fileName = "Contrato alquiler 2027.pdf", mime = "application/pdf", size = 482_000,
            title = "Contrato de alquiler", gist = "Vence el 31 de marzo; aviso con 60 días.", created = now - 86_400_000L * 2, tone = 5))
        Store.add(Item(kind = Kind.PHOTO, file = photo.name, mime = "image/jpeg", title = "Atardecer en la playa",
            gist = "Puesta de sol sobre el mar.", created = now - 86_400_000L, tone = 4))
        Store.add(Item(kind = Kind.TODO, title = "Compras", tone = 2, created = now - 7_200_000,
            checks = listOf(Check("Verduras", true), Check("Frutas", true), Check("Café"), Check("Pan integral"))))
        Store.add(Item(kind = Kind.NOTE, title = "Idea del día", text = "La curiosidad es una forma de valentía.", tone = 0, created = now - 3_600_000))
        Store.add(Item(kind = Kind.AUDIO, file = "a.aac", mime = "audio/aac", title = "Reunión con Ana", durationMs = 94_000,
            gist = "Lanzamiento el viernes; Ana prepara el demo.", waveform = List(40) { 0.25f + 0.7f * kotlin.math.abs(kotlin.math.sin(it * 0.55f)) },
            created = now - 600_000, tone = 1, pinned = true,
            ai = listOf(AiNote(action = "Resumir", text = "Se acordó **lanzar el viernes**.\n\n- Ana prepara el demo\n- Revisar precios el jueves\n- Enviar correo a clientes"))))
    }

    @Test fun home() {
        Prefs.setTheme(1)
        rule.setContent { AppRoot() }
        rule.onRoot().captureRoboImage("$out/01-inicio.png")
        rule.onNodeWithContentDescription("Añadir").performClick()
        rule.mainClock.advanceTimeBy(1200)
        rule.onRoot().captureRoboImage("$out/02-capturar.png")
    }

    @Test fun homeDark() {
        Prefs.setTheme(2)
        rule.setContent { AppRoot() }
        rule.onRoot().captureRoboImage("$out/03-inicio-oscuro.png")
        Prefs.setTheme(1)
    }

    @Test fun detail() {
        Prefs.setTheme(1)
        val audio = Store.items.value.first { it.kind == Kind.AUDIO }
        Nav.stack.add(Screen.Detail(audio.id))
        rule.setContent { AppRoot() }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("$out/04-detalle.png")
    }

    @Test fun ask() {
        Prefs.setTheme(1)
        AskSession.messages.clear()
        AskSession.messages += AskSession.Msg(true, "¿Qué tengo pendiente esta semana?")
        AskSession.messages += AskSession.Msg(false, "Esto es lo que tienes pendiente:\n\n- Comprar **café** y **pan integral** [#3]\n- Preparar el lanzamiento del viernes con Ana [#1]\n- Revisar el aviso del contrato antes del 31 de marzo [#5]")
        Nav.stack.add(Screen.Ask)
        rule.setContent { AppRoot() }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("$out/05-preguntar.png")
    }

    @Test fun settings() {
        Prefs.setTheme(1)
        Nav.stack.add(Screen.Settings)
        rule.setContent { AppRoot() }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("$out/06-ajustes.png")
    }

    @Test fun editor() {
        Prefs.setTheme(1)
        Nav.stack.add(Screen.Edit(null, Kind.TODO))
        rule.setContent { AppRoot() }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("$out/07-nueva-lista.png")
    }
}
