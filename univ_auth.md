univ_auth: Authentication & authorization data (users_auth table)
univ_auth Seed Script

    -- Table Structure for users_auth
    CREATE TABLE IF NOT EXISTS users_auth (
        user_id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(80) NOT NULL UNIQUE,
        role ENUM('admin','instructor','student') NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        status VARCHAR(20) DEFAULT 'active',
        last_login DATETIME NULL
    );

    -- Insert sample users into the users_auth table
    -- The password_hash values provided below correspond to the plain-text password 'password'
    -- hashed using BCrypt.
    INSERT INTO users_auth (username, role, password_hash, status) VALUES
    ('admin', 'admin', 'Stud$2a$12$qOHaMXBpXV2c0P2Cr3WHH.QA5lNHb.WSzwjX1aWL8ZynuB6ArlIbqent', 'active'),
    ('instructor1', 'instructor', 'Stud$2a$12$qOHaMXBpXV2c0P2Cr3WHH.QA5lNHb.WSzwjX1aWL8ZynuB6ArlIbqent', 'active'),
    ('student1', 'student', 'Stud$2a$12$qOHaMXBpXV2c0P2Cr3WHH.QA5lNHb.WSzwjX1aWL8ZynuB6ArlIbqent', 'active');