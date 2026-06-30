--liquibase formatted sql

--changeset kafka:order-notification-001
CREATE TABLE IF NOT EXISTS restaurant_schema.restaurant_order_notification (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    restaurant_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_notification_restaurant_id
    ON restaurant_schema.restaurant_order_notification(restaurant_id);
