CREATE DATABASE IF NOT EXISTS finance_db_v2;
USE finance_db_v2;

CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'user') NOT NULL DEFAULT 'user',
    status ENUM('active', 'inactive') NOT NULL DEFAULT 'inactive',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS account_types (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_type (user_id, name)
    );

CREATE TABLE IF NOT EXISTS transfer_types (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id BIGINT NOT NULL,
                                              name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_transfer_type (user_id, name)
    );

CREATE TABLE IF NOT EXISTS accounts (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        user_id BIGINT NOT NULL,
                                        name VARCHAR(100) NOT NULL,
    account_type_id BIGINT NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    maturity_date DATE,
    installment_amount DECIMAL(15, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (account_type_id) REFERENCES account_types(id),
    INDEX idx_user_id (user_id)
    );

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts' AND COLUMN_NAME = 'show_in_goals'
);
SET @alter_stmt = IF(@col_exists = 0,
    'ALTER TABLE accounts ADD COLUMN show_in_goals BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt FROM @alter_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS categories (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          user_id BIGINT NOT NULL,
                                          name VARCHAR(100) NOT NULL,
    type ENUM('INCOME', 'EXPENSE', 'BOTH') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_category (user_id, name, type)
    );

ALTER TABLE categories MODIFY COLUMN type ENUM('INCOME', 'EXPENSE', 'BOTH') NOT NULL;

CREATE TABLE IF NOT EXISTS transactions (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            user_id BIGINT NOT NULL,
                                            date DATE NOT NULL,
                                            type ENUM('INCOME', 'EXPENSE', 'TRANSFER') NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    category_id BIGINT,
    from_account_id BIGINT,
    to_account_id BIGINT,
    note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (from_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (to_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_date (date),
    INDEX idx_from_account (from_account_id),
    INDEX idx_to_account (to_account_id)
    );

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'transfer_type_id'
);
SET @alter_stmt = IF(@col_exists = 0,
    'ALTER TABLE transactions ADD COLUMN transfer_type_id BIGINT NULL, ADD FOREIGN KEY (transfer_type_id) REFERENCES transfer_types(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt FROM @alter_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS monthly_expense_overrides (
                                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                          user_id BIGINT NOT NULL,
                                                          account_type_name VARCHAR(100) NOT NULL,
    expense_year INT NOT NULL,
    expense_month INT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_type_year_month (user_id, account_type_name, expense_year, expense_month)
    );

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_expense_overrides' AND COLUMN_NAME = 'is_manual'
);
SET @alter_stmt = IF(@col_exists = 0,
    'ALTER TABLE monthly_expense_overrides ADD COLUMN is_manual BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt FROM @alter_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS account_logs (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            account_id BIGINT NOT NULL,
                                            user_id BIGINT NOT NULL,
                                            change_type ENUM('CREDIT', 'DEBIT', 'ADJUSTMENT') NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_before DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    reference_type ENUM('INCOME', 'EXPENSE', 'TRANSFER', 'MANUAL') NOT NULL,
    reference_id BIGINT,
    note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_account_id (account_id),
    INDEX idx_user_id (user_id)
    );