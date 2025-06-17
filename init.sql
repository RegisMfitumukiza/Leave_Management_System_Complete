-- Create database if not exists
CREATE DATABASE IF NOT EXISTS leave_management;
USE leave_management;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role ENUM('STAFF', 'MANAGER', 'ADMIN') DEFAULT 'STAFF',
    department VARCHAR(100),
    position VARCHAR(100),
    hire_date DATE,
    profile_picture_url VARCHAR(255),
    google_id VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create leave_types table
CREATE TABLE IF NOT EXISTS leave_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    default_days_per_year INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create leave_balances table
CREATE TABLE IF NOT EXISTS leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    total_days DECIMAL(5,2) DEFAULT 0,
    used_days DECIMAL(5,2) DEFAULT 0,
    remaining_days DECIMAL(5,2) DEFAULT 0,
    year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (leave_type_id) REFERENCES leave_types(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_leave_year (user_id, leave_type_id, year)
);

-- Create leaves table
CREATE TABLE IF NOT EXISTS leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(5,2) NOT NULL,
    reason TEXT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT,
    document_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (leave_type_id) REFERENCES leave_types(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'SUCCESS', 'WARNING', 'ERROR') DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create public_holidays table
CREATE TABLE IF NOT EXISTS public_holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default leave types
INSERT INTO leave_types (name, description, default_days_per_year) VALUES
('Personal Time Off (PTO)', 'Annual personal leave', 20),
('Sick Leave', 'Medical leave for illness', 30),
('Compassionate Leave', 'Bereavement leave', 5),
('Maternity Leave', 'Maternity leave for mothers', 90),
('Paternity Leave', 'Paternity leave for fathers', 10),
('Study Leave', 'Leave for educational purposes', 10),
('Other', 'Other types of leave', 0);

-- Insert default admin user (password: admin123)
INSERT INTO users (username, email, password, first_name, last_name, role, department, position, hire_date) VALUES
('admin', 'admin@africahr.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'System', 'Administrator', 'ADMIN', 'IT', 'System Administrator', CURDATE());

-- Insert sample users
INSERT INTO users (username, email, password, first_name, last_name, role, department, position, hire_date) VALUES
('john.doe', 'john.doe@africahr.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'John', 'Doe', 'STAFF', 'Engineering', 'Software Developer', '2023-01-15'),
('jane.smith', 'jane.smith@africahr.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Jane', 'Smith', 'MANAGER', 'Engineering', 'Engineering Manager', '2022-06-01'),
('mike.johnson', 'mike.johnson@africahr.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Mike', 'Johnson', 'STAFF', 'Marketing', 'Marketing Specialist', '2023-03-10');

-- Insert sample public holidays (Rwanda 2024)
INSERT INTO public_holidays (name, date, description) VALUES
('New Year\'s Day', '2024-01-01', 'New Year celebration'),
('Heroes Day', '2024-02-01', 'National Heroes Day'),
('Genocide Memorial Day', '2024-04-07', 'Commemoration of the 1994 Genocide'),
('Independence Day', '2024-07-01', 'Independence from Belgium'),
('Liberation Day', '2024-07-04', 'Liberation from genocide'),
('Assumption Day', '2024-08-15', 'Religious holiday'),
('Christmas Day', '2024-12-25', 'Christmas celebration');

-- Insert sample leave balances for 2024
INSERT INTO leave_balances (user_id, leave_type_id, total_days, used_days, remaining_days, year) VALUES
(2, 1, 20.0, 5.0, 15.0, 2024),
(2, 2, 30.0, 2.0, 28.0, 2024),
(3, 1, 20.0, 8.0, 12.0, 2024),
(3, 2, 30.0, 0.0, 30.0, 2024),
(4, 1, 20.0, 3.0, 17.0, 2024),
(4, 2, 30.0, 1.0, 29.0, 2024);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_leaves_user_id ON leaves(user_id);
CREATE INDEX idx_leaves_status ON leaves(status);
CREATE INDEX idx_leaves_dates ON leaves(start_date, end_date);
CREATE INDEX idx_leave_balances_user_year ON leave_balances(user_id, year);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read); 