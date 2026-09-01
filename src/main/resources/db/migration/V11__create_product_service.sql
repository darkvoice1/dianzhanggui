CREATE TABLE product_service
(
    id             BIGSERIAL PRIMARY KEY,
    merchant_id    BIGINT         NOT NULL REFERENCES merchant (id),
    name           VARCHAR(120)   NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    description    VARCHAR(500),
    original_price NUMERIC(12, 2) NOT NULL,
    selling_price  NUMERIC(12, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_service_type CHECK (type IN ('PRODUCT', 'SERVICE')),
    CONSTRAINT ck_product_service_price_nonnegative
        CHECK (original_price >= 0 AND selling_price >= 0),
    CONSTRAINT ck_product_service_selling_price
        CHECK (selling_price <= original_price),
    CONSTRAINT ck_product_service_status
        CHECK (status IN ('DRAFT', 'ON_SALE', 'OFF_SALE'))
);

CREATE INDEX idx_product_service_merchant_status
    ON product_service (merchant_id, status);
CREATE INDEX idx_product_service_merchant_type
    ON product_service (merchant_id, type);
CREATE INDEX idx_product_service_merchant_name
    ON product_service (merchant_id, name);
