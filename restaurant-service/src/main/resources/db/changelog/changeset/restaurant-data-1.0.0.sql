--liquibase formatted sql

--changeset seed:restaurants-001
INSERT INTO restaurant_schema.restaurant (id, name, cuisine, address)
VALUES
    (1, 'Burger House', 'American', '123 Main St, Minsk'),
    (2, 'Pizza Roma', 'Italian', '45 Lenina Ave, Minsk'),
    (3, 'Sushi Zen', 'Japanese', '78 Nezavisimosti Blvd, Minsk')
ON CONFLICT (id) DO NOTHING;

SELECT setval('restaurant_schema.restaurant_id_seq', (SELECT COALESCE(MAX(id), 1) FROM restaurant_schema.restaurant));

--changeset seed:dishes-002
INSERT INTO restaurant_schema.dish (id, name, description, price, restaurant_id)
VALUES
    (1, 'Classic Burger', 'Beef patty, lettuce, tomato, cheese', 450, 1),
    (2, 'Cheese Burger', 'Double cheese, beef patty, pickles', 520, 1),
    (3, 'French Fries', 'Crispy golden fries', 180, 1),
    (4, 'Margherita', 'Tomato, mozzarella, basil', 380, 2),
    (5, 'Pepperoni', 'Spicy pepperoni, mozzarella', 420, 2),
    (6, 'Quattro Formaggi', 'Four cheese blend', 460, 2),
    (7, 'California Roll', 'Crab, avocado, cucumber', 550, 3),
    (8, 'Salmon Nigiri', 'Fresh salmon over rice', 320, 3),
    (9, 'Miso Soup', 'Traditional miso with tofu', 150, 3)
ON CONFLICT (id) DO NOTHING;

SELECT setval('restaurant_schema.dish_id_seq', (SELECT COALESCE(MAX(id), 1) FROM restaurant_schema.dish));
