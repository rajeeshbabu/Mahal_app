-- Add users to backend H2 database (local)
-- Run this in H2 Console: http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:file:./data/mahal_db

-- Step 1: Create admins table if it doesn't exist
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    active INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Step 2: Insert your two users
-- REPLACE THE VALUES BELOW WITH YOUR ACTUAL USER DATA
-- Get the data from your SQLite database: frontend/mahal.db
-- Run: SELECT id, name, password, full_name, active FROM admins ORDER BY id;

INSERT INTO admins (id, name, password, full_name, active, created_at, updated_at)
VALUES 
    -- User 1: Replace with actual values from your SQLite database
    (1, 'user1@example.com', '$2a$10$REPLACE_WITH_ACTUAL_BCRYPT_HASH', 'User One Full Name', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- User 2: Replace with actual values from your SQLite database
    (2, 'user2@example.com', '$2a$10$REPLACE_WITH_ACTUAL_BCRYPT_HASH', 'User Two Full Name', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    
ON DUPLICATE KEY UPDATE 
    password = VALUES(password),
    full_name = VALUES(full_name),
    active = VALUES(active),
    updated_at = CURRENT_TIMESTAMP;

-- Verification query
SELECT id, name, full_name, active, created_at FROM admins ORDER BY id;

