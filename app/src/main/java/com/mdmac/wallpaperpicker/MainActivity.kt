package com.mdmac.wallpaperpicker

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val supportUrl = "https://buymeacoffee.com/mdmac1983"

    private lateinit var imgPreview: ImageView
    private lateinit var adapter: WallpaperThumbAdapter

    /** What's currently shown in the big preview / would be applied if "Set wallpaper" is tapped. */
    private var currentSource: WallpaperSource =
        WallpaperSource.Bundled(BundledWallpapers.all.first())

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                currentSource = WallpaperSource.Gallery(uri)
                adapter.setSelected(-1) // clear bundled-strip selection highlight
                imgPreview.setImageBitmap(OrientedBitmapLoader.fromUri(this, uri))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgPreview = findViewById(R.id.imgPreview)
        val recyclerThumbs = findViewById<RecyclerView>(R.id.recyclerThumbs)
        val btnSetWallpaper = findViewById<android.view.View>(R.id.btnSetWallpaper)

        // Show the first bundled wallpaper by default
        imgPreview.setImageBitmap(
            OrientedBitmapLoader.fromResource(
                this,
                (currentSource as WallpaperSource.Bundled).wallpaper.drawableResId
            )
        )

        adapter = WallpaperThumbAdapter(
            wallpapers = BundledWallpapers.all,
            onWallpaperClicked = { wallpaper ->
                currentSource = WallpaperSource.Bundled(wallpaper)
                imgPreview.setImageBitmap(OrientedBitmapLoader.fromResource(this, wallpaper.drawableResId))
            },
            onAddFromGalleryClicked = { pickImageLauncher.launch("image/*") }
        )
        adapter.setSelected(0)
        recyclerThumbs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerThumbs.adapter = adapter

        btnSetWallpaper.setOnClickListener { promptCustomTextThenApply() }

        findViewById<android.view.View>(R.id.btnSupport).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl)))
        }
    }

    // ---- Set-wallpaper flow: text overlay -> target (home/lock/both) -> apply ----

    private fun promptCustomTextThenApply() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wallpaper_text, null)

        val editText = dialogView.findViewById<EditText>(R.id.editWallpaperText)
        val seekFont = dialogView.findViewById<SeekBar>(R.id.seekFontSize)
        val labelFont = dialogView.findViewById<TextView>(R.id.labelFontSize)
        val seekDpi = dialogView.findViewById<SeekBar>(R.id.seekDpi)
        val labelDpi = dialogView.findViewById<TextView>(R.id.labelDpi)

        val defaultSp = 40
        seekFont.max = WallpaperExportUtil.FONT_SIZE_MAX - WallpaperExportUtil.FONT_SIZE_MIN
        seekFont.progress = defaultSp - WallpaperExportUtil.FONT_SIZE_MIN
        labelFont.text = getString(R.string.font_size_label, defaultSp)
        seekFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                labelFont.text = getString(
                    R.string.font_size_label,
                    progress + WallpaperExportUtil.FONT_SIZE_MIN
                )
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // DPI slider is now 120-170, so progress is offset by EXPORT_DPI_MIN
        // the same way the font-size slider is offset by FONT_SIZE_MIN.
        seekDpi.max = WallpaperExportUtil.EXPORT_DPI_MAX - WallpaperExportUtil.EXPORT_DPI_MIN
        seekDpi.progress = WallpaperExportUtil.DEFAULT_EXPORT_DPI.toInt() - WallpaperExportUtil.EXPORT_DPI_MIN
        labelDpi.text = getString(R.string.export_dpi_label, seekDpi.progress + WallpaperExportUtil.EXPORT_DPI_MIN)
        seekDpi.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                labelDpi.text = getString(
                    R.string.export_dpi_label,
                    progress + WallpaperExportUtil.EXPORT_DPI_MIN
                )
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_add_text_title)
            .setView(dialogView)
            .setPositiveButton("Next") { _, _ ->
                val text = editText.text?.toString()
                val fontSizeSp = (seekFont.progress + WallpaperExportUtil.FONT_SIZE_MIN).toFloat()
                val exportDpi = (seekDpi.progress + WallpaperExportUtil.EXPORT_DPI_MIN).toFloat()
                val finalBitmap = buildFinalBitmapFromCurrentSource(text, fontSizeSp, exportDpi)
                showPreview(finalBitmap)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun buildFinalBitmapFromCurrentSource(
        text: String?,
        fontSizeSp: Float,
        exportDpi: Float
    ): Bitmap {
        val sourceBitmap: Bitmap = when (val source = currentSource) {
            is WallpaperSource.Bundled ->
                OrientedBitmapLoader.fromResource(this, source.wallpaper.drawableResId)
            is WallpaperSource.Gallery ->
                OrientedBitmapLoader.fromUri(this, source.uri)
        }
        return WallpaperExportUtil.buildFinalBitmap(text, fontSizeSp, sourceBitmap, exportDpi)
    }

    // ---- Preview step: shows exactly what will be applied before committing ----

    private fun showPreview(finalBitmap: Bitmap) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wallpaper_preview, null)
        dialogView.findViewById<ImageView>(R.id.imgWallpaperPreview).setImageBitmap(finalBitmap)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_preview_title)
            .setView(dialogView)
            .setPositiveButton(R.string.set_wallpaper) { _, _ -> promptWallpaperTarget(finalBitmap) }
            .setNegativeButton(R.string.action_edit) { _, _ -> promptCustomTextThenApply() }
            .show()
    }

    private fun promptWallpaperTarget(finalBitmap: Bitmap) {
        val options = arrayOf(
            getString(R.string.target_home),
            getString(R.string.target_lock),
            getString(R.string.target_both)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_target_title)
            .setItems(options) { _, which ->
                val target = when (which) {
                    0 -> WallpaperExportUtil.WallpaperTarget.HOME
                    1 -> WallpaperExportUtil.WallpaperTarget.LOCK
                    else -> WallpaperExportUtil.WallpaperTarget.BOTH
                }
                applySelectedWallpaper(finalBitmap, target)
            }
            .show()
    }

    private fun applySelectedWallpaper(finalBitmap: Bitmap, target: WallpaperExportUtil.WallpaperTarget) {
        WallpaperExportUtil.applyWallpaper(this, finalBitmap, target)
        Toast.makeText(this, R.string.wallpaper_set_confirmation, Toast.LENGTH_SHORT).show()
    }
}
