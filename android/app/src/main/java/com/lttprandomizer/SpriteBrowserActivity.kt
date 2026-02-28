package com.lttprandomizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpriteBrowserActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SPRITE_PATH    = "sprite_path"
        const val EXTRA_SPRITE_PREVIEW = "sprite_preview"
        const val EXTRA_IS_DEFAULT     = "is_default"
    }

    private lateinit var spriteGrid: RecyclerView
    private lateinit var scrollToTopFab: FloatingActionButton
    private lateinit var cancelFab: FloatingActionButton

    private lateinit var adapter: SpriteAdapter
    private var favorites = mutableSetOf<String>()
    private var allSprites: List<SpriteEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sprite_browser)

        spriteGrid     = findViewById(R.id.spriteGrid)
        scrollToTopFab = findViewById(R.id.scrollToTopFab)
        cancelFab      = findViewById(R.id.cancelFab)

        favorites = PresetManager.loadFavorites(this)

        adapter = SpriteAdapter(
            onSelect = { entry -> selectSprite(entry) },
            onToggleFavorite = { entry -> toggleFavorite(entry) },
            onSearchChanged = { /* filtering handled inside adapter */ },
            onRandomAll = { returnResult(SpriteManager.RANDOM_ALL_SENTINEL) },
            onRandomFav = { returnResult(SpriteManager.RANDOM_FAVORITES_SENTINEL) },
            onDefaultLink = {
                val intent = Intent()
                intent.putExtra(EXTRA_IS_DEFAULT, true)
                setResult(Activity.RESULT_OK, intent)
                finish()
            },
        )

        val layoutManager = GridLayoutManager(this, 4)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (position == 0) 4 else 1  // Header spans all columns
        }
        spriteGrid.layoutManager = layoutManager
        spriteGrid.adapter = adapter

        // Show/hide scroll-to-top FAB based on scroll position
        spriteGrid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible > 0) {
                    scrollToTopFab.show()
                } else {
                    scrollToTopFab.hide()
                }
            }
        })

        scrollToTopFab.setOnClickListener {
            spriteGrid.scrollToPosition(0)
        }

        cancelFab.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        loadSprites()
    }

    private fun loadSprites() {
        adapter.setLoading(true)
        adapter.setSpriteCount("Loading sprites...")

        lifecycleScope.launch {
            try {
                val sprites = withContext(Dispatchers.IO) {
                    SpriteManager.fetchSpriteList(this@SpriteBrowserActivity)
                }
                allSprites = sprites
                adapter.setData(sprites, favorites)
                adapter.setSpriteCount("${sprites.size} sprites")
                adapter.setLoading(false)
            } catch (e: Exception) {
                adapter.setLoading(false)
                adapter.setError("Failed to load sprites:\n${e.message}")
                adapter.setSpriteCount("")
            }
        }
    }

    private fun selectSprite(entry: SpriteEntry) {
        lifecycleScope.launch {
            adapter.setLoading(true)
            adapter.setSpriteCount("Downloading ${entry.name}...")
            try {
                val path = withContext(Dispatchers.IO) {
                    SpriteManager.downloadSprite(this@SpriteBrowserActivity, entry)
                }
                returnResult(path, entry.preview)
            } catch (e: Exception) {
                adapter.setLoading(false)
                adapter.setSpriteCount("Download failed: ${e.message}")
            }
        }
    }

    private fun toggleFavorite(entry: SpriteEntry) {
        if (entry.name in favorites) {
            favorites.remove(entry.name)
        } else {
            favorites.add(entry.name)
        }
        PresetManager.saveFavorites(this, favorites)
        adapter.updateFavorites(favorites)
    }

    private fun returnResult(spritePath: String, previewUrl: String = "") {
        val intent = Intent()
        intent.putExtra(EXTRA_SPRITE_PATH, spritePath)
        intent.putExtra(EXTRA_SPRITE_PREVIEW, previewUrl)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
