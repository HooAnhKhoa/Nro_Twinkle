param([string]$Base='http://127.0.0.1:8768')
$ErrorActionPreference='Stop';$script:checks=0
function Check($ok,$text){if(!$ok){throw $text};$script:checks++;Write-Output "PASS: $text"}
function Snapshot{(Invoke-WebRequest "$Base/qa-shop.php").Content|ConvertFrom-Json}
$session=[Microsoft.PowerShell.Commands.WebRequestSession]::new()
$null=Invoke-WebRequest "$Base/qa-login.php" -WebSession $session
$baseline=Snapshot
foreach($file in @('vang.php','ngoc.php')){
    $url="$Base/admin/$file";$r=Invoke-WebRequest $url -WebSession $session
    $csrf=([regex]::Match($r.Content,'const shopCsrf = ("[^"]+")').Groups[1].Value|ConvertFrom-Json)
    $items=([regex]::Match($r.Content,'const allItems = (.*?);').Groups[1].Value|ConvertFrom-Json)
    Check ($items.Count -eq 2103) "$file picker retains full catalog"
    Check (($items|Where-Object id -eq 2075).available -and !($items|Where-Object id -eq 2082).available -and !($items|Where-Object id -eq 1749).available) "$file ready Nappa selectable; missing costume and mount disabled"
    foreach($action in @('submit_add_item','submit_edit_full')){
        $body=@{csrf=$csrf;temp_id=2082;cost=12345;icon_spec=930;type_sell=0;tab_id=$baseline.tab;sort_order=9876;id=$baseline.last.id};$body[$action]=1
        $r=Invoke-WebRequest $url -Method Post -WebSession $session -Body $body -SkipHttpErrorCheck
        Check ($r.StatusCode -eq 422 -and $r.Content.Contains('Không thể đưa vào cửa hàng')) "$file $action rejects unavailable item"
        Check ((Snapshot).hash -eq $baseline.hash) "$file $action preserves shop rows and options exactly"
    }
    $body=@{csrf='bad';temp_id=2;cost=12345;icon_spec=930;type_sell=0;tab_id=$baseline.tab;sort_order=9876;submit_add_item=1}
    $r=Invoke-WebRequest $url -Method Post -WebSession $session -Body $body -SkipHttpErrorCheck
    Check ($r.StatusCode -eq 403 -and (Snapshot).hash -eq $baseline.hash) "$file CSRF mismatch cannot write"
    $body.csrf=$csrf;$body.temp_id=32000
    $r=Invoke-WebRequest $url -Method Post -WebSession $session -Body $body -SkipHttpErrorCheck
    Check ($r.StatusCode -eq 422 -and (Snapshot).hash -eq $baseline.hash) "$file nonexistent template rejected"
    $body.temp_id=2
    $r=Invoke-WebRequest $url -Method Post -WebSession $session -Body $body -SkipHttpErrorCheck
    $added=Snapshot
    try{Check ($r.StatusCode -eq 200 -and $added.count -eq $baseline.count+1 -and $added.last.temp_id -eq 2 -and $added.last.cost -eq 12345) "$file valid item can still be added"}
    finally{if($added.count -eq $baseline.count+1){$null=Invoke-WebRequest "$Base/qa-shop.php?cleanup=$($added.last.id)"}}
    Check ((Snapshot).hash -eq $baseline.hash) "$file QA fixture restored exactly"
}
Write-Output "PASS: $script:checks shop availability HTTP checks"
