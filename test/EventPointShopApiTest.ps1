param([string]$Base='http://127.0.0.1:8779')
$ErrorActionPreference='Stop';$script:n=0
function Check($b,$label){if(!$b){throw $label};$script:n++;"PASS $label"}
$session=[Microsoft.PowerShell.Commands.WebRequestSession]::new();$csrf=(Invoke-RestMethod "$Base/qa-login.php" -WebSession $session).csrf
function Call($body,$expected=200){$body.csrf=$csrf;$r=Invoke-WebRequest "$Base/admin/event_point_shops.php" -Method Post -ContentType application/json -Body ($body|ConvertTo-Json -Depth 10 -Compress) -WebSession $session -SkipHttpErrorCheck;if([int]$r.StatusCode-ne $expected){throw "HTTP $($r.StatusCode): $($r.Content)"};$r.Content|ConvertFrom-Json}
$r=Invoke-WebRequest "$Base/admin/event_point_shops.php" -Method Post -Body '{}' -SkipHttpErrorCheck;Check ([int]$r.StatusCode-eq 403) 'anonymous rejected'
$r=Invoke-WebRequest "$Base/admin/event_point_shops.php" -Method Post -ContentType application/json -Body '{"action":"read","csrf":"wrong"}' -WebSession $session -SkipHttpErrorCheck;Check ([int]$r.StatusCode-eq 403) 'CSRF required'
$d=Call @{action='read'};$shop=$d.shops|Where-Object {$_.tag_name-eq 'EVENT_POINTS_OCTOBER20'};Check ($null-ne $shop-and [int]$shop.item_count-eq 7) 'seeded ready shop listed'
Call @{action='save';event_key='october20';shop_id=[int]$shop.id}|Out-Null; $d=Call @{action='read'};Check ([int]($d.bindings|Where-Object event_key -eq 'october20').shop_id-eq [int]$shop.id) 'event shop selection persisted'
Call @{action='save';event_key='invalid';shop_id=[int]$shop.id} 422|Out-Null;Check $true 'unknown event rejected'
Call @{action='save';event_key='october20';shop_id=1} 409|Out-Null;Check $true 'non-point or other NPC shop rejected'
Call @{action='save';event_key='october20';shop_id=-1} 422|Out-Null;Check $true 'negative shop ID rejected'
Call @{action='save';event_key='october20';shop_id=0}|Out-Null;$d=Call @{action='read'};Check (@($d.bindings|Where-Object event_key -eq 'october20').Count-eq 0) 'event shop can be unbound'
Call @{action='save';event_key='october20';shop_id=[int]$shop.id}|Out-Null;Check $true 'event shop can be rebound without editing items'
"PASS $script:n event shop HTTP checks"