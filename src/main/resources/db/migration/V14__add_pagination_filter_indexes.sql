CREATE INDEX idx_merchant_member_merchant_role_created
    ON merchant_member (merchant_id, role, created_at);

CREATE INDEX idx_customer_profile_merchant_created
    ON customer_profile (merchant_id, created_at);

CREATE INDEX idx_staff_profile_merchant_created
    ON staff_profile (merchant_id, created_at);

CREATE INDEX idx_product_merchant_created
    ON product (merchant_id, created_at);
