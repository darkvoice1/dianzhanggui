CREATE UNIQUE INDEX uq_reservation_active_customer_availability
    ON reservation (merchant_id, product_availability_id, customer_profile_id)
    WHERE status = 'RESERVED';
