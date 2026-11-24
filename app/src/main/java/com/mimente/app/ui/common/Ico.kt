package com.mimente.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import com.mimente.app.data.Kind

/** Iconos de la app en un solo sitio. */
object Ico {
    val Back = Icons.Rounded.ChevronLeft
    val Add = Icons.Rounded.Add
    val Mic = Icons.Rounded.Mic
    val Spark = Icons.Rounded.AutoAwesome
    val Settings = Icons.Rounded.Tune
    val Search = Icons.Rounded.Search
    val Close = Icons.Rounded.Close
    val Check = Icons.Rounded.Check
    val More = Icons.Rounded.MoreHoriz
    val Delete = Icons.Rounded.Delete
    val Share = Icons.Rounded.Share
    val Pin = Icons.Rounded.PushPin
    val Open = Icons.Rounded.OpenInNew
    val Play = Icons.Rounded.PlayArrow
    val Pause = Icons.Rounded.Pause
    val Stop = Icons.Rounded.Stop
    val Camera = Icons.Rounded.PhotoCamera
    val Gallery = Icons.Rounded.PhotoLibrary
    val Attach = Icons.Rounded.AttachFile
    val Paste = Icons.Rounded.ContentPaste
    val Copy = Icons.Rounded.ContentCopy
    val Send = Icons.AutoMirrored.Rounded.Send
    val Edit = Icons.Rounded.Edit
    val Web = Icons.Rounded.Public
    val Key = Icons.Rounded.Key
    val Refresh = Icons.Rounded.Refresh
    val Export = Icons.Rounded.Upload
    val Import = Icons.Rounded.Download
    val Eye = Icons.Rounded.Visibility
    val EyeOff = Icons.Rounded.VisibilityOff

    fun of(kind: Kind) = when (kind) {
        Kind.NOTE -> Icons.AutoMirrored.Rounded.StickyNote2
        Kind.TODO -> Icons.Rounded.Checklist
        Kind.PHOTO -> Icons.Rounded.Image
        Kind.AUDIO -> Icons.Rounded.GraphicEq
        Kind.VIDEO -> Icons.Rounded.Videocam
        Kind.FILE -> Icons.Rounded.Description
        Kind.LINK -> Icons.Rounded.Link
    }
}
