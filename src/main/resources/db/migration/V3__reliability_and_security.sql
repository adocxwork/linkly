-- V3: reliability, security and performance fixes.
--
-- Written to be safe on BOTH kinds of database this app has run against:
--   * a fresh database built by V1 + V2, and
--   * the production database, whose schema was created by Hibernate's ddl-auto=update
--     (Flyway was never actually running before, so production gets baselined at V1).
-- Every statement is therefore guarded (IF EXISTS / IF NOT EXISTS / DO blocks).

-- 1. Legacy table from the removed password-reset feature (previously dropped on every boot).
DROP TABLE IF EXISTS password_reset_tokens CASCADE;

-- 2. Users: token versioning (lets us revoke JWTs) and NOT NULL flags.
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version integer NOT NULL DEFAULT 0;

UPDATE users SET is_suspended = false WHERE is_suspended IS NULL;
ALTER TABLE users ALTER COLUMN is_suspended SET DEFAULT false;
ALTER TABLE users ALTER COLUMN is_suspended SET NOT NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS enable_upi_payment boolean NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS enable_public_messaging boolean NOT NULL DEFAULT false;
ALTER TABLE users ALTER COLUMN enable_upi_payment SET DEFAULT false;
ALTER TABLE users ALTER COLUMN enable_public_messaging SET DEFAULT false;

-- 3. System settings: admin-controlled Redis switch, plus a guaranteed default row.
ALTER TABLE system_settings ADD COLUMN IF NOT EXISTS redis_enabled boolean NOT NULL DEFAULT true;
INSERT INTO system_settings (id, keep_alive_enabled, redis_enabled)
VALUES ('DEFAULT', false, true)
ON CONFLICT (id) DO NOTHING;

-- 4. Click analytics: stop storing raw visitor IP addresses (personal data).
--    New rows store a salted hash, which is enough to count unique visitors.
--    Existing raw IPs are cleared because they cannot be hashed with the app secret from SQL.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema()
                 AND table_name = 'click_analytics'
                 AND column_name = 'ip_address') THEN
        ALTER TABLE click_analytics RENAME COLUMN ip_address TO ip_hash;
        UPDATE click_analytics SET ip_hash = NULL;
    END IF;
END $$;

-- 5. Foreign keys with ON DELETE CASCADE.
--    Hibernate-created foreign keys have no cascade, which forced the app to load every
--    link and click row into memory just to delete a user. Let the database do it.
DO $$
DECLARE
    fk record;
BEGIN
    FOR fk IN
        SELECT c.conname, c.conrelid::regclass AS tbl
        FROM pg_constraint c
        WHERE c.contype = 'f'
          AND c.conrelid IN ('links'::regclass, 'messages'::regclass, 'click_analytics'::regclass)
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.tbl, fk.conname);
    END LOOP;
END $$;

DELETE FROM click_analytics ca WHERE NOT EXISTS (SELECT 1 FROM links l WHERE l.id = ca.link_id);
DELETE FROM links lk WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = lk.user_id);
DELETE FROM messages m WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = m.user_id);

ALTER TABLE links
    ADD CONSTRAINT fk_links_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE messages
    ADD CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE click_analytics
    ADD CONSTRAINT fk_analytics_link FOREIGN KEY (link_id) REFERENCES links(id) ON DELETE CASCADE;

-- 6. Indexes for the queries the app actually runs.
CREATE INDEX IF NOT EXISTS idx_links_user_id ON links(user_id);
CREATE INDEX IF NOT EXISTS idx_messages_user_created ON messages(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_click_analytics_link_time ON click_analytics(link_id, clicked_at);
DROP INDEX IF EXISTS idx_messages_user_id;
DROP INDEX IF EXISTS idx_click_analytics_link_id;
