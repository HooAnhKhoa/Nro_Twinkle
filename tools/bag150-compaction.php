<?php
declare(strict_types=1);

/** Return complete occupied records, preserving order, quantity, options and timestamps. */
function bag150Items(array $bag): array {
    $items=[];
    foreach($bag as $raw){
        $row=is_string($raw)?json_decode($raw,true,512,JSON_THROW_ON_ERROR):$raw;
        if(!is_array($row)||count($row)!==4)throw new RuntimeException('Unknown inventory encoding');
        if((int)$row[0]>=0){
            if((int)$row[1]<1)throw new RuntimeException('Invalid live item quantity');
            $items[]=$raw;
        }
    }
    return $items;
}

/** Refuse overflow instead of deleting, merging or replacing owned items. No database writes. */
function bag150Compact(array $bag,int $now): array {
    $items=bag150Items($bag);
    if(count($items)>150)throw new RuntimeException('More than 150 occupied slots; compaction refused');
    $next=count($bag)>150?$items:$bag;
    while(count($next)<150)$next[]=json_encode([-1,0,'[]',$now],JSON_THROW_ON_ERROR);
    if(bag150Items($next)!==$items)throw new RuntimeException('Compaction changed items');
    return $next;
}
