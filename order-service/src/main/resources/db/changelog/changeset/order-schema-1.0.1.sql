--liquibase formatted sql

--changeset copilot:os-audit-001
ALTER TABLE order_schema.orders
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW();

ALTER TABLE order_schema.order_item
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW();

ALTER TABLE order_schema.payment
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW();

--changeset copilot:os-indexes-002
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON order_schema.orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_restaurant_id ON order_schema.orders(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_schema.order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON order_schema.payment(order_id);

--changeset copilot:os-constraints-003
ALTER TABLE order_schema.orders
    ADD CONSTRAINT IF NOT EXISTS chk_orders_total_price_non_negative CHECK (total_price >= 0);

ALTER TABLE order_schema.order_item
    ADD CONSTRAINT IF NOT EXISTS chk_order_item_quantity_positive CHECK (quantity > 0),
    ADD CONSTRAINT IF NOT EXISTS chk_order_item_price_non_negative CHECK (price >= 0);

ALTER TABLE order_schema.payment
    ADD CONSTRAINT IF NOT EXISTS chk_payment_amount_non_negative CHECK (amount >= 0);
