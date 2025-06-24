-- PostgreSQL Database Initialization Script
-- This script runs automatically when PostgreSQL container starts for the first time
-- The default database (auth_db) is created by the container using POSTGRES_DB
-- If you need leave_db, create it manually or via a migration tool

-- Create leave_db database for leave service
CREATE DATABASE IF NOT EXISTS leave_db;

-- Connect to auth_db (default database) for auth service tables
-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) DEFAULT 'STAFF' CHECK (role IN ('STAFF', 'MANAGER', 'ADMIN')),
    department_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    is_on_leave BOOLEAN DEFAULT FALSE,
    has_pending_approvals BOOLEAN DEFAULT FALSE,
    manager_id BIGINT,
    email_notifications_enabled BOOLEAN DEFAULT TRUE,
    avatar_url VARCHAR(255),
    google_id VARCHAR(100) UNIQUE,
    locale VARCHAR(10),
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create departments table
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    manager_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Insert default admin user (password: admin123)
INSERT INTO users (email, password, first_name, last_name, role, department_id, is_active, is_on_leave, has_pending_approvals, email_notifications_enabled) VALUES
('admin@africahr.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'System', 'Administrator', 'ADMIN', NULL, TRUE, FALSE, FALSE, TRUE)
ON CONFLICT (email) DO NOTHING;

-- Insert sample users
INSERT INTO users (email, password, first_name, last_name, role, department_id, is_active, is_on_leave, has_pending_approvals, email_notifications_enabled) VALUES
('john.doe@africahr.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'John', 'Doe', 'STAFF', NULL, TRUE, FALSE, FALSE, TRUE),
('jane.smith@africahr.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Jane', 'Smith', 'MANAGER', NULL, TRUE, FALSE, FALSE, TRUE),
('mike.johnson@africahr.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Mike', 'Johnson', 'STAFF', NULL, TRUE, FALSE, FALSE, TRUE)
ON CONFLICT (email) DO NOTHING;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_department_id ON users(department_id);
CREATE INDEX IF NOT EXISTS idx_users_manager_id ON users(manager_id);
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);

-- Insert sample data
INSERT INTO departments (name, description, is_active) VALUES
('Human Resources', 'Handles all employee-related matters.', true),
('Engineering', 'Develops and maintains our software products.', true),
('Sales', 'Drives business and revenue.', true);

INSERT INTO users (first_name, last_name, email, password, role, department_id, is_active, has_pending_approvals, is_on_leave, manager_id, created_at, updated_at) VALUES
('Admin', 'User', 'admin@daking.com', '$2a$10$e.4.QPk3r2s5s5Xw2Y/6d.4.QPk3r2s5s5Xw2Y/6d.4.QPk3r2s5', 'ADMIN', 1, true, false, false, NULL, NOW(), NOW()),
('Manager', 'User', 'manager@daking.com', '$2a$10$e.4.QPk3r2s5s5Xw2Y/6d.4.QPk3r2s5s5Xw2Y/6d.4.QPk3r2s5', 'MANAGER', 2, true, false, false, 1, NOW(), NOW()),
('Staff', 'User', 'staff@daking.com', '$2a$10$e.4.QPk3r2s5s5Xw2Y/6d.4.QPk3r2s5s5Xw2Y/6d.4.QPk3r2s5', 'STAFF', 2, true, false, false, 2, NOW(), NOW()); 