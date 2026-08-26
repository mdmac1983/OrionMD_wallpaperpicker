package com.mdmac.wallpaperpicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

private const val TYPE_WALLPAPER = 0
private const val TYPE_ADD = 1

class WallpaperThumbAdapter(
    private val wallpapers: List<BundledWallpaper>,
    private val onWallpaperClicked: (BundledWallpaper) -> Unit,
    private val onAddFromGalleryClicked: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedPosition = 0 // index into `wallpapers`; -1 when a gallery image is active

    fun setSelected(position: Int) {
        val previous = selectedPosition
        selectedPosition = position
        if (previous in wallpapers.indices) notifyItemChanged(previous)
        if (position in wallpapers.indices) notifyItemChanged(position)
    }

    override fun getItemCount(): Int = wallpapers.size + 1 // + the "add from gallery" tile

    override fun getItemViewType(position: Int): Int =
        if (position < wallpapers.size) TYPE_WALLPAPER else TYPE_ADD

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_WALLPAPER) {
            WallpaperViewHolder(inflater.inflate(R.layout.item_wallpaper_thumb, parent, false))
        } else {
            AddViewHolder(inflater.inflate(R.layout.item_wallpaper_add, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is WallpaperViewHolder) {
            val wallpaper = wallpapers[position]
            holder.imgThumb.setImageResource(wallpaper.drawableResId)
            holder.selectedBorder.visibility =
                if (position == selectedPosition) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                setSelected(position)
                onWallpaperClicked(wallpaper)
            }
        } else if (holder is AddViewHolder) {
            holder.itemView.setOnClickListener { onAddFromGalleryClicked() }
        }
    }

    private class WallpaperViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)
        val selectedBorder: View = itemView.findViewById(R.id.viewSelectedBorder)
    }

    private class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
