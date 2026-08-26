package com.mdmac.wallpaperpicker

import androidx.annotation.DrawableRes

/** One bundled wallpaper shown in the thumbnail strip. */
data class BundledWallpaper(
    val name: String,
    @DrawableRes val drawableResId: Int
)

/** The 12 wallpapers bundled with the app (res/drawable-nodpi). */
object BundledWallpapers {
    val all: List<BundledWallpaper> = listOf(
        BundledWallpaper("architecture", R.drawable.wallpaper_architecture),
        BundledWallpaper("bubblegum", R.drawable.wallpaper_bubblegum),
        BundledWallpaper("canyon", R.drawable.wallpaper_canyon),
        BundledWallpaper("chroma", R.drawable.wallpaper_chroma),
        BundledWallpaper("escape", R.drawable.wallpaper_escape),
        BundledWallpaper("fidelity", R.drawable.wallpaper_fidelity),
        BundledWallpaper("flora", R.drawable.wallpaper_flora),
        BundledWallpaper("kepler", R.drawable.wallpaper_kepler),
        BundledWallpaper("leaf", R.drawable.wallpaper_leaf),
        BundledWallpaper("noir", R.drawable.wallpaper_noir),
        BundledWallpaper("outofthebox", R.drawable.wallpaper_outofthebox),
        BundledWallpaper("work", R.drawable.wallpaper_work)
    )
}

/** Wraps either a bundled drawable or a gallery pick as the active preview source. */
sealed class WallpaperSource {
    data class Bundled(val wallpaper: BundledWallpaper) : WallpaperSource()
    data class Gallery(val uri: android.net.Uri) : WallpaperSource()
}
