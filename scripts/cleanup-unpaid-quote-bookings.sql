-- Removes the unpaid PENDING bookings that the old version created as soon as a quote's status
-- was set to ACCEPTED (before any payment), and puts each of those quotes back to the status it
-- had before. Since the fix, a quote becomes ACCEPTED only through a paid checkout.
--
-- Run it against the right database (dev: ruzinn, prod: group-rathin-prod) in MySQL Workbench
-- or the mysql client. TAKE A BACKUP FIRST, e.g.:
--   mysqldump -u dbuser -p group-rathin-prod booking_main booking_details quote_main change_history > before-cleanup.sql
--
-- Step 1 only reads. Step 2 changes data inside a transaction and does NOT commit:
-- check the results, then run COMMIT (or ROLLBACK to undo).

-- Only bookings created before this moment are touched. Set it to when the fixed backend was
-- deployed, so bookings paid through the new checkout are never included.
SET @cutoff = NOW();

-- ------------------------------------------------------------------------------------------------
-- Step 1: preview (read only)
-- ------------------------------------------------------------------------------------------------

-- A candidate is a booking linked from a quote that is still PENDING (main row and every item),
-- has no voucher or ticket uploaded, and was created before the cutoff.
DROP TEMPORARY TABLE IF EXISTS cleanup_candidates;
CREATE TEMPORARY TABLE cleanup_candidates AS
SELECT
  q.quote_ref_num,
  b.booking_ref_num,
  b.company_id,
  b.company_name,
  b.lead_email,
  b.booking_value,
  b.created_date,
  -- Status the quote had before it was set to ACCEPTED (PENDING if the history has no record).
  COALESCE(
    (SELECT NULLIF(h.old_value, '')
       FROM change_history h
      WHERE h.entity_type = 'QUOTE_MAIN'
        AND h.entity_id = q.quote_ref_num
        AND h.field_name = 'quote_status'
        AND h.new_value = 'ACCEPTED'
        AND h.old_value <> 'ACCEPTED'
      ORDER BY h.changed_at DESC
      LIMIT 1),
    'PENDING') AS restore_status
FROM quote_main q
JOIN booking_main b ON b.booking_ref_num = q.booking_ref_num
WHERE b.booking_status = 'PENDING'
  AND b.created_date < @cutoff
  AND b.voucher_file_name IS NULL
  AND b.ticket_file_name IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM booking_details d
     WHERE d.booking_ref_num = b.booking_ref_num
       AND (COALESCE(d.item_status, 'PENDING') <> 'PENDING'
            OR d.voucher_file_name IS NOT NULL
            OR d.ticket_file_name IS NOT NULL));

-- 1a. Bookings that will be deleted, and the status each quote goes back to.
SELECT * FROM cleanup_candidates ORDER BY created_date;

-- 1b. Possibly PAID bookings for the same quote. In the old version, after Accepted the user could
--     still finish the checkout, which created a second, paid booking that was not linked to the
--     quote. These rows are guesses (same company and lead email, created within a day after).
--     For a real match, re-link the quote to the paid booking in step 2c, so the quote is not
--     reopened and booked (and paid) a second time.
SELECT c.quote_ref_num,
       c.booking_ref_num AS unpaid_booking,
       p.booking_ref_num AS possible_paid_booking,
       p.booking_status, p.booking_value, p.created_date
FROM cleanup_candidates c
JOIN booking_main p
  ON p.company_id = c.company_id
 AND p.lead_email = c.lead_email
 AND p.booking_ref_num <> c.booking_ref_num
 AND p.created_date BETWEEN c.created_date AND c.created_date + INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM quote_main q2 WHERE q2.booking_ref_num = p.booking_ref_num)
ORDER BY c.quote_ref_num, p.created_date;

-- 1c. Linked PENDING bookings that are NOT touched because work has started on them (item status
--     changed or documents uploaded). Review these by hand.
SELECT q.quote_ref_num, b.booking_ref_num, b.booking_status, b.created_date
FROM quote_main q
JOIN booking_main b ON b.booking_ref_num = q.booking_ref_num
WHERE b.booking_status = 'PENDING'
  AND b.created_date < @cutoff
  AND b.booking_ref_num NOT IN (SELECT booking_ref_num FROM cleanup_candidates);

-- ------------------------------------------------------------------------------------------------
-- Step 2: clean up (inside a transaction, not committed)
-- ------------------------------------------------------------------------------------------------

START TRANSACTION;

-- 2a. Audit trail: record the status reset on each quote, like the app does for status changes.
INSERT INTO change_history
  (entity_type, entity_id, booking_ref_num, action, field_name, old_value, new_value,
   actor_user_name, actor_role, company_id, company_name, changed_at)
SELECT 'QUOTE_MAIN', c.quote_ref_num, c.booking_ref_num, 'STATUS_CHANGE', 'quote_status',
       'ACCEPTED', c.restore_status, 'cleanup script', 'SYSTEM', c.company_id, c.company_name, NOW()
FROM cleanup_candidates c;

-- 2b. Unlink the quotes, restore their earlier status, and delete the unpaid bookings.
UPDATE quote_main q
JOIN cleanup_candidates c ON c.quote_ref_num = q.quote_ref_num
SET q.booking_ref_num = NULL,
    q.quote_status = c.restore_status,
    q.updated_date = NOW();

DELETE d FROM booking_details d
JOIN cleanup_candidates c ON c.booking_ref_num = d.booking_ref_num;

DELETE b FROM booking_main b
JOIN cleanup_candidates c ON c.booking_ref_num = b.booking_ref_num;

-- 2c. Optional, one line per real match from 1b: link the quote to the booking that was paid.
-- UPDATE quote_main SET booking_ref_num = 'PRK_PAID_REF', quote_status = 'ACCEPTED', updated_date = NOW()
--  WHERE quote_ref_num = 'QTE_REF';

-- 2d. Check: should return no rows.
SELECT b.booking_ref_num FROM booking_main b
JOIN cleanup_candidates c ON c.booking_ref_num = b.booking_ref_num;
SELECT q.quote_ref_num, q.quote_status, q.booking_ref_num FROM quote_main q
JOIN cleanup_candidates c ON c.quote_ref_num = q.quote_ref_num;

-- When the results look right:
-- COMMIT;
-- To undo everything from step 2:
-- ROLLBACK;
