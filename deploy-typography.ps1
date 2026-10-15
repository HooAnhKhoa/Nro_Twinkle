param([string]$SiteRoot='C:\xampp\htdocs', [switch]$Deploy)
$ErrorActionPreference='Stop'
$repoRoot=$PSScriptRoot
$stageRoot=Join-Path $repoRoot 'build\typography-stage'
$backupRoot=Join-Path 'D:\NroFreeSetup\Backups' ('web-typography-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
$link='<link rel="stylesheet" href="/typography.css?v=20261014-1">'
$files=Get-ChildItem -LiteralPath $SiteRoot -Recurse -File -Filter '*.php' | Where-Object { [IO.File]::ReadAllText($_.FullName) -match '</head>' }
$changed=@()
foreach($file in $files) {
    $relative=$file.FullName.Substring($SiteRoot.TrimEnd('\').Length+1)
    $text=[IO.File]::ReadAllText($file.FullName)
    if($text.Contains('/typography.css?')) { continue }
    $updated=$text.Replace('</head>',$link+'</head>')
    $stage=Join-Path $stageRoot $relative
    New-Item -ItemType Directory -Force -Path (Split-Path $stage) | Out-Null
    [IO.File]::WriteAllText($stage,$updated,(New-Object Text.UTF8Encoding($false)))
    & C:\xampp\php\php.exe -l $stage | Out-Null
    if($LASTEXITCODE -ne 0) {
        & C:\xampp\php\php.exe -l $file.FullName 2>&1 | Out-Null
        if($LASTEXITCODE -ne 0) { Write-Warning "Skipped page with pre-existing PHP syntax error: $relative"; continue }
        throw "PHP syntax check failed: $relative"
    }
    $changed+=$relative
}
Write-Output "Prepared and syntax-checked $($changed.Count) pages."
if(!$Deploy) { return }
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
foreach($relative in $changed) {
    $backup=Join-Path $backupRoot $relative
    New-Item -ItemType Directory -Force -Path (Split-Path $backup) | Out-Null
    Copy-Item -LiteralPath (Join-Path $SiteRoot $relative) -Destination $backup
    Copy-Item -LiteralPath (Join-Path $stageRoot $relative) -Destination (Join-Path $SiteRoot $relative)
    $source=Join-Path (Join-Path $repoRoot 'web') $relative
    if(Test-Path -LiteralPath $source) { Copy-Item -LiteralPath (Join-Path $stageRoot $relative) -Destination $source }
}
if(Test-Path -LiteralPath (Join-Path $SiteRoot 'typography.css')) { Copy-Item -LiteralPath (Join-Path $SiteRoot 'typography.css') -Destination (Join-Path $backupRoot 'typography.css') }
Copy-Item -LiteralPath (Join-Path $repoRoot 'web\typography.css') -Destination (Join-Path $SiteRoot 'typography.css')
Write-Output "Deployed. Backup: $backupRoot"
