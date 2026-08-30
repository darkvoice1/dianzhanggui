CREATE TABLE customer_profile
(
    id          BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT       NOT NULL REFERENCES merchant (id),
    user_id     BIGINT                REFERENCES app_user (id),
    name        VARCHAR(120) NOT NULL,
    phone       VARCHAR(32),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_customer_profile_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uk_customer_profile_merchant_user
    ON customer_profile (merchant_id, user_id)
    WHERE user_id IS NOT NULL;
CREATE INDEX idx_customer_profile_merchant_status
    ON customer_profile (merchant_id, status);
CREATE INDEX idx_customer_profile_merchant_name
    ON customer_profile (merchant_id, name);

CREATE TABLE staff_profile
(
    id          BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT       NOT NULL REFERENCES merchant (id),
    user_id     BIGINT       NOT NULL REFERENCES app_user (id),
    name        VARCHAR(120) NOT NULL,
    phone       VARCHAR(32),
    position    VARCHAR(60)  NOT NULL DEFAULT 'GENERAL',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_staff_profile_merchant_user UNIQUE (merchant_id, user_id),
    CONSTRAINT ck_staff_profile_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_staff_profile_merchant_status
    ON staff_profile (merchant_id, status);
CREATE INDEX idx_staff_profile_merchant_name
    ON staff_profile (merchant_id, name);
