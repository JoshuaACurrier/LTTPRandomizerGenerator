using System;
using System.IO;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Imaging;

namespace LTTPRandomizerGenerator.Controls;

/// <summary>
/// Displays a sprite preview image with an animated triforce loading indicator.
/// Downloads via HttpClient (not BitmapImage.UriSource) so HTTPS works reliably in .NET 8.
/// Preview images are cached to disk so subsequent opens load instantly and work offline.
/// </summary>
public partial class SpriteImageControl : UserControl
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(15) };

    private static readonly string PreviewCacheDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "LTTPRandomizerGenerator", "SpriteCache", "Previews");

    public static readonly DependencyProperty ImageUrlProperty =
        DependencyProperty.Register(
            nameof(ImageUrl), typeof(string), typeof(SpriteImageControl),
            new PropertyMetadata(null, OnImageUrlChanged));

    public string? ImageUrl
    {
        get => (string?)GetValue(ImageUrlProperty);
        set => SetValue(ImageUrlProperty, value);
    }

    private static void OnImageUrlChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        => ((SpriteImageControl)d).LoadImage(e.NewValue as string);

    // Per-control CTS so a URL change cancels the in-flight download
    private CancellationTokenSource? _cts;

    public SpriteImageControl()
    {
        InitializeComponent();
    }

    private async void LoadImage(string? url)
    {
        _cts?.Cancel();
        _cts?.Dispose();
        var cts = new CancellationTokenSource();
        _cts = cts;

        SpriteImage.Source = null;
        TriforceCanvas.Visibility = Visibility.Visible;

        if (string.IsNullOrEmpty(url)) return;

        try
        {
            byte[] bytes;
            bool isWeb = url.StartsWith("http", StringComparison.OrdinalIgnoreCase);

            if (isWeb)
            {
                var cachePath = GetPreviewCachePath(url);
                if (File.Exists(cachePath))
                {
                    bytes = await File.ReadAllBytesAsync(cachePath, cts.Token);
                }
                else
                {
                    bytes = await Http.GetByteArrayAsync(url, cts.Token);
                    _ = SavePreviewAsync(cachePath, bytes);
                }
            }
            else
            {
                bytes = await File.ReadAllBytesAsync(url, cts.Token);
            }

            if (cts.IsCancellationRequested) return;

            var bmp = new BitmapImage();
            using var ms = new MemoryStream(bytes);
            bmp.BeginInit();
            bmp.DecodePixelWidth = 64;
            bmp.StreamSource = ms;
            bmp.CacheOption = BitmapCacheOption.OnLoad;
            bmp.EndInit();
            bmp.Freeze();

            if (cts.IsCancellationRequested) return;

            SpriteImage.Source = bmp;
            TriforceCanvas.Visibility = Visibility.Collapsed;
        }
        catch (OperationCanceledException) { }
        catch { /* Keep triforce visible on error */ }
    }

    private static string GetPreviewCachePath(string url)
    {
        try
        {
            var fileName = Path.GetFileName(new Uri(url).LocalPath);
            if (string.IsNullOrEmpty(fileName))
                fileName = Math.Abs(url.GetHashCode()).ToString("x8") + ".png";
            fileName = string.Concat(fileName.Split(Path.GetInvalidFileNameChars()));
            return Path.Combine(PreviewCacheDir, fileName);
        }
        catch
        {
            return Path.Combine(PreviewCacheDir, Math.Abs(url.GetHashCode()).ToString("x8") + ".png");
        }
    }

    private static async Task SavePreviewAsync(string path, byte[] bytes)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            await File.WriteAllBytesAsync(path, bytes);
        }
        catch { }
    }
}
