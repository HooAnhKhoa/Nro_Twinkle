param([string]$Base='http://127.0.0.1:8766')
$ErrorActionPreference='Stop'
if((Invoke-WebRequest "$Base/qa-db.php").Content.Trim() -ne 'nro_panel_qa_20261013'){throw 'QA database only'}
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$Base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$Base/admin/boss.php" -WebSession $session
$token=[regex]::Match($page.Content,'const bossCsrf="([a-f0-9]+)"').Groups[1].Value
if(!$token){throw 'Missing boss CSRF'}
function Api($body){$body.csrf=$token;Invoke-RestMethod "$Base/admin/boss.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json -Depth 12) -WebSession $session}
function ExpectError($body,$status,$label){try{Api $body|Out-Null;throw "Unexpected success: $label"}catch{if($_.Exception.Response.StatusCode.value__ -ne $status){throw};Write-Output "PASS: $label"}}
$key='nro.models.boss.QaTiming'
$before=(Api @{action='drop_get';key=$key}).config
try{
 $found=(Api @{action='drop_search';query='1632'}).items|Where-Object id -eq 1632
 if(!$found.name -or !$found.icon_id){throw 'Missing item identity'}
 Write-Output 'PASS: item ID search returns actual name and icon'
 if(!(Api @{action='drop_catalog'}).options.Count){throw 'Missing option catalog'}
 $item=@{id=1632;min=2;max=4;chance=12.5;options=@(@{id=50;param=25},@{id=93;param=7})}
 foreach($mode in @('original','add','replace','off')){
  $current=(Api @{action='drop_get';key=$key}).config
  $saved=Api @{action='drop_save';key=$key;mode=$mode;items=@($item);revision=[int]$current.revision}
  if($saved.config.mode -ne $mode -or $saved.config.items[0].id -ne 1632 -or $saved.config.items[0].chance -ne 12.5 -or $saved.config.items[0].options[0].param -ne 25){throw "Roundtrip failed $mode"}
  Write-Output "PASS: $mode roundtrips quantity, chance and options"
  ExpectError @{action='drop_save';key=$key;mode='off';items=@();revision=[int]$current.revision} 409 'stale revision cannot overwrite a newer edit'
 }
 $revision=[int](Api @{action='drop_get';key=$key}).config.revision
 foreach($invalid in @(@{id=32767;min=1;max=1;chance=100;options=@()},@{id=1632;min=0;max=1;chance=100;options=@()},@{id=1632;min=3;max=1;chance=100;options=@()},@{id=1632;min=1;max=1;chance=100.1;options=@()},@{id=1632;min=1;max=1;chance=-1;options=@()},@{id=1632;min=1;max=1;chance=10;options=@(@{id=50;param=32768})},@{id=1632;min=1;max=1;chance=10;options=@(@{id=50;param=1},@{id=50;param=2})})){
  ExpectError @{action='drop_save';key=$key;mode='replace';items=@($invalid);revision=$revision} 422 'invalid item/range/chance/option rejected'
 }
 ExpectError @{action='drop_save';key=$key;mode='unsafe';items=@();revision=$revision} 422 'unknown mode rejected'
 ExpectError @{action='drop_save';key=$key;mode='replace';items=@(1..31|ForEach-Object{$item});revision=$revision} 422 'more than 30 rows rejected'
 $after=(Api @{action='drop_get';key=$key}).config
 if([int]$after.revision -ne $revision){throw 'Validation error modified the saved revision'}
 Write-Output 'PASS: validation failures preserve saved policy'
}finally{
 $current=(Api @{action='drop_get';key=$key}).config
 Api @{action='drop_save';key=$key;mode=$before.mode;items=@($before.items);revision=[int]$current.revision}|Out-Null
}
