package com.lttprandomizer

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import coil.load

class SpriteAdapter(
    private val onSelect: (SpriteEntry) -> Unit,
    private val onToggleFavorite: (SpriteEntry) -> Unit,
    private val onSearchChanged: (String) -> Unit,
    private val onRandomAll: () -> Unit,
    private val onRandomFav: () -> Unit,
    private val onDefaultLink: () -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SPRITE = 1
    }

    private var allItems: List<SpriteEntry> = emptyList()
    private var favorites: Set<String> = emptySet()
    private var query: String = ""
    private var displayList: List<SpriteEntry> = emptyList()

    // Header state
    private var isLoading = false
    private var spriteCountText = ""
    private var errorMessage: String? = null
    private var headerHolder: HeaderViewHolder? = null

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val searchBox: EditText = view.findViewById(R.id.headerSearchBox)
        val spriteCount: TextView = view.findViewById(R.id.headerSpriteCount)
        val loadingSpinner: ProgressBar = view.findViewById(R.id.headerLoadingSpinner)
        val errorText: TextView = view.findViewById(R.id.headerErrorText)
        val randomAllBtn: Button = view.findViewById(R.id.headerRandomAllBtn)
        val randomFavBtn: Button = view.findViewById(R.id.headerRandomFavBtn)
        val defaultLinkBtn: Button = view.findViewById(R.id.headerDefaultLinkBtn)
    }

    class SpriteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val preview: ImageView = view.findViewById(R.id.spritePreview)
        val name: TextView = view.findViewById(R.id.spriteName)
        val author: TextView = view.findViewById(R.id.spriteAuthor)
        val star: TextView = view.findViewById(R.id.starButton)
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_SPRITE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_sprite_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_sprite_card, parent, false)
            SpriteViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            bindHeader(holder)
        } else if (holder is SpriteViewHolder) {
            val entry = displayList[position - 1]
            bindSprite(holder, entry)
        }
    }

    private fun bindHeader(holder: HeaderViewHolder) {
        headerHolder = holder

        holder.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                filter(text)
                onSearchChanged(text)
            }
        })

        holder.randomAllBtn.setOnClickListener { onRandomAll() }
        holder.randomFavBtn.setOnClickListener { onRandomFav() }
        holder.defaultLinkBtn.setOnClickListener { onDefaultLink() }

        // Apply current header state
        updateHeaderState(holder)
    }

    private fun bindSprite(holder: SpriteViewHolder, entry: SpriteEntry) {
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

    override fun getItemCount(): Int = displayList.size + 1

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

    fun setLoading(loading: Boolean) {
        isLoading = loading
        headerHolder?.let { updateHeaderState(it) }
    }

    fun setSpriteCount(count: String) {
        spriteCountText = count
        headerHolder?.let { updateHeaderState(it) }
    }

    fun setError(message: String?) {
        errorMessage = message
        headerHolder?.let { updateHeaderState(it) }
    }

    private fun updateHeaderState(holder: HeaderViewHolder) {
        holder.loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        holder.spriteCount.text = spriteCountText
        if (errorMessage != null) {
            holder.errorText.visibility = View.VISIBLE
            holder.errorText.text = errorMessage
        } else {
            holder.errorText.visibility = View.GONE
        }
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
