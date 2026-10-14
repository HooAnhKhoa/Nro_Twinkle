$ErrorActionPreference='Stop';$base='http://127.0.0.1:8765'
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$base/admin/panel.php" -WebSession $session
$token=[regex]::Match($page.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
if(!$token){throw 'Dashboard CSRF token not found'}
$response=Invoke-WebRequest "$base/admin/panel_api.php" -Method Post -ContentType application/json -Body (@{action='overview';csrf=$token}|ConvertTo-Json) -WebSession $session
$data=$response.Content|ConvertFrom-Json
if($data.gold_top[0].id -ne 2 -or $data.gold_top[0].gold_bars -ne 20 -or $data.gold_top[1].gold_bars -ne 10){throw 'Gold leaderboard mismatch'}
Write-Output 'PASS: overview API ranks combined bag and box quantities'
if($data.counts.players -ne 3 -or !$data.updated_at){throw 'Overview counts mismatch'}
Write-Output 'PASS: dashboard counts and timestamp come from database'
$blocked=Invoke-WebRequest "$base/admin/panel_api.php" -Method Post -ContentType application/json -Body '{"action":"overview"}' -SkipHttpErrorCheck
if($blocked.StatusCode -ne 403){throw 'Private dashboard exposed'}
Write-Output 'PASS: ranking requires authenticated admin access'
