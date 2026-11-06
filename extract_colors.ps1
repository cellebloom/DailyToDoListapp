Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile('C:\Users\celle\Downloads\ChatGPT Image May 21, 2026, 06_55_43 PM.png')
$bmp = New-Object System.Drawing.Bitmap($img)
$w = $bmp.Width
$h = $bmp.Height
Write-Host "Size: $w x $h"

# Sample every 20px across the whole image, find unique non-white colors
$allColors = @{}
for ($x = 0; $x -lt $w; $x += 10) {
    for ($y = 0; $y -lt $h; $y += 10) {
        $p = $bmp.GetPixel($x, $y)
        # Skip near-white and near-black
        if ($p.R -gt 250 -and $p.G -gt 250 -and $p.B -gt 250) { continue }
        if ($p.R -lt 10 -and $p.G -lt 10 -and $p.B -lt 10) { continue }
        $hex = '#{0:X2}{1:X2}{2:X2}' -f $p.R, $p.G, $p.B
        if ($allColors.ContainsKey($hex)) { $allColors[$hex]++ } else { $allColors[$hex] = 1 }
    }
}

Write-Host "`nNon-white/non-black colors found: $($allColors.Count)"
Write-Host "`nTop 30 colors:"
$allColors.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 30 | ForEach-Object {
    Write-Host "  $($_.Key): $($_.Value)"
}

# Also find the most saturated pixels (likely accent colors)
Write-Host "`nMost saturated pixels (likely accents):"
$saturated = @()
for ($x = 0; $x -lt $w; $x += 5) {
    for ($y = 0; $y -lt $h; $y += 5) {
        $p = $bmp.GetPixel($x, $y)
        $sat = [Math]::Max($p.R, $p.G, $p.B) - [Math]::Min($p.R, $p.G, $p.B)
        $saturated += ,@($sat, $p, $x, $y)
    }
}
$saturated | Sort-Object { $_[0] } -Descending | Select-Object -First 20 | ForEach-Object {
    $hex = '#{0:X2}{1:X2}{2:X2}' -f $_[1].R, $_[1].G, $_[1].B
    Write-Host "  $hex at ($($_[2]),$($_[3])) sat=$($_[0])"
}

$bmp.Dispose()
$img.Dispose()
