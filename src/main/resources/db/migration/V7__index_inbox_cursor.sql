CREATE INDEX idx_messages_user_created_id ON messages (user_id, created_at DESC, id DESC);
DROP INDEX IF EXISTS idx_messages_user_created;
