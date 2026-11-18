-- Member 5 reconciliation, MySQL. Change the dates to inspect another reporting period.
SET @report_from = '2026-10-29 00:00:00';
SET @report_until = '2026-11-05 00:00:00';

-- Expected: 24 transactions and 18920000.00. End bound is exclusive.
SELECT COUNT(*) AS successful_transactions, COALESCE(SUM(p.amount), 0) AS total_revenue
FROM Payment p JOIN Orders o ON o.order_id = p.order_id
WHERE p.status = 'SUCCESSFUL' AND o.status = 'COMPLETED'
  AND p.payment_time >= @report_from AND p.payment_time < @report_until;

-- Expected: 1960000, 2480000, 3120000, 2840000, 3380000, 3300000, 1840000 VND across seven days.
SELECT DATE(p.payment_time) AS report_date, COUNT(*) AS transactions, SUM(p.amount) AS revenue
FROM Payment p JOIN Orders o ON o.order_id = p.order_id
WHERE p.status = 'SUCCESSFUL' AND o.status = 'COMPLETED'
  AND p.payment_time >= @report_from AND p.payment_time < @report_until
GROUP BY DATE(p.payment_time) ORDER BY report_date;

-- Expected: 4 excluded rows (2 Pending, 2 Failed), not part of revenue.
SELECT p.payment_id, p.order_id, p.status, p.amount, o.status AS order_status
FROM Payment p JOIN Orders o ON o.order_id = p.order_id
WHERE p.payment_time >= @report_from AND p.payment_time < @report_until
  AND (p.status <> 'SUCCESSFUL' OR o.status <> 'COMPLETED')
ORDER BY p.payment_time, p.payment_id;

-- Expected: no rows. A difference must be reviewed, not silently repaired by a report.
SELECT o.order_id, o.total_amount, SUM(CASE WHEN d.preparation_status <> 'CANCELLED'
       THEN d.quantity * d.unit_price_at_order ELSE 0 END) AS item_total
FROM Orders o JOIN Order_Detail d ON d.order_id = o.order_id
WHERE o.notes LIKE 'DEMO%'
GROUP BY o.order_id, o.total_amount
HAVING o.total_amount <> item_total;

-- Reconcile payment-method buckets with the same dates and inclusion rule.
-- Legacy values remain unchanged. Unknown includes NULL/blank/unsupported methods.
SELECT CASE
         WHEN UPPER(TRIM(p.payment_method)) = 'CASH' THEN 'CASH'
         WHEN UPPER(TRIM(p.payment_method)) IN ('BANK_TRANSFER', 'BANK TRANSFER')
           THEN 'BANK_TRANSFER'
         ELSE 'UNKNOWN'
       END AS payment_method_group,
       COUNT(*) AS transactions, COALESCE(SUM(p.amount), 0) AS revenue
FROM Payment p JOIN Orders o ON o.order_id = p.order_id
WHERE p.status = 'SUCCESSFUL' AND o.status = 'COMPLETED'
  AND p.payment_time >= @report_from AND p.payment_time < @report_until
GROUP BY payment_method_group
ORDER BY payment_method_group;
