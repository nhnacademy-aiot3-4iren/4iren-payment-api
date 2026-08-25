CREATE TABLE billing_keys (
    billing_key_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    provider           VARCHAR(20) NOT NULL,
    provider_credential TEXT NOT NULL,
    masked_info        VARCHAR(100) NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at         DATETIME(6) NULL,
    created_at         DATETIME(6) NOT NULL,

    KEY idx_billing_keys_user_id_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plan_prices (
    plan_price_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan          VARCHAR(20) NOT NULL,
    amount        BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(6) NOT NULL,

    KEY idx_plan_prices_plan_status (plan, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 초기 시드값 - 테스트용 placeholder, 실제 가격 확정되면 PlanPricesService.changePrice로 교체
INSERT INTO plan_prices (plan, amount, status, created_at) VALUES
    ('MONTHLY', 29000, 'ACTIVE', NOW(6)),
    ('YEARLY', 290000, 'ACTIVE', NOW(6));

CREATE TABLE subscriptions (
    subscription_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    billing_key_id        BIGINT NOT NULL,
    plan_price_id          BIGINT NOT NULL,
    plan                 VARCHAR(20) NOT NULL,
    amount               BIGINT NOT NULL,
    status               VARCHAR(20) NOT NULL,
    current_period_end    DATETIME(6) NOT NULL,
    next_billing_date     DATE NOT NULL,
    retry_count           INT NOT NULL DEFAULT 0,
    canceled_at           DATETIME(6) NULL,
    expired_at            DATETIME(6) NULL,
    role_downgraded_at    DATETIME(6) NULL,
    created_at            DATETIME(6) NOT NULL,

    CONSTRAINT uq_subscriptions_billing_key_id UNIQUE (billing_key_id),
    CONSTRAINT fk_subscriptions_billing_key
        FOREIGN KEY (billing_key_id) REFERENCES billing_keys (billing_key_id),
    CONSTRAINT fk_subscriptions_plan_price
        FOREIGN KEY (plan_price_id) REFERENCES plan_prices (plan_price_id),

    KEY idx_subscriptions_status_next_billing_date (status, next_billing_date),
    KEY idx_subscriptions_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
    payment_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id        BIGINT NOT NULL,
    order_id                VARCHAR(64) NOT NULL,
    provider_transaction_id VARCHAR(100) NULL,
    pay_token                VARCHAR(100) NULL,
    amount                  BIGINT NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    failure_reason           VARCHAR(255) NULL,
    raw_response             JSON NULL,
    attempted_at             DATETIME(6) NOT NULL,
    approved_at              DATETIME(6) NULL,
    created_at               DATETIME(6) NOT NULL,

    CONSTRAINT uq_payments_order_id UNIQUE (order_id),
    CONSTRAINT fk_payments_subscription
        FOREIGN KEY (subscription_id) REFERENCES subscriptions (subscription_id),

    KEY idx_payments_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
