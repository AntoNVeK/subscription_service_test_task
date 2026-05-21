
ALTER TABLE subscription_status_history
ADD COLUMN IF NOT EXISTS changed_by VARCHAR(50) DEFAULT 'system';

UPDATE subscription_status_history SET changed_by = 'user' WHERE changed_by IS NULL;