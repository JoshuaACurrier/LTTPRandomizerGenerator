using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using LTTPRandomizerGenerator.Models;
using LTTPRandomizerGenerator.Services;
using Microsoft.Win32;

namespace LTTPRandomizerGenerator
{
    public partial class MainWindow : Window, INotifyPropertyChanged
    {
        public MainWindow()
        {
            InitializeComponent();
            DataContext = this;

            LoadPresets();
            RestoreLastSettings();
            _initialized = true;
            TryMatchPreset();
        }

        // ── Preset matching state ─────────────────────────────────────────────

        private bool _suppressPresetApply = false;
        private bool _initialized = false;

        // ── Observable properties ─────────────────────────────────────────────

        private string _romPath = string.Empty;
        public string RomPath
        {
            get => _romPath;
            set { _romPath = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanGenerate)); PresetManager.SavePaths(_romPath, _outputFolder); }
        }

        private string _outputFolder = string.Empty;
        public string OutputFolder
        {
            get => _outputFolder;
            set { _outputFolder = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanGenerate)); PresetManager.SavePaths(_romPath, _outputFolder); }
        }

        private bool _isGenerating;
        public bool IsGenerating
        {
            get => _isGenerating;
            set { _isGenerating = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanGenerate)); }
        }

        private string _statusMessage = string.Empty;
        public string StatusMessage
        {
            get => _statusMessage;
            set { _statusMessage = value; OnPropertyChanged(); }
        }

        private Brush _statusColor = Brushes.Gray;
        public Brush StatusColor
        {
            get => _statusColor;
            set { _statusColor = value; OnPropertyChanged(); }
        }

        private string _seedPermalink = string.Empty;
        public string SeedPermalink
        {
            get => _seedPermalink;
            set { _seedPermalink = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasSeedLink)); }
        }

        public bool HasSeedLink => !string.IsNullOrEmpty(SeedPermalink);

        private string _newPresetName = string.Empty;
        public string NewPresetName
        {
            get => _newPresetName;
            set { _newPresetName = value; OnPropertyChanged(); }
        }

        public bool CanGenerate =>
            !IsGenerating &&
            !string.IsNullOrWhiteSpace(RomPath) &&
            !string.IsNullOrWhiteSpace(OutputFolder);

        private bool _isSettingsExpanded = false;
        public bool IsSettingsExpanded
        {
            get => _isSettingsExpanded;
            set { _isSettingsExpanded = value; OnPropertyChanged(); OnPropertyChanged(nameof(SettingsToggleLabel)); }
        }

        public string SettingsToggleLabel => IsSettingsExpanded ? "▲ RANDOMIZER SETTINGS" : "▶ RANDOMIZER SETTINGS";

        // ── Preset state ──────────────────────────────────────────────────────

        public ObservableCollection<RandomizerPreset> AllPresets { get; } = new();

        private RandomizerPreset? _selectedPreset;
        public RandomizerPreset? SelectedPreset
        {
            get => _selectedPreset;
            set { _selectedPreset = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanDeletePreset)); OnPropertyChanged(nameof(IsPresetUnsaved)); }
        }

        public bool CanDeletePreset => SelectedPreset is { IsBuiltIn: false };
        public bool IsPresetUnsaved => SelectedPreset is null;

        // ── Settings rows (drives the XAML ItemsControl) ─────────────────────

        public ObservableCollection<SettingRow> SettingRows { get; } = new();

        private RandomizerSettings CurrentSettings()
        {
            var s = new RandomizerSettings();
            foreach (var row in SettingRows)
            {
                if (row.SelectedOption is null) continue;
                string v = row.SelectedOption.ApiValue;
                switch (row.FieldKey)
                {
                    case "glitches":             s.Glitches              = v; break;
                    case "item_placement":       s.ItemPlacement         = v; break;
                    case "dungeon_items":        s.DungeonItems          = v; break;
                    case "accessibility":        s.Accessibility         = v; break;
                    case "goal":                 s.Goal                  = v; break;
                    case "tower_open":           s.Crystals.Tower        = v; break;
                    case "ganon_open":           s.Crystals.Ganon        = v; break;
                    case "world_state":          s.Mode                  = v; break;
                    case "entrance_shuffle":     s.Entrances             = v; break;
                    case "boss_shuffle":         s.Enemizer.BossShuffle  = v; break;
                    case "enemy_shuffle":        s.Enemizer.EnemyShuffle = v; break;
                    case "enemy_damage":         s.Enemizer.EnemyDamage  = v; break;
                    case "enemy_health":         s.Enemizer.EnemyHealth  = v; break;
                    case "pot_shuffle":          s.Enemizer.PotShuffle   = v; break;
                    case "hints":                s.Hints                 = v; break;
                    case "weapons":              s.Weapons               = v; break;
                    case "item_pool":            s.Item.Pool             = v; break;
                    case "item_functionality":   s.Item.Functionality    = v; break;
                    case "spoilers":             s.Spoilers              = v; break;
                    case "pegasus_boots":        s.Pseudoboots           = v == "on"; break;
                }
            }
            return s;
        }

        private void ApplySettingsToRows(RandomizerSettings s)
        {
            SetRow("glitches",           s.Glitches);
            SetRow("item_placement",     s.ItemPlacement);
            SetRow("dungeon_items",      s.DungeonItems);
            SetRow("accessibility",      s.Accessibility);
            SetRow("goal",               s.Goal);
            SetRow("tower_open",         s.Crystals.Tower);
            SetRow("ganon_open",         s.Crystals.Ganon);
            SetRow("world_state",        s.Mode);
            SetRow("entrance_shuffle",   s.Entrances);
            SetRow("boss_shuffle",       s.Enemizer.BossShuffle);
            SetRow("enemy_shuffle",      s.Enemizer.EnemyShuffle);
            SetRow("enemy_damage",       s.Enemizer.EnemyDamage);
            SetRow("enemy_health",       s.Enemizer.EnemyHealth);
            SetRow("pot_shuffle",        s.Enemizer.PotShuffle);
            SetRow("hints",              s.Hints);
            SetRow("weapons",            s.Weapons);
            SetRow("item_pool",          s.Item.Pool);
            SetRow("item_functionality", s.Item.Functionality);
            SetRow("spoilers",           s.Spoilers);
            SetRow("pegasus_boots",      s.Pseudoboots ? "on" : "off");
        }

        private void SetRow(string key, string apiValue)
        {
            var row = SettingRows.FirstOrDefault(r => r.FieldKey == key);
            if (row is null) return;
            row.SelectedOption = row.Options.FirstOrDefault(o => o.ApiValue == apiValue)
                               ?? row.Options.FirstOrDefault();
        }

        private void BuildSettingRows()
        {
            SettingRows.Clear();
            SettingRows.Add(new("glitches",           "Glitches",                 SettingsOptions.Glitches));
            SettingRows.Add(new("item_placement",     "Item Placement",           SettingsOptions.ItemPlacement));
            SettingRows.Add(new("dungeon_items",      "Dungeon Items",            SettingsOptions.DungeonItems));
            SettingRows.Add(new("accessibility",      "Accessibility",            SettingsOptions.Accessibility));
            SettingRows.Add(new("goal",               "Goal",                     SettingsOptions.Goal));
            SettingRows.Add(new("tower_open",         "Tower Open (crystals)",    SettingsOptions.CrystalCount));
            SettingRows.Add(new("ganon_open",         "Ganon Open (crystals)",    SettingsOptions.CrystalCount));
            SettingRows.Add(new("world_state",        "World State",              SettingsOptions.WorldState));
            SettingRows.Add(new("entrance_shuffle",   "Entrance Shuffle",         SettingsOptions.EntranceShuffle));
            SettingRows.Add(new("boss_shuffle",       "Boss Shuffle",             SettingsOptions.BossShuffle));
            SettingRows.Add(new("enemy_shuffle",      "Enemy Shuffle",            SettingsOptions.EnemyShuffle));
            SettingRows.Add(new("enemy_damage",       "Enemy Damage",             SettingsOptions.EnemyDamage));
            SettingRows.Add(new("enemy_health",       "Enemy Health",             SettingsOptions.EnemyHealth));
            SettingRows.Add(new("pot_shuffle",        "Pot Shuffle",              SettingsOptions.PotShuffle));
            SettingRows.Add(new("hints",              "Hints",                    SettingsOptions.Hints));
            SettingRows.Add(new("weapons",            "Weapons",                  SettingsOptions.Weapons));
            SettingRows.Add(new("item_pool",          "Item Pool",                SettingsOptions.ItemPool));
            SettingRows.Add(new("item_functionality", "Item Functionality",       SettingsOptions.ItemFunctionality));
            SettingRows.Add(new("spoilers",           "Spoiler Log",              SettingsOptions.Spoilers));
            SettingRows.Add(new("pegasus_boots",      "Pegasus Boots Start",      SettingsOptions.PegasusBoots));

            foreach (var row in SettingRows)
                row.PropertyChanged += OnSettingRowChanged;
        }

        private void OnSettingRowChanged(object? sender, PropertyChangedEventArgs e)
            => TryMatchPreset();

        private void TryMatchPreset()
        {
            if (!_initialized || _suppressPresetApply) return;
            string currentJson = System.Text.Json.JsonSerializer.Serialize(CurrentSettings());
            RandomizerPreset? match = AllPresets.FirstOrDefault(p =>
                System.Text.Json.JsonSerializer.Serialize(p.Settings) == currentJson);
            _suppressPresetApply = true;
            SelectedPreset = match;
            _suppressPresetApply = false;
        }

        // ── Initialization ────────────────────────────────────────────────────

        private void LoadPresets()
        {
            AllPresets.Clear();
            foreach (var p in BuiltInPresets.All)
                AllPresets.Add(p);
            foreach (var p in PresetManager.LoadUserPresets())
                AllPresets.Add(p);

            BuildSettingRows();
        }

        private void RestoreLastSettings()
        {
            var last = PresetManager.LoadLastSettings();
            ApplySettingsToRows(last);

            var (romPath, outputFolder) = PresetManager.LoadPaths();
            if (!string.IsNullOrEmpty(romPath))      _romPath      = romPath;
            if (!string.IsNullOrEmpty(outputFolder)) _outputFolder = outputFolder;
            OnPropertyChanged(nameof(RomPath));
            OnPropertyChanged(nameof(OutputFolder));
            OnPropertyChanged(nameof(CanGenerate));
        }

        // ── Event handlers ────────────────────────────────────────────────────

        private void BrowseRom_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new OpenFileDialog
            {
                Title = "Select ALttP Base ROM",
                Filter = "SNES ROM files (*.sfc;*.smc;*.rom)|*.sfc;*.smc;*.rom|All files (*.*)|*.*",
            };
            if (dlg.ShowDialog() == true) RomPath = dlg.FileName;
        }

        private void BrowseOutput_Click(object sender, RoutedEventArgs e)
        {
            // FolderBrowserDialog not available in WPF by default; use OpenFileDialog trick
            var dlg = new OpenFileDialog
            {
                Title = "Select Output Folder (pick any file in it, or type a path)",
                ValidateNames = false,
                CheckFileExists = false,
                FileName = "Select Folder",
            };
            if (dlg.ShowDialog() == true)
                OutputFolder = Path.GetDirectoryName(dlg.FileName) ?? string.Empty;
        }

        private void PresetCombo_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            if (_suppressPresetApply || SelectedPreset is null) return;
            _suppressPresetApply = true;
            ApplySettingsToRows(SelectedPreset.Settings);
            _suppressPresetApply = false;
            NewPresetName = SelectedPreset.IsBuiltIn ? string.Empty : SelectedPreset.Name;
        }

        private void ToggleSettings_Click(object sender, MouseButtonEventArgs e)
            => IsSettingsExpanded = !IsSettingsExpanded;

        private void SavePreset_Click(object sender, RoutedEventArgs e)
        {
            string name = NewPresetName.Trim();
            string? err = PresetManager.SavePreset(name, CurrentSettings());
            if (err is not null) { ShowStatus(err, isError: true); return; }

            // Refresh list
            LoadPresets();
            _suppressPresetApply = true;
            SelectedPreset = AllPresets.FirstOrDefault(p => p.Name == name);
            _suppressPresetApply = false;
            ShowStatus($"Preset \"{name}\" saved.", isError: false);
        }

        private void DeletePreset_Click(object sender, RoutedEventArgs e)
        {
            if (SelectedPreset is null) return;
            string name = SelectedPreset.Name;
            if (MessageBox.Show($"Delete preset \"{name}\"?", "Confirm",
                    MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;

            string? err = PresetManager.DeletePreset(name);
            if (err is not null) { ShowStatus(err, isError: true); return; }
            LoadPresets();
            ShowStatus($"Preset \"{name}\" deleted.", isError: false);
        }

        private CancellationTokenSource? _cts;

        private async void Generate_Click(object sender, RoutedEventArgs e)
        {
            IsGenerating = true;
            SeedPermalink = string.Empty;
            _cts = new CancellationTokenSource();

            try
            {
                var settings = CurrentSettings();
                PresetManager.SaveLastSettings(settings);

                string boots = settings.Pseudoboots ? "Boots" : "No Boots";
                ShowStatus($"Sending: {settings.Mode} | {settings.Goal} | {boots}", isError: false);

                string? romErr = RomValidator.Validate(RomPath, out byte[] romBytes);
                if (romErr is not null) { ShowStatus(romErr, isError: true); return; }

                var progress = new Progress<string>(msg => ShowStatus(msg, isError: false));
                var seed = await AlttprApiClient.GenerateAsync(settings, progress, _cts.Token);
                if (seed is null) return;

                ShowStatus("Applying patches...", isError: false);
                byte[] output = await Task.Run(() =>
                    BpsPatcher.Apply(romBytes, seed.BpsPatchBytes, seed.DictionaryPatches, seed.RomSizeMb),
                    _cts.Token);

                string outFile = Path.Combine(OutputFolder, $"lttp_rand_{seed.Hash}.sfc");
                await File.WriteAllBytesAsync(outFile, output, _cts.Token);

                SeedPermalink = seed.Permalink;
                ShowStatus($"Done! Saved to: {outFile}", isError: false);
                ShowStatus($"Seed hash: {seed.Hash}   |   {SeedPermalink}", isError: false);
            }
            catch (OperationCanceledException)
            {
                ShowStatus("Cancelled.", isError: false);
            }
            catch (Exception ex)
            {
                ShowStatus($"Error: {ex.Message}", isError: true);
            }
            finally
            {
                IsGenerating = false;
                _cts?.Dispose();
                _cts = null;
            }
        }

        private void SeedLink_Click(object sender, MouseButtonEventArgs e)
        {
            if (!string.IsNullOrEmpty(SeedPermalink))
                Process.Start(new ProcessStartInfo(SeedPermalink) { UseShellExecute = true });
        }

        private void ShowStatus(string message, bool isError)
        {
            StatusMessage = message;
            StatusColor = isError
                ? new SolidColorBrush(Color.FromRgb(0xFF, 0x6B, 0x6B))
                : new SolidColorBrush(Color.FromRgb(0xB0, 0xB0, 0xCC));
        }

        // ── INotifyPropertyChanged ────────────────────────────────────────────

        public event PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged([CallerMemberName] string? name = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    // ── SettingRow helper (one row in the settings grid) ─────────────────────

    public class SettingRow : INotifyPropertyChanged
    {
        public string FieldKey { get; }
        public string Label { get; }
        public DropdownOption[] Options { get; }

        private DropdownOption? _selectedOption;
        public DropdownOption? SelectedOption
        {
            get => _selectedOption;
            set { _selectedOption = value; OnPropertyChanged(); }
        }

        public SettingRow(string fieldKey, string label, DropdownOption[] options)
        {
            FieldKey = fieldKey;
            Label = label;
            Options = options;
            _selectedOption = options.FirstOrDefault();
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged([CallerMemberName] string? name = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
