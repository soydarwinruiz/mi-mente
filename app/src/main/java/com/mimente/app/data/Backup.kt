package com.mimente.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Copia de seguridad: un .zip con items.json y la carpeta media. */
object Backup {
    fun export(context: Context, target: Uri) {
        context.contentResolver.openOutputStream(target)!!.use { os ->
            ZipOutputStream(os).use { zip ->
                zip.putNextEntry(ZipEntry("items.json"))
                zip.write(JSONArray().apply { Store.items.value.forEach { put(it.toJson()) } }.toString().toByteArray())
                zip.closeEntry()
                Store.mediaDir.listFiles()?.forEach { f ->
                    zip.putNextEntry(ZipEntry("media/${f.name}"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /** Añade lo que hay en la copia a lo que ya existe (no borra nada). Devuelve cuántos elementos llegaron. */
    fun import(context: Context, source: Uri): Int {
        var incoming: List<Item> = emptyList()
        context.contentResolver.openInputStream(source)!!.use { ins ->
            ZipInputStream(ins).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    when {
                        e.name == "items.json" -> {
                            val a = JSONArray(zip.readBytes().decodeToString())
                            incoming = List(a.length()) { Item.fromJson(a.getJSONObject(it)) }
                        }
                        e.name.startsWith("media/") && !e.isDirectory -> {
                            val name = File(e.name).name
                            if (name.isNotBlank() && !name.contains("..")) {
                                File(Store.mediaDir, name).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                }
            }
        }
        val existing = Store.items.value.map { it.id }.toSet()
        val fresh = incoming.filter { it.id !in existing }
        Store.replaceAll((Store.items.value + fresh).sortedByDescending { it.created })
        return fresh.size
    }
}
