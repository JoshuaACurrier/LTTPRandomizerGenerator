using System;
using System.IO;
using System.Windows;

namespace LTTPRandomizerGenerator
{
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            AppDomain.CurrentDomain.UnhandledException += (_, args) =>
            {
                string log = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "LTTPRandomizerGenerator", "crash.log");
                Directory.CreateDirectory(Path.GetDirectoryName(log)!);
                File.WriteAllText(log, args.ExceptionObject.ToString());
                MessageBox.Show($"Unexpected error. Log saved to:\n{log}", "Crash", MessageBoxButton.OK, MessageBoxImage.Error);
            };
        }
    }
}
