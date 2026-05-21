-- liquibase formatted sql

-- changeset your-name:001-initial-schema
-- Создание расширения для генерации UUID
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Создание таблицы подписок
CREATE TABLE IF NOT EXISTS subscriptions
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID                     NOT NULL,
    service_name    VARCHAR(255)             NOT NULL,
    status          VARCHAR(50)              NOT NULL,
    description     TEXT,
    cost            NUMERIC(10,2)            NOT NULL,
    start_at        TIMESTAMPTZ              NOT NULL,
    end_at          TIMESTAMPTZ              NOT NULL,
    created_at      TIMESTAMPTZ              NOT NULL,
    updated_at      TIMESTAMPTZ              NOT NULL
);

-- Создание индексов для таблицы подписок
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_service_name ON subscriptions(service_name);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_start_at ON subscriptions(start_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_end_at ON subscriptions(end_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status ON subscriptions(user_id, status);

-- Создание таблицы истории статусов
CREATE TABLE IF NOT EXISTS subscription_status_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    old_status      VARCHAR(50),
    new_status      VARCHAR(50) NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Создание индексов для таблицы истории
CREATE INDEX IF NOT EXISTS idx_status_history_subscription_id ON subscription_status_history(subscription_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON subscription_status_history(changed_at);

-- Добавление колонки changed_by в историю
ALTER TABLE subscription_status_history
ADD COLUMN IF NOT EXISTS changed_by VARCHAR(50) DEFAULT 'system';

-- Обновление существующих записей (меняем NULL на 'user')
UPDATE subscription_status_history
SET changed_by = 'user'
WHERE changed_by = 'system' AND changed_by IS NOT NULL;

-- Добавление колонки version для оптимистической блокировки
ALTER TABLE subscriptions
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Создание индекса для версии
CREATE INDEX idx_subscriptions_version ON subscriptions(version);

-- Комментарии к таблицам и колонкам (опционально)
COMMENT ON TABLE subscriptions IS 'Таблица подписок пользователей';
COMMENT ON COLUMN subscriptions.id IS 'Уникальный идентификатор подписки';
COMMENT ON COLUMN subscriptions.user_id IS 'Идентификатор пользователя';
COMMENT ON COLUMN subscriptions.service_name IS 'Название сервиса';
COMMENT ON COLUMN subscriptions.status IS 'Статус подписки (ACTIVE, PAUSED, CANCELLED, EXPIRED)';
COMMENT ON COLUMN subscriptions.cost IS 'Стоимость подписки';
COMMENT ON COLUMN subscriptions.start_at IS 'Дата начала подписки';
COMMENT ON COLUMN subscriptions.end_at IS 'Дата окончания подписки';
COMMENT ON COLUMN subscriptions.version IS 'Версия для оптимистической блокировки';

COMMENT ON TABLE subscription_status_history IS 'История изменений статусов подписок';
COMMENT ON COLUMN subscription_status_history.subscription_id IS 'Ссылка на подписку';
COMMENT ON COLUMN subscription_status_history.old_status IS 'Предыдущий статус';
COMMENT ON COLUMN subscription_status_history.new_status IS 'Новый статус';
COMMENT ON COLUMN subscription_status_history.changed_at IS 'Дата и время изменения';
COMMENT ON COLUMN subscription_status_history.changed_by IS 'Кто изменил (user/system/scheduler)';