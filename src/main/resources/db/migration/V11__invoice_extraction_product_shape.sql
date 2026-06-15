-- Refocus invoice extraction on product-creation data only (VLM emits product fields directly),
-- and store the raw JSON the model returned.

ALTER TABLE invoice_extractions
    DROP COLUMN supplier_name,
    DROP COLUMN invoice_number,
    DROP COLUMN invoice_date,
    DROP COLUMN currency,
    DROP COLUMN total_amount,
    ADD COLUMN raw_json text;

ALTER TABLE invoice_line_items
    RENAME COLUMN raw_description TO name;

ALTER TABLE invoice_line_items
    DROP COLUMN unit,
    DROP COLUMN unit_price,
    DROP COLUMN line_total;
