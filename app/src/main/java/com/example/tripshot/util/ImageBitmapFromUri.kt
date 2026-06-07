package com.example.tripshot.util

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Decodes a local content [Uri] (e.g. from FileProvider or the photo picker) into an
 * [ImageBitmap] suitable for Compose [androidx.compose.foundation.Image]. Returns null while
 * decoding is in progress or if [uri] is null.
 *
 * Uses [android.graphics.ImageDecoder] on API 28+ and the deprecated
 * [MediaStore.Images.Media.getBitmap] below that (minSdk 26).
 */
@Composable
fun rememberImageBitmapFromUri(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val imageBitmapState by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = uri?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(contentResolver, it)
                android.graphics.ImageDecoder.decodeBitmap(source).asImageBitmap()
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, it).asImageBitmap()
            }
        }
    }

    return imageBitmapState
}
