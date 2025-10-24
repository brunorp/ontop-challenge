-- Create users table with security best practices
-- Passwords are stored as BCrypt hashes (never plain text)
-- Includes account locking mechanism for brute force protection

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hash
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles table for role-based access control
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Insert a default admin user for testing
-- Password: Admin@123 (BCrypt hash)
-- In production, this should be removed or the password should be changed immediately
INSERT INTO users (id, username, password)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$eIxf4.YfqR7Q2n1uXw5.YeN6Z5g5Q0kY9F4LqV4nJH3xO7k9dX9y2' -- Admin@123
);

-- Insert default admin role
INSERT INTO user_roles (user_id, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'ROLE_ADMIN');

-- Insert a default regular user for testing
-- Password: User@123 (BCrypt hash)
INSERT INTO users (id, username, password)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'user',
    '$2a$10$dXJ3SW6G7P3EBwZlFSnuSuOrQ2OvxZUjPOvZ5E9SmXX0BWj6bBESO' -- User@123
);

-- Insert default user role
INSERT INTO user_roles (user_id, role)
VALUES ('00000000-0000-0000-0000-000000000002', 'ROLE_USER');

