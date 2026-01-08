# Mahal Management System - JavaFX Desktop Application

This is a JavaFX desktop application that connects to the Spring Boot backend API from the MAHAL project.

## Features

The application provides a desktop interface for managing:

1. **Masjid Management** - Manage masjid and committee information
2. **Staff Management** - Staff details and salary management
3. **Member Management** - Member registration and management
4. **Accounts** - Income, expenses, and dues collection
5. **Certificates** - Marriage, Death, Jamath, and Custom certificates
6. **Events** - Event management and calendar
7. **Inventory** - Inventory and rental management
8. **General** - Role and user management
9. **Settings** - Application settings
10. **About** - System information

## Prerequisites

- Java 21 or higher
- JavaFX 21+ libraries (included in `lib` folder)
- Spring Boot backend running on `http://localhost:8080`
- MySQL database configured in the backend

## Setup

1. **Ensure the Spring Boot backend is running:**
   ```bash
   cd "C:\Users\revathy\Desktop\MAHAL (2)\MAHAL\backend"
   mvn spring-boot:run
   ```

2. **Run the JavaFX application:**
   - Use VS Code's Run and Debug (F5)
   - Select "Mahal Management System" configuration
   - Or run from command line:
     ```bash
     cd d:\JavaGUI\JavaApp\src
     javac -cp "..\lib\*" com\mahal\*.java com\mahal\**\*.java
     java -cp "..\lib\*;." com.mahal.MahalApplication
     ```

## Default Login

- Create an account using the "Create Account" button on the login screen
- Or use existing credentials from the backend database

## Project Structure

```
JavaApp/
├── src/
│   └── com/
│       └── mahal/
│           ├── MahalApplication.java      # Main entry point
│           ├── controller/                 # UI controllers
│           │   ├── LoginController.java
│           │   ├── DashboardController.java
│           │   ├── masjid/
│           │   ├── staff/
│           │   ├── member/
│           │   ├── accounts/
│           │   ├── certificate/
│           │   ├── event/
│           │   ├── inventory/
│           │   ├── general/
│           │   ├── settings/
│           │   └── about/
│           ├── model/                      # Data models
│           ├── service/                    # API services
│           │   ├── ApiService.java
│           │   └── AuthService.java
│           └── util/                       # Utilities
│               └── SessionManager.java
└── lib/                                    # Dependencies
    ├── javafx-*.jar
    └── org.json.jar
```

## API Connection

The application connects to the Spring Boot backend API at:
- Base URL: `http://localhost:8080/api`
- Authentication: JWT Bearer tokens
- All API calls are made through `ApiService`

## Features Implemented

✅ Login and Registration
✅ Main Dashboard with Navigation
✅ Masjid Management (Full CRUD)
✅ Staff Management (View/List)
✅ Member Management (View/List)
✅ Accounts Module (Structure)
✅ Certificates Module (Structure)
✅ Events Module (Structure)
✅ Inventory Module (Structure)
✅ General Settings (Structure)
✅ Settings Page
✅ About Page

## Notes

- The application uses JavaFX for the desktop UI
- All data is fetched from and saved to the Spring Boot backend
- The UI is designed to match the web application's functionality
- Some modules have basic structure and can be extended with full CRUD operations

## Troubleshooting

1. **Connection Error**: Ensure the Spring Boot backend is running on port 8080
2. **Class Not Found**: Make sure all JAR files in the `lib` folder are included in the classpath
3. **Login Fails**: Verify the backend database has user accounts or create one via registration

## License

Proprietary - All rights reserved

