package com.mimente.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.mimente.app.ai.AiRunner
import com.mimente.app.data.Intake
import com.mimente.app.data.Item
import com.mimente.app.data.Kind
import com.mimente.app.data.Prefs
import com.mimente.app.data.Store
import com.mimente.app.ui.AppRoot
import com.mimente.app.ui.common.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(applicationContext)
        Prefs.init(applicationContext)
        SingletonImageLoader.setSafe { ctx ->
            ImageLoader.Builder(ctx).components { add(VideoFrameDecoder.Factory()) }.build()
        }
        if (savedInstanceState == null) handle(intent)
        setContent { AppRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) importAll(listOf(uri), intent.type, intent.getStringExtra(Intent.EXTRA_TEXT))
                else intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    val item = Intake.sharedText(text, intent.getStringExtra(Intent.EXTRA_SUBJECT))
                    saved(listOf(item))
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                if (uris.isNotEmpty()) importAll(uris, intent.type, null)
            }
            "com.mimente.app.NEW_NOTE" -> { Nav.home(); Nav.go(Screen.Edit(null, Kind.NOTE)) }
            "com.mimente.app.NEW_VOICE" -> { Nav.home(); Nav.command = "voice" }
            "com.mimente.app.NEW_PHOTO" -> { Nav.home(); Nav.command = "photo" }
            "com.mimente.app.ASK" -> { Nav.home(); Nav.go(Screen.Ask) }
        }
    }

    private fun importAll(uris: List<Uri>, type: String?, text: String?) {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                uris.mapNotNull { Intake.importUri(applicationContext, it, type?.takeIf { t -> !t.contains('*') }) }
            }
            if (items.isEmpty()) { Toaster.show("No se pudo guardar lo compartido"); return@launch }
            if (items.size == 1 && !text.isNullOrBlank()) Store.update(items[0].id) { it.copy(text = text) }
            saved(items)
        }
    }

    private fun saved(items: List<Item>) {
        items.forEach(AiRunner::autoProcess)
        Toaster.show(if (items.size == 1) "Guardado en Mi Mente" else "${items.size} elementos guardados")
        Nav.home()
        if (items.size == 1) Nav.go(Screen.Detail(items[0].id))
    }
}
