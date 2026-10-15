param([string]$Base='http://127.0.0.1:8766')
$ErrorActionPreference='Stop'
if((Invoke-WebRequest "$Base/qa-db.php").Content.Trim() -ne 'nro_panel_qa_20261013'){throw 'Refusing non-QA endpoint'}
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$Base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$Base/admin/events.php" -WebSession $session
$token=[regex]::Match($page.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
function Api($body){$body.csrf=$token;$r=Invoke-WebRequest "$Base/admin/events.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json) -WebSession $session -SkipHttpErrorCheck;@{code=[int]$r.StatusCode;body=($r.Content|ConvertFrom-Json)}}
function Check($ok,$name){if(!$ok){throw "FAIL: $name"};Write-Output "PASS: $name"}
$state=(Api @{action='read'}).body
Check ($state.events.Count -eq 8) 'eight supported switches'
$original=[int]($state.events|Where-Object event_key -eq halloween).enabled
$revision=[long]$state.config.revision
Check ((Api @{action='set';event_key='halloween';enabled=($original -eq 0);revision=$revision}).code -eq 200) 'toggle event'
Check ((Api @{action='set';event_key='halloween';enabled=($original -eq 1);revision=$revision}).code -eq 409) 'stale revision rejected'
$state=(Api @{action='read'}).body
Check ([int]($state.events|Where-Object event_key -eq halloween).enabled -eq (1-$original)) 'state persisted'
Check ((Api @{action='set';event_key='top_up';enabled=$true;revision=[long]$state.config.revision}).code -eq 422) 'stub event rejected'
Check ((Api @{action='set';event_key='halloween';enabled='false';revision=[long]$state.config.revision}).code -eq 422) 'string boolean rejected'
$audit=$state.history|Where-Object event_key -eq halloween|Select-Object -First 1
Check ((Api @{action='undo';event_key='halloween';audit_id=[long]$audit.id;revision=[long]$state.config.revision}).code -eq 200) 'undo restores previous switch'
$state=(Api @{action='read'}).body
Check ([int]($state.events|Where-Object event_key -eq halloween).enabled -eq $original) 'undo value persisted'
Check ((Api @{action='undo';event_key='halloween';audit_id=[long]$audit.id;revision=[long]$state.config.revision}).code -eq 409) 'duplicate undo rejected'
Check ((Api @{action='set';event_key='halloween';enabled=($original -eq 1);revision=[long]$state.config.revision}).code -eq 200) 'unchanged save accepted'
$same=(Api @{action='read'}).body
Check ($same.config.revision -eq $state.config.revision) 'unchanged save keeps revision'
$bad=Invoke-WebRequest "$Base/admin/events.php" -Method Post -ContentType application/json -Body '{"action":"read","csrf":"invalid"}' -WebSession $session -SkipHttpErrorCheck
Check ($bad.StatusCode -eq 403) 'CSRF enforced'
$unauth=Invoke-WebRequest "$Base/admin/events.php" -Method Post -ContentType application/json -Body '{}' -SkipHttpErrorCheck
Check ($unauth.StatusCode -eq 403) 'admin authentication enforced'
foreach($event in $same.events){
 $next=([int]$event.enabled -eq 0);$latest=(Api @{action='read'}).body
 Check ((Api @{action='set';event_key=$event.event_key;enabled=$next;revision=[long]$latest.config.revision}).code -eq 200) ("toggle supported event "+$event.event_key)
 $latest=(Api @{action='read'}).body;$last=$latest.history|Select-Object -First 1
 Check ((Api @{action='undo';event_key=$event.event_key;audit_id=[long]$last.id;revision=[long]$latest.config.revision}).code -eq 200) ("restore supported event "+$event.event_key)
}
