CREATE TABLE IF NOT EXISTS subscription_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_status_history_subscription_id ON subscription_status_history(subscription_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON subscription_status_history(changed_at);