CREATE TABLE merchant
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE store
(
    id          BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT       NOT NULL REFERENCES merchant (id),
    name        VARCHAR(120) NOT NULL,
    address     VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_store_merchant_name UNIQUE (merchant_id, name)
);

CREATE TABLE merchant_member
(
    id          BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT      NOT NULL REFERENCES merchant (id),
    user_id     BIGINT      NOT NULL REFERENCES app_user (id),
    role        VARCHAR(30) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_merchant_member_user UNIQUE (merchant_id, user_id)
);

CREATE INDEX idx_store_merchant_id ON store (merchant_id);
CREATE INDEX idx_merchant_member_user_id ON merchant_member (user_id);
