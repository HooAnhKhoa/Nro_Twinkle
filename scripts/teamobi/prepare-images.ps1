param([string]$Archive='C:\Users\THUY HIEN\Downloads\TeaMobi-anh.zip',[string]$Output='build/teamobi-import/assets')
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.Drawing
foreach($zoom in 1..4){New-Item -ItemType Directory -Force -Path (Join-Path $Output "x$zoom")|Out-Null}
$zip=[IO.Compression.ZipFile]::OpenRead($Archive)
$count=0
try {foreach($entry in $zip.Entries){if($entry.FullName -notmatch '/Icons/(\d+)\.png$'){continue};$id=$Matches[1];$stream=$entry.Open();try{$image=[Drawing.Image]::FromStream($stream);try{
 [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,(Join-Path $Output "x4/$id.png"),$true)
 foreach($zoom in 1..3){$width=[Math]::Max(1,[Math]::Ceiling($image.Width*$zoom/4));$height=[Math]::Max(1,[Math]::Ceiling($image.Height*$zoom/4));$bitmap=New-Object Drawing.Bitmap $width,$height;$graphics=[Drawing.Graphics]::FromImage($bitmap);try{$graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half;$graphics.DrawImage($image,0,0,$width,$height);$bitmap.Save((Join-Path $Output "x$zoom/$id.png"),[Drawing.Imaging.ImageFormat]::Png)}finally{$graphics.Dispose();$bitmap.Dispose()}}
 }finally{$image.Dispose()}}finally{$stream.Dispose()};$count++;if($count%2000 -eq 0){Write-Output "Prepared $count icons at x1-x4"}}
}finally{$zip.Dispose()}
Write-Output "Prepared $count canonical icons at all four zoom levels."
