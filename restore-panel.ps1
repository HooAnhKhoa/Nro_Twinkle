param([string]$BackupPath = 'D:\NroFreeSetup\Backups\panel-before-20261013')
$ErrorActionPreference = 'Stop'
$backupRoot = (Resolve-Path -LiteralPath $BackupPath).Path
if (!(Test-Path -LiteralPath (Join-Path $backupRoot 'admin/panel.php')) -or !(Test-Path -LiteralPath (Join-Path $backupRoot 'run.bat'))) { throw 'Backup is incomplete.' }
Copy-Item -LiteralPath (Join-Path $backupRoot 'admin') -Destination 'C:\xampp\htdocs' -Recurse -Force
Copy-Item -LiteralPath (Join-Path $backupRoot 'run.bat') -Destination (Join-Path $PSScriptRoot 'run.bat') -Force
Write-Output 'Original PHP panel and startup script restored. Restart the game normally to use the original JAR. Player data and panel audit records were preserved.'
