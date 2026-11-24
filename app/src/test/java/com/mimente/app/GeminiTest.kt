package com.mimente.app

import androidx.test.core.app.ApplicationProvider
import com.mimente.app.ai.Gemini
import com.mimente.app.ai.GeminiException
import com.mimente.app.ai.Models
import com.mimente.app.data.Prefs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GeminiTest {
    @Test fun rankPrefersAliasesThenNewestStableFlash() {
        val ranked = Models.rank(listOf("gemini-2.5-pro", "gemini-3.5-flash-lite", "gemini-3-flash-preview", "gemini-3.8-flash", "gemini-2.5-flash", "gemini-flash-latest"))
        assertEquals(listOf("gemini-flash-latest", "gemini-3.8-flash", "gemini-2.5-flash", "gemini-3.5-flash-lite"), ranked.take(4))
        assertEquals("gemini-3-flash-preview", ranked.last())
    }

    /** Contra el servidor real: una clave falsa debe dar un error de clave, no de modelo. */
    @Test fun fakeKeyIsReportedAsKeyProblem() = runBlocking {
        Prefs.init(ApplicationProvider.getApplicationContext())
        val e = runCatching { Gemini.listModels("AIzaFAKE-key-for-test") }.exceptionOrNull()
        assumeTrue("sin red", e is GeminiException)
        e as GeminiException
        println("listModels → ${e.message}")
        assertFalse(e.modelProblem)
        assertTrue(e.message!!.contains("clave"))
        val e2 = runCatching { Models.ensureWorking("AIzaFAKE-key-for-test") }.exceptionOrNull() as GeminiException
        println("ensureWorking → ${e2.message}")
        assertFalse(e2.modelProblem)
    }

    /** Con una clave real (GEMINI_API_KEY) comprueba listado, elección de modelo y streaming. */
    @Test fun realKeyEndToEnd() = runBlocking {
        val key = System.getenv("GEMINI_API_KEY").orEmpty()
        assumeTrue("sin GEMINI_API_KEY", key.isNotBlank())
        Prefs.init(ApplicationProvider.getApplicationContext())
        Prefs.setModel("gemini-2.5-flash")
        val list = Gemini.listModels(key)
        println("modelos: $list")
        val m = Models.ensureWorking(key, list)
        println("modelo elegido: $m")
        var out = ""
        Gemini.stream(key, m, listOf(Gemini.Turn("user", listOf(Gemini.Part.Text("Di hola en una palabra"))))).collect { out = it }
        println("respuesta: $out")
        assertTrue(out.isNotBlank())
    }
}
