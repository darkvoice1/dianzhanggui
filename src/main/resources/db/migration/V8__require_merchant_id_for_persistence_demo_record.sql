ALTER TABLE persistence_demo_record
    ADD CONSTRAINT ck_persistence_demo_record_merchant_id_not_null
        CHECK (merchant_id IS NOT NULL) NOT VALID;
