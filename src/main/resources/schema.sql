-- Users table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'user') NOT NULL DEFAULT 'user',
    status ENUM('active', 'inactive') NOT NULL DEFAULT 'inactive',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- Account Types table (per-user, dynamic)
CREATE TABLE IF NOT EXISTS account_types (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_type (user_id, name)
    );

-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        user_id BIGINT NOT NULL,
                                        name VARCHAR(100) NOT NULL,
    type VARCHAR(100) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    maturity_date DATE,
    installment_amount DECIMAL(15, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
    );

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          user_id BIGINT NOT NULL,
                                          name VARCHAR(100) NOT NULL,
    type ENUM('income', 'expense', 'both') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_category (user_id, name, type)
    );

-- Alter existing categories table to add 'both' if it exists without it
ALTER TABLE categories MODIFY COLUMN type ENUM('income', 'expense', 'both') NOT NULL;

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            user_id BIGINT NOT NULL,
                                            date DATE NOT NULL,
                                            type ENUM('income', 'expense', 'transfer') NOT NULL,
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