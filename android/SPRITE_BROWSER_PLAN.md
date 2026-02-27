# Plan: Android Sprite Browser Feature

## Context

The Windows version of LTTPRandomizerGenerator has a full sprite browser (card grid, search, favorites, disk cache, random all/favorites, ZSPR ROM injection). The Android app needs the same so users can pick a custom Link sprite when generating a ROM. The sprite is applied in-memory after BPS + cosmetic patches, before writing the output file.

---

## New Files to Create

| File | Purpose |
|------|---------|
| `android/app/src/main/java/com/lttprandomizer/SpriteEntry.kt` | `@Serializable` data class for alttpr.com/sprites API |
| `android/app/src/main/java/com/lttprandomizer/SpriteApplier.kt` | Kotlin port of C# SpriteApplier — writes ZSPR data into ROM ByteArray |
| `android/app/src/main/java/com/lttprandomizer/SpriteAdapter.kt` | RecyclerView adapter with search filter, star toggle |
| `android/app/src/main/java/com/lttprandomizer/SpriteBrowserActivity.kt` | Full-screen sprite browser Activity |
| `android/app/src/main/res/layout/activity_sprite_browser.xml` | Browser layout: search + random buttons + RecyclerView + status |
| `android/app/src/main/res/layout/item_sprite_card.xml` | Individual card: preview image + name + author + star button |
| `android/app/src/main/res/layout/row_sprite.xml` | Sprite row for customization section: label + name display + Browse + Clear |

---

## Existing Files to Modify

| File | Change |
|------|--------|
| `android/app/build.gradle` | Add `coil` (image loading) and `recyclerview` dependencies |
| `android/app/src/main/AndroidManifest.xml` | Register `SpriteBrowserActivity` |
| `android/app/src/main/java/com/lttprandomizer/CustomizationSettings.kt` | Add `spritePath: String = ""` field |
| `android/app/src/main/java/com/lttprandomizer/PresetManager.kt` | Add `saveFavorites` / `loadFavorites` (SharedPreferences `Set<String>`) |
| `android/app/src/main/java/com/lttprandomizer/MainActivity.kt` | Sprite property, row inflation, browse/clear handlers, generate hook, save/restore |

---

## Key Technical Details

### Dependencies to add (`app/build.gradle`)
```kotlin
implementation("io.coil-kt:coil:2.7.0")
implementation("androidx.recyclerview:recyclerview:1.3.2")
```
Coil is the standard Kotlin-idiomatic image loader. It handles disk caching of preview images automatically. RecyclerView is needed for the sprite grid.

---

### `SpriteEntry.kt`
```kotlin
@Serializable
data class SpriteEntry(
    val name: String = "",
    val author: String = "",
    val file: String = "",
    val preview: String = "",
    val tags: List<String> = emptyList(),
    val usage: List<String> = emptyList(),
) {
    @Transient var isFavorite: Boolean = false
    val starGlyph get() = if (isFavorite) "★" else "☆"
}
```

---

### `SpriteApplier.kt` (port of `Services/SpriteApplier.cs`)
ROM write addresses (same as Windows):
- `0x80000` — graphics/pixel data
- `0xDD308` — main palette data (`palLength - 4` bytes)
- `0xDEDF5` — gloves palette (last 4 bytes of palette block)

```kotlin
object SpriteApplier {
    fun apply(zspr: ByteArray, rom: ByteArray): String? {
        if (zspr.size < 4) return "File too small"
        val magic = String(zspr, 0, 4, Charsets.US_ASCII)
        if (magic == "ZSPR") {
            if (zspr.size < 21) return "ZSPR header truncated"
            val gfxOffset = readUInt32LE(zspr, 9)
            val gfxLength = readUInt16LE(zspr, 13)
            val palOffset = readUInt32LE(zspr, 15)
            val palLength = readUInt16LE(zspr, 19)
            zspr.copyInto(rom, 0x80000, gfxOffset, gfxOffset + gfxLength)
            if (palLength >= 4) {
                zspr.copyInto(rom, 0xDD308, palOffset, palOffset + palLength - 4)
                zspr.copyInto(rom, 0xDEDF5, palOffset + palLength - 4, palOffset + palLength)
            }
        } else {
            // Legacy .spr — raw gfx only
            if (zspr.size < 0x7000) return "SPR file too small"
            zspr.copyInto(rom, 0x80000, 0, 0x7000)
        }
        return null
    }
    private fun readUInt32LE(b: ByteArray, i: Int) =
        (b[i].toInt() and 0xFF) or ((b[i+1].toInt() and 0xFF) shl 8) or
        ((b[i+2].toInt() and 0xFF) shl 16) or ((b[i+3].toInt() and 0xFF) shl 24)
    private fun readUInt16LE(b: ByteArray, i: Int) =
        (b[i].toInt() and 0xFF) or ((b[i+1].toInt() and 0xFF) shl 8)
}
```

---

### `SpriteAdapter.kt`
- `RecyclerView.Adapter` backed by a mutable filtered list
- `fun filter(query: String)` — updates filtered list and calls `notifyDataSetChanged()`
- `onItemClick: (SpriteEntry) -> Unit` and `onStarClick: (SpriteEntry) -> Unit` callbacks
- Binds preview image via `imageView.load(entry.preview)` (Coil extension)
- Binds star glyph as `TextView.text = entry.starGlyph`

---

### `SpriteBrowserActivity.kt`
Sentinel values (same as Windows):
```kotlin
companion object {
    const val RANDOM_ALL_SENTINEL = "__random_all__"
    const val RANDOM_FAVORITES_SENTINEL = "__random_favorites__"
    const val EXTRA_SPRITE_PATH = "sprite_path"
    const val EXTRA_IS_DEFAULT = "is_default"
}
```

**Data loading pattern** (matches Windows `FetchSpriteListAsync`):
- Cache JSON at `cacheDir/sprites_list.json`
- On load: read from cache unless `forceRefresh = true`
- On cache miss: fetch from `https://alttpr.com/sprites` via OkHttp, save to cache
- On network error with cache present: use cache (offline fallback)

**Favorites:** load via `PresetManager.loadFavorites(this)` → `MutableSet<String>`; save on star toggle

**Sorting:** favorites first (`compareByDescending { it.isFavorite }.thenBy { it.name }`) — re-sort + `notifyDataSetChanged()` after star toggle

**Sprite download:** `cacheDir/zspr_cache/{safeName}.zspr` — download once, reuse

**Return values to `MainActivity`:**
- Specific sprite: `Intent().putExtra(EXTRA_SPRITE_PATH, localPath)`, `setResult(RESULT_OK)`
- Default (reset): `Intent().putExtra(EXTRA_IS_DEFAULT, true)`, `setResult(RESULT_OK)`
- Random All: `Intent().putExtra(EXTRA_SPRITE_PATH, RANDOM_ALL_SENTINEL)`, `setResult(RESULT_OK)`
- Random Fav: `Intent().putExtra(EXTRA_SPRITE_PATH, RANDOM_FAVORITES_SENTINEL)`, `setResult(RESULT_OK)`
- Cancel: `setResult(RESULT_CANCELED)`

**Layout structure** (`activity_sprite_browser.xml`):
- Top: search bar (`EditText`)
- Below search: horizontal row of "? Random All" + "?★ Random Fav" buttons (visible only after list loads)
- Fill: `RecyclerView` with `GridLayoutManager` (3 columns)
- Bottom bar: `↻ Refresh` + status `TextView` (left), `Cancel` + `Select Sprite` buttons (right)
- Loading overlay: `ProgressBar` centered (collapsed once list is loaded)

---

### `PresetManager.kt` additions
```kotlin
private const val KEY_FAVORITES = "sprite_favorites"

fun loadFavorites(context: Context): MutableSet<String> =
    prefs(context).getStringSet(KEY_FAVORITES, emptySet())!!.toMutableSet()

fun saveFavorites(context: Context, favorites: Set<String>) {
    prefs(context).edit().putStringSet(KEY_FAVORITES, favorites).apply()
}
```

---

### `CustomizationSettings.kt` addition
Add one field:
```kotlin
val spritePath: String = "",
```

---

### `MainActivity.kt` additions

**New property:**
```kotlin
private var spritePath: String = ""
val spriteDisplayName get() = when (spritePath) {
    SpriteBrowserActivity.RANDOM_ALL_SENTINEL       -> "Random (any)"
    SpriteBrowserActivity.RANDOM_FAVORITES_SENTINEL -> "Random (favorites)"
    ""                                              -> "Default (Link)"
    else -> File(spritePath).nameWithoutExtension
}
val isRandomSprite get() =
    spritePath == SpriteBrowserActivity.RANDOM_ALL_SENTINEL ||
    spritePath == SpriteBrowserActivity.RANDOM_FAVORITES_SENTINEL
```

**Activity result launcher:**
```kotlin
private val pickSprite = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode != RESULT_OK) return@registerForActivityResult
    val data = result.data ?: return@registerForActivityResult
    spritePath = if (data.getBooleanExtra(SpriteBrowserActivity.EXTRA_IS_DEFAULT, false)) ""
                 else data.getStringExtra(SpriteBrowserActivity.EXTRA_SPRITE_PATH) ?: ""
    updateSpriteRow()
    PresetManager.saveCustomization(this, currentCustomization())
}
```

**Inflate sprite row (in `setupUi()`, before customization rows loop):**
```kotlin
val spriteRowView = layoutInflater.inflate(R.layout.row_sprite, binding.customizationContainer, false)
spriteNameText = spriteRowView.findViewById(R.id.spriteNameText)
spriteRowView.findViewById<Button>(R.id.browseSpriteBtn).setOnClickListener {
    pickSprite.launch(Intent(this, SpriteBrowserActivity::class.java))
}
spriteRowView.findViewById<Button>(R.id.clearSpriteBtn).setOnClickListener {
    spritePath = ""; updateSpriteRow()
    PresetManager.saveCustomization(this, currentCustomization())
}
binding.customizationContainer.addView(spriteRowView)
```

**`currentCustomization()` — include spritePath:**
```kotlin
return CustomizationSettings(
    heartBeepSpeed = cv("heart_beep_speed"),
    heartColor     = cv("heart_color"),
    menuSpeed      = cv("menu_speed"),
    quickSwap      = cv("quick_swap"),
    spritePath     = spritePath,
)
```

**`restoreCustomization()` — restore spritePath:**
```kotlin
val c = PresetManager.loadCustomization(this)
setCustomizationRow(...)   // existing 4 rows
spritePath = c.spritePath
// updateSpriteRow() called after setupUi() completes
```

**`updateSpriteRow()` helper:**
```kotlin
private fun updateSpriteRow() { spriteNameText?.text = spriteDisplayName }
```
(needs `private var spriteNameText: TextView? = null` field in MainActivity)

**`generate()` — apply sprite after cosmetics:**
```kotlin
// After CosmeticPatcher.apply, before writeOutput:
if (spritePath.isNotEmpty()) {
    showStatus("Applying sprite…")
    val resolvedPath = if (isRandomSprite) {
        val favOnly = spritePath == SpriteBrowserActivity.RANDOM_FAVORITES_SENTINEL
        withContext(Dispatchers.IO) { pickRandomSprite(favOnly) }
            ?: run { showStatus("No sprites available for random selection.", isError = true); return@launch }
    } else spritePath
    val zspr = withContext(Dispatchers.IO) { File(resolvedPath).readBytes() }
    val spriteErr = SpriteApplier.apply(zspr, patchedRom)
    if (spriteErr != null) { showStatus("Sprite error: $spriteErr", isError = true); return@launch }
}
```

**`pickRandomSprite(favoritesOnly: Boolean): String?`** (suspending, called from `Dispatchers.IO`):
1. Load `cacheDir/sprites_list.json` or fetch from `https://alttpr.com/sprites`
2. If `favoritesOnly`: filter pool to sprites whose name is in `PresetManager.loadFavorites(this)`
3. If pool is empty → return null
4. Pick random: `pool[Random.nextInt(pool.size)]`
5. Download ZSPR to `cacheDir/zspr_cache/{safeName}.zspr` if not cached
6. Return absolute path, or null on error

---

### `row_sprite.xml` layout
```xml
<LinearLayout orientation="horizontal" gravity="center_vertical" padding="4dp">
    <TextView text="Sprite" textColor="#888899" width="wrap_content" marginEnd="8dp"/>
    <TextView id="@+id/spriteNameText" textColor="#E0E0F0" weight="1" ellipsize="end"/>
    <Button id="@+id/browseSpriteBtn" text="Browse..." backgroundTint="#3A3A55" textColor="#E0E0F0" marginStart="4dp"/>
    <Button id="@+id/clearSpriteBtn" text="Clear" backgroundTint="#3A3A55" textColor="#E0E0F0" marginStart="4dp"/>
</LinearLayout>
```

---

### `item_sprite_card.xml` layout
```xml
<LinearLayout orientation="vertical" width="~110dp" padding="8dp">
    <ImageView id="@+id/spriteImage" width="64dp" height="64dp" scaleType="fitCenter"/>
    <TextView id="@+id/spriteName" textSize="11sp" textStyle="bold" maxLines="1" ellipsize="end"/>
    <TextView id="@+id/spriteAuthor" textSize="10sp" textColor="#888899" maxLines="1" ellipsize="end"/>
    <TextView id="@+id/starBtn" text="☆" textSize="16sp" gravity="end"/>  <!-- clickable star -->
</LinearLayout>
```

---

## Implementation Order

1. `app/build.gradle` — add Coil + RecyclerView
2. `AndroidManifest.xml` — register `SpriteBrowserActivity`
3. `SpriteEntry.kt`
4. `SpriteApplier.kt`
5. `PresetManager.kt` — add `saveFavorites` / `loadFavorites`
6. `CustomizationSettings.kt` — add `spritePath` field
7. `res/layout/item_sprite_card.xml`
8. `res/layout/activity_sprite_browser.xml`
9. `res/layout/row_sprite.xml`
10. `SpriteAdapter.kt`
11. `SpriteBrowserActivity.kt`
12. `MainActivity.kt` — sprite field, launcher, row inflation, generate hook, restore
13. Build and verify 0 errors

---

## Verification

1. Build succeeds with 0 errors
2. App launches → CUSTOMIZATION section shows "Sprite | Default (Link) | Browse... | Clear"
3. Tap Browse → `SpriteBrowserActivity` opens, loads sprites from alttpr.com (or cache)
4. Search filters by name/author; star persists across open/close of browser
5. Tap a sprite → browser closes → sprite name appears in sprite row
6. Close and reopen app → sprite selection is still shown
7. Tap Random All / Random Fav → row shows "Random (any)" / "Random (favorites)"
8. Generate → sprite applied; open ROM in emulator and verify Link's appearance
9. Tap Clear → resets to "Default (Link)"; generate produces unmodified sprite
