-- Update currency wording in the original demo descriptions only.
-- No price, order total, payment amount or invoice amount is converted.
-- Re-running this migration is safe; edited customer descriptions do not match.
START TRANSACTION;
UPDATE Menu_item
SET description = 'DEMO price in VND'
WHERE item_id IN (9111,9112,9113,9114,9115,9116,9117,9118)
  AND CAST(description AS BINARY) = CAST('DEMO price in USD' AS BINARY);
COMMIT;
