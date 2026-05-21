-- Расширение для генерации UUID
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Таблица подписок
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    description TEXT,
    cost NUMERIC(10,2) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- Индексы для подписок
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_service_name ON subscriptions(service_name);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_start_at ON subscriptions(start_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_end_at ON subscriptions(end_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status ON subscriptions(user_id, status);

-- Таблица истории статусов
CREATE TABLE IF NOT EXISTS subscription_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Индексы для истории
CREATE INDEX IF NOT EXISTS idx_status_history_subscription_id ON subscription_status_history(subscription_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON subscription_status_history(changed_at);

-- Добавляем колонку changed_by
ALTER TABLE subscription_status_history
ADD COLUMN IF NOT EXISTS changed_by VARCHAR(50) DEFAULT 'system';

-- Добавляем колонку version для оптимистической блокировки
ALTER TABLE subscriptions
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Индекс для version
CREATE INDEX IF NOT EXISTS idx_subscriptions_version ON subscriptions(version);