package com.mimente.app.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Store
import java.io.File

object Sharing {
    fun uriFor(ctx: Context, file: File): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)

    fun textOf(item: Item): String = buildString {
        if (item.title.isNotBlank()) append(item.title).append("\n\n")
        when (item.kind) {
            Kind.TODO -> item.checks.forEach { append(if (it.done) "☑ " else "☐ ").append(it.text).append('\n') }
            Kind.LINK -> { append(item.url.orEmpty()); if (item.text.isNotBlank()) append("\n\n").append(item.text) }
            else -> append(item.text)
        }
    }.trim()

    fun open(ctx: Context, item: Item) {
        val intent = if (item.kind == Kind.LINK) Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
        else {
            val f = Store.fileOf(item) ?: run { Toaster.show("El archivo ya no está disponible"); return }
            Intent(Intent.ACTION_VIEW).setDataAndType(uriFor(ctx, f), item.mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { ctx.startActivity(Intent.createChooser(intent, "Abrir con").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toaster.show("No hay ninguna app para abrir esto") }
    }

    fun share(ctx: Context, item: Item) {
        val f = Store.fileOf(item)
        val send = Intent(Intent.ACTION_SEND)
        if (f != null) {
            send.type = item.mime ?: "*/*"
            send.putExtra(Intent.EXTRA_STREAM, uriFor(ctx, f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            textOf(item).takeIf { it.isNotBlank() }?.let { send.putExtra(Intent.EXTRA_TEXT, it) }
        } else {
            send.type = "text/plain"
            send.putExtra(Intent.EXTRA_TEXT, textOf(item))
        }
        ctx.startActivity(Intent.createChooser(send, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareText(ctx: Context, text: String) {
        ctx.startActivity(
            Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Compartir")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun copy(ctx: Context, text: String) {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Mi Mente", text))
        Toaster.show("Copiado")
    }

    fun clipboard(ctx: Context): String? =
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(ctx)?.toString()?.takeIf { it.isNotBlank() }
}
