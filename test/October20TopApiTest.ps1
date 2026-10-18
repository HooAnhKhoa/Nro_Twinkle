param([string]$Base='http://127.0.0.1:8778')
$ErrorActionPreference='Stop';$script:checks=0
function Check($ok,$label){if(!$ok){throw $label};$script:checks++;"PASS: $label"}
$session=[Microsoft.PowerShell.Commands.WebRequestSession]::new();$csrf=(Invoke-RestMethod "$Base/qa-login.php" -WebSession $session).csrf
function Call($body,$expected=200){$body.csrf=$csrf;$r=Invoke-WebRequest "$Base/admin/october20_top.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json -Depth 40 -Compress) -WebSession $session -SkipHttpErrorCheck;if([int]$r.StatusCode-ne $expected){throw "HTTP $($r.StatusCode): $($r.Content)"};return ($r.Content|ConvertFrom-Json)}
$anonymous=Invoke-WebRequest "$Base/admin/october20_top.php" -Method Post -Body '{}' -SkipHttpErrorCheck;Check ([int]$anonymous.StatusCode-eq 403) 'anonymous blocked'
$bad=Invoke-WebRequest "$Base/admin/october20_top.php" -Method Post -ContentType application/json -Body '{"action":"read","csrf":"wrong"}' -WebSession $session -SkipHttpErrorCheck;Check ([int]$bad.StatusCode-eq 403) 'CSRF required'
$d=Call @{action='read'};Check ($null-eq $d.season-and $d.defaults.roses.Count-eq 6) 'three complete default boards returned'
$page=(Invoke-WebRequest "$Base/qa-page.php" -WebSession $session).Content;Check ($page.Contains('octTopPointsEnd')-and $page.Contains('octTopPause')-and $page.Contains('octTopCard')) 'web exposes separate cutoff pause and arbitrary-gem price'
$start=[DateTime]::UtcNow.AddHours(7).AddMinutes(10);$body=@{action='save';season_id=0;name='QA October20';start=$start.ToString('yyyy-MM-ddTHH:mm');points_end=$start.AddHours(1).ToString('yyyy-MM-ddTHH:mm');end=$start.AddHours(2).ToString('yyyy-MM-ddTHH:mm');scheduled=$false;rose_gems=20;card_gems=50;gift_gold=10000000;rewards=$d.defaults}
Call $body|Out-Null;$d=Call @{action='read'};Check ([int]$d.season.rose_gems-eq 20-and [long]$d.season.end_ms-gt [long]$d.season.points_end_ms) 'draft saves prices and three times'
Call $body 409|Out-Null;Check $true 'stale season ID rejected'
$body.season_id=[int]$d.season.id;$body.rose_gems=0;Call $body 409|Out-Null;Check $true 'zero rose price rejected';$body.rose_gems=20
$body.points_end=$body.start;Call $body 409|Out-Null;Check $true 'inverted point cutoff rejected';$body.points_end=$start.AddHours(1).ToString('yyyy-MM-ddTHH:mm')
$body.scheduled=$true;Call $body|Out-Null;$d=Call @{action='read'};Check ([int]$d.season.scheduled-eq 1) 'future schedule enabled on isolated QA with staged assets'
Call @{action='pause';season_id=[int]$d.season.id;paused=$true}|Out-Null;$d=Call @{action='read'};Check ([int]$d.season.paused-eq 1) 'pause saved independently of schedule'
Call @{action='pause';season_id=[int]$d.season.id;paused=$false}|Out-Null;$d=Call @{action='read'};Check ([int]$d.season.paused-eq 0) 'resume retains schedule'
& C:\xampp\php\php.exe test/October20TopApiFixture.php start;if($LASTEXITCODE-ne 0){throw 'QA fixture failed'}
Call $body 409|Out-Null;Check $true 'started season locks price time and rewards'
& C:\xampp\php\php.exe test/October20TopApiFixture.php end;if($LASTEXITCODE-ne 0){throw 'QA fixture failed'}
$body.scheduled=$false;Call $body|Out-Null;$next=Call @{action='read'};Check ([int]$next.season.id-gt [int]$d.season.id) 'ended season creates a new draft without overwriting old season'
"PASS: $script:checks October20 HTTP checks"
