univ_erp: Academic data (courses, sections, enrollments, grades, settings)

univ_erp Seed Script

    -- Table Structure for students
    CREATE TABLE IF NOT EXISTS students (
        user_id INT PRIMARY KEY,
        roll_no VARCHAR(30) UNIQUE,
        program VARCHAR(100),
        year INT,
        FOREIGN KEY (user_id) REFERENCES univ_auth.users_auth(user_id) ON DELETE CASCADE
    );

    -- Table Structure for instructors
    CREATE TABLE IF NOT EXISTS instructors (
        user_id INT PRIMARY KEY,
        department VARCHAR(100),
        FOREIGN KEY (user_id) REFERENCES univ_auth.users_auth(user_id) ON DELETE CASCADE
    );

    -- Table Structure for courses
    CREATE TABLE IF NOT EXISTS courses (
        course_id INT AUTO_INCREMENT PRIMARY KEY,
        code VARCHAR(30) NOT NULL UNIQUE,
        title VARCHAR(200) NOT NULL,
        credits INT NOT NULL
    );

    -- Table Structure for sections
    CREATE TABLE IF NOT EXISTS sections (
        section_id INT AUTO_INCREMENT PRIMARY KEY,
        course_id INT NOT NULL,
        instructor_id INT,
        day_time VARCHAR(100),
        room VARCHAR(50),
        capacity INT DEFAULT 30,
        semester VARCHAR(20),
        year INT,
        FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
        FOREIGN KEY (instructor_id) REFERENCES instructors(user_id) ON DELETE SET NULL
    );

    -- Table Structure for evaluation_components
    CREATE TABLE IF NOT EXISTS evaluation_components (
        component_id INT AUTO_INCREMENT PRIMARY KEY,
        section_id INT NOT NULL,
        name VARCHAR(80) NOT NULL,
        weight DECIMAL(5,2) DEFAULT 0,
        max_score DECIMAL(6,2) DEFAULT 100,
        FOREIGN KEY (section_id) REFERENCES sections(section_id) ON DELETE CASCADE,
        UNIQUE KEY uniq_section_comp (section_id, name)
    );

    -- Table Structure for enrollments
    CREATE TABLE IF NOT EXISTS enrollments (
        enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
        student_id INT NOT NULL,
        section_id INT NOT NULL,
        status VARCHAR(20) DEFAULT 'enrolled',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uniq_student_section (student_id, section_id),
        FOREIGN KEY (student_id) REFERENCES students(user_id) ON DELETE CASCADE,
        FOREIGN KEY (section_id) REFERENCES sections(section_id) ON DELETE CASCADE
    );

    -- Table Structure for grades
    CREATE TABLE IF NOT EXISTS grades (
        grade_id INT AUTO_INCREMENT PRIMARY KEY,
        enrollment_id INT NOT NULL,
        component VARCHAR(80) NOT NULL,
        score DECIMAL(6,2),
        final_grade VARCHAR(10),
        UNIQUE KEY uniq_enroll_comp (enrollment_id, component),
        FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE
    );

    -- Table Structure for settings
    CREATE TABLE IF NOT EXISTS settings (
        `key` VARCHAR(100) PRIMARY KEY,
        `value` VARCHAR(255)
    );


    -- Insert sample student and instructor records, mapping them to users created above
    -- user_id 3 is student1, user_id 2 is instructor1
    INSERT INTO students (user_id, roll_no, program, year) VALUES
    (3, 'S2025001', 'Computer Science', 2025);

    INSERT INTO instructors (user_id, department) VALUES
    (2, 'Computer Science');

    -- Insert sample courses
    INSERT INTO courses (code, title, credits) VALUES
    ('CS101', 'Introduction to Programming', 3),
    ('CS202', 'Data Structures and Algorithms', 4),
    ('MA201', 'Calculus I', 4);

    -- Insert sample sections
    -- Assign CS101 and CS202 to instructor1 (user_id 2)
    INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) VALUES
    (1, 2, 'Mon/Wed 10:00-11:30', 'A-101', 30, 'Fall', 2025), -- CS101 Section
    (2, 2, 'Tue/Thu 14:00-15:30', 'B-203', 25, 'Fall', 2025); -- CS202 Section

    -- Enroll student1 (user_id 3) in the CS101 section (section_id 1)
    INSERT INTO enrollments (student_id, section_id, status) VALUES
    (3, 1, 'enrolled');

    -- Add evaluation components for the CS101 section (section_id 1)
    INSERT INTO evaluation_components (section_id, name, weight, max_score) VALUES
    (1, 'Assignments', 20.00, 100.00),
    (1, 'Mid-Term Exam', 30.00, 100.00),
    (1, 'Final Exam', 30.00, 100.00),
    (1, 'Quiz', 20.00, 100.00);