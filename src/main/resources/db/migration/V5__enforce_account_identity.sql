-- The API treats usernames and emails as case-insensitive. Enforce that under concurrent writes.
CREATE UNIQUE INDEX uq_users_username_ignore_case ON users (lower(username));
CREATE UNIQUE INDEX uq_users_email_ignore_case ON users (lower(email));
