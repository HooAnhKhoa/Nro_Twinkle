-- Restore server behavior omitted by the read-only canonical client catalog. Equipment stays separate.
UPDATE item_template SET is_up_to_up=1 WHERE TYPE IN (6,7,8,12,13,14,15,16,22,27,29,30,31,33,35,37) AND is_up_to_up=0;
