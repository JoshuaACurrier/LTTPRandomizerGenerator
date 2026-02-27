package com.lttprandomizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class SpriteAdapter(
    private val onSelect: (SpriteEntry) -> Unit,
    private val onToggleFavorite: (SpriteEntry) -> Unit,
) : RecyclerView.Adapter<SpriteAdapter.ViewHolder>() {

    private var allItems: List<SpriteEntry> = emptyList()
    private var favorites: Set<String> = emptySet()
    private var query: String = ""
    private var displayList: List<SpriteEntry> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val preview: ImageView = view.findViewById(R.id.spritePreview)
        val name: TextView = view.findViewById(R.id.spriteName)
        val author: TextView = view.findViewById(R.id.spriteAuthor)
        val star: TextView = view.findViewById(R.id.starButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sprite_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = displayList[position]
        val isFav = entry.name in favorites

        holder.name.text = entry.name
        holder.author.text = entry.author
        holder.star.text = if (isFav) "\u2605" else "\u2606"

        if (entry.preview.isNotBlank()) {
            holder.preview.load(entry.preview) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }
        } else {
            holder.preview.setImageDrawable(null)
            holder.preview.setBackgroundColor(0xFF1E1E2E.toInt())
        }

        holder.itemView.setOnClickListener { onSelect(entry) }
        holder.star.setOnClickListener { onToggleFavorite(entry) }
    }

    override fun getItemCount(): Int = displayList.size

    fun setData(sprites: List<SpriteEntry>, favs: Set<String>) {
        allItems = sprites
        favorites = favs
        rebuildDisplay()
    }

    fun updateFavorites(favs: Set<String>) {
        favorites = favs
        rebuildDisplay()
    }

    fun filter(text: String) {
        query = text.trim().lowercase()
        rebuildDisplay()
    }

    private fun rebuildDisplay() {
        val filtered = if (query.isEmpty()) allItems else {
            allItems.filter {
                it.name.lowercase().contains(query) ||
                it.author.lowercase().contains(query)
            }
        }
        // Sort: favorites first, then alphabetical by name
        displayList = filtered.sortedWith(compareByDescending<SpriteEntry> { it.name in favorites }
            .thenBy { it.name.lowercase() })
        notifyDataSetChanged()
    }
}
