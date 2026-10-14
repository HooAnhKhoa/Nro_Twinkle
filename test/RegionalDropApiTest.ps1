param([string]$Base='http://127.0.0.1:8766')
$ErrorActionPreference='Stop'
if((Invoke-WebRequest "$Base/qa-db.php").Content.Trim() -ne 'nro_panel_qa_20261013'){throw 'Refusing non-QA endpoint'}
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$Base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$Base/admin/drop_rates.php" -WebSession $session
$token=[regex]::Match($page.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
function CallDrop($body){$body.csrf=$token;$r=Invoke-WebRequest "$Base/admin/drop_rates.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json) -WebSession $session -SkipHttpErrorCheck;@{code=[int]$r.StatusCode;body=($r.Content|ConvertFrom-Json)}}
function Check($ok,$name){if(!$ok){throw $name};Write-Output "PASS: $name"}
$initial=(CallDrop @{action='read'}).body
$save=@{action='rule';rule='upgrade_stone.normal';value=7;revision=$initial.rates.revision}
Check ((CallDrop $save).code -eq 200) 'regional percentage saved'
$next=(CallDrop @{action='read'}).body
Check ([double]$next.regional.'upgrade_stone.normal' -eq 7 -and $null -eq $next.regional.'upgrade_stone.cold' -and $next.groups.upgrade_stone.value -eq $initial.groups.upgrade_stone.value -and $next.rates.drop_multiplier -eq $initial.rates.drop_multiplier) 'normal override independent of cold and shared multipliers'
Check ((CallDrop $save).code -eq 409) 'stale regional revision rejected'
foreach($bad in @(-1,101,'wrong')){Check ((CallDrop @{action='rule';rule='gold.cold';value=$bad;revision=$next.rates.revision}).code -eq 422) 'invalid percent rejected'}
Check ((CallDrop @{action='rule';rule='unknown';value=1;revision=$next.rates.revision}).code -eq 422) 'unknown branch rejected'
Check ((CallDrop @{action='rule';rule='upgrade_stone.cold';value=0;revision=$next.rates.revision}).code -eq 200) 'zero override accepted'
$next=(CallDrop @{action='read'}).body
Check ($null -ne $next.regional.'upgrade_stone.cold' -and [double]$next.regional.'upgrade_stone.cold' -eq 0 -and [double]$next.regional.'upgrade_stone.normal' -eq 7) 'zero retained independently'
Check ((CallDrop @{action='inherit';rule='upgrade_stone.normal';revision=$next.rates.revision}).code -eq 200) 'default restore accepted'
$next=(CallDrop @{action='read'}).body
Check ($null -eq $next.regional.'upgrade_stone.normal' -and [double]$next.regional.'upgrade_stone.cold' -eq 0) 'default restore only clears selected branch'
Check ((CallDrop @{action='rule';rule='gem.all';value=0.00012345;revision=$next.rates.revision}).code -eq 200) 'small percentages accepted'
$next=(CallDrop @{action='read'}).body
Check ([Math]::Abs([double]$next.regional.'gem.all' - 0.00012345) -lt 0.000000001) 'eight decimal places preserved'
