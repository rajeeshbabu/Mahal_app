# Backend Startup Guide

## Quick Start

The subscription feature requires the Spring Boot backend to be running. Follow these steps:

### 1. Navigate to Backend Directory

```bash
cd path/to/your/spring-boot/backend
```

### 2. Start the Backend Server

**Option A: Using Maven**
```bash
mvn spring-boot:run
```

**Option B: Using Gradle**
```bash
./gradlew bootRun
```

**Option C: Run the Main Class**
```bash
java -jar target/your-app.jar
```

### 3. Verify Backend is Running

You should see output like:
```
Started Application in X.XXX seconds
```

The backend should be accessible at: `http://localhost:8080`

### 4. Test the API

Open a browser or use curl:
```bash
curl http://localhost:8080/api/subscriptions/status
```

### 5. Start the JavaFX App

Once the backend is running, start your JavaFX application. The subscription feature will now work.

## Troubleshooting

### Port 8080 Already in Use

If you see "Port 8080 is already in use":
1. Find the process using port 8080:
   ```bash
   # Windows
   netstat -ano | findstr :8080
   
   # Linux/Mac
   lsof -i :8080
   ```
2. Kill the process or change the port in `application.properties`:
   ```properties
   server.port=8081
   ```
3. Update `ApiService.java` in JavaFX app to match:
   ```java
   private static final String BASE_URL = "http://localhost:8081/api";
   ```

### Backend Won't Start

1. **Check Java Version**: Ensure Java 17+ is installed
   ```bash
   java -version
   ```

2. **Check Dependencies**: Ensure all dependencies are downloaded
   ```bash
   mvn clean install
   ```

3. **Check Database**: Ensure database is running and accessible
   - MySQL should be running
   - Database credentials in `application.properties` should be correct

4. **Check Logs**: Look at the console output for specific error messages

### Connection Refused Error

If you see "Connection refused" in the JavaFX app:

1. **Verify Backend is Running**:
   - Check if backend process is running
   - Verify it's listening on port 8080
   - Check backend logs for errors

2. **Check Firewall**:
   - Ensure firewall isn't blocking localhost connections
   - Try accessing `http://localhost:8080` in a browser

3. **Check Backend URL**:
   - Verify `ApiService.java` has correct BASE_URL
   - Default is: `http://localhost:8080/api`

## Required Backend Configuration

Before starting the backend, ensure:

1. **Database is configured** in `application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/mahal_db
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

2. **Razorpay credentials** are set (for subscription feature):
   ```properties
   razorpay.key.id=your_key_id
   razorpay.key.secret=your_key_secret
   ```

3. **Subscriptions table** exists in database:
   - Run the migration script: `V1__create_subscriptions_table.sql`
   - Or let Hibernate create it automatically with `spring.jpa.hibernate.ddl-auto=update`

## Development vs Production

### Development
- Backend runs on `localhost:8080`
- JavaFX app connects to `http://localhost:8080/api`
- Use Razorpay test credentials

### Production
- Backend runs on a server (e.g., `https://api.yourdomain.com`)
- Update `ApiService.java` BASE_URL to production URL
- Use Razorpay live credentials
- Configure webhook URL in Razorpay dashboard

## Next Steps

Once backend is running:
1. Start the JavaFX application
2. Login to the app
3. If subscription is inactive, you'll see the subscription screen
4. Click "Subscribe" on a plan
5. Complete payment via Razorpay checkout

For more details, see `SUBSCRIPTION_SETUP.md`.

