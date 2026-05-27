Add-Type -AssemblyName System.Drawing

$srcPath = "F:\vibe coding\apps\spark-inbox-preview\design\icon.png"
$resDir = "F:\vibe coding\apps\spark-inbox-preview\native\app\src\main\res"

# Open source image
$srcImg = [System.Drawing.Image]::FromFile($srcPath)
$w = $srcImg.Width
$h = $srcImg.Height
Write-Host "Source image: ${w}x${h}"

# Image shows 3 icons: A (left), B (center), C (right)
# B is in the center third
$left = [int]($w * 0.33)
$right = [int]($w * 0.67)

# Create a bitmap for the cropped region
$rect = New-Object System.Drawing.Rectangle($left, 0, ($right - $left), $h)
$iconB = New-Object System.Drawing.Bitmap($rect.Width, $rect.Height)
$g = [System.Drawing.Graphics]::FromImage($iconB)
$g.DrawImage($srcImg, 0, 0, $rect, [System.Drawing.GraphicsUnit]::Pixel)
$g.Dispose()

Write-Host "Cropped Icon B: $($iconB.Width)x$($iconB.Height)"

# Save preview
$iconB.Save("$resDir\drawable\ic_launcher_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "Saved preview"

# Create mipmap directories with proper icons
$mipmapSizes = @{
    "mipmap-mdpi"    = 48
    "mipmap-hdpi"    = 72
    "mipmap-xhdpi"   = 96
    "mipmap-xxhdpi"  = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $mipmapSizes.GetEnumerator()) {
    $folder = $entry.Key
    $size = $entry.Value

    $mipmapDir = Join-Path $resDir $folder
    if (-not (Test-Path $mipmapDir)) {
        New-Item -ItemType Directory -Path $mipmapDir -Force | Out-Null
    }

    $resized = New-Object System.Drawing.Bitmap($size, $size)
    $g2 = [System.Drawing.Graphics]::FromImage($resized)
    $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g2.DrawImage($iconB, 0, 0, $size, $size)
    $g2.Dispose()

    $outPath = Join-Path $mipmapDir "ic_launcher.png"
    $resized.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $resized.Dispose()
    Write-Host "Created: $outPath (${size}x${size})"
}

$iconB.Dispose()
$srcImg.Dispose()

Write-Host "Done!"
