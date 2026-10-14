$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8765/admin/'
$page=Invoke-WebRequest 'http://127.0.0.1:8765/qa-login.php' -SessionVariable qaSession
$csrf=[regex]::Match($page.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
if(!$csrf){throw 'Missing admin CSRF token'}
function Call-Panel($data,$file='panel_api.php') {
    if(!$data.ContainsKey('csrf')){$data.csrf=$csrf}
    $r=Invoke-WebRequest ($base+$file) -WebSession $qaSession -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes(($data|ConvertTo-Json -Compress))) -SkipHttpErrorCheck
    return @{code=[int]$r.StatusCode;body=($r.Content|ConvertFrom-Json)}
}
function Check($condition,$name) {if(!$condition){throw "FAIL: $name"};Write-Output "PASS: $name"}
$j=Call-Panel @{action='status'};Check ($j.code -eq 200 -and $j.body.giftcodes.Count -eq 2) 'dashboard status and giftcodes'
$j=Call-Panel @{action='status';csrf='wrong'};Check ($j.code -eq 403) 'CSRF rejected'
$noAuth=Invoke-WebRequest ($base+'panel_api.php') -Method Post -ContentType 'application/json' -Body '{"action":"status"}' -SkipHttpErrorCheck;Check ($noAuth.StatusCode -eq 403) 'unauthenticated request rejected'
$j=Call-Panel @{action='players';query='QA Player'};Check ($j.body.players.Count -eq 1) 'character search'
$initial=[int]$j.body.players[0].vnd
$key=[guid]::NewGuid().ToString()
$j=Call-Panel @{action='gift';player_id=2;amount=100;request_key=$key};Check ($j.code -eq 200) 'VND gift succeeds'
$j=Call-Panel @{action='gift';player_id=2;amount=100;request_key=$key};Check ($j.code -eq 200) 'duplicate gift handled'
$j=Call-Panel @{action='players';query='QA Player'};Check ([int]$j.body.players[0].vnd -eq $initial+100) 'duplicate does not add twice'
$j=Call-Panel @{action='gift_history'};$gift=$j.body.rows|Where-Object request_key -eq $key
$j=Call-Panel @{action='undo_gift';id=[int]$gift.id};Check ($j.code -eq 200) 'VND undo succeeds'
$j=Call-Panel @{action='players';query='QA Player'};Check ([int]$j.body.players[0].vnd -eq $initial) 'undo restores balance'
$j=Call-Panel @{action='undo_gift';id=[int]$gift.id};Check ($j.code -eq 409) 'undo cannot repeat'
$j=Call-Panel @{action='gift';player_id=3;amount=100;request_key=[guid]::NewGuid().ToString()};Check ($j.code -eq 409) 'online character protected'
$j=Call-Panel @{action='gift';player_id=2;amount=-1;request_key=[guid]::NewGuid().ToString()};Check ($j.code -eq 422) 'negative gift rejected'
$j=Call-Panel @{action='gift';player_id=2;amount=2147483647;request_key=[guid]::NewGuid().ToString()};Check ($j.code -eq 409) 'VND overflow protected'
$j=Call-Panel @{action='kick';player_id=2};Check ($j.code -eq 409) 'offline server cannot accept kick'
$bossPage=Invoke-WebRequest ($base+'boss.php?embed=1') -WebSession $qaSession
$bossToken=[regex]::Match($bossPage.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
$j=Call-Panel @{action='list';csrf=$bossToken} 'boss.php';Check ($j.body.rows.Count -gt 0 -and $j.body.rows.Count -le 89) 'grouped boss list fixture'
Check ($j.body.rows[0].image -match '^boss_image\.php\?head=\d+$') 'boss portrait resolves through game image endpoint'
$j=Call-Panel @{action='save';csrf=$bossToken;key='qa.boss.1';hp=500000000;damage=500000;rest=600;enabled=$true} 'boss.php';Check ($j.code -eq 200) 'boss save'
$j=Call-Panel @{action='spawn';csrf=$bossToken;key='qa.boss.1'} 'boss.php';Check ($j.code -eq 409) 'boss spawn rejects disconnected server'
$ratesPage=Invoke-WebRequest ($base+'rates.php') -WebSession $qaSession
$rateToken=[regex]::Match($ratesPage.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
$j=Call-Panel @{action='read';csrf=$rateToken} 'rates.php';$rev=[int]$j.body.row.revision
$j=Call-Panel @{action='save';csrf=$rateToken;revision=$rev;exp=3;drop=1;upgrades=@(80,50,20,10,7,5,1,.3);stars=@(50,20,10,5,1,.7,.5,.1,.1)} 'rates.php';Check ($j.code -eq 200) 'EXP/drop/upgrade settings save'
$j=Call-Panel @{action='save';csrf=$rateToken;revision=$rev;exp=3;drop=1;upgrades=@(80,50,20,10,7,5,1,.3);stars=@(50,20,10,5,1,.7,.5,.1,.1)} 'rates.php';Check ($j.code -eq 409) 'stale configuration cannot overwrite newer version'
$j=Call-Panel @{id=0;multiplier=1} 'skill_rates.php';Check ($j.code -eq 200) 'skill multiplier save'
$j=Call-Panel @{action='read';player_id=2} 'intrinsic.php';Check ($j.code -eq 200) 'intrinsic player data loads'
$intrinsic=$j.body.templates|Where-Object { [int]$_.id -gt 0 -and ([int]$_.gender -eq 0 -or [int]$_.gender -eq 3) } | Select-Object -First 1
$beforeIntrinsic=$j.body.player.data_intrinsic
$j=Call-Panel @{action='save';player_id=2;intrinsic_id=[int]$intrinsic.id;param1=[int]$intrinsic.param_from_1;param2=[int]$intrinsic.param_from_2} 'intrinsic.php';Check ($j.code -eq 200) 'intrinsic respects game template and saves'
$j=Call-Panel @{action='read';player_id=2} 'intrinsic.php';$intrinsicAudit=$j.body.history[0].id
$j=Call-Panel @{action='undo';player_id=2;id=[int]$intrinsicAudit} 'intrinsic.php';Check ($j.code -eq 200) 'intrinsic undo succeeds'
$j=Call-Panel @{action='read';player_id=2} 'intrinsic.php';Check ($j.body.player.data_intrinsic -eq $beforeIntrinsic) 'intrinsic undo restores original JSON'
Write-Output 'Integration checks complete. Production player data was not used.'
