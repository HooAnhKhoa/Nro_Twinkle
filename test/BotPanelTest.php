<?php
require __DIR__.'/../web/admin/bot_panel_lib.php';
$checks=0;function ok($value,$text){global $checks;if(!$value)throw new Exception($text);$checks++;echo 'PASS: '.$text.PHP_EOL;}
$root=sys_get_temp_dir().'/nro-bot-panel-'.bin2hex(random_bytes(6));mkdir($root);mkdir($root.'/data');file_put_contents($root.'/bot-trial.properties',"enabled=true\nfarm=2\nhunt=1\n");
$c=botPanelConfig($root);ok($c['enabled']&&$c['farm']===2&&$c['hunt']===1,'read current properties');
$r=botPanelChange($root,'save',['enabled'=>true,'farm'=>1,'hunt'=>1,'revision'=>$c['revision']],1);$saved=botPanelConfig($root);ok($saved['farm']===1&&$saved['hunt']===1&&$saved['enabled'],'save selected bot counts');ok(botPanelCanUndo($root,$saved),'undo available for latest change');
try{botPanelChange($root,'save',['enabled'=>false,'farm'=>2,'hunt'=>1,'revision'=>$c['revision']],1);ok(false,'stale revision');}catch(RuntimeException $e){ok(true,'stale revision rejected');}ok(botPanelConfig($root)===$saved,'conflict preserves config');
botPanelChange($root,'undo',['revision'=>$saved['revision']],1);$restored=botPanelConfig($root);ok($restored['farm']===2&&$restored['hunt']===1&&$restored['enabled'],'undo restores previous settings');ok(!botPanelCanUndo($root,$restored),'cannot undo twice');
botPanelChange($root,'save',['enabled'=>false,'farm'=>1,'hunt'=>2,'revision'=>$restored['revision']],1);$off=botPanelConfig($root);ok(!$off['enabled']&&$off['farm']===1&&$off['hunt']===2,'stop preserves desired counts');
foreach([
 ['enabled'=>true,'farm'=>3,'hunt'=>1],['enabled'=>true,'farm'=>0,'hunt'=>0],['enabled'=>true,'farm'=>-1,'hunt'=>1],['enabled'=>true,'farm'=>'2','hunt'=>1],['enabled'=>'true','farm'=>2,'hunt'=>1],['enabled'=>true,'farm'=>1.5,'hunt'=>1]
] as $i=>$bad){try{botPanelChange($root,'save',$bad+['revision'=>$off['revision']],1);ok(false,'invalid '.$i);}catch(InvalidArgumentException $e){ok(true,'invalid setting rejected '.$i);}}
ok(botPanelConfig($root)===$off,'invalid inputs preserve config');
file_put_contents($root.'/bot-trial.properties',"enabled=false\nfarm=0\nhunt=0\n");ok(!botPanelCanUndo($root,botPanelConfig($root)),'external config change invalidates undo');
$empty=$root.'/empty';mkdir($empty);$first=botPanelConfig($empty);ok(!$first['enabled'],'missing config defaults off');botPanelChange($empty,'save',['enabled'=>true,'farm'=>3,'hunt'=>0,'revision'=>$first['revision']],1);ok(botPanelConfig($empty)['farm']===3,'create missing config atomically');
$before=botPanelConfig($empty);botPanelChange($empty,'save',$before,1);ok(botPanelConfig($empty)===$before,'same settings are idempotent');
echo "PASS: $checks bot panel checks\n";
