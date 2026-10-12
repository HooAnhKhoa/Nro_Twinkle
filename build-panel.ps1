$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$jdkBin = 'C:\Program Files\Java\jdk-17\bin'
$classesPath = Join-Path $projectRoot 'build/panel-runtime'
New-Item -ItemType Directory -Path $classesPath -Force | Out-Null
$sourceList = Join-Path $projectRoot 'build/panel-runtime-sources.txt'
$sources = Get-ChildItem (Join-Path $projectRoot 'src') -Recurse -Filter '*.java' | ForEach-Object { '"'+$_.FullName.Replace('\','/')+'"' }
[IO.File]::WriteAllLines($sourceList,$sources,(New-Object Text.UTF8Encoding($false)))
& "$jdkBin\javac.exe" -encoding UTF-8 -cp "$projectRoot\lib\*" -d $classesPath "@$sourceList"
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed; original JAR remains unchanged.' }
$outputJar = Join-Path $projectRoot 'Teamobi2026-panel.jar'
Copy-Item -LiteralPath (Join-Path $projectRoot 'Teamobi2026.jar') -Destination $outputJar -Force
& "$jdkBin\jar.exe" uf $outputJar -C $classesPath nro
if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed; original JAR remains unchanged.' }
Write-Output "Built $outputJar"
