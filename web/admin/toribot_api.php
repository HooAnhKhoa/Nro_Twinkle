<?php
function toriInt($v,int $min,int $max):int{if(!is_int($v)||$v<$min||$v>$max)throw new InvalidArgumentException("Giá trị phải là số nguyên từ $min đến $max.");return $v;}
function toriBool($v):bool{if(!is_bool($v))throw new InvalidArgumentException('Trạng thái không hợp lệ.');return $v;}
function toriDate($v):string{if(!is_string($v))throw new InvalidArgumentException('Ngày giờ không hợp lệ.');$v=str_replace(' ','T',$v);if(strlen($v)===16)$v.=':00';$d=DateTimeImmutable::createFromFormat('!Y-m-d\TH:i:s',$v,new DateTimeZone('Asia/Ho_Chi_Minh'));if(!$d||$d->format('Y-m-d\TH:i:s')!==$v||$d->format('Y')<2000||$d->format('Y')>2100)throw new InvalidArgumentException('Ngày giờ phải hợp lệ từ năm 2000 đến 2100.');return $v;}
function toriValidate(mysqli $conn,$raw):array{
 if(!is_array($raw))throw new InvalidArgumentException('Thiếu cấu hình.');$key=$raw['season_key']??'';if(!is_string($key)||!preg_match('/^[A-Za-z0-9_-]{1,32}$/D',$key))throw new InvalidArgumentException('Mã mùa: 1–32 ký tự a-z, số, gạch ngang/gạch dưới.');
 $start=toriDate($raw['starts_at']??null);$end=toriDate($raw['ends_at']??null);if($start>=$end)throw new InvalidArgumentException('Thời gian kết thúc phải sau thời gian bắt đầu.');
 $tiers=$raw['tiers']??null;if(!is_array($tiers)||$tiers!==array_values($tiers)||count($tiers)!==4)throw new InvalidArgumentException('Cần đủ 4 gói VIP.');$clean=[];
 foreach($tiers as $index=>$tier){if(toriInt($tier['level']??null,1,4)!==$index+1)throw new InvalidArgumentException('Thứ tự gói VIP không hợp lệ.');$items=$tier['items']??null;if(!is_array($items)||$items!==array_values($items)||count($items)>30)throw new InvalidArgumentException('Tối đa 30 vật phẩm mỗi gói.');$rewards=[];
  foreach($items as $row){$id=toriInt($row['id']??null,0,32767);$q=$conn->prepare('SELECT TYPE FROM item_template WHERE id=?');$q->bind_param('i',$id);$q->execute();$template=$q->get_result()->fetch_assoc();if(!$template||in_array((int)$template['TYPE'],[9,10,34],true)||in_array($id,[517,518],true))throw new InvalidArgumentException("Vật phẩm #$id không hỗ trợ trao vào hành trang.");$opts=$row['options']??[];if(!is_array($opts)||$opts!==array_values($opts)||count($opts)>15)throw new InvalidArgumentException('Tối đa 15 chỉ số mỗi món.');$options=[];$seen=[];
   foreach($opts as $opt){$oid=toriInt($opt['id']??null,0,255);if(isset($seen[$oid]))throw new InvalidArgumentException('Chỉ số bị lặp.');$seen[$oid]=true;$q=$conn->prepare('SELECT id FROM item_option_template WHERE id=?');$q->bind_param('i',$oid);$q->execute();if(!$q->get_result()->num_rows)throw new InvalidArgumentException('Chỉ số không tồn tại.');$options[]=['id'=>$oid,'param'=>toriInt($opt['param']??null,0,32767)];}
   $rewards[]=['id'=>$id,'quantity'=>toriInt($row['quantity']??null,1,99999),'options'=>$options];
  }
  $clean[]=['level'=>$index+1,'enabled'=>toriBool($tier['enabled']??null),'cost'=>toriInt($tier['cost']??null,0,2000000000),'vip_days'=>toriInt($tier['vip_days']??null,1,3650),'exp_bonus'=>toriInt($tier['exp_bonus']??null,0,10000),'normal_pet'=>toriBool($tier['normal_pet']??null),'items'=>$rewards];
 }
 return ['enabled'=>toriBool($raw['enabled']??null),'season_key'=>$key,'starts_at'=>$start,'ends_at'=>$end,'max_purchases'=>toriInt($raw['max_purchases']??null,1,100),'tiers'=>$clean];
}
function toriReply(mysqli $conn):void{
 $row=$conn->query('SELECT *,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) age FROM tori_vip_config WHERE id=1')->fetch_assoc();$row['config']=json_decode($row['config'],true);
 $catalog=[];foreach($conn->query('SELECT id,NAME AS name,icon_id FROM item_template')->fetch_all(MYSQLI_ASSOC) as $item)$catalog[(int)$item['id']]=$item;
 foreach($row['config']['tiers'] as &$tier)foreach($tier['items'] as &$item){$item['name']=$catalog[$item['id']]['name']??('Vật phẩm #'.$item['id']);$item['icon_id']=$catalog[$item['id']]['icon_id']??1;}unset($tier,$item);
 panelReply(['settings'=>$row,'options'=>$conn->query('SELECT id,NAME AS name FROM item_option_template ORDER BY id')->fetch_all(MYSQLI_ASSOC),'history'=>$conn->query('SELECT id,revision,admin_id,created_at FROM tori_vip_audit ORDER BY id DESC LIMIT 20')->fetch_all(MYSQLI_ASSOC),'purchases'=>$conn->query('SELECT a.id,a.player_id,p.name,a.vip_level,a.cost,a.until_ms,a.created_at,a.season_key FROM tori_vip_purchase a LEFT JOIN player p ON p.id=a.player_id ORDER BY a.id DESC LIMIT 20')->fetch_all(MYSQLI_ASSOC)]);
}