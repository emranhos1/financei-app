# Daily Finance Management System

A production-ready desktop finance application built with Java 11, Spring Boot, JavaFX, and MySQL.

## Features

- **Account Management**: Create and manage multiple account types (Cash, Bank, DPS, FDR)
- **Transaction Logging**: Record income and expense transactions with categories
- **Internal Transfers**: Transfer money between accounts with double-entry bookkeeping
- **Dashboard**: Real-time financial summary (daily, monthly, yearly)
- **Reports**: Detailed financial reports with category breakdown and account balances
- **User Management**: Multi-user support with role-based access control
- **Admin Panel**: User activation/deactivation and administrative functions
- **Security**: BCrypt password hashing, session management, and data isolation

## Prerequisites

- **Java 11** or higher
- **MySQL** running on localhost
- **Maven 3.6** or higher

## Quick Start

### 1. Database Setup

Create a database named `finance_db` in MySQL:

```sql
CREATE DATABASE finance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finance_db
spring.datasource.username=root
spring.datasource.password=your_password

# Admin credentials (for first-run initialization)
admin.username=admin
admin.password=admin123
```

### 3. Run the Application

```bash
mvn javafx:run
```

The application will:
1. Automatically check MySQL connection
2. Create all tables if they don't exist
3. Create default admin user if not exists
4. Launch the JavaFX login window

### 4. Login

- **Username**: `admin`
- **Password**: `admin123`

## Default Admin Credentials

| Field | Value |
|-------|-------|
| Username | admin |
| Password | admin123 |

**Important**: Change these credentials after first login in production.

## User Registration

1. Click "Register" on the login screen
2. Enter username (minimum 3 characters) and password (minimum 6 characters)
3. New accounts are created as INACTIVE by default
4. Admin must activate the account before user can login
5. Activate users from the Admin Panel → User Management

## Project Structure

```
finance-app/
├── src/
│   ├── main/
│   │   ├── java/com/finance/
│   │   │   ├── controller/        # JavaFX controllers
│   │   │   ├── service/           # Business logic layer
│   │   │   ├── repository/        # Data access layer
│   │   │   ├── entity/            # JPA entities
│   │   │   ├── context/           # Session context
│   │   │   ├── config/            # Spring configuration
│   │   │   └── FinanceApplication.java  # Entry point
│   │   └── resources/
│   │       ├── fxml/              # JavaFX UI layouts
│   │       ├── application.properties  # Configuration
│   │       └── schema.sql         # Database schema
│   └── test/                      # Unit tests
└── pom.xml                        # Maven configuration
```

## Database Schema

### Tables

- **users**: User accounts with authentication
- **accounts**: Financial accounts (Cash, Bank, DPS, FDR)
- **categories**: Income/Expense categories
- **transactions**: All financial transactions
  - Supports: Income, Expense, Transfer (with double-entry bookkeeping)

## Security Features

- ✅ All passwords hashed with BCrypt
- ✅ SQL injection prevention via JPA/Hibernate
- ✅ Session-based authentication
- ✅ Role-based access control (Admin/User)
- ✅ User data isolation (users see only their own data)
- ✅ No hardcoded credentials
- ✅ All configuration in `application.properties`

## Double-Entry Bookkeeping

Every transaction maintains accounting integrity:

- **Income**: Increases account balance
- **Expense**: Decreases account balance
- **Transfer**: Debits source account, credits destination account

All operations are atomic database transactions.

## Features by User Role

### Regular User
- Create and manage personal accounts
- Record income and expense transactions
- Transfer money between own accounts
- View personal financial reports
- Create and manage personal categories

### Admin User
- All regular user features
- Manage all user accounts
- Activate/deactivate users
- View system-wide reports

## Troubleshooting

### MySQL Connection Failed

**Error**: "Database connection failed. Ensure MySQL is running and configured in application.properties"

**Solution**:
1. Ensure MySQL service is running: `mysqld` or `mysql.server start` (macOS)
2. Check `application.properties` has correct host, port, username, password
3. Create database if it doesn't exist: `CREATE DATABASE finance_db;`

### Port Already in Use

**Error**: "Address already in use"

**Solution**: Change the JavaFX window port or close other instances of the app.

### Table Already Exists

This is normal. The application uses `CREATE TABLE IF NOT EXISTS` to safely handle repeated runs.

## Configuration

### application.properties Options

| Property | Default | Description |
|----------|---------|-------------|
| spring.datasource.url | jdbc:mysql://localhost:3306/finance_db | MySQL connection URL |
| spring.datasource.username | root | MySQL username |
| spring.datasource.password | (empty) | MySQL password |
| admin.username | admin | Default admin username |
| admin.password | admin123 | Default admin password |

## Development

### Build

```bash
mvn clean build
```

### Run Tests

```bash
mvn test
```

### Package

```bash
mvn clean package
```

## Technical Stack

- **Frontend**: JavaFX 20.0.1
- **Backend**: Spring Boot 2.7.14
- **Database**: MySQL 8.0.33
- **ORM**: Hibernate/JPA
- **Security**: Spring Security (BCrypt)
- **Build**: Maven 3.11.0
- **Java**: 11

## License

This project is provided as-is for educational purposes.

## Support

For issues or questions, refer to the application logs in the console.
