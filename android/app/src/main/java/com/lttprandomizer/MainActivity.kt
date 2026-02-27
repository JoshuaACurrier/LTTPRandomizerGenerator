package com.lttprandomizer

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lttprandomizer.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var romUri: Uri? = null
    private var outputUri: Uri? = null
    private var lastSeedPermalink: String? = null

    // All presets = built-ins + user presets (rebuilt on load)
    private val allPresets = mutableListOf<RandomizerPreset>()
    private val settingRows = mutableListOf<SettingRowModel>()

    // Guards against feedback loops when applying settings programmatically
    private var suppressPresetApply = false

    // Settings panel collapse state (collapsed by default)
    private var settingsExpanded = false

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // ── File pickers ─────────────────────────────────────────────────────────

    private val pickRom = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            romUri = uri
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            binding.romPathText.text = uri.lastPathSegment ?: uri.toString()
            updateGenerateButton()
        }
    }

    private val pickOutput = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            outputUri = uri
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            binding.outputPathText.text = uri.lastPathSegment ?: uri.toString()
            updateGenerateButton()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buildSettingRows()
        loadPresets()
        restoreLastSettings()
        setupUi()
        tryMatchPreset()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun buildSettingRows() {
        settingRows.clear()
        settingRows += listOf(
            SettingRowModel("glitches",           "Glitches",                 SettingsOptions.glitches),
            SettingRowModel("item_placement",     "Item Placement",           SettingsOptions.itemPlacement),
            SettingRowModel("dungeon_items",      "Dungeon Items",            SettingsOptions.dungeonItems),
            SettingRowModel("accessibility",      "Accessibility",            SettingsOptions.accessibility),
            SettingRowModel("goal",               "Goal",                     SettingsOptions.goal),
            SettingRowModel("tower_open",         "Tower Open (crystals)",    SettingsOptions.crystalCount),
            SettingRowModel("ganon_open",         "Ganon Open (crystals)",    SettingsOptions.crystalCount),
            SettingRowModel("world_state",        "World State",              SettingsOptions.worldState),
            SettingRowModel("entrance_shuffle",   "Entrance Shuffle",         SettingsOptions.entranceShuffle),
            SettingRowModel("boss_shuffle",       "Boss Shuffle",             SettingsOptions.bossShuffle),
            SettingRowModel("enemy_shuffle",      "Enemy Shuffle",            SettingsOptions.enemyShuffle),
            SettingRowModel("enemy_damage",       "Enemy Damage",             SettingsOptions.enemyDamage),
            SettingRowModel("enemy_health",       "Enemy Health",             SettingsOptions.enemyHealth),
            SettingRowModel("pot_shuffle",        "Pot Shuffle",              SettingsOptions.potShuffle),
            SettingRowModel("hints",              "Hints",                    SettingsOptions.hints),
            SettingRowModel("weapons",            "Weapons",                  SettingsOptions.weapons),
            SettingRowModel("item_pool",          "Item Pool",                SettingsOptions.itemPool),
            SettingRowModel("item_functionality", "Item Functionality",       SettingsOptions.itemFunctionality),
            SettingRowModel("spoilers",           "Spoiler Log",              SettingsOptions.spoilers),
            SettingRowModel("pegasus_boots",      "Pegasus Boots Start",      SettingsOptions.pegasusBoots),
        )
    }

    private fun setupUi() {
        // ROM / output pickers
        binding.browseRomBtn.setOnClickListener { pickRom.launch(arrayOf("*/*")) }
        binding.browseOutputBtn.setOnClickListener { pickOutput.launch(null) }

        // Settings rows — inflate with saved indices, then attach listeners
        suppressPresetApply = true
        settingRows.forEach { row ->
            val rowView = layoutInflater.inflate(R.layout.row_setting, binding.settingsContainer, false)
            rowView.findViewById<TextView>(R.id.settingLabel).text = row.label
            val spinner = rowView.findViewById<Spinner>(R.id.settingSpinner)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, row.options)
            spinner.setSelection(row.selectedIndex, false)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    row.selectedIndex = pos
                    tryMatchPreset()
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
            row.spinnerRef = spinner
            binding.settingsContainer.addView(rowView)
        }
        suppressPresetApply = false

        // Settings toggle
        binding.settingsToggle.setOnClickListener {
            settingsExpanded = !settingsExpanded
            binding.settingsContainer.visibility = if (settingsExpanded) View.VISIBLE else View.GONE
            binding.settingsToggle.text = if (settingsExpanded) "▲ RANDOMIZER SETTINGS" else "▶ RANDOMIZER SETTINGS"
        }

        // Preset spinner
        refreshPresetSpinner()
        binding.presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (suppressPresetApply) return
                applyPreset(allPresets[pos])
                val isBuiltIn = pos < BuiltInPresets.all.size
                binding.presetNameEdit.setText(if (isBuiltIn) "" else allPresets[pos].name)
                binding.deletePresetBtn.isEnabled = !isBuiltIn
                binding.unsavedText.visibility = View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.savePresetBtn.setOnClickListener {
            val name = binding.presetNameEdit.text.toString().trim()
            val err = PresetManager.savePreset(this, name, currentSettings())
            if (err != null) showStatus(err, isError = true)
            else {
                loadPresets()
                tryMatchPreset()
                showStatus("Preset \"$name\" saved.", isError = false)
            }
        }

        binding.deletePresetBtn.setOnClickListener {
            val pos = binding.presetSpinner.selectedItemPosition
            if (pos < 0 || pos >= allPresets.size) return@setOnClickListener
            val name = allPresets[pos].name
            AlertDialog.Builder(this)
                .setMessage("Delete preset \"$name\"?")
                .setPositiveButton("Delete") { _, _ ->
                    val err = PresetManager.deletePreset(this, name)
                    if (err != null) showStatus(err, isError = true)
                    else { loadPresets(); showStatus("Deleted \"$name\".", isError = false) }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Generate
        binding.generateBtn.setOnClickListener { generate() }
        binding.seedLinkText.setOnClickListener {
            lastSeedPermalink?.let { url ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        updateGenerateButton()
    }

    // ── Preset helpers ────────────────────────────────────────────────────────

    private fun loadPresets() {
        allPresets.clear()
        allPresets.addAll(BuiltInPresets.all)
        allPresets.addAll(PresetManager.loadUserPresets(this))
        suppressPresetApply = true
        refreshPresetSpinner()
        suppressPresetApply = false
    }

    private fun refreshPresetSpinner() {
        binding.presetSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            allPresets.map { it.name }
        )
    }

    private fun restoreLastSettings() {
        suppressPresetApply = true
        applySettings(PresetManager.loadLastSettings(this))
        suppressPresetApply = false
    }

    private fun applyPreset(preset: RandomizerPreset) {
        suppressPresetApply = true
        applySettings(preset.settings)
        suppressPresetApply = false
    }

    private fun applySettings(s: RandomizerSettings) {
        setRow("glitches",           s.glitches)
        setRow("item_placement",     s.itemPlacement)
        setRow("dungeon_items",      s.dungeonItems)
        setRow("accessibility",      s.accessibility)
        setRow("goal",               s.goal)
        setRow("tower_open",         s.crystals.tower)
        setRow("ganon_open",         s.crystals.ganon)
        setRow("world_state",        s.mode)
        setRow("entrance_shuffle",   s.entrances)
        setRow("boss_shuffle",       s.enemizer.bossShuffle)
        setRow("enemy_shuffle",      s.enemizer.enemyShuffle)
        setRow("enemy_damage",       s.enemizer.enemyDamage)
        setRow("enemy_health",       s.enemizer.enemyHealth)
        setRow("pot_shuffle",        s.enemizer.potShuffle)
        setRow("hints",              s.hints)
        setRow("weapons",            s.weapons)
        setRow("item_pool",          s.item.pool)
        setRow("item_functionality", s.item.functionality)
        setRow("spoilers",           s.spoilers)
        setRow("pegasus_boots",      if (s.pseudoboots) "on" else "off")
    }

    private fun setRow(key: String, apiValue: String) {
        val row = settingRows.firstOrNull { it.key == key } ?: return
        val idx = row.options.indexOfFirst { it.apiValue == apiValue }
        if (idx >= 0) {
            row.selectedIndex = idx
            row.spinnerRef?.setSelection(idx, false)
        }
    }

    private fun tryMatchPreset() {
        if (suppressPresetApply) return
        val currentJson = json.encodeToString(currentSettings())
        val matchIdx = allPresets.indexOfFirst { json.encodeToString(it.settings) == currentJson }
        suppressPresetApply = true
        if (matchIdx >= 0) {
            binding.presetSpinner.setSelection(matchIdx)
            binding.unsavedText.visibility = View.GONE
            val isBuiltIn = matchIdx < BuiltInPresets.all.size
            binding.presetNameEdit.setText(if (isBuiltIn) "" else allPresets[matchIdx].name)
            binding.deletePresetBtn.isEnabled = !isBuiltIn
        } else {
            binding.unsavedText.visibility = View.VISIBLE
            binding.deletePresetBtn.isEnabled = false
        }
        suppressPresetApply = false
    }

    private fun currentSettings(): RandomizerSettings {
        fun v(key: String) = settingRows.firstOrNull { it.key == key }
            ?.let { it.options[it.selectedIndex].apiValue } ?: ""
        return RandomizerSettings(
            glitches      = v("glitches"),
            itemPlacement = v("item_placement"),
            dungeonItems  = v("dungeon_items"),
            accessibility = v("accessibility"),
            goal          = v("goal"),
            crystals      = CrystalsSettings(tower = v("tower_open"), ganon = v("ganon_open")),
            mode          = v("world_state"),
            entrances     = v("entrance_shuffle"),
            hints         = v("hints"),
            weapons       = v("weapons"),
            item          = ItemSettings(pool = v("item_pool"), functionality = v("item_functionality")),
            spoilers      = v("spoilers"),
            pseudoboots   = v("pegasus_boots") == "on",
            enemizer      = EnemizerSettings(
                bossShuffle  = v("boss_shuffle"),
                enemyShuffle = v("enemy_shuffle"),
                enemyDamage  = v("enemy_damage"),
                enemyHealth  = v("enemy_health"),
                potShuffle   = v("pot_shuffle"),
            ),
        )
    }

    // ── Generate ──────────────────────────────────────────────────────────────

    private fun generate() {
        val rom = romUri ?: return
        val output = outputUri ?: return
        val settings = currentSettings()

        PresetManager.saveLastSettings(this, settings)
        setGenerating(true)
        lastSeedPermalink = null
        binding.seedLinkText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val (romErr, romBytes) = withContext(Dispatchers.IO) {
                    RomValidator.validate(this@MainActivity, rom)
                }
                if (romErr != null) { showStatus(romErr, isError = true); return@launch }

                val seed = withContext(Dispatchers.IO) {
                    AlttprApiClient.generate(settings) { msg -> runOnUiThread { showStatus(msg) } }
                }

                showStatus("Applying patches…")
                val patchedRom = withContext(Dispatchers.IO) {
                    BpsPatcher.apply(romBytes, seed.bpsBytes, seed.dictPatches, seed.sizeMb)
                }

                showStatus("Writing output ROM…")
                withContext(Dispatchers.IO) { writeOutput(output, seed.hash, patchedRom) }

                lastSeedPermalink = seed.permalink
                binding.seedLinkText.text = seed.permalink
                binding.seedLinkText.visibility = View.VISIBLE
                showStatus("Done! Seed: ${seed.hash}")
            } catch (e: Exception) {
                showStatus("Error: ${e.message}", isError = true)
            } finally {
                setGenerating(false)
            }
        }
    }

    private fun writeOutput(treeUri: Uri, hash: String, rom: ByteArray) {
        val docTree = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, treeUri)!!
        val file = docTree.createFile("application/octet-stream", "lttp_rand_$hash.sfc")
            ?: throw IllegalStateException("Cannot create output file in selected folder.")
        contentResolver.openOutputStream(file.uri)!!.use { it.write(rom) }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun updateGenerateButton() {
        binding.generateBtn.isEnabled = romUri != null && outputUri != null
    }

    private fun setGenerating(generating: Boolean) {
        binding.progressBar.visibility = if (generating) View.VISIBLE else View.GONE
        binding.generateBtn.isEnabled = !generating && romUri != null && outputUri != null
    }

    private fun showStatus(message: String, isError: Boolean = false) {
        binding.statusText.text = message
        binding.statusText.setTextColor(
            if (isError) 0xFFFF6B6B.toInt() else 0xFFB0B0CC.toInt()
        )
    }
}

// ── SettingRowModel ───────────────────────────────────────────────────────────

data class SettingRowModel(
    val key: String,
    val label: String,
    val options: List<DropdownOption>,
    var selectedIndex: Int = 0,
    var spinnerRef: Spinner? = null,
)
