# 🎓 Student Attendance System

<div align="center">

![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-FF6C37?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)

**A comprehensive facial recognition-based attendance management system built for CS102 coursework**

[Report Bug](https://github.com/divsmoo/Smart-Attendance-System-with-Facial-Recognition/issues) · [Request Feature](https://github.com/divsmoo/Smart-Attendance-System-with-Facial-Recognition/issues)

</div>

---

## 📋 Table of Contents

- [About The Project](#-about-the-project)
- [Features](#-features)
- [Built With](#-built-with)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Project Structure](#-project-structure)
- [Usage](#-usage)
- [Dependencies](#-dependencies)
- [Contributing](#-contributing)
- [License](#-license)
- [Contact](#-contact)

---

## 🚀 About The Project

The Student Attendance System is an intelligent attendance management solution that leverages facial recognition technology to automate and streamline attendance tracking. As part of a CS102 course project, this system demonstrates the integration of modern technologies including computer vision, database management, and desktop application development.

### Key Highlights:

- 📸 **Facial Recognition** - Automated attendance marking using ArcFace model
- 🎨 **Modern UI** - Built with JavaFX for a responsive desktop experience
- 🔐 **Secure Backend** - Spring Boot with JPA for robust data management
- ☁️ **Cloud Database** - Supabase (PostgreSQL) for scalable data storage
- 📊 **Analytics Dashboard** - Visual insights with JFreeChart
- 👥 **Role-Based Access** - Different interfaces for students, professors, and admins

---

## ✨ Features

### For Students
- ✅ Mark attendance using facial recognition
- 📅 View attendance history and records
- 📈 Track attendance percentage
- 🔔 Get notified of attendance status

### For Professors
- 👥 Manage class rosters
- 📊 View class attendance statistics
- 📋 Generate attendance reports
- ⏰ Set attendance periods and schedules

### For Administrators
- 🛠️ System configuration and management
- 👤 User account management
- 📈 View system-wide analytics
- 🔒 Access control management

### Technical Features
- 📷 OpenCV-based face detection YuNet model
- 🧠 ONNX Runtime with ArcFace model for face recognition
- 💾 Persistent data storage with PostgreSQL
- 🔄 Real-time database synchronization
- 📊 Interactive charts and visualizations

---

## 🛠️ Built With

### Core Technologies
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

### Frontend
![JavaFX](https://img.shields.io/badge/JavaFX_21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JFreeChart](https://img.shields.io/badge/JFreeChart_1.5.4-ED8B00?style=for-the-badge&logo=java&logoColor=white)

### Backend & Database
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white) 
![Spring Data](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white) 
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)

### Computer Vision
![OpenCV](https://img.shields.io/badge/OpenCV-5C3EE8?style=for-the-badge&logo=opencv&logoColor=white)
![ONNX Runtime](https://img.shields.io/badge/ONNX_Runtime-005CED?style=for-the-badge&logo=onnx&logoColor=white)
- **ArcFace** - State-of-the-art face recognition model (512-dimensional embeddings)
- **YuNet** - Real-time face detection model

### Tools & Platforms
![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![Git LFS](https://img.shields.io/badge/Git_LFS-ED8B00?style=for-the-badge&logo=git&logoColor=white)
![VSCode](https://img.shields.io/badge/VSCode-007ACC?style=for-the-badge&logo=visual-studio-code&logoColor=white)

---

## 🏁 Getting Started

Follow these instructions to get the project running on your local machine.

### Prerequisites

Ensure you have the following installed:

- **Java Development Kit (JDK) 17 or higher**
  ```bash
  java -version
  ```

- **Apache Maven 3.6+**
  ```bash
  mvn -version
  ```

- **Git**
  ```bash
  git -version
  ```

- **Supabase Account** (for database)
  - Sign up at [supabase.com](https://supabase.com)

### 🔧 Installation & Configuration

#### 1. Clone the Repository
```bash
git clone https://github.com/divsmoo/Smart-Attendance-System-with-Facial-Recognition
cd Smart-Attendance-System-with-Facial-Recognition
```

#### 2. Configure Environment Variables

Duplicate and Rename `src/main/resources/application.properties`:
```bash
cp application.properties.example application.properties
```

Edit the following lines
```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://<AWS Server Region>.pooler.supabase.com:5432/postgres}
spring.datasource.username=${DB_USERNAME:<Database Username>}
spring.datasource.password=${DB_PASSWORD:<Database Password>}

spring.jpa.properties.hibernate.jdbc.time_zone=<Country & Timezone>

supabase.url=${SUPABASE_URL:<Supabase URL>}
supabase.api.key=${SUPABASE_API_KEY:<Supabase Anon Key>}
```

**Getting Supabase Credentials:**
1. Log in to your Supabase dashboard
2. Go to Project Settings → Database
3. Copy the connection string and extract the values

#### 3. Install Dependencies

```bash
mvn clean install
```

This will download all required dependencies including:
- Spring Boot starters
- JavaFX libraries
- OpenCV
- ONNX Runtime
- JFreeChart
- PostgreSQL driver

#### 4. Download Face Recognition Model

Download the ArcFace ONNX model and place it in `src/main/resources/models/`:

```bash
# Create models directory
mkdir -p src/main/resources/models

# Download arcface model
# Place your arcface.onnx model in this directory
wget https://huggingface.co/garavv/arcface-onnx/resolve/main/arc.onnx?download=true -O arcface.onnx

# Download
 wget ttps://huggingface.co/spaces/sam749/YuNet-face-detection/blob/main/face_detection_yunet_2023mar.onnx -O face_detection_yunet_2023mar.onnx
```

#### 5. Set Up Database Schema
```bash
# Open and Run FINAL_SCHEMA.sql in supabase
CREATE TABLE profiles (
    user_id VARCHAR(20) PRIMARY KEY,  -- e.g., S12345, P67890
    database_id UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
                ...

```

#### 6. Run the Application

```bash
mvn javafx:run
```

---

## 📁 Project Structure

```
CS102-/
├── 📂 src/
│   └── 📂 main/
│       ├── 📂 java/
│       │   └── 📂 com/cs102/
│       │       ├── 📂 ui/                                # JavaFX UI components
│       │       │   ├── UIApplication.java          
│       │       │   ├── StudentView.java            
│       │       │   └── ... 
│       │       ├── 📂 service/                           # Entity classes
│       │       │   ├── FacialRecognitionService.java
│       │       │   ├── IntrusionDetectionService.java
│       │       │   └── ...
│       │       ├── 📂 repository/                        # JPA repositories
│       │       │   ├── AttendanceRecordRepository.java
│       │       │   ├── ClassRepository.java
│       │       │   └── ...
│       │       ├── 📂 recognition/                       # Face recognition
│       │       │   └── ArcFaceRecognizer.java
│       │       ├── 📂 model/                             # Model classes
│       │       │   ├── AttendanceService.java
│       │       │   ├── SecurityEvent.java
│       │       │   └── ...
│       │       ├── 📂 manager/                           # Manager classes
│       │       │   ├── AuthenticationManager.java
│       │       │   ├── BackupManager.java
│       │       │   └── ...
│       │       └── 📂 config/                            # Config files
│       │           └── IDSConfig.java
|       |
│       └── 📂 resources/
│           ├── face_detection_yunet_2023mar.onnx         # Facial Detection Model
│           ├── arc.onxx                                  # Facial Recognition Model
│           └── application.properties.example            # Example environment file
|
├── 📄 pom.xml                                            # Maven configuration
├── 📄 README.md                                          # Project documentation
├── 📄 FINAL_SCHEMA.sql                                   # Database Schema
└── 📄 .gitignore                                         # Git ignore rules
└── 📄 .gitattributes                                     # Git LFS configuration
```

---

## 💻 Usage

### Running App

```bash
# Run in Developer mode
mvn clean compile

mvn javafx:run
```

### Building for Production

```bash
# Create executable JAR
mvn clean package

# Run the JAR
java -jar target/student-attendance-system-1.0.0.jar
```

### Common Maven Commands

```bash
# Clean build artifacts
mvn clean

# Compile the project
mvn compile

# Install to local repository
mvn install

# Skip tests during build
mvn clean install -DskipTests

# Run with specific profile
mvn javafx:run
```

---

## 📦 Dependencies

### Core Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.0 | Application framework |
| Spring Data JPA | 3.2.0 | Database ORM |
| JavaFX Controls | 21 | UI components |
| JavaFX FXML | 21 | UI markup language |
| PostgreSQL Driver | Latest | Database connectivity |

### Computer Vision & ML

| Dependency | Version | Purpose |
|------------|---------|---------|
| OpenCV | 4.9.0-0 | Face detection |
| ONNX Runtime | 1.16.3 | Face recognition inference |

### Data Visualization

| Dependency | Version | Purpose |
|------------|---------|---------|
| JFreeChart | 1.5.4 | Chart generation |
| JFreeChart FX | 2.0.2 | JavaFX chart integration |

### Testing

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot Test | 3.2.0 | Testing framework |
| JUnit | 5.x | Unit testing |

---

## 🐛 Troubleshooting

### Common Issues

**Issue: JavaFX runtime not found**
```bash
# Solution: Ensure JavaFX is properly configured in your IDE
# For IntelliJ: Add VM options
--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
```

**Issue: OpenCV library not loading**
```bash
# Solution: Verify OpenCV native libraries are extracted
# Maven should handle this automatically, but if not:
mvn dependency:unpack
```

**Issue: Database connection failure**
```bash
# Solution: Check Supabase credentials and network connection
# Test connection:
psql -h your-host -U your-username -d your-database
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📧 Project Link

**Project Link:** [https://github.com/divsmoo/Smart-Attendance-System-with-Facial-Recognition](https://github.com/divsmoo/Smart-Attendance-System-with-Facial-Recognition)

---

## 👥 Authors

<div align="center">

| Jeryl Khoo | Aaron Quek | Choo Jia Hong | Tan Sze Teng |
|:--------:|:--------:|:--------:|:--------:|
| [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/JerylKhoo) | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/treepillow) | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/jiahong2002) | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/sztng) |

| Zhuo Yu | Divyesh Rohan Ramesh | Ngm Yujia |
|:--------:|:--------:|:--------:|
| [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/yongzhuoyu) | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/divsmoo) | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/yujjjia) |

</div>

---

## 🙏 Acknowledgments

- CS102 Professor: Dr Zhang Zhi Yuan
- SMU School of Computing and Information Systems
- [OpenCV](https://opencv.org/) - Computer Vision Library
- [Spring Boot](https://spring.io/projects/spring-boot) - Application Framework
- [JavaFX](https://openjfx.io/) - Desktop UI Framework
- [Supabase](https://supabase.com/) - Cloud Database Platform
- [ArcFace](https://github.com/deepinsight/insightface) - Face Recognition Model

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

Made with ❤️ for CS102

</div>