package com.mdmac.wallpaperpicker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Composites the selected wallpaper with an optional custom text overlay,
 * then applies it as the home screen and/or lock screen wallpaper.
 *
 * EXPORT CANVAS: fixed at 600x1024 px (portrait) to match the target
 * Android 10 tablet's actual screen. The bundled wallpapers are landscape
 * (960x800). To fill the portrait canvas edge-to-edge with no crop and no
 * letterbox/blur bands, the source is rotated 90° counter-clockwise
 * (960x800 -> 800x960, already portrait-shaped) and then stretched
 * (squeezed horizontally, stretched vertically) to exactly 600x1024.
 *
 * [exportDpi] is the virtual density used to convert "1 inch of bottom
 * padding" and the 30-60 font-size slider into actual pixels on that
 * canvas — exposed as its own 120-170 slider so it can be tuned by eye
 * against the real device instead of being hardcoded. 135 is the
 * default/baseline where the chosen sp value maps 1:1 to px.
 */
object WallpaperExportUtil {

    const val EXPORT_WIDTH = 600
    const val EXPORT_HEIGHT = 1024

    const val DEFAULT_EXPORT_DPI = 135f
    const val EXPORT_DPI_MIN = 120
    const val EXPORT_DPI_MAX = 170

    private const val BOTTOM_PADDING_INCHES = 1f // "~1 inch from the bottom"

    private val TEXT_COLOR = Color.parseColor("#CCFFFFFF") // 80% white
    private val SHADOW_COLOR = Color.argb(128, 0, 0, 0)    // slight outer shadow
    private const val SHADOW_BLUR = 1f  // "blur effect 1"
    private const val SHADOW_DX = 0f    // "distance 0" / "direction 0" -> no offset,
    private const val SHADOW_DY = 0f    // just a soft edge

    const val FONT_SIZE_MIN = 30
    const val FONT_SIZE_MAX = 60

    /**
     * Rotates [source] 90° counter-clockwise, stretches it to exactly
     * EXPORT_WIDTH x EXPORT_HEIGHT (full-bleed, no crop, no bars), then
     * draws [text] bottom-center anchored if it isn't blank.
     *
     * @param fontSizeSp value from the 30-60 slider, already clamped by the caller
     * @param exportDpi value from the 120-170 DPI slider; scales both the
     *   bottom padding and the font size. Defaults to 135.
     */
    fun buildFinalBitmap(
        text: String?,
        fontSizeSp: Float,
        source: Bitmap,
        exportDpi: Float = DEFAULT_EXPORT_DPI
    ): Bitmap {
        val output = Bitmap.createBitmap(EXPORT_WIDTH, EXPORT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val rotated = rotate90CounterClockwise(source)
        val stretched = Bitmap.createScaledBitmap(rotated, EXPORT_WIDTH, EXPORT_HEIGHT, true)
        canvas.drawBitmap(stretched, 0f, 0f, null)

        if (!text.isNullOrBlank()) {
            drawOverlayText(canvas, text, fontSizeSp, exportDpi)
        }
        return output
    }

    /** Rotates [source] 90° counter-clockwise (e.g. 960x800 -> 800x960). */
    private fun rotate90CounterClockwise(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(-90f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun drawOverlayText(canvas: Canvas, text: String, fontSizeSp: Float, exportDpi: Float) {
        val clampedSize = fontSizeSp.coerceIn(FONT_SIZE_MIN.toFloat(), FONT_SIZE_MAX.toFloat())
        val densityScale = exportDpi / DEFAULT_EXPORT_DPI

        // Roboto is Android's system default sans-serif face, so no bundled
        // .ttf is required to get it.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = clampedSize * densityScale
            color = TEXT_COLOR
            textAlign = Paint.Align.CENTER
            setShadowLayer(
                SHADOW_BLUR * densityScale.coerceAtLeast(0.01f),
                SHADOW_DX,
                SHADOW_DY,
                SHADOW_COLOR
            )
        }

        val bottomPaddingPx = BOTTOM_PADDING_INCHES * exportDpi
        val x = EXPORT_WIDTH / 2f
        // Anchor point sits bottomPaddingPx above the bottom edge; centering
        // the glyphs' own vertical extent on that point (rather than the raw
        // baseline) keeps the visual gap accurate regardless of font size.
        val anchorY = EXPORT_HEIGHT - bottomPaddingPx
        val baseline = anchorY - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, x, baseline, paint)
    }

    enum class WallpaperTarget { HOME, LOCK, BOTH }

    /** Applies [bitmap] as wallpaper for the chosen [target] (API 24+, fine on Android 10). */
    fun applyWallpaper(context: Context, bitmap: Bitmap, target: WallpaperTarget) {
        val manager = WallpaperManager.getInstance(context)
        val flags = when (target) {
            WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        manager.setBitmap(bitmap, null, true, flags)
    }
}
