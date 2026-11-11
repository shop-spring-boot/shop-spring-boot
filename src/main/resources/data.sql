MERGE INTO product (name, price, stock) KEY (name) VALUES ('Laptop',     899.99, 10);
MERGE INTO product (name, price, stock) KEY (name) VALUES ('Mouse',       19.99, 50);
MERGE INTO product (name, price, stock) KEY (name) VALUES ('Keyboard',    49.50, 30);
MERGE INTO product (name, price, stock) KEY (name) VALUES ('Monitor',    189.00, 15);
MERGE INTO product (name, price, stock) KEY (name) VALUES ('Headphones',  59.90, 25);

INSERT INTO customer(name) SELECT 'Alice' WHERE NOT EXISTS (SELECT 1 FROM customer WHERE name = 'Alice');
INSERT INTO customer(name) SELECT 'Bob'   WHERE NOT EXISTS (SELECT 1 FROM customer WHERE name = 'Bob');