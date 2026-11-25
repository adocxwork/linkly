-- Hibernate-created columns on some older installations were nullable.
UPDATE users SET enable_upi_payment = false WHERE enable_upi_payment IS NULL;
UPDATE users SET enable_public_messaging = false WHERE enable_public_messaging IS NULL;
ALTER TABLE users ALTER COLUMN enable_upi_payment SET NOT NULL;
ALTER TABLE users ALTER COLUMN enable_public_messaging SET NOT NULL;
