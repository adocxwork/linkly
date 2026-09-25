CREATE TABLE users (
    id uuid NOT NULL PRIMARY KEY,
    bio character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL UNIQUE,
    name character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    username character varying(255) NOT NULL UNIQUE,
    is_suspended boolean DEFAULT false NOT NULL,
    role character varying(255) DEFAULT 'ROLE_USER' NOT NULL,
    upi_id character varying(255),
    enable_upi_payment boolean DEFAULT false NOT NULL,
    enable_public_messaging boolean DEFAULT false NOT NULL
);

CREATE TABLE links (
    id uuid NOT NULL PRIMARY KEY,
    active boolean NOT NULL,
    click_count bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) without time zone NOT NULL,
    original_url character varying(2048) NOT NULL,
    short_url character varying(255) NOT NULL UNIQUE,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    CONSTRAINT fk_links_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE messages (
    id uuid NOT NULL PRIMARY KEY,
    sender_name character varying(255),
    content text NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE click_analytics (
    id uuid NOT NULL PRIMARY KEY,
    link_id uuid NOT NULL,
    ip_address character varying(255),
    country character varying(255),
    city character varying(255),
    device_type character varying(255),
    browser character varying(255),
    clicked_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_analytics_link FOREIGN KEY (link_id) REFERENCES links(id) ON DELETE CASCADE
);

CREATE TABLE system_settings (
    id character varying(255) NOT NULL PRIMARY KEY,
    keep_alive_enabled boolean NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_links_user_id ON links(user_id);
CREATE INDEX idx_messages_user_id ON messages(user_id);
CREATE INDEX idx_click_analytics_link_id ON click_analytics(link_id);
