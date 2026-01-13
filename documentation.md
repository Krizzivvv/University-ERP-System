Project Documentation

This document provides instructions on how to run the ERP application, database setup and seed scripts, a detailed test plan, and a test summary.

How to Run

1.  Java Version

    This project requires Java 15 or newer to compile and run. The codebase utilizes modern Java features such as text blocks, which are not supported in older Java versions. Please ensure you have a compatible Java Development Kit (JDK) installed and configured on your system.

2.  Database Setup

    The application is designed to operate with a MySQL database. You must have a MySQL server instance actively running and accessible from the environment and also Maven should be installed.

3.  Database Connection Settings

    The application interacts with two distinct databases: 'univ_auth' (for user authentication and authorization) and 'univ_erp' (for core ERP functionalities like courses, sections, and grades). These databases are automatically created by the application during its initial startup sequence if they do not already exist on the configured MySQL server.

    Database connection parameters are primarily managed through environment variables. If these environment variables are not explicitly set in your operating system or application runtime configuration, the system will revert to the following default values:

    -   ERP_DB_HOST: (Default: localhost) - Specifies the hostname or IP address of your MySQL server.
    -   ERP_DB_USER: (Default: erp_user) - The username used to authenticate with the MySQL server. This user must have privileges to create databases and tables.
    -   ERP_DB_PASS: (Default: 122023!@#) - The password associated with the specified database user.

    To override these defaults, configure the respective environment variables prior to launching the application.

    Use the following command 

        mvn -U clean compile
        mvn exec:java 

4.  Default Accounts

    Upon its initial run, the application does not provision any default user accounts within the 'users_auth' database. Consequently, the user authentication database will be empty.

    To gain access and begin using the application, you must initiate the creation of at least one user account. This can be accomplished either by utilizing the "Register" functionality presented within the application's user interface, or by executing the provided database seed scripts (detailed in the subsequent section) to pre-populate the database with sample user data. It is strongly recommended to establish an 'admin' user account as your first step, as this will grant you immediate access to all administrative features and controls within the ERP system.
