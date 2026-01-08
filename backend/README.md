# Mahal Backend - Spring Boot Application

## Prerequisites

1. **Java 17 or higher**
   ```bash
   java -version
   ```

2. **Maven 3.6+**
   ```bash
   mvn --version
   ```
   If not installed, download from: https://maven.apache.org/download.cgi

3. **MySQL Database** (or use H2 for development)
   - MySQL 8.0+ recommended
   - Create database: `mahal_db`
   - Or configure H2 in-memory database (see application.properties)

## Quick Start

### Option 1: Using Maven (Recommended)

```bash
cd backend
mvn spring-boot:run
```

### Option 2: Using Batch Script (Windows)

```bash
cd backend
run-backend.bat
```

### Option 3: Build and Run JAR

```bash
cd backend
mvn clean package
java -jar target/mahal-backend-1.0.0.jar
```

## Configuration

### 1. Database Setup

Edit `src/main/resources/application.properties`:

**For MySQL:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mahal_db
spring.datasource.username=root
spring.datasource.password=your_password
```

**For H2 (Development/Testing):**
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.h2.console.enabled=true
```

### 2. Razorpay Configuration

Set environment variables or edit `application.properties`:

```properties
razorpay.key.id=your_razorpay_key_id
razorpay.key.secret=your_razorpay_key_secret
```

Or set environment variables:
```bash
export RAZORPAY_KEY_ID=your_key_id
export RAZORPAY_KEY_SECRET=your_key_secret
export RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
```

## API Endpoints

Once running, the backend will be available at: `http://localhost:8080`

### Subscription Endpoints

- `GET /api/subscriptions/status` - Check subscription status
- `POST /api/subscriptions/create` - Create new subscription
- `GET /api/subscriptions/details` - Get subscription details

### Webhook Endpoint

- `POST /api/webhooks/razorpay` - Razorpay webhook handler

## Troubleshooting

### Port 8080 Already in Use

Change port in `application.properties`:
```properties
server.port=8081
```

### Database Connection Error

1. Ensure MySQL is running
2. Check database credentials
3. Verify database `mahal_db` exists
4. Or switch to H2 for testing

### Maven Not Found

1. Install Maven from https://maven.apache.org/download.cgi
2. Add Maven to PATH
3. Restart terminal/command prompt

## Development Notes

- The backend runs on port 8080 by default
- CORS is enabled for all origins (development mode)
- Authentication is temporarily disabled for development
- Database tables are auto-created on startup (Hibernate DDL)

## Next Steps

1. Start the backend: `mvn spring-boot:run`
2. Verify it's running: Open `http://localhost:8080/api/subscriptions/status`
3. Start the JavaFX application
4. Test subscription flow

