package com.mdmac.wallpaperpicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream

/**
 * BitmapFactory (used for both resource drawables and gallery Uris) decodes
 * raw pixel data only — it never looks at the EXIF orientation tag. Several
 * of the bundled wallpapers are stored sideways with a rotation tag telling
 * viewers how to display them correctly; without correcting for that here,
 * every crop/composite downstream works against the wrong dimensions and
 * the result comes out rotated and over-zoomed.
 */
object OrientedBitmapLoader {

    fun fromResource(context: Context, @DrawableRes resId: Int): Bitmap {
        val bytes = context.resources.openRawResource(resId).use { it.readBytes() }
        return decodeAndCorrect(bytes)
    }

    fun fromUri(context: Context, uri: Uri): Bitmap {
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        return decodeAndCorrect(bytes)
    }

    private fun decodeAndCorrect(bytes: ByteArray): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val orientation = ByteArrayInputStream(bytes).use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
        return applyOrientation(bitmap, orientation)
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap // ORIENTATION_NORMAL / ORIENTATION_UNDEFINED — nothing to do
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
