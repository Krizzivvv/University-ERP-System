Project Structure
        src/main/java/edu/univ/erp/
        |-- db/           Database connection management
        |-- model/        Data models (AuthUser, CourseSection, etc.)
        |-- service/      Business logic (AuthService, GradeService, etc.)
        |-- ui/           Swing GUI components
        |-- util/         Utility classes
        |-- tools/        Database initialization tools

    Backend Architecture

        Database Layer (db/)
            DBManager.java
                The core database connection manager that handles all MySQL interactions. It maintains connections to both univ_auth and univ_erp databases, creates them automatically if missing, and ensures proper schema initialization. Uses connection pooling for efficiency and includes a cleanup hook to gracefully close connections on shutdown.


        Service Layer (service/)
            AuthService.java
                Handles user authentication and authorization. Implements BCrypt password hashing for security, brute-force protection with a 5-attempt lockout mechanism, and tracks user login history and managing user status (active/disabled).
            
            CourseService.java
                Manages course catalog operations including creating, updating, and deleting courses. Provides methods to list courses with enrollment statistics and instructor assignments, and supports searching courses by ID or code.
            
            SectionService.java
                Handles section management for courses. Creates and updates sections with instructor assignments, tracks enrollment capacity, manages registration windows (start/end dates and drop deadlines), and lists sections for instructors with enrollment counts.
            
            StudentService.java
                Manages student-specific operations. Handles course enrollment with concurrency control (pessimistic locking to prevent overbooking), implements registration window validation, supports course dropping with deadline checks, calculates GPA using weighted credits, and provides grade reports with all component scores.
            
            GradeService.java
                The grading engine that manages all grade-related operations. Supports dynamic evaluation components (configurable weights per section), validates scores against maximum values, computes weighted final grades automatically, converts percentages to letter grades (A+ to F scale), provides class statistics for instructors, and enforces maintenance mode restrictions on write operations.
            
            EvaluationService.java
                Manages evaluation components for course sections. Creates, updates, and deletes grading components (e.g., Midsem, Endsem, Assignment, Quiz), configures component weights and maximum scores, and ensures components are section-specific to allow per-section customization.
            
            CatalogService.java
                Provides read-only access to the course catalog. Lists all available sections with enrollment status, filters sections by course, and displays instructor assignments and capacity information.
            
            EnrollmentException.java
                A custom exception class for enrollment-related errors. Provides clear error messages for enrollment failures and supports exception chaining for debugging.


        Model Layer (model/)
            AuthUser.java
                Represents an authenticated user in the system. Stores user ID, username, and role (admin/instructor/student). Used as the session object throughout the application to maintain user context.
            
            CourseSection.java
                Represents a course section with all its attributes. Contains course details (code, title, credits), scheduling information (day/time, room), capacity and enrollment counts, instructor assignment, and semester/year information.
            
            TimeSlot.java
                Models a time slot for timetable management. Uses Java Time API (DayOfWeek, LocalTime) for accurate time representation, validates slot ranges (no Sunday slots, end after start), and provides formatted display strings.


        Utility Layer (util/)
            Session.java
                Manages the application session state. Stores the currently logged-in user, provides session validation methods, and handles logout/cleanup operations.
            
            SettingsDao.java
                A simple key-value store for system settings. Manages maintenance mode flags, stores configuration values in the database, and provides type-safe getter/setter methods.
            
            MaintenanceChecker.java
                Utility class to check maintenance mode status. Used by services to block write operations during maintenance and provides a centralized maintenance status source.
            
            TranscriptExporter.java
                Exports student transcripts to CSV format. Uses reflection to locate grade data provider methods dynamically, handles missing data gracefully with blanks, calculates percentages and letter grades, and formats output for easy import into spreadsheet applications.
            
            CsvUtil.java
                Utility methods for CSV formatting. Escapes special characters (commas, quotes, newlines) and handles null values safely.
            
            ParseUtil.java
                Type-safe parsing utilities. Converts objects to int/double with default values, handles null and empty strings gracefully, and prevents NumberFormatException crashes.
            
            ComboBoxItem.java
                A generic wrapper for JComboBox items. Stores both ID (for database operations) and label (for display), and provides type-safe ID retrieval.
            
            UiMaintenanceHelper.java
                UI helper for maintenance mode indicators. Creates maintenance warning banners, updates banner visibility dynamically, and provides user-friendly maintenance warnings.
            
            LoadingDialog.java
            A modal loading dialog with a spinner. Shows/hides loading indicator during background operations, prevents user interaction during data loading, and provides visual feedback for long-running tasks.

    Frontend Architecture
        Main Entry Point (ui/)
            LoginFrame.java
                The application entry point and login screen. Styled with a sepia theme (matching the dashboard aesthetic), validates credentials before attempting login, displays remaining attempts on failed logins, shows lockout messages for brute-force protection, toggles password visibility with SHOW/HIDE button, and routes users to appropriate dashboards based on role.

        Student Interface
            StudentDashboard.java
                The main student interface with a sidebar navigation system. Features include:
                    
                    Registered Courses View: Lists all enrolled courses with search/filter and alphabetical sorting
                    Course Registration: Shows available sections with seat availability, registration window validation, and course conflict detection (prevents enrolling in multiple sections of same course)
                    Timetable View: Weekly grid-based timetable with color-coded course blocks
                    Grades View: Detailed component-wise grades with GPA calculation and letter grade display
                    Transcript Export: Downloads complete transcript as CSV
                    Change Password: Secure password update with current password verification

                Uses background threads (SwingWorker) for all data loading to keep UI responsive.
    
            TimetablePanel.java
                A visual weekly timetable grid. Shows day/time slots (Monday-Saturday, 8 AM - 6 PM), displays course codes and room numbers in grid cells, color-codes course slots for easy identification, includes a detailed legend table below the grid, and parses various day/time formats from the database.

        Instructor Interface
            InstructorDashboard.java
                The instructor portal for managing courses and grades. Features include:

                    My Sections: Lists all assigned sections with enrollment counts
                    Grade Management: Opens grade entry dialogs for enrolled students, supports component-wise grade input with validation, and auto-computes final grades
                    Component Management: Configures evaluation components (weights, max scores), validates that weights sum to 100%, and updates component definitions per section
                    Class Statistics: Displays average scores and class size
                    Transcript Export: Exports section-wise grade reports as CSV

                Uses maintenance mode checks to block grade modifications during system updates.
            
            EnrollmentsDialog.java
                A dual-pane dialog for viewing and editing grades. Left pane shows enrolled students (roll number, name, program), right pane shows grade components for selected student, supports in-cell editing with validation, computes letter grades dynamically as scores are entered, and displays final totals in bold/highlighted rows.
            
            GradesDialog.java
                A simplified grade entry dialog for individual students. Shows all evaluation components with current scores, allows inline editing of scores with validation (score cannot exceed max score), displays max score and weight percentage for each component, computes final total and letter grade on save, and runs save operation in background to prevent UI freeze.
            
            ComponentsDialog.java
                Dialog for managing evaluation components. Lists current components (Midsem, Endsem, Assignment, Quiz by default), allows editing weights and max scores, validates that weights sum to exactly 100%, saves all changes atomically (all or nothing), and initializes default components if none exist.

        Admin Interface
            AdminDashboard.java
                The comprehensive admin control panel with tabbed navigation. Features include:

                Course Management: CRUD operations for courses, displays enrollment counts per course, and shows assigned instructors
                Section Management: Create/edit/delete sections, assign instructors to sections, set registration windows and drop deadlines, and manage capacity limits
                User Management: Add/edit/delete users (students, instructors, admins), sort users alphabetically, and enable/disable accounts
                Database Backups: Manual backup creation using mysqldump, restore from backup files, delete old backups, and displays backup size and creation date
                Maintenance Mode: Global toggle to block write operations, and displays maintenance status across all interfaces
                Bulk Registration Windows: Set registration/drop windows for multiple courses at once

            Uses background threads extensively for all database operations.
        
            RegisterUI.java
                Unified user registration dialog for all roles. Creates users with BCrypt-hashed passwords, handles student-specific fields (roll number, program, year), automatically creates linked student profile for student role, and validates required fields before submission.
            
            CourseDialog.java
                Modal dialog for adding/editing courses. Input fields for course code, title, and credits, validates that code and title are non-empty, and uses styled components matching the theme.
            
            SectionDialog.java
                Dialog for creating new course sections. Dropdown lists for course and instructor selection (instructor optional), input fields for day/time, room, capacity, semester, and year, loads courses and instructors dynamically in background, and validates that a course is selected.
            
            EditSectionDialog.java
                Edit existing section details. Pre-populates current section data, allows reassigning instructors, updating schedule and room, adjusting capacity, and changing semester/year, loads section data in background to avoid UI freeze, and saves changes atomically.
            
            BulkRegistrationDialog.java
                Bulk update registration windows across courses. Radio buttons to select "All courses" or "Selected courses", multi-select list for choosing courses, date/time pickers for start, end, and drop deadline, validates that end is after start, performs batch database updates efficiently (single transaction), and displays success message with update count.

    Shared Dialogs
        ChangePasswordDialog.java
            Secure password change dialog for all roles. Requires current password for verification, validates new password strength (minimum 6 characters), confirms new password entry, hashes new password with BCrypt (cost factor 12), runs verification and update in background thread, and displays clear error messages for incorrect current password.
        
        TimetableDialog.java
            Time slot picker for scheduling sections. Dropdown for day of week (Monday-Saturday only, no Sunday), time pickers for start and end times using LGoodDatePicker library, validates that end time is after start time, returns a TimeSlot object with validated data, and can be integrated with conflict detection (future enhancement).
        
        SectionsPanel.java
            A standalone window for viewing sections of a specific course. Table shows all sections with instructor, schedule, room, enrollment, lists sections with refresh capability, allows editing individual sections via EditSectionDialog, and optionally integrates with TimetableDialog for visual scheduling.
        
        UsersPanel.java
            A standalone user management window (alternative to AdminDashboard's user tab). Table-based view of all users, add/edit/delete operations, uses RegisterUI for adding users, and provides a focused interface for user administration.

    UI Styling
        UIStyle.java
            Centralized UI styling constants and helper methods. Defines color palette (sepia theme: BACKGROUND, PANEL, PRIMARY, ACCENT, TEXT_PRIMARY), sets global font defaults for Swing components, provides utility methods for creating styled labels, buttons, and panels, and ensures visual consistency across all interfaces.