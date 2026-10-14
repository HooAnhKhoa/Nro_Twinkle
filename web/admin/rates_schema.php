<?php
// Additive migration; existing level upgrade rates and revisions remain intact.
function ensureStarRates(mysqli $conn): void {
    foreach([50,20,10,5,1,.7,.5,.1,.1] as $index=>$default){
        $column='star_'.($index+1);
        if(!$conn->query("SHOW COLUMNS FROM server_rates LIKE '$column'")->num_rows)
            $conn->query("ALTER TABLE server_rates ADD $column DECIMAL(6,3) NOT NULL DEFAULT $default");
    }
}
