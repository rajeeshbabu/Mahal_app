CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    plan_duration VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date DATETIME,
    end_date DATETIME,
    razorpay_subscription_id VARCHAR(255) UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_razorpay_subscription_id (razorpay_subscription_id)
);

