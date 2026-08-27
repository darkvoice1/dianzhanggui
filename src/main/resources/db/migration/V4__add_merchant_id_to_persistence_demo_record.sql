ALTER TABLE persistence_demo_record
    ADD COLUMN merchant_id BIGINT REFERENCES merchant (id);

CREATE INDEX idx_persistence_demo_record_merchant_id ON persistence_demo_record (merchant_id);
