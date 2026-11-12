-- sequences: Hibernate reserves 50 ids at a time (allocationSize = 50)
CREATE SEQUENCE customer_seq   START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE product_seq    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE tag_seq        START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE orders_seq     START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE order_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE customer (
    id             BIGINT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    street         VARCHAR(120),          -- billing (Address embeddable)
    city           VARCHAR(80),
    zip            VARCHAR(20),
    ship_street    VARCHAR(120),          -- shipping (@AttributeOverride)
    ship_city      VARCHAR(80),
    ship_zip       VARCHAR(20)
);

CREATE TABLE product (
    id        BIGINT PRIMARY KEY,
    sku       VARCHAR(40)  NOT NULL UNIQUE,
    name      VARCHAR(80)  NOT NULL,
    category  VARCHAR(20)  NOT NULL CHECK (category IN ('BOOKS','HARDWARE','TOYS','FOOD')),
    amount    DECIMAL(12,2) NOT NULL,     -- Money.amount
    currency  CHAR(3)       NOT NULL,     -- Money.currency
    stock     INT NOT NULL CHECK (stock >= 0)
);

CREATE TABLE tag (
    id   BIGINT PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE product_tag (
    product_id BIGINT NOT NULL REFERENCES product(id),
    tag_id     BIGINT NOT NULL REFERENCES tag(id),
    PRIMARY KEY (product_id, tag_id)
);

CREATE TABLE orders (
    id          BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('NEW','PAID','SHIPPED','CANCELLED')),
    placed_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT NOT NULL
);

CREATE TABLE order_line (
    id         BIGINT PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity   INT NOT NULL CHECK (quantity > 0),
    amount     DECIMAL(12,2) NOT NULL,    -- unit price, Money.amount
    currency   CHAR(3)       NOT NULL
);