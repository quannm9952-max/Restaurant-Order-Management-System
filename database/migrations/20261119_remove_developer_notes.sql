-- Remove only the original developer/test annotations from demo records.
-- Custom notes and descriptions, including changes on these IDs, are preserved.
-- Run on the intended existing database after taking a backup. Safe to rerun.
START TRANSACTION;

UPDATE Orders
SET notes = NULL
WHERE (order_id = 1015 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1016 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1017 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1018 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1019 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1020 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1021 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1022 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1023 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1024 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1025 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1026 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1027 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1028 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1029 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1030 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1031 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1032 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1033 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1034 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1035 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1037 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1039 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1042 AND CAST(notes AS BINARY) = CAST('DEMO reporting fixture' AS BINARY))
   OR (order_id = 1036 AND CAST(notes AS BINARY) = CAST('DEMO failed payment; order remains Open' AS BINARY))
   OR (order_id = 1050 AND CAST(notes AS BINARY) = CAST('DEMO cancelled before preparation; no successful payment' AS BINARY))
   OR (order_id = 1051 AND CAST(notes AS BINARY) = CAST('DEMO pending items and pending payment' AS BINARY))
   OR (order_id = 1052 AND CAST(notes AS BINARY) = CAST('DEMO Preparing and Ready items; pending payment' AS BINARY))
   OR (order_id = 1100 AND CAST(notes AS BINARY) = CAST('DEMO: Served fixture for independent checkout verification' AS BINARY));

UPDATE Menu_item
SET description = NULL
WHERE (item_id = 9001 AND CAST(description AS BINARY) = CAST('TEST current menu price intentionally differs from historical order price' AS BINARY))
   OR (item_id = 9002 AND CAST(description AS BINARY) = CAST('TEST/DEMO' AS BINARY))
   OR (item_id = 9003 AND CAST(description AS BINARY) = CAST('TEST/DEMO discontinued item' AS BINARY))
   OR (item_id = 9111 AND CAST(description AS BINARY) = CAST('DEMO price in VND' AS BINARY))
   OR (item_id = 9112 AND CAST(description AS BINARY) = CAST('DEMO price in VND' AS BINARY))
   OR (item_id = 9113 AND CAST(description AS BINARY) = CAST('DEMO price in VND' AS BINARY))
   OR (item_id = 9114 AND CAST(description AS BINARY) = CAST('DEMO price in VND' AS BINARY))
   OR (item_id = 9115 AND CAST(description AS BINARY) = CAST('DEMO price in VND' AS BINARY))
   OR (item_id = 9116 AND CAST(description AS BINARY) = CAST('DEMO price in VND' AS BINARY))
   OR (item_id = 9117 AND CAST(description AS BINARY) = CAST('DEMO price in VND' AS BINARY))
   OR (item_id = 9118 AND CAST(description AS BINARY) = CAST('DEMO price in VND' AS BINARY));

UPDATE Menu_category
SET description = NULL
WHERE (category_id = 9001 AND CAST(description AS BINARY) = CAST('TEST/DEMO menu data' AS BINARY))
   OR (category_id = 9101 AND CAST(description AS BINARY) = CAST('DEMO catalog' AS BINARY))
   OR (category_id = 9102 AND CAST(description AS BINARY) = CAST('DEMO catalog' AS BINARY))
   OR (category_id = 9103 AND CAST(description AS BINARY) = CAST('DEMO catalog' AS BINARY))
   OR (category_id = 9104 AND CAST(description AS BINARY) = CAST('DEMO catalog' AS BINARY));

COMMIT;
