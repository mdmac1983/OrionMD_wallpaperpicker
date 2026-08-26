package com.mdmac.wallpaperpicker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
                imgPreview.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgPreview = findViewById(R.id.imgPreview)
        val recyclerThumbs = findViewById<RecyclerView>(R.id.recyclerThumbs)
        val btnSetWallpaper = findViewById<android.view.View>(R.id.btnSetWallpaper)

        // Show the first bundled wallpaper by default
        imgPreview.setImageResource(currentSource.let {
            (it as WallpaperSource.Bundled).wallpaper.drawableResId
        })

        adapter = WallpaperThumbAdapter(
            wallpapers = BundledWallpapers.all,
            onWallpaperClicked = { wallpaper ->
                currentSource = WallpaperSource.Bundled(wallpaper)
                imgPreview.setImageResource(wallpaper.drawableResId)
            },
            onAddFromGalleryClicked = { pickImageLauncher.launch("image/*") }
        )
        adapter.setSelected(0)
        recyclerThumbs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerThumbs.adapter = adapter

        btnSetWallpaper.setOnClickListener { promptCustomTextThenApply() }
    }

    // ---- Set-wallpaper flow: text overlay -> target (home/lock/both) -> apply ----

    private fun promptCustomTextThenApply() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wallpaper_text, null)

        val editText = dialogView.findViewById<EditText>(R.id.editWallpaperText)
        val seekFont = dialogView.findViewById<SeekBar>(R.id.seekFontSize)
        val labelFont = dialogView.findViewById<TextView>(R.id.labelFontSize)
        val seekDpi = dialogView.findViewById<SeekBar>(R.id.seekDpi)
        val labelDpi = dialogView.findViewById<TextView>(R.id.labelDpi)

        val defaultSp = 24
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

        seekDpi.max = WallpaperExportUtil.EXPORT_DPI_MAX
        seekDpi.progress = WallpaperExportUtil.DEFAULT_EXPORT_DPI.toInt()
        labelDpi.text = getString(R.string.export_dpi_label, seekDpi.progress)
        seekDpi.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                labelDpi.text = getString(R.string.export_dpi_label, progress)
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
                val exportDpi = seekDpi.progress.toFloat()
                promptWallpaperTarget(text, fontSizeSp, exportDpi)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun promptWallpaperTarget(text: String?, fontSizeSp: Float, exportDpi: Float) {
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
                applySelectedWallpaper(text, fontSizeSp, exportDpi, target)
            }
            .show()
    }

    private fun applySelectedWallpaper(
        text: String?,
        fontSizeSp: Float,
        exportDpi: Float,
        target: WallpaperExportUtil.WallpaperTarget
    ) {
        val bitmap: Bitmap = when (val source = currentSource) {
            is WallpaperSource.Bundled ->
                BitmapFactory.decodeResource(resources, source.wallpaper.drawableResId)
            is WallpaperSource.Gallery ->
                contentResolver.openInputStream(source.uri).use { BitmapFactory.decodeStream(it) }
        }
        val finalBitmap = WallpaperExportUtil.buildFinalBitmap(text, fontSizeSp, bitmap, exportDpi)
        WallpaperExportUtil.applyWallpaper(this, finalBitmap, target)
        Toast.makeText(this, R.string.wallpaper_set_confirmation, Toast.LENGTH_SHORT).show()
    }
}
