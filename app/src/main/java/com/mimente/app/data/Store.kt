package com.mimente.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File

/** Todo lo guardado vive en el teléfono: un JSON con los elementos y una carpeta con sus archivos. */
object Store {
    private lateinit var dir: File
    lateinit var mediaDir: File
        private set
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()

    fun init(context: Context) {
        if (::dir.isInitialized) return
        dir = context.filesDir
        mediaDir = File(dir, "media").apply { mkdirs() }
        _items.value = load()
    }

    private fun dataFile() = File(dir, "items.json")

    private fun load(): List<Item> = runCatching {
        val a = JSONArray(dataFile().readText())
        List(a.length()) { Item.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun reload() { _items.value = load() }

    private fun persist() {
        val snapshot = _items.value
        io.launch {
            lock.withLock {
                val tmp = File(dir, "items.json.tmp")
                tmp.writeText(JSONArray().apply { snapshot.forEach { put(it.toJson()) } }.toString())
                tmp.renameTo(dataFile())
            }
        }
    }

    fun get(id: String): Item? = _items.value.firstOrNull { it.id == id }

    fun add(item: Item): Item {
        _items.value = listOf(item) + _items.value
        persist()
        return item
    }

    fun update(id: String, touch: Boolean = true, f: (Item) -> Item) {
        _items.value = _items.value.map {
            if (it.id == id) f(it).let { n -> if (touch) n.copy(updated = System.currentTimeMillis()) else n } else it
        }
        persist()
    }

    fun delete(id: String) {
        get(id)?.file?.let { File(mediaDir, it).delete() }
        _items.value = _items.value.filterNot { it.id == id }
        persist()
    }

    fun fileOf(item: Item): File? = item.file?.let { File(mediaDir, it) }?.takeIf { it.exists() }

    fun replaceAll(list: List<Item>) { _items.value = list; persist() }
}
