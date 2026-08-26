package com.mdmac.wallpaperpicker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

/**
 * Composites the selected wallpaper with an optional custom text overlay,
 * then applies it as the home screen and/or lock screen wallpaper.
 *
 * EXPORT CANVAS: fixed at 600x1024 px (portrait) to match the target
 * Android 10 tablet's actual screen. The bundled wallpapers are landscape
 * (960x800, ratio 1.2) — cropping them to exactly fill a 0.586-ratio
 * portrait canvas would throw away more than half the image width, which
 * is what "zoomed in" was. Instead: the full photo is scaled to fit
 * uncropped (letterboxed), centered, and the leftover top/bottom space is
 * filled with a blurred, edge-to-edge stretched copy of the same photo
 * instead of a hard crop or blank bars.
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
     * Builds the final wallpaper: blurred cover-fill background (no crop
     * math visible to the eye, just soft filler), the full source photo
     * centered on top with no cropping, then [text] bottom-center anchored
     * if it isn't blank.
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

        // Blurred, edge-to-edge background fills any leftover space instead
        // of a hard crop or a blank bar.
        val coverBackground = buildCoverBitmap(source, EXPORT_WIDTH, EXPORT_HEIGHT)
        val blurredBackground = cheapBlur(coverBackground)
        canvas.drawBitmap(blurredBackground, 0f, 0f, null)

        // The actual photo, fully visible and uncropped, centered on top.
        val contained = buildContainBitmap(source, EXPORT_WIDTH, EXPORT_HEIGHT)
        val left = (EXPORT_WIDTH - contained.width) / 2f
        val top = (EXPORT_HEIGHT - contained.height) / 2f
        canvas.drawBitmap(contained, left, top, null)

        if (!text.isNullOrBlank()) {
            drawOverlayText(canvas, text, fontSizeSp, exportDpi)
        }
        return output
    }

    /** Scales/crops [source] to exactly fill [targetW]x[targetH] with no gaps (same as a standard center-crop). */
    private fun buildCoverBitmap(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val dstRatio = targetW.toFloat() / targetH.toFloat()
        val srcRect: Rect = if (srcRatio > dstRatio) {
            val cropWidth = (source.height * dstRatio).toInt()
            val left = (source.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, source.height)
        } else {
            val cropHeight = (source.width / dstRatio).toInt()
            val top = (source.height - cropHeight) / 2
            Rect(0, top, source.width, top + cropHeight)
        }
        val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(source, srcRect, Rect(0, 0, targetW, targetH), null)
        return out
    }

    /** Scales [source] down (never crops) so it fits entirely within [maxW]x[maxH]. */
    private fun buildContainBitmap(source: Bitmap, maxW: Int, maxH: Int): Bitmap {
        val scale = minOf(maxW.toFloat() / source.width, maxH.toFloat() / source.height)
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    /** Cheap, dependency-free blur (no RenderScript/RenderEffect needed): downscale then upscale with bilinear filtering. */
    private fun cheapBlur(bitmap: Bitmap): Bitmap {
        val factor = 0.06f // ~16x downscale, then back up
        val smallW = (bitmap.width * factor).toInt().coerceAtLeast(1)
        val smallH = (bitmap.height * factor).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        return Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
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
