$ErrorActionPreference='Stop';$base='http://127.0.0.1:8765'
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$base/admin/drop_rates.php" -WebSession $session
$token=[regex]::Match($page.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
function CallDrop($body){$body.csrf=$token;$response=Invoke-WebRequest "$base/admin/drop_rates.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json) -WebSession $session -SkipHttpErrorCheck;return @{code=[int]$response.StatusCode;body=($response.Content|ConvertFrom-Json)}}
$state=(CallDrop @{action='read'}).body
$save=@{action='group';category='gold';mode='multiplier';value=0;revision=$state.rates.revision}
if((CallDrop $save).code -ne 200){throw 'Group save failed'}
$next=(CallDrop @{action='read'}).body
if([double]$next.groups.gold.value -ne 0 -or [double]$next.groups.crystal.value -ne [double]$state.groups.crystal.value -or [double]$next.rates.drop_multiplier -ne [double]$state.rates.drop_multiplier){throw 'Group changes leaked'}
Write-Output 'PASS: group save is independent of global and other groups'
if((CallDrop $save).code -ne 409){throw 'Stale revision accepted'}
Write-Output 'PASS: concurrent edits are protected'
$save.revision=$next.rates.revision;$save.category='upgrade_stone'
if((CallDrop $save).code -ne 422){throw 'Unsupported group accepted'}
Write-Output 'PASS: unsupported drop branch cannot be configured'
$save.category='gold';$save.value=-1
if((CallDrop $save).code -ne 422){throw 'Invalid value accepted'}
Write-Output 'PASS: invalid multiplier rejected'
$reply=CallDrop @{action='global';value=2;revision=$next.rates.revision}
if($reply.code -ne 200){throw 'Global save failed'}
$final=(CallDrop @{action='read'}).body
if([double]$final.rates.drop_multiplier -ne 2 -or [double]$final.groups.gold.value -ne 0){throw 'Global changed group'}
Write-Output 'PASS: global save retains group settings'
