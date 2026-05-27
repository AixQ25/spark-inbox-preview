Add-Type -AssemblyName System.Drawing

$srcPath = "F:\vibe coding\apps\spark-inbox-preview\design\icon_new.png"
$resDir = "F:\vibe coding\apps\spark-inbox-preview\native\app\src\main\res"

$srcImg = [System.Drawing.Image]::FromFile($srcPath)
$w = $srcImg.Width
$h = $srcImg.Height
Write-Host "Source image: ${w}x${h}"

$bmp = New-Object System.Drawing.Bitmap($srcImg)

function Get-Brightness([System.Drawing.Bitmap]$b, [int]$x, [int]$y) {
    $c = $b.GetPixel($x, $y)
    return [int](($c.R + $c.G + $c.B) / 3)
}

# Strategy: scan from each edge toward center, find where brightness drops
# The outer background is slightly brighter (~17-23) than the icon interior (~7-15)
# Look for the first pixel that is significantly darker than the edge average

# Sample edge brightness to establish baseline
$edgeBright = @()
for ($i = 0; $i -lt 20; $i++) {
    $edgeBright += (Get-Brightness $bmp $i ([int]($h/2)))  # left edge
    $edgeBright += (Get-Brightness $bmp ($w-1-$i) ([int]($h/2)))  # right edge
}
$avgEdge = ($edgeBright | Measure-Object -Average).Average
Write-Host "Average edge brightness: $([int]($avgEdge))"

# The threshold: icon interior is darker than edge by this much
$dropThreshold = [int]($avgEdge * 0.65)
Write-Host "Drop threshold: $dropThreshold"

# Scan from left edge rightward
$left = 0
for ($x = 0; $x -lt [int]($w/2); $x++) {
    $b = Get-Brightness $bmp $x ([int]($h/2))
    if ($b -lt $dropThreshold) {
        $left = $x
        break
    }
}

# Scan from right edge leftward
$right = $w - 1
for ($x = $w - 1; $x -ge [int]($w/2); $x--) {
    $b = Get-Brightness $bmp $x ([int]($h/2))
    if ($b -lt $dropThreshold) {
        $right = $x
        break
    }
}

# Scan from top edge downward
$top = 0
for ($y = 0; $y -lt [int]($h/2); $y++) {
    $b = Get-Brightness $bmp ([int]($w/2)) $y
    if ($b -lt $dropThreshold) {
        $top = $y
        break
    }
}

# Scan from bottom edge upward
$bottom = $h - 1
for ($y = $h - 1; $y -ge [int]($h/2); $y--) {
    $b = Get-Brightness $bmp ([int]($w/2)) $y
    if ($b -lt $dropThreshold) {
        $bottom = $y
        break
    }
}

Write-Host "Detected boundaries: left=$left, right=$right, top=$top, bottom=$bottom"
$cropW = $right - $left + 1
$cropH = $bottom - $top + 1
Write-Host "Crop size: ${cropW}x${cropH}"

# Make square, centered
$size = [Math]::Min($cropW, $cropH)
$cropX = $left + [int](($cropW - $size) / 2)
$cropY = $top + [int](($cropH - $size) / 2)
Write-Host "Final crop: x=$cropX, y=$cropY, size=${size}x${size}"

# Crop
$rect = New-Object System.Drawing.Rectangle($cropX, $cropY, $size, $size)
$iconB = New-Object System.Drawing.Bitmap($size, $size)
$g = [System.Drawing.Graphics]::FromImage($iconB)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.DrawImage($bmp, 0, 0, $rect, [System.Drawing.GraphicsUnit]::Pixel)
$g.Dispose()

# Save preview
$iconB.Save("$resDir\drawable\ic_launcher_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "Saved cropped preview"

# Generate mipmap icons
$mipmapSizes = @{
    "mipmap-mdpi"    = 48
    "mipmap-hdpi"    = 72
    "mipmap-xhdpi"   = 96
    "mipmap-xxhdpi"  = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $mipmapSizes.GetEnumerator()) {
    $folder = $entry.Key
    $sizeVal = $entry.Value

    $mipmapDir = Join-Path $resDir $folder
    if (-not (Test-Path $mipmapDir)) {
        New-Item -ItemType Directory -Path $mipmapDir -Force | Out-Null
    }

    $resized = New-Object System.Drawing.Bitmap($sizeVal, $sizeVal)
    $g2 = [System.Drawing.Graphics]::FromImage($resized)
    $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g2.DrawImage($iconB, 0, 0, $sizeVal, $sizeVal)
    $g2.Dispose()

    $outPath = Join-Path $mipmapDir "ic_launcher.png"
    $resized.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $resized.Dispose()
    Write-Host "Created: $outPath (${sizeVal}x${sizeVal})"
}

$iconB.Dispose()
$bmp.Dispose()
$srcImg.Dispose()

Write-Host "Done!"
