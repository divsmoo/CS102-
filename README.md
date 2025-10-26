# Student Attendance System

CS102 Project - A JavaFX-based attendance system with facial recognition, integrated with Supabase authentication and PostgreSQL database.

## Features

- ✅ User authentication with Supabase Auth
- ✅ Role-based access (Student/Professor)
- ✅ Facial recognition capture during registration
- ✅ Automatic camera capture (5 images)
- ✅ Maximum FPS camera performance
- ✅ Platform-agnostic build (Windows/Linux/Mac)
- 🚧 Facial recognition login (TODO)
- 🚧 Attendance tracking (TODO)

## Prerequisites

- **Java 17 or higher** (required)
- **Maven 3.6+** (or use included Maven wrapper)
- **Webcam** (for facial recognition features)
- **Internet connection** (for Supabase database)

## Quick Start

### 1. Verify Java Version
```bash
java -version
```
Should show Java 17 or higher. If not, download from:
- https://adoptium.net/temurin/releases/

### 2. Clone and Build
```bash
cd CS102-
mvn clean install
```

### 3. Setup Database
Run this SQL in Supabase SQL Editor (https://rcbqthvswbrafceqflbz.supabase.co):
```sql
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS face_image BYTEA;
```

### 4. Run Application
```bash
mvn javafx:run
```

## Platform Support

This project now supports **all platforms**:
- ✅ Windows (x64)
- ✅ Linux (x64)
- ✅ Mac (Intel and Apple Silicon)

JavaFX dependencies will automatically download the correct native libraries for your platform.

## Building on Different Platforms

### Windows
```cmd
mvnw.cmd clean install
mvnw.cmd javafx:run
```

### Linux/Mac
```bash
./mvnw clean install
./mvnw javafx:run
```

## Project Structure

```
src/main/java/com/cs102/
├── model/          # JPA entities (User, UserRole)
├── repository/     # Spring Data JPA repositories
├── manager/        # Business logic layer
├── service/        # External service integrations (Supabase)
└── ui/             # JavaFX views
    ├── UIApplication.java    # Main application
    ├── AuthView.java         # Login/Register
    ├── FaceCaptureView.java  # Facial recognition
    ├── ProfessorView.java      # Professor dashboard
    └── StudentView.java      # Student dashboard
```

## Configuration

Database and API credentials are in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres
spring.datasource.username=postgres.rcbqthvswbrafceqflbz
spring.datasource.password=uvnxEJugdbewiJeS
supabase.url=https://rcbqthvswbrafceqflbz.supabase.co
supabase.api.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Technology Stack

- **JavaFX 21** - Desktop UI framework
- **Spring Boot 3.2.0** - Core framework (without web)
- **Spring Data JPA** - ORM and repository pattern
- **Hibernate** - JPA implementation
- **PostgreSQL** - Database (via Supabase)
- **Supabase** - Authentication and database hosting
- **OpenCV 4.9.0** - Face detection and image processing
- **Haar Cascade** - Face detection classifier

## Troubleshooting

### Build Fails
- Ensure Java 17+ is installed: `java -version`
- Clean and rebuild: `mvn clean install -X`
- See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for detailed solutions

### Camera Not Working
- Grant camera permissions in system settings
- Close other apps using the camera (Zoom, Teams, etc.)
- Check console for camera FPS output

### Database Connection Failed
- Check internet connection
- Verify Supabase database is running
- Run SQL to create `face_image` column (see step 3 above)

### Platform Issues
- **Windows:** Use `mvnw.cmd` instead of `./mvnw`
- **Linux:** May need to install v4l-utils: `sudo apt install v4l-utils`
- **Mac:** Should work out-of-box with Java 17+

For detailed troubleshooting, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

## Running from VSCode

1. Install extensions:
   - Extension Pack for Java
   - Maven for Java
   - Spring Boot Extension Pack

2. Open `UIApplication.java`

3. Click "Run" button above `main()` method, or press `F5`

## Camera Performance

The camera runs at **maximum FPS** (typically 30-60 FPS depending on hardware):
- Console shows real-time FPS: `Camera FPS: XX.XX`
- No artificial throttling
- Optimized for low latency

## Database Schema

```sql
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id),
  email TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('STUDENT', 'PROFESSOR')),
  face_encoding TEXT,
  face_image BYTEA
);
```

## Development Roadmap

### Completed ✅
- User registration with Supabase Auth
- Automatic login after registration
- Role-based dashboard routing
- Facial image capture (5 images automatically)
- Full camera view with face detection rectangles
- Detailed error messages in UI
- Platform-agnostic build system
- Maximum FPS camera capture

### In Progress 🚧
- Facial recognition login for students
- Attendance check-in/check-out
- View attendance history
- Professor session management

### Planned 📋
- Python backend for face encoding/matching
- REST API for face verification
- Export attendance reports
- Email notifications
- Multi-factor authentication

## Contributing

This is a CS102 academic project. For questions or issues:
1. Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. Check [SETUP_INSTRUCTIONS.md](SETUP_INSTRUCTIONS.md)
3. Review console output for detailed errors

## License

Academic project for CS102 - Singapore Management University

## Authors

CS102 Project Team