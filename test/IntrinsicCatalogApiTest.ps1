param([string]$Base='http://127.0.0.1:8766')
$ErrorActionPreference='Stop'
if((Invoke-WebRequest "$Base/qa-db.php").Content.Trim() -ne 'nro_panel_qa_20261013'){throw 'Refusing non-QA endpoint'}
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$Base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$Base/admin/intrinsic.php" -WebSession $session
$token=[regex]::Match($page.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
function Api($body){$body.csrf=$token;$r=Invoke-WebRequest "$Base/admin/intrinsic_templates.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json) -WebSession $session -SkipHttpErrorCheck;@{code=[int]$r.StatusCode;body=($r.Content|ConvertFrom-Json)}}
function Check($ok,$name){if(!$ok){throw $name};Write-Output "PASS: $name"}
function Read{(Api @{action='read'}).body}
function Entry($data,$id){$data.templates|Where-Object {$_.template.id -eq $id}|Select-Object -First 1}
function SaveBody($data,$id,$from,$to){$entry=Entry $data $id;@{action='save';revision=$data.config.revision;intrinsic_id=$id;etag=$entry.etag;param_from_1=$from;param_to_1=$to;param_from_2=$entry.template.param_from_2;param_to_2=$entry.template.param_to_2}}
$initial=Read;$old=Entry $initial 16;$sibling=Entry $initial 17
Check ($initial.templates.Count -eq 26 -and !$old.conflict) 'all templates deduplicated by intrinsic ID'
$save=SaveBody $initial 16 7 35
Check ((Api $save).code -eq 200) 'global intrinsic range saved'
$next=Read;$current=Entry $next 16
Check ($current.template.param_from_1 -eq 7 -and $current.template.param_to_1 -eq 35 -and $current.copies -eq $old.copies -and !$current.conflict) 'all duplicate records updated consistently without deletion'
Check ((Entry $next 17).etag -eq $sibling.etag) 'other intrinsic unchanged'
Check ((Api $save).code -eq 409) 'stale revision rejected'
foreach($bounds in @(@(-1,20),@(30,20),@(0,32768))){$bad=SaveBody $next 16 $bounds[0] $bounds[1];Check ((Api $bad).code -eq 422) 'invalid range rejected'}
$bad=SaveBody $next 16 8 36;$bad.etag='wrong';Check ((Api $bad).code -eq 409) 'stale template fingerprint rejected'
$bad=SaveBody $next 16 8 36;$bad.param_from_2=1;$bad.param_to_2=2;Check ((Api $bad).code -eq 409) 'unused second range cannot be invented'
$bad=SaveBody $next 16 8 36;$bad.intrinsic_id=0;Check ((Api $bad).code -eq 422) 'no-intrinsic sentinel protected'
$audit=$next.history|Where-Object {$_.intrinsic_id -eq 16 -and !$_.undone_at}|Select-Object -First 1
Check ((Api @{action='undo';intrinsic_id=16;audit_id=$audit.id;revision=$next.config.revision}).code -eq 200) 'template undo accepted'
$restored=Read
Check ((Entry $restored 16).etag -eq $old.etag) 'undo restores original ranges and duplicate count'
Check ((Api @{action='undo';intrinsic_id=16;audit_id=$audit.id;revision=$restored.config.revision}).code -eq 409) 'repeated undo rejected'
$save=SaveBody $restored 3 12 36;$save.param_from_2=14;$save.param_to_2=40
Check ((Api $save).code -eq 200) 'both parameter ranges supported'
$next=Read;$two=Entry $next 3
Check ($two.template.param_from_1 -eq 12 -and $two.template.param_to_2 -eq 40) 'both parameter ranges persisted'
$audit=$next.history|Where-Object {$_.intrinsic_id -eq 3 -and !$_.undone_at}|Select-Object -First 1
Check ((Api @{action='undo';intrinsic_id=3;audit_id=$audit.id;revision=$next.config.revision}).code -eq 200) 'two-parameter edit can be undone'
function PlayerApi($body){$body.csrf=$token;$r=Invoke-WebRequest "$Base/admin/intrinsic.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json) -WebSession $session -SkipHttpErrorCheck;@{code=[int]$r.StatusCode;body=($r.Content|ConvertFrom-Json)}}
$playerBefore=(PlayerApi @{action='read';player_id=2}).body
Check ($playerBefore.templates.Count -eq 26) 'per-player editor also deduplicates identical templates'
Check ((PlayerApi @{action='save';player_id=2;intrinsic_id=23;param1=25;param2=0}).code -eq 200) 'existing per-player edit remains available'
$playerAfter=(PlayerApi @{action='read';player_id=2}).body
Check ($playerAfter.values[3] -eq $playerBefore.values[3]) 'per-player edit preserves open-count field'
$audit=$playerAfter.history|Where-Object {!$_.undone_at}|Select-Object -First 1
Check ((PlayerApi @{action='undo';player_id=2;id=$audit.id}).code -eq 200) 'existing per-player undo remains available'
Check ((PlayerApi @{action='save';player_id=3;intrinsic_id=23;param1=25;param2=0}).code -eq 409) 'online player protection retained'
