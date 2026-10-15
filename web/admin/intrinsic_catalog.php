<?php
function intrinsicCanonical(array $row): array {
    $result=['id'=>(int)$row['id'],'name'=>(string)($row['NAME']??$row['name'])];
    foreach(['param_from_1','param_to_1','param_from_2','param_to_2','icon','gender'] as $field)$result[$field]=(int)$row[$field];
    return $result;
}
function intrinsicTemplates(array $rows): array {
    $result=[];
    foreach($rows as $row){
        $template=intrinsicCanonical($row);$id=$template['id'];
        if(!isset($result[$id]))$result[$id]=['template'=>$template,'copies'=>0,'conflict'=>false];
        $result[$id]['copies']++;
        if($template!==$result[$id]['template'])$result[$id]['conflict']=true;
    }
    foreach($result as &$entry){$entry['etag']=hash('sha256',json_encode([$entry['template'],$entry['copies'],$entry['conflict']],JSON_UNESCAPED_UNICODE));}unset($entry);
    return $result;
}
