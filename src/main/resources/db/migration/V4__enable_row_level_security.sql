-- V4: close Supabase's auto-generated public REST API (PostgREST) on every table.
--
-- Supabase exposes all tables in the "public" schema over HTTP to the "anon" and "authenticated"
-- roles. Linkly never uses that API: the backend connects directly as the table OWNER, and table
-- owners bypass Row Level Security. Enabling RLS with NO policies therefore blocks the public API
-- completely while the application keeps full access.
--
-- On a plain PostgreSQL (local dev, tests) this is harmless for the same reason.

ALTER TABLE users                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE links                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages              ENABLE ROW LEVEL SECURITY;
ALTER TABLE click_analytics       ENABLE ROW LEVEL SECURITY;
ALTER TABLE system_settings       ENABLE ROW LEVEL SECURITY;
ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY;
