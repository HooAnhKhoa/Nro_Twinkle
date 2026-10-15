<?php
require __DIR__.'/../tools/bag150-compaction.php';
$checks=0;
function check150(bool $ok,string $text):void {global $checks;if(!$ok)throw new RuntimeException($text);$checks++;echo "PASS: $text\n";}
$empty=json_encode([-1,0,'[]',123]);
$first=json_encode([888,99999,'[[30,0],[93,7]]',456]);
$last=json_encode([735,1,'[[49,25],[93,7]]',789]);
$bag=array_fill(0,300,$empty);$bag[0]=$first;$bag[299]=$last;
$next=bag150Compact($bag,1000);
check150(count($next)===150&&$next[0]===$first&&$next[1]===$last,'last-slot299 item retained in compact150 bag');
check150(bag150Items($next)===bag150Items($bag),'full records, order, options, quantities and timestamps unchanged');
$full=array_fill(0,150,$first);check150(bag150Compact($full,1000)===$full,'150 occupied slots need no mutation');
$overflow=array_fill(0,300,$empty);for($i=0;$i<151;$i++)$overflow[$i]=$first;
$refused=false;try{bag150Compact($overflow,1000);}catch(RuntimeException $expected){$refused=true;}
check150($refused&&count(bag150Items($overflow))===151,'151 occupied slots refused without changing original inventory');
$small=array_fill(0,30,$empty);$small[5]=$last;$grown=bag150Compact($small,1000);check150(count($grown)===150&&array_slice($grown,0,30)===$small,'expanding smaller test bag preserves old slot positions');
check150(bag150Compact($next,1000)===$next,'repeat pure compaction is idempotent');
echo "PASS: $checks isolated compaction checks; no database connection\n";
