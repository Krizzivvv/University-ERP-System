package edu.univ.erp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class DBManager {
    private static final String DB_HOST = System.getenv().getOrDefault("ERP_DB_HOST", "localhost");
    private static final String DB_USER = System.getenv().getOrDefault("ERP_DB_USER", "erp_user");
    private static final String DB_PASS = System.getenv().getOrDefault("ERP_DB_PASS", "122023!@#");

    private static final String URL_ROOT = "jdbc:mysql://" + DB_HOST + ":3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";
    private static final String URL_AUTH = "jdbc:mysql://" + DB_HOST + ":3306/univ_auth?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";
    private static final String URL_ERP  = "jdbc:mysql://" + DB_HOST + ":3306/univ_erp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";

    private static final String USER = DB_USER;
    private static final String PASS = DB_PASS;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        }
        registerMysqlCleanupHook();
    }

    public static Connection getAuthConnection() throws SQLException {
        return DriverManager.getConnection(URL_AUTH, USER, PASS);
    }

    public static Connection getErpConnection() throws SQLException {
        return DriverManager.getConnection(URL_ERP, USER, PASS);
    }
    
    private static void ensureDatabasesExist() {
        try (Connection root = DriverManager.getConnection(URL_ROOT, USER, PASS);
             Statement st = root.createStatement()) {

            st.execute("CREATE DATABASE IF NOT EXISTS univ_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            st.execute("CREATE DATABASE IF NOT EXISTS univ_erp  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("Databases ensured (created if missing).");

        } catch (SQLException e) {
            System.err.println("Could not ensure databases: " + e.getMessage());
        }
    }
    
    public static void initDatabases() {
        ensureDatabasesExist();

        try (Connection connAuth = getAuthConnection();
             Connection connErp = getErpConnection();
             Statement stAuth = connAuth.createStatement();
             Statement stErp = connErp.createStatement()) {

            System.out.println("Checking/creating tables if needed...");
            
            // --- AUTH TABLES ---
            stAuth.execute("""
                CREATE TABLE IF NOT EXISTS users_auth (
                    user_id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(80) NOT NULL UNIQUE,
                    role ENUM('admin','instructor','student') NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    status VARCHAR(20) DEFAULT 'active',
                    last_login DATETIME NULL
                )
            """);
            
            // --- ERP TABLES ---
            stErp.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    user_id INT PRIMARY KEY,
                    roll_no VARCHAR(30) UNIQUE,
                    program VARCHAR(100),
                    year INT,
                    FOREIGN KEY (user_id) REFERENCES univ_auth.users_auth(user_id) ON DELETE CASCADE
                )
            """);

            stErp.execute("""
                CREATE TABLE IF NOT EXISTS instructors (
                    user_id INT PRIMARY KEY,
                    department VARCHAR(100),
                    FOREIGN KEY (user_id) REFERENCES univ_auth.users_auth(user_id) ON DELETE CASCADE
                )
            """);

            stErp.execute("""
                CREATE TABLE IF NOT EXISTS courses (
                    course_id INT AUTO_INCREMENT PRIMARY KEY,
                    code VARCHAR(30) NOT NULL UNIQUE,
                    title VARCHAR(200) NOT NULL,
                    credits INT NOT NULL
                )
            """);

            stErp.execute("""
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
                )
            """);

            stErp.execute("""
                CREATE TABLE IF NOT EXISTS evaluation_components (
                    component_id INT AUTO_INCREMENT PRIMARY KEY,
                    section_id INT NOT NULL,
                    name VARCHAR(80) NOT NULL,
                    weight DECIMAL(5,2) DEFAULT 0,
                    max_score DECIMAL(6,2) DEFAULT 100,
                    FOREIGN KEY (section_id) REFERENCES sections(section_id) ON DELETE CASCADE,
                    UNIQUE KEY uniq_section_comp (section_id, name)
                )
            """);

            stErp.execute("""
                CREATE TABLE IF NOT EXISTS enrollments (
                    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
                    student_id INT NOT NULL,
                    section_id INT NOT NULL,
                    status VARCHAR(20) DEFAULT 'enrolled',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uniq_student_section (student_id, section_id),
                    FOREIGN KEY (student_id) REFERENCES students(user_id) ON DELETE CASCADE,
                    FOREIGN KEY (section_id) REFERENCES sections(section_id) ON DELETE CASCADE
                )
            """);

            // Removed the Duplicate definition. Kept the one with the UNIQUE KEY constraint.
            stErp.execute("""
                CREATE TABLE IF NOT EXISTS grades (
                    grade_id INT AUTO_INCREMENT PRIMARY KEY,
                    enrollment_id INT NOT NULL,
                    component VARCHAR(80) NOT NULL,
                    score DECIMAL(6,2),
                    final_grade VARCHAR(10),
                    UNIQUE KEY uniq_enroll_comp (enrollment_id, component),
                    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE
                )
            """);

            stErp.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    `key` VARCHAR(100) PRIMARY KEY,
                    `value` VARCHAR(255)
                )
            """);

            System.out.println("Tables verified/created successfully.");
            registerMysqlCleanupHook();

        } catch (SQLException e) {
            System.err.println("Error initializing databases: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void testConnection() {
        try (Connection conn = getErpConnection()) {
            System.out.println("Connected to ERP database: " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }

    private static void registerMysqlCleanupHook() {
        try {
            Class<?> cls = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            Method checkedShutdown = cls.getMethod("checkedShutdown");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    checkedShutdown.invoke(null);
                    System.out.println("MySQL cleanup thread shut down.");
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    System.err.println("Warning: failed to invoke MySQL cleanup: " + ex.getMessage());
                }
            }));

        } catch (ClassNotFoundException | NoSuchMethodException e) {
        } catch (SecurityException e) {
        }
    }
}