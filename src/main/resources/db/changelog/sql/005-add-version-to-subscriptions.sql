
ALTER TABLE subscriptions
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;


CREATE INDEX idx_subscriptions_version ON subscriptions(version);