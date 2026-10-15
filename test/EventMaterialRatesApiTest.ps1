param([string]$Base='http://127.0.0.1:8766')
$ErrorActionPreference='Stop'
if((Invoke-WebRequest "$Base/qa-db.php").Content.Trim() -ne 'nro_panel_qa_20261013'){throw 'QA only'}
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$Base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$Base/admin/drop_rates.php" -WebSession $session
$token=[regex]::Match($page.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
function Api($body){$body.csrf=$token;Invoke-RestMethod "$Base/admin/drop_rates.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json) -WebSession $session}
$keys=@('hung.offering.1220','hung.offering.1221','hung.offering.1222','hung.cooking.1544','hung.cooking.1545','hung.cooking.1546','hung.cooking.1547','hung.cooking.1548','hung.cooking.1549','hung.stamp')
foreach($key in $keys){
 $before=Api @{action='read'}
 $original=$before.regional.$key
 try{
  Api @{action='rule';rule=$key;value=17.25;revision=$before.rates.revision} | Out-Null
  $after=Api @{action='read'}
  if([double]$after.regional.$key -ne 17.25){throw "Rate not saved: $key"}
  foreach($other in $keys){if($other -ne $key -and $after.regional.$other -ne $before.regional.$other){throw "Sibling changed: $other"}}
  if($after.rates.drop_multiplier -ne $before.rates.drop_multiplier -or $after.groups.hung_vuong.value -ne $before.groups.hung_vuong.value){throw 'Shared rate changed'}
  Write-Output "PASS: independent percentage and sibling preservation: $key"
 }finally{
  $current=Api @{action='read'}
  $restore=@{action='inherit';rule=$key;revision=$current.rates.revision}
  if($null -ne $original){$restore.action='rule';$restore.value=$original}
  Api $restore | Out-Null
 }
}
