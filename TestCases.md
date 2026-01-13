This test plan details the manual testing procedures for the ERP application, ensuring its core functionalities and user experience are stable.

1.  Introduction

    The testing scope encompasses the primary features accessible through the Admin, Instructor, and Student dashboards. All tests are designed for manual execution, leveraging the pre-configured accounts and sample data established by the seed scripts.

2.  Test Accounts

    The following user accounts are designated for testing purposes. The universal password for all these accounts is 'password'.

    -   Admin: 'admin'
    -   Instructor: 'instructor1'
    -   Student: 'student1'

3.  Acceptance Tests

    3.1. Authentication    
        Test Case ID: AUTH-01
        Description: Verify successful login for admin, instructor, and student roles.
        Steps:
            1.  Launch the application.
            2.  Input a valid username (e.g., 'admin').
            3.  Input the correct password ('password').
            4.  Click the "Login" button.
        Expected Result: The user is successfully redirected to their respective dashboard interface.

    -   Test Case ID: AUTH-02
        Description: Confirm failed login with invalid credentials.
        Steps:
            1.  Launch the application.
            2.  Input an invalid username or an incorrect password.
            3.  Click the "Login" button.
        Expected Result: An informative error message (e.g., "Invalid username or password") is displayed, and the user remains on the login screen.

    -   Test Case ID: AUTH-03
        Description: Validate user logout functionality.
        Steps:
            1.  Successfully log in to the application using any valid account.
            2.  Locate and click the "Logout" button, typically found in the header or navigation panel.
        Expected Result: The user is successfully returned to the application's login page.

    3.2. Admin Dashboard

    -   Test Case ID: Admin-01
        Description: Add a new course to the system.
        Steps:
            1.  Log in as 'admin'.
            2.  Navigate to the "Courses" management panel.
            3.  Click the "Add Course" button.
            4.  Complete all required course details in the input form (e.g., Course Code, Title, Credits).
            5.  Click the "Save" button to confirm.
        Expected Result: The newly added course is visible and correctly listed in the courses table.

    -   Test Case ID: Admin-02
        Description: Edit an existing course's details.
        Steps:
            1.  Log in as 'admin'.
            2.  Navigate to the "Courses" management panel.
            3.  Select an existing course from the table.
            4.  Click the "Edit" button.
            5.  Modify one or more course details (e.g., update credits or title).
            6.  Click the "Save" button.
        Expected Result: The courses table is updated, reflecting the modified information for the selected course.

    -   Test Case ID: Admin-03
        Description: Delete a course from the system.
        Steps:
            1.  Log in as 'admin'.
            2.  Navigate to the "Courses" management panel.
            3.  Select a course from the table that you wish to delete.
            4.  Click the "Delete" button.
            5.  Confirm the deletion when prompted by the confirmation dialog.
        Expected Result: The selected course is permanently removed from the course list.

    -   Test Case ID: Admin-04
        Description: Add a new section for an existing course.
        Steps:
            1.  Log in as 'admin'.
            2.  Navigate to the "Sections" management panel.
            3.  Click the "Add Section" button.
            4.  Fill in all necessary section details, ensuring to assign an existing course and an instructor.
            5.  Click "Save".
        Expected Result: The newly created section is correctly displayed within the sections list.

    -   Test Case ID: Admin-05
        Description: Add a new user account (admin, instructor, or student).
        Steps:
            1.  Log in as 'admin'.
            2.  Navigate to the "Users" management panel.
            3.  Click the "Add User" button.
            4.  Input all required user details, including username, role, and password.
            5.  Click "Save".
        Expected Result: The newly created user account appears in the user list with the specified role and details.

    -   Test Case ID: Admin-06
        Description: Toggle maintenance mode and observe effects.
        Steps:
            1.  Log in as 'admin'.
            2.  Locate and click the "Maintenance Mode" checkbox in the header.
            3.  Observe the confirmation message.
            4.  Log out and attempt to log in as a non-admin user (e.g., 'student1').
        Expected Result: The maintenance mode state changes. Non-admin users should encounter a "Maintenance Mode" banner and/or experience restricted application functionality. Logging back in as admin should show the correct state of the checkbox.

    3.3. Instructor Dashboard

    -   Test Case ID: instructor-01
        Description: Verify that an instructor can view their assigned course sections.
        Steps:
            1.  Log in as 'instructor1'.
            2.  The default view upon login should present the instructor's assigned sections.
        Expected Result: A comprehensive list of course sections allocated to 'instructor1' is accurately displayed.

    -   Test Case ID: instructor-02
        Description: Enter and update student grades within a section.
        Steps:
            1.  Log in as 'instructor1'.
            2.  Navigate to the "Manage Grades" section via the navigation panel.
            3.  Select a specific course section from the dropdown list.
            4.  Choose a student enrolled in that section.
            5.  Input or modify grade scores for various evaluation components (e.g., Assignments, Mid-Term Exam).
            6.  Click the "Save & Compute Final" button.
        Expected Result: The updated grades are successfully saved to the database, and the final grade (both total score and letter grade) for the student is calculated and displayed correctly.

    -   Test Case ID: instructor-03
        Description: Import grades from a CSV file.
        Steps:
            1.  Log in as 'instructor1'.
            2.  Navigate to the "Export Grade Reports" section.
            3.  Select a section from the dropdown list.
            4.  Click the "Import from CSV" button.
            5.  Select a CSV file containing valid student IDs and grades for students enrolled in the chosen section.
        Expected Result: A success message is displayed, and the grades for the students listed in the CSV file are updated in the system.

    -   Test Case ID: instructor-04
        Description: Attempt to import grades using a CSV with an invalid or unenrolled student.
        Steps:
            1.  Log in as 'instructor1'.
            2.  Navigate to the "Export Grade Reports" section.
            3.  Select a section.
            4.  Click the "Import from CSV" button.
            5.  Select a CSV file that contains a student ID that is either non-existent or not enrolled in the selected section.
        Expected Result: An error message is displayed, clearly indicating "Invalid user ID [ID] on line [line_number]: not enrolled in this section.", and the import process is aborted without modifying valid entries.

    3.4. Student Dashboard (Conceptual)

    -   Test Case ID: Student-01
        Description: View enrolled courses.
        Steps:
            1.  Log in as 'student1'.
            2.  The dashboard should display a list of all courses 'student1' is currently enrolled in.
        Expected Result: The enrolled courses are accurately presented, including details like course title, instructor, and schedule.

    -   Test Case ID: Student-02
        Description: View grades for enrolled courses.
        Steps:
            1.  Log in as 'student1'.
            2.  Navigate to the "Grades" section of the dashboard.
            3.  Select an enrolled course.
        Expected Result: The student's grades for all evaluation components in the selected course, along with the calculated final grade, are clearly displayed.