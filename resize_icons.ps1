Add-Type -AssemblyName System.Drawing
$source = 'C:\TB Games\ICON.png'
$img = [System.Drawing.Image]::FromFile($source)
$sizes = @{
    'mdpi' = 48
    'hdpi' = 72
    'xhdpi' = 96
    'xxhdpi' = 144
    'xxxhdpi' = 192
}
foreach ($kvp in $sizes.GetEnumerator()) {
    $folder = 'C:\TB Games\app\src\main\res\mipmap-' + $kvp.Key
    if (-not (Test-Path $folder)) { New-Item -ItemType Directory -Force -Path $folder | Out-Null }
    $bmp = New-Object System.Drawing.Bitmap($img, $kvp.Value, $kvp.Value)
    $bmp.Save("\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save("\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}
$img.Dispose()
Remove-Item -Recurse -Force 'C:\TB Games\app\src\main\res\mipmap-anydpi-v26' -ErrorAction SilentlyContinue
