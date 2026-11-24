package com.mimente.app.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.mimente.app.data.Intake
import java.io.File
import kotlin.math.sqrt

/** Grabadora de voz en AAC (formato que Gemini entiende directamente). */
class Recorder(private val context: Context) {
    private var rec: MediaRecorder? = null
    var file: File? = null
        private set
    private val levels = mutableListOf<Float>()
    var startedAt = 0L
        private set

    fun start() {
        val f = Intake.newMediaFile("aac")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(44100)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioChannels(1)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        rec = r
        file = f
        levels.clear()
        startedAt = System.currentTimeMillis()
    }

    /** Nivel actual 0..1 (y lo guarda para la forma de onda). */
    fun level(): Float {
        val amp = runCatching { rec?.maxAmplitude ?: 0 }.getOrDefault(0)
        val v = sqrt((amp / 32767f).coerceIn(0f, 1f))
        levels += v
        return v
    }

    /** Detiene y devuelve el archivo y una forma de onda de 40 barras. */
    fun stop(): Pair<File, List<Float>>? {
        val r = rec ?: return null
        rec = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        val f = file ?: return null
        if (!ok || f.length() < 512) { f.delete(); return null }
        return f to downsample(levels, 40)
    }

    fun cancel() {
        rec?.let { runCatching { it.stop() }; it.release() }
        rec = null
        file?.delete()
    }

    private fun downsample(src: List<Float>, n: Int): List<Float> {
        if (src.isEmpty()) return List(n) { 0.3f }
        val max = (src.maxOrNull() ?: 1f).coerceAtLeast(0.05f)
        return List(n) { i ->
            val a = i * src.size / n
            val b = ((i + 1) * src.size / n).coerceAtLeast(a + 1).coerceAtMost(src.size)
            (src.subList(a, b).average().toFloat() / max).coerceIn(0.12f, 1f)
        }
    }
}
