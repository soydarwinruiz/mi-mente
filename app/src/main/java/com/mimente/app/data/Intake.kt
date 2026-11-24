package com.mimente.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

/** Convierte lo que llega (archivos, fotos, texto compartido) en elementos de la mente. */
object Intake {

    fun kindFor(mime: String?): Kind = when {
        mime == null -> Kind.FILE
        mime.startsWith("image/") -> Kind.PHOTO
        mime.startsWith("audio/") -> Kind.AUDIO
        mime.startsWith("video/") -> Kind.VIDEO
        else -> Kind.FILE
    }

    fun newMediaFile(ext: String): File = File(Store.mediaDir, "${UUID.randomUUID()}.$ext")

    /** Copia el contenido de [uri] a la carpeta de la app y crea el elemento. */
    fun importUri(context: Context, uri: Uri, mimeHint: String? = null): Item? = runCatching {
        val cr = context.contentResolver
        var name: String? = null
        var size = 0L
        cr.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
            }
        }
        val ext0 = name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        var mime = cr.getType(uri) ?: mimeHint
        if (mime == null || mime == "application/octet-stream") {
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext0) ?: mime
        }
        val ext = ext0.ifEmpty { MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin" }
        val out = newMediaFile(ext)
        cr.openInputStream(uri)!!.use { input -> out.outputStream().use { input.copyTo(it) } }
        fromFile(context, out, mime, name)
    }.getOrNull()

    /** Crea un elemento a partir de un archivo que ya está en la carpeta media. */
    fun fromFile(context: Context, file: File, mime: String?, name: String? = null, waveform: List<Float> = emptyList()): Item {
        val kind = kindFor(mime)
        var duration = 0L
        if (kind == Kind.AUDIO || kind == Kind.VIDEO) {
            runCatching {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(file.absolutePath)
                    duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
                } finally {
                    r.release()
                }
            }
        }
        val item = Item(
            kind = kind,
            title = if (kind == Kind.FILE) "" else name?.substringBeforeLast('.')?.takeIf { kind != Kind.PHOTO } ?: "",
            file = file.name,
            mime = mime,
            fileName = name ?: file.name,
            size = file.length(),
            durationMs = duration,
            waveform = waveform.ifEmpty { if (kind == Kind.AUDIO) pseudoWave(file.name) else emptyList() },
            tone = (0..7).random(),
        )
        return Store.add(item)
    }

    fun note(text: String, title: String = ""): Item =
        Store.add(Item(kind = Kind.NOTE, title = title, text = text, tone = (0..7).random()))

    fun link(url: String, title: String = "", text: String = ""): Item =
        Store.add(Item(kind = Kind.LINK, url = normalizeUrl(url), title = title, text = text, tone = 3))

    /** Texto compartido: si es un enlace lo guarda como enlace, si no como nota. */
    fun sharedText(text: String, subject: String?): Item {
        val urlMatch = Regex("https?://\\S+").find(text)
        return if (urlMatch != null && text.trim().length - urlMatch.value.length < 140) {
            link(urlMatch.value, subject.orEmpty(), text.replace(urlMatch.value, "").trim())
        } else note(text, subject.orEmpty())
    }

    /** Reduce fotos grandes para que la IA las lea rápido. */
    fun scaledJpeg(file: File, maxSide: Int = 2048): ByteArray? = runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, o)
        var sample = 1
        while (maxOf(o.outWidth, o.outHeight) / sample > maxSide * 2) sample *= 2
        val bmp = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
        val b = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        java.io.ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
    }.getOrNull()

    private fun pseudoWave(seed: String): List<Float> {
        val r = java.util.Random(seed.hashCode().toLong())
        return List(36) { 0.25f + r.nextFloat() * 0.75f }
    }
}
