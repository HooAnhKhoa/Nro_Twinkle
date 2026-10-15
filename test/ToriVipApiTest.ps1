param([string]$Base='http://127.0.0.1:8766')
$ErrorActionPreference='Stop'
if((Invoke-WebRequest "$Base/qa-db.php").Content.Trim() -ne 'nro_panel_qa_20261013'){throw 'QA guard'}
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$Base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$Base/admin/toribot.php" -WebSession $session
$token=[regex]::Match($page.Content,'const toriCsrf="([a-f0-9]+)"').Groups[1].Value
if(!$token){throw 'CSRF missing'}
function Api($b){$b.csrf=$token;Invoke-RestMethod "$Base/admin/toribot.php" -Method Post -ContentType application/json -Body ($b|ConvertTo-Json -Depth 25) -WebSession $session}
function Bad($b,$code){try{Api $b|Out-Null;throw 'Unexpected success'}catch{if($_.Exception.Response.StatusCode.value__ -ne $code){throw};Write-Output "PASS: rejected request $code"}}
$before=Api @{action='read'};$original=$before.settings.config
try{
 if($original.tiers.Count-ne 4 -or !$original.tiers[0].items[0].name -or !$original.tiers[0].items[0].icon_id){throw 'Missing reward identity'}
 Write-Output 'PASS: 4 tiers with actual item names and images'
 $search=Api @{action='search';query='1632';page=1};if(!($search.items|Where-Object id -eq 1632)){throw 'ID search'}
 $catalog=Api @{action='search';query='';page=9999};if($catalog.items.Count -gt 24 -or $catalog.page -ne $catalog.pages){throw 'Pagination'}
 Write-Output 'PASS: catalog ID search and clamped pagination'
 $draft=$original|ConvertTo-Json -Depth 25|ConvertFrom-Json;$draft.tiers[0].cost=12345;$draft.tiers[0].vip_days=12;$draft.tiers[0].exp_bonus=42
 $saved=Api @{action='save';config=$draft;revision=[int]$before.settings.revision}
 if($saved.settings.config.tiers[0].cost -ne 12345 -or $saved.settings.config.tiers[0].vip_days-ne 12 -or $saved.settings.config.tiers[0].exp_bonus-ne 42){throw 'Roundtrip'}
 Write-Output 'PASS: price duration EXP roundtrip'
 Bad @{action='save';config=$draft;revision=[int]$before.settings.revision} 409
 $rev=[int]$saved.settings.revision
 foreach($kind in 0..5){$bad=$draft|ConvertTo-Json -Depth 25|ConvertFrom-Json;switch($kind){0{$bad.ends_at=$bad.starts_at}1{$bad.tiers[0].cost=-1}2{$bad.tiers[0].items[0].id=32767}3{$bad.tiers[0].items[0].quantity=0}4{$bad.tiers[0].items[0].options=@(@{id=50;param=1},@{id=50;param=2})}5{$bad.starts_at='2026-02-30T00:00:00'}};Bad @{action='save';config=$bad;revision=$rev} 422}
 $undone=Api @{action='undo';id=[int]$saved.history[0].id;revision=$rev};if($undone.settings.config.tiers[0].cost-ne $original.tiers[0].cost){throw 'Undo failure'}
 Write-Output 'PASS: undo restores previous config as a new revision'
 $bad=Api @{action='read'};if($bad.settings.revision-ne $rev+1){throw 'Unexpected revision'}
 Write-Output 'PASS: validation failures never changed config'
}finally{$latest=Api @{action='read'};Api @{action='save';config=$original;revision=[int]$latest.settings.revision}|Out-Null}