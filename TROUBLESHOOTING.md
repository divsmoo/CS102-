## Common Issues and Solutions

### 1. Java Version Mismatch

**Symptoms:**
```
[ERROR] invalid source release: 17
[ERROR] Source option 17 is no longer supported
```

**Cause:** This project requires **Java 17 or higher**. VSCode's Java extension might have installed Java 11 or an older version.

**Solution:**
1. Check Java version:
   ```bash
   java -version
   ```
   Should show: `java version "17"` or higher

2. Download and install Java 17 (or Java 21):
   - **Windows/Linux:** https://adoptium.net/temurin/releases/
   - **Alternative:** https://www.oracle.com/java/technologies/downloads/

3. In VSCode, set the Java version:
   - Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac)
   - Type: `Java: Configure Java Runtime`
   - Set Java 17 as the project JDK

4. Set `JAVA_HOME` environment variable:
   - **Windows:**
     ```cmd
     setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.x"
     ```
   - **Linux/Mac:**
     ```bash
     export JAVA_HOME=/path/to/jdk-17
     ```

---

### 2. Maven Not Installed or Not in PATH

**Symptoms:**
```
'mvn' is not recognized as an internal or external command
```

**Solution:**

#### Option A: Use Maven Wrapper (Included in Project)
Instead of `mvn`, use:
- **Windows:** `mvnw.cmd clean install`
- **Linux/Mac:** `./mvnw clean install`

#### Option B: Install Maven Globally
1. Download Maven from: https://maven.apache.org/download.cgi
2. Extract to a folder (e.g., `C:\Program Files\Apache\maven`)
3. Add to PATH:
   - **Windows:** Add `C:\Program Files\Apache\maven\bin` to System PATH
   - **Linux/Mac:** Add to `~/.bashrc` or `~/.zshrc`:
     ```bash
     export PATH=/path/to/maven/bin:$PATH
     ```
4. Verify installation:
   ```bash
   mvn -version
   ```

---

### 3. VSCode Java Extension Issues

**Solution:**

1. Install required VSCode extensions:
   - **Extension Pack for Java** (by Microsoft)
   - **Maven for Java** (by Microsoft)
   - **Spring Boot Extension Pack** (by VMware)

2. Reload VSCode:
   - Press `Ctrl+Shift+P` → `Developer: Reload Window`

3. Clean and rebuild:
   - Press `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
   - Then run: `mvn clean install`

---

### 4. Database Connection Issues

**Symptoms:**
```
Unable to acquire JDBC connection
Connection refused
```

**Cause:** Environment variables not set, or running on a network that blocks Supabase.

**Solution:**

1. Create a `.env` file or set environment variables:
   ```properties
   DB_URL=jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres
   DB_USERNAME=postgres.rcbqthvswbrafceqflbz
   DB_PASSWORD=uvnxEJugdbewiJeS
   SUPABASE_URL=https://rcbqthvswbrafceqflbz.supabase.co
   SUPABASE_API_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

2. Alternatively, edit `src/main/resources/application.properties` directly with the values.

3. Check firewall/network:
   - Some school/corporate networks block external databases
   - Try connecting from a different network or use VPN

---

### 5. OpenCV Native Library Issues

**Symptoms:**
```
UnsatisfiedLinkError: no opencv_java490 in java.library.path
```

**Cause:** OpenCV native libraries not loading properly for your platform.

**Solution:**

The project uses `org.openpnp:opencv:4.9.0-0` which should auto-detect platform, but if it fails:

1. Add explicit platform dependency to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.openpnp</groupId>
       <artifactId>opencv</artifactId>
       <version>4.9.0-0</version>
       <classifier>natives-windows-x86_64</classifier> <!-- For Windows -->
       <!-- OR -->
       <classifier>natives-linux-x86_64</classifier>   <!-- For Linux -->
       <!-- OR -->
       <classifier>natives-macosx-x86_64</classifier>  <!-- For Intel Mac -->
   </dependency>
   ```

2. Run: `mvn clean install` again

---

### 6. Camera/Webcam Not Working

**Symptoms:**
```
Failed to open camera
VideoCapture cannot open device
```

**Solution:**

1. **Windows:** Give camera permissions:
   - Settings → Privacy → Camera
   - Allow desktop apps to access camera

2. **Linux:** Install v4l-utils:
   ```bash
   sudo apt-get install v4l-utils
   ```

3. Check if camera is being used by another application (Zoom, Teams, etc.)

4. Test with a different camera index:
   - Edit `FaceCaptureView.java` line 214: Try `new VideoCapture(1)` instead of `0`

---

## How to Build and Run

### Step 1: Build the Project
```bash
mvn clean install
```

Expected output:
```
[INFO] BUILD SUCCESS
```

### Step 2: Run the Application
```bash
mvn javafx:run
```

**Alternative:** Run from VSCode:
- Open `UIApplication.java`
- Click the "Run" button above the `main` method
- Or press `F5` to debug

---

## How to Get More Detailed Error Information

If you're still getting errors, run with verbose output:

```bash
mvn clean install -X
```

The `-X` flag shows detailed debug information. Share the output to diagnose the issue.

---

## Quick Checklist for Your Friend

- [ ] Java 17 or higher installed (`java -version`)
- [ ] Maven installed or using Maven wrapper (`mvn -version`)
- [ ] VSCode Java extensions installed
- [ ] Database credentials configured in `application.properties`
- [ ] Camera permissions granted (for face capture)
- [ ] Firewall not blocking Supabase connection
- [ ] Run `mvn clean install` successfully
- [ ] Run `mvn javafx:run` to start application

---

## Still Having Issues?

1. **Get detailed error logs:**
   ```bash
   mvn clean install -X > build.log 2>&1
   ```
   This saves all output to `build.log` for analysis.

2. **Check Maven effective POM:**
   ```bash
   mvn help:effective-pom
   ```
   This shows the final resolved configuration.

3. **Verify dependencies:**
   ```bash
   mvn dependency:tree
   ```
   This shows all dependencies and their platforms.

---

## Platform-Specific Notes

### Windows Users
- Use Command Prompt or PowerShell (not Git Bash for Maven commands)
- Path separators are `\` instead of `/`
- Use `mvnw.cmd` instead of `./mvnw`

### Linux Users
- May need to install OpenJDK: `sudo apt install openjdk-17-jdk`
- May need to install Maven: `sudo apt install maven`
- Ensure webcam drivers are installed

### Mac Users (Intel)
- The project now auto-detects Mac vs Mac ARM
- Should work out of the box with Java 17+

---

## Contact

If errors persist after following this guide, please share:
1. Operating System (Windows/Linux/Mac)
2. Java version (`java -version`)
3. Maven version (`mvn -version`)
4. Full error message from `mvn clean install -X`
