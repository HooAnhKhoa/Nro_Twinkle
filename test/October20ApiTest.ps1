param([string]$Base='http://127.0.0.1:8766')
$ErrorActionPreference='Stop';$checks=0
function Check($ok,$label){if(!$ok){throw $label};$script:checks++;Write-Output "PASS: $label"}
if((Invoke-WebRequest "$Base/qa-db.php").Content.Trim()-ne 'nro_panel_qa_20261013'){throw 'QA guard'}
$session=[Microsoft.PowerShell.Commands.WebRequestSession]::new();Invoke-WebRequest "$Base/qa-login.php" -WebSession $session | Out-Null
$page=(Invoke-WebRequest "$Base/admin/events.php" -WebSession $session).Content
$csrf=[regex]::Match($page,'id="october20Settings" data-csrf="([a-f0-9]+)"').Groups[1].Value
Check ($csrf.Length-eq 64-and $page.Contains('data-event="october20"')) 'event page exposes the new toggle and collapsed configuration'
function Api($body){$body.csrf=$csrf;Invoke-RestMethod "$Base/admin/october20_api.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json -Depth 8) -WebSession $session}
function Fails($call,$code,$label){try{&$call|Out-Null;throw 'Unexpected success'}catch{if($_.Exception.Response.StatusCode.value__-ne $code){throw};Check $true $label}}
& C:\xampp\php\php.exe test/October20ApiFixture.php backup;if($LASTEXITCODE-ne 0){throw 'QA backup failed'}
try{
 Fails {Invoke-WebRequest "$Base/admin/october20_api.php" -Method Post -Body '{}'} 403 'anonymous cannot change event settings'
 Fails {Invoke-WebRequest "$Base/admin/october20_api.php" -Method Post -ContentType application/json -Body '{"csrf":"wrong","action":"read"}' -WebSession $session} 403 'admin requests require CSRF'
 $initial=Api @{action='read'};$cfg=@{};$initial.settings.PSObject.Properties|ForEach-Object{$cfg[$_.Name]=[int]$_.Value}
 $cfg['sd.min']=16;$cfg['sd.max']=21;Api @{action='save';revision=[int]$initial.revision;settings=$cfg}|Out-Null
 $saved=Api @{action='read'};Check ($saved.settings.'sd.min'-eq 16-and [int]$saved.revision-eq [int]$initial.revision+1) 'valid settings save with an incremented revision'
 Fails {Api @{action='save';revision=[int]$initial.revision;settings=$cfg}} 409 'stale editor cannot overwrite a newer save'
 $cfg['normal.accessory']=90;$cfg['normal.stones']=60
 Fails {Api @{action='save';revision=[int]$saved.revision;settings=$cfg}} 422 'total reward probability over 100 percent rejected'
 $cfg['normal.accessory']=2;$cfg['normal.stones']=60;$cfg['sd.min']=22
 Fails {Api @{action='save';revision=[int]$saved.revision;settings=$cfg}} 422 'inverted stat ranges rejected'
 $cfg['sd.min']=16;$cfg['permanent']=1.5
 Fails {Api @{action='save';revision=[int]$saved.revision;settings=$cfg}} 422 'fractional values rejected consistently with Java'
 $cfg['permanent']=1;$cfg['alien']=1
 Fails {Api @{action='save';revision=[int]$saved.revision;settings=$cfg}} 422 'unknown setting cannot be injected'
 Api @{action='undo';revision=[int]$saved.revision}|Out-Null;$undo=Api @{action='read'}
 Check ($undo.settings.'sd.min'-eq $initial.settings.'sd.min'-and @($undo.history).Count-ge 2) 'undo restores previous config and records who changed it'
 $drop=(Invoke-WebRequest "$Base/admin/drop_rates.php" -WebSession $session).Content
 Check ($drop.Contains('Vật phẩm 20/10')-and $drop.Contains('october20.roses')) 'drop page includes all four editable material rules'
 Write-Output "PASS: $checks October 20 HTTP checks"
}finally{& C:\xampp\php\php.exe test/October20ApiFixture.php restore;if($LASTEXITCODE-ne 0){throw 'QA restore failed'}}
