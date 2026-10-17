param([string]$Base='http://127.0.0.1:8768')
$ErrorActionPreference='Stop';$script:checks=0
function Check($ok,$message){if(!$ok){throw $message};$script:checks++;Write-Output "PASS: $message"}
function Post($value,$session){Invoke-WebRequest "$Base/admin/item.php" -Method Post -WebSession $session -ContentType 'application/json' -Body ($value|ConvertTo-Json -Depth 8 -Compress) -SkipHttpErrorCheck}
function Payload($item,$csrf){$h=@{csrf=$csrf};$item.PSObject.Properties|ForEach-Object {$h[$_.Name]=$_.Value};return $h}
$anon=[Microsoft.PowerShell.Commands.WebRequestSession]::new()
$r=Post @{csrf='wrong'} $anon;Check ($r.StatusCode -eq 403) 'anonymous writes rejected'
$session=[Microsoft.PowerShell.Commands.WebRequestSession]::new()
$r=Invoke-WebRequest "$Base/qa-login.php" -WebSession $session -SkipHttpErrorCheck
Check ($r.StatusCode -eq 200) 'admin catalog opens'
$m=[regex]::Match($r.Content,'<script id="catalogData" type="application/json">([\s\S]*?)</script>')
$data=$m.Groups[1].Value|ConvertFrom-Json
Check ($data.items.Count -eq 2103) 'all 2103 canonical templates searchable'
Check (!$r.Content.Contains('JSON gốc') -and !$r.Content.Contains('Tải về') -and !$r.Content.Contains('Server 1')) 'technical and export controls removed'
$original=$data.items|Where-Object id -eq 2
$payload=Payload $original $data.csrf
$payload.csrf='bad';$r=Post $payload $session;Check ($r.StatusCode -eq 403) 'invalid CSRF rejected';$payload.csrf=$data.csrf
$payload.power_require=-1;$r=Post $payload $session;Check ($r.StatusCode -eq 422) 'negative power rejected'
$payload.power_require=2147483648;$r=Post $payload $session;Check ($r.StatusCode -eq 422) 'power exceeding game protocol rejected'
$payload.power_require=1200;$payload.level=1.5;$r=Post $payload $session;Check ($r.StatusCode -eq 422) 'fractional level rejected'
$payload.level=1;$payload.NAME='';$r=Post $payload $session;Check ($r.StatusCode -eq 422) 'empty name rejected'
$payload.NAME='QA Áo vải thô <script>test</script>';$payload.description='Mô tả tiếng Việt: sức mạnh, chỉ số & ngoại hình';$payload.level=2;$payload.power_require=2400;$payload.is_up_to_up=1
$r=Post $payload $session;Check ($r.StatusCode -eq 200) 'valid template edit accepted'
$saved=($r.Content|ConvertFrom-Json).item
Check ($saved.NAME -ceq $payload.NAME -and $saved.description -ceq $payload.description -and $saved.level -eq 2 -and $saved.power_require -eq 2400 -and $saved.is_up_to_up -eq 1 -and $saved.icon_id -eq 392) 'Vietnamese text and parameters persisted without changing appearance'
$r=Post $payload $session;Check ($r.StatusCode -eq 409) 'stale edits rejected without overwrite'
$r=Invoke-WebRequest "$Base/admin/item.php" -WebSession $session
Check (!$r.Content.Contains('<script>test</script>') -and $r.Content.Contains('\u003Cscript\u003E')) 'stored markup escaped in page data'
$restore=Payload $original $data.csrf;$restore.revision=$saved.revision
$r=Post $restore $session;Check ($r.StatusCode -eq 200) 'original QA item restored'
$restored=($r.Content|ConvertFrom-Json).item
Check ($restored.revision -ceq $original.revision) 'all original fields restored exactly'
$other=[Microsoft.PowerShell.Commands.WebRequestSession]::new();$null=Invoke-WebRequest "$Base/qa-login.php?nonadmin=1" -WebSession $other -SkipHttpErrorCheck
$r=Post $restore $other;Check ($r.StatusCode -eq 403) 'non-admin writes rejected'
Write-Output "PASS: $script:checks item catalog HTTP checks"
