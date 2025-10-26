# Face Capture System - Setup Instructions

## What's New

The system now captures **15 preprocessed face images** per user during registration with advanced image processing:

### Image Preprocessing Pipeline
1. **Grayscale Conversion** - Reduces data size and focuses on facial features
2. **Histogram Equalization** - Normalizes lighting conditions
3. **Standardized Resizing** - All images resized to 224x224 pixels
4. **Gaussian Blur** - Reduces noise for better recognition
5. **Normalization** - Pixel values normalized to standard range

### Storage
- **15 images per user** stored in `face_images` table
- **1 primary image** stored in `profiles.face_image` for backwards compatibility
- Total capture time: ~15 seconds

---

## Database Setup (REQUIRED)

### Step 1: Run SQL Commands

Go to Supabase SQL Editor (https://rcbqthvswbrafceqflbz.supabase.co) and run:

```sql
-- Add face_image column to profiles table
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS face_image BYTEA;

-- Create face_images table for storing multiple preprocessed images
CREATE TABLE IF NOT EXISTS face_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    image_data BYTEA NOT NULL,
    image_number INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_face_images_user_id ON face_images(user_id);
```

### Step 2: Verify Schema

Run this to verify the tables were created:

```sql
SELECT
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name IN ('profiles', 'face_images')
ORDER BY table_name, ordinal_position;
```

Expected output:
- `profiles` table should have `face_image BYTEA` column
- `face_images` table should exist with columns: `id`, `user_id`, `image_data`, `image_number`, `created_at`

---

## Running the Application

### Build and Run

```bash
mvn clean compile
mvn javafx:run
```

Or use the JAR file:

```bash
mvn clean package -DskipTests
java -jar target/student-attendance-system-1.0.0.jar
```

---

## Registration Flow with Face Capture

### What Happens During Registration

1. **User fills registration form**
   - Name, Email, Password, Role (Student/Professor)

2. **Face capture starts automatically**
   - Camera activates
   - User should turn head slightly for variety
   - 15 images captured over ~15 seconds
   - Each image is preprocessed:
     - Converted to grayscale
     - Lighting normalized
     - Resized to 224x224
     - Noise reduced
     - Pixel values normalized

3. **Account creation**
   - User created in Supabase Auth
   - Profile created in `profiles` table
   - 15 preprocessed images saved to `face_images` table
   - 1 primary image saved to `profiles.face_image`

4. **Auto-login**
   - User automatically logged in
   - Routed to Student or Professor dashboard

### Console Output

During capture, you'll see:
```
Camera configured - Requested: 60 FPS, Actual: 30.0 FPS
Camera FPS: 29.87 (Frame #30)
Preprocessed face captured: 5432 bytes (224x224 grayscale, equalized, normalized)
Preprocessed face captured: 5124 bytes (224x224 grayscale, equalized, normalized)
...
Storing 15 face images for user: test@example.com
Saved 15 face images for user: a1b2c3d4-...
Successfully stored all 15 face images
```

---

## Troubleshooting

### "bad SQL grammar" Error

**Cause**: Database tables not created

**Solution**: Run the SQL commands from Step 1 above

### "Unable to connect to JDBC"

**Cause**: Network/firewall blocking Supabase

**Solutions**:
1. Check internet connection
2. Try different network (mobile hotspot)
3. Check if firewall is blocking port 6543
4. Verify credentials in `application.properties`

### Camera Not Working

**Causes**:
- Another app using camera (Zoom, Teams, etc.)
- Camera permissions not granted
- Camera index wrong

**Solutions**:
1. Close other applications using camera
2. Grant camera permissions in System Settings
3. Check console for camera FPS output
4. Try changing camera index in FaceCaptureView.java line 214

### Images Not Saving

**Check**:
1. Console shows: "Storing 15 face images..."
2. Console shows: "Saved 15 face images for user..."
3. No errors in console

**Verify in Database**:
```sql
SELECT user_id, COUNT(*) as image_count
FROM face_images
GROUP BY user_id;
```

Should show 15 images per user.

---

## Technical Details

### Image Preprocessing Code

Located in: `FaceCaptureView.java:328-367`

```java
// 1. Convert to grayscale
Imgproc.cvtColor(processedFace, grayFace, Imgproc.COLOR_BGR2GRAY);

// 2. Normalize lighting
Imgproc.equalizeHist(grayFace, equalizedFace);

// 3. Resize to 224x224
Imgproc.resize(equalizedFace, resizedFace, new Size(224, 224), 0, 0, Imgproc.INTER_LANCZOS4);

// 4. Reduce noise
Imgproc.GaussianBlur(resizedFace, blurredFace, new Size(3, 3), 0);

// 5. Normalize pixels
Core.normalize(blurredFace, normalizedFace, 0, 255, Core.NORM_MINMAX);
```

### Database Schema

**profiles table:**
```sql
id            UUID PRIMARY KEY
email         TEXT UNIQUE NOT NULL
name          TEXT NOT NULL
role          TEXT NOT NULL
face_encoding TEXT
face_image    BYTEA  -- Primary face image
```

**face_images table:**
```sql
id            UUID PRIMARY KEY
user_id       UUID REFERENCES auth.users(id)
image_data    BYTEA NOT NULL  -- Preprocessed 224x224 grayscale image
image_number  INTEGER         -- 1-15
created_at    TIMESTAMP
```

### File Structure

```
src/main/java/com/cs102/
├── model/
│   ├── User.java                 # User entity with face_image field
│   └── FaceImage.java            # NEW: Entity for multiple face images
├── repository/
│   ├── UserRepository.java
│   └── FaceImageRepository.java  # NEW: Repository for face images
├── manager/
│   ├── AuthenticationManager.java  # Added saveFaceImages() method
│   └── DatabaseManager.java        # Added face image management methods
├── service/
│   └── SupabaseAuthService.java
└── ui/
    ├── FaceCaptureView.java      # UPDATED: 15 images + preprocessing
    └── AuthView.java              # UPDATED: Saves all 15 images
```

---

## Performance Benefits

### Why Preprocessing?

1. **Grayscale** - Reduces image size by ~66% (3 channels → 1)
2. **Histogram Equalization** - Handles different lighting conditions
3. **Standard Size (224x224)** - Consistent dimensions for ML models
4. **Gaussian Blur** - Removes camera noise and artifacts
5. **Normalization** - Ensures consistent pixel value ranges

### Storage Efficiency

- Preprocessed image size: ~5-8 KB each
- Total per user: ~75-120 KB for 15 images
- Original color images would be: ~300-500 KB each (~6 MB total)
- **Space savings: ~95%**

### Recognition Benefits

- Multiple angles improve recognition accuracy
- Preprocessing makes images robust to lighting changes
- Standard size works with all ML face recognition models
- Grayscale focuses on structural features (not color)

---

## Next Steps

After setup, you can:

1. Test registration with face capture
2. Implement facial recognition login (TODO)
3. Add Python backend for face encoding/matching
4. Create face verification API endpoints

---

## Support

If you encounter issues:
1. Check console output for detailed errors
2. Verify database schema with SQL queries
3. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues
4. Check [database_schema_update.sql](database_schema_update.sql) for reference
