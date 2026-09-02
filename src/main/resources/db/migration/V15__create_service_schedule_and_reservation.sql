CREATE TABLE service_schedule
(
    id                BIGSERIAL PRIMARY KEY,
    merchant_id       BIGINT      NOT NULL REFERENCES merchant (id),
    product_id        BIGINT      NOT NULL REFERENCES product (id),
    start_at          TIMESTAMP   NOT NULL,
    end_at            TIMESTAMP   NOT NULL,
    capacity          INTEGER     NOT NULL,
    remaining_capacity INTEGER    NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_service_schedule_time CHECK (end_at > start_at),
    CONSTRAINT ck_service_schedule_capacity
        CHECK (capacity > 0 AND remaining_capacity >= 0 AND remaining_capacity <= capacity),
    CONSTRAINT ck_service_schedule_status
        CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'CANCELLED'))
);

CREATE INDEX idx_service_schedule_merchant_status_start
    ON service_schedule (merchant_id, status, start_at);
CREATE INDEX idx_service_schedule_merchant_product
    ON service_schedule (merchant_id, product_id);

CREATE TABLE reservation
(
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         BIGINT      NOT NULL REFERENCES merchant (id),
    service_schedule_id BIGINT      NOT NULL REFERENCES service_schedule (id),
    customer_profile_id BIGINT      NOT NULL REFERENCES customer_profile (id),
    status              VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at        TIMESTAMP,
    CONSTRAINT ck_reservation_status
        CHECK (status IN ('RESERVED', 'CANCELLED')),
    CONSTRAINT ck_reservation_cancelled_at
        CHECK ((status = 'CANCELLED' AND cancelled_at IS NOT NULL)
            OR (status = 'RESERVED' AND cancelled_at IS NULL))
);

CREATE INDEX idx_reservation_merchant_schedule
    ON reservation (merchant_id, service_schedule_id);
CREATE INDEX idx_reservation_merchant_customer
    ON reservation (merchant_id, customer_profile_id);
CREATE INDEX idx_reservation_merchant_status
    ON reservation (merchant_id, status);
