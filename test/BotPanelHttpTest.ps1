param([string]$Base='http://127.0.0.1:8767')
$ErrorActionPreference='Stop';$script:checks=0
function Check($ok,$text){if(!$ok){throw $text};$script:checks++;Write-Output "PASS: $text"}
function Post($body,$session){Invoke-WebRequest "$Base/admin/bots.php" -Method Post -WebSession $session -ContentType 'application/json' -Body ($body|ConvertTo-Json -Compress) -SkipHttpErrorCheck}
$anon=[Microsoft.PowerShell.Commands.WebRequestSession]::new();$r=Post @{action='read';csrf='invalid'} $anon;Check ($r.StatusCode -eq 403) 'anonymous read forbidden'
$session=[Microsoft.PowerShell.Commands.WebRequestSession]::new();$page=Invoke-WebRequest "$Base/qa-login.php" -WebSession $session -SkipHttpErrorCheck
Check ($page.StatusCode -eq 200 -and $page.Content.Contains('Bot tự động')) 'authenticated admin page loads'
$csrf=([regex]::Match($page.Content,'const botCsrf=("[^"]+")').Groups[1].Value | ConvertFrom-Json)
$r=Post @{action='read';csrf='wrong'} $session;Check ($r.StatusCode -eq 403) 'CSRF mismatch rejected'
$r=Post @{action='read';csrf=$csrf} $session;Check ($r.StatusCode -eq 200) 'admin read succeeds';$data=$r.Content|ConvertFrom-Json
Check ($data.config.farm -eq 2 -and $data.config.hunt -eq 1 -and $data.runtime.bots.Count -eq 3) 'config and runtime delivered'
Check ($data.runtime.bots[0].map_name.Length -gt 0) 'map names resolved from database'
$rev=$data.config.revision
$r=Post @{action='save';csrf=$csrf;enabled=$true;farm=3;hunt=1;revision=$rev} $session;Check ($r.StatusCode -eq 422) 'over three bots rejected'
$r=Post @{action='save';csrf=$csrf;enabled=$true;farm=1;hunt=1;revision=$rev} $session;Check ($r.StatusCode -eq 200) 'save two bots accepted';$saved=$r.Content|ConvertFrom-Json
Check ($saved.config.farm -eq 1 -and $saved.config.hunt -eq 1 -and $saved.can_undo) 'saved counts and undo returned'
$r=Post @{action='save';csrf=$csrf;enabled=$false;farm=2;hunt=1;revision=$rev} $session;Check ($r.StatusCode -eq 409) 'stale revision rejected over HTTP'
$r=Post @{action='undo';csrf=$csrf;revision=$saved.config.revision} $session;Check ($r.StatusCode -eq 200) 'undo accepted';$restored=$r.Content|ConvertFrom-Json
Check ($restored.config.farm -eq 2 -and $restored.config.hunt -eq 1 -and !$restored.can_undo) 'undo restored original counts exactly once'
$r=Post @{action='save';csrf=$csrf;enabled=$false;farm=2;hunt=1;revision=$restored.config.revision} $session;Check ($r.StatusCode -eq 200) 'stop accepted';$off=$r.Content|ConvertFrom-Json
Check (!$off.config.enabled -and $off.config.farm -eq 2 -and $off.config.hunt -eq 1) 'stop keeps next-start counts'
$other=[Microsoft.PowerShell.Commands.WebRequestSession]::new();$null=Invoke-WebRequest "$Base/qa-login.php?nonadmin=1" -WebSession $other -SkipHttpErrorCheck
$r=Post @{action='save';csrf=$csrf;enabled=$true;farm=1;hunt=1;revision=$off.config.revision} $other;Check ($r.StatusCode -eq 403) 'non-admin cannot change bots'
Write-Output "PASS: $script:checks bot panel HTTP checks"
