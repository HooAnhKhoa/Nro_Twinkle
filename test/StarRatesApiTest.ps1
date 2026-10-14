$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8765'
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest "$base/qa-login.php" -WebSession $session | Out-Null
$page=Invoke-WebRequest "$base/admin/rates.php" -WebSession $session
$token=[regex]::Match($page.Content,'const csrf="([a-f0-9]+)"').Groups[1].Value
function CallRate($body) {
    $body.csrf=$token
    $response=Invoke-WebRequest "$base/admin/rates.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json) -WebSession $session -SkipHttpErrorCheck
    return @{code=[int]$response.StatusCode;body=($response.Content|ConvertFrom-Json)}
}
$row=(CallRate @{action='read'}).body.row
$save=@{action='save';revision=$row.revision;exp=$row.exp_rate;drop=$row.drop_multiplier;upgrades=@(80,50,20,10,7,5,1,.3);stars=@(0,20,10,5,1,.7,.5,.1,100)}
$reply=CallRate $save
if($reply.code -ne 200 -or !$reply.body.message){throw 'Save failed'}
$saved=(CallRate @{action='read'}).body.row
if([double]$saved.star_1 -ne 0 -or [double]$saved.star_9 -ne 100 -or [double]$saved.upgrade_1 -ne 80){throw 'Independent rates mismatch'}
Write-Output 'PASS: API stores star and level rates independently'
$save.revision=$saved.revision;$save.stars[0]=-1
if((CallRate $save).code -ne 422){throw 'Invalid chance accepted'}
Write-Output 'PASS: negative star chance rejected'
$save.stars[0]=101
if((CallRate $save).code -ne 422){throw 'Invalid chance accepted'}
Write-Output 'PASS: star chance above 100 rejected'
$save.stars[0]=50;$save.revision=$row.revision
if((CallRate $save).code -ne 409){throw 'Stale revision accepted'}
Write-Output 'PASS: concurrent configuration changes protected'
