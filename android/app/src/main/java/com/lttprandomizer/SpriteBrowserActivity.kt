package com.lttprandomizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var searchBox: EditText
    private lateinit var spriteCount: TextView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var errorText: TextView

    private lateinit var adapter: SpriteAdapter
    private var favorites = mutableSetOf<String>()
    private var allSprites: List<SpriteEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sprite_browser)

        spriteGrid     = findViewById(R.id.spriteGrid)
        searchBox      = findViewById(R.id.searchBox)
        spriteCount    = findViewById(R.id.spriteCount)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        errorText      = findViewById(R.id.errorText)

        favorites = PresetManager.loadFavorites(this)

        adapter = SpriteAdapter(
            onSelect = { entry -> selectSprite(entry) },
            onToggleFavorite = { entry -> toggleFavorite(entry) },
        )

        spriteGrid.layoutManager = GridLayoutManager(this, 4)
        spriteGrid.adapter = adapter

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { adapter.filter(s?.toString() ?: "") }
        })

        // Random All
        findViewById<Button>(R.id.randomAllBtn).setOnClickListener {
            returnResult(SpriteManager.RANDOM_ALL_SENTINEL)
        }

        // Random Favorites
        findViewById<Button>(R.id.randomFavBtn).setOnClickListener {
            returnResult(SpriteManager.RANDOM_FAVORITES_SENTINEL)
        }

        // Default Link
        findViewById<Button>(R.id.defaultLinkBtn).setOnClickListener {
            val intent = Intent()
            intent.putExtra(EXTRA_IS_DEFAULT, true)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        // Cancel
        findViewById<Button>(R.id.cancelBtn).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        loadSprites()
    }

    private fun loadSprites() {
        loadingSpinner.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        spriteCount.text = "Loading sprites..."

        lifecycleScope.launch {
            try {
                val sprites = withContext(Dispatchers.IO) {
                    SpriteManager.fetchSpriteList(this@SpriteBrowserActivity)
                }
                allSprites = sprites
                adapter.setData(sprites, favorites)
                spriteCount.text = "${sprites.size} sprites"
                loadingSpinner.visibility = View.GONE
            } catch (e: Exception) {
                loadingSpinner.visibility = View.GONE
                errorText.visibility = View.VISIBLE
                errorText.text = "Failed to load sprites:\n${e.message}"
                spriteCount.text = ""
            }
        }
    }

    private fun selectSprite(entry: SpriteEntry) {
        lifecycleScope.launch {
            loadingSpinner.visibility = View.VISIBLE
            spriteCount.text = "Downloading ${entry.name}..."
            try {
                val path = withContext(Dispatchers.IO) {
                    SpriteManager.downloadSprite(this@SpriteBrowserActivity, entry)
                }
                returnResult(path, entry.preview)
            } catch (e: Exception) {
                loadingSpinner.visibility = View.GONE
                spriteCount.text = "Download failed: ${e.message}"
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
