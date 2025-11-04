package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.manager.DatabaseManager;
import com.cs102.model.AttendanceRecord;
import com.cs102.model.Session;
import com.cs102.model.StudentAttendanceData;
import com.cs102.model.User;
import com.cs102.service.FaceAntiSpoofingService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.*;

public class StudentView {

    private Stage stage;
    private User student;
    private AuthenticationManager authManager;
    private DatabaseManager dbManager;

    private BorderPane mainLayout;
    private ComboBox<String> yearDropdown;
    private ComboBox<String> semesterDropdown;
    private TableView<StudentAttendanceData> attendanceTable;
    private ObservableList<StudentAttendanceData> attendanceData;
    private String currentPage = "Home";

    public StudentView(Stage stage, User student, AuthenticationManager authManager) {
        this.stage = stage;
        this.student = student;
        this.authManager = authManager;
        this.dbManager = authManager.getDatabaseManager();
        this.attendanceData = FXCollections.observableArrayList();
    }

    public Scene createScene() {
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // Navigation bar at the very top
        HBox navbar = createNavbar();

        // Top Section - Header with year/semester selection
        VBox topSection = createTopSection();

        // Combine navbar and top section
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(navbar, topSection);

        mainLayout.setTop(topContainer);

        // Center Section - Attendance Table
        VBox centerSection = createCenterSection();
        mainLayout.setCenter(centerSection);

        Scene scene = new Scene(mainLayout, 1200, 800);

        // Auto-load current year and semester
        loadYearSemesterOptions();

        return scene;
    }

    private HBox createNavbar() {
        HBox navbar = new HBox(20);
        navbar.setPadding(new Insets(15, 30, 15, 30));
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 2 0;");

        // App title/logo
        Label appTitle = new Label("Student Portal");
        appTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));
        appTitle.setStyle("-fx-text-fill: white;");

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Home button
        Button homeButton = createNavButton("Home");
        homeButton.setOnAction(e -> navigateTo("Home"));

        // Settings button
        Button settingsButton = createNavButton("Settings");
        settingsButton.setOnAction(e -> navigateTo("Settings"));

        // Logout button
        Button logoutButton = new Button("Logout");
        logoutButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-padding: 8 20 8 20; -fx-cursor: hand;");
        logoutButton.setOnMouseEntered(e -> logoutButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 8 20 8 20; -fx-cursor: hand;"));
        logoutButton.setOnMouseExited(e -> logoutButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 8 20 8 20; -fx-cursor: hand;"));
        logoutButton.setOnAction(e -> {
            AuthView authView = new AuthView(stage, authManager);
            stage.setScene(authView.createScene());
        });

        navbar.getChildren().addAll(appTitle, spacer, homeButton, settingsButton, logoutButton);
        return navbar;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");

        btn.setOnMouseEntered(e -> {
            if (!currentPage.equals(text)) {
                btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");
            }
        });

        btn.setOnMouseExited(e -> {
            if (!currentPage.equals(text)) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");
            }
        });

        if (currentPage.equals(text)) {
            btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");
        }

        return btn;
    }

    private void navigateTo(String page) {
        currentPage = page;

        switch (page) {
            case "Home":
                showHomePage();
                break;
            case "Settings":
                showSettingsPage();
                break;
        }
    }

    private void showHomePage() {
        // Update navbar
        HBox navbar = createNavbar();

        // Top Section - Header with year/semester selection
        VBox topSection = createTopSection();

        // Combine navbar and top section
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(navbar, topSection);

        mainLayout.setTop(topContainer);

        // Center Section - Attendance Table
        VBox centerSection = createCenterSection();
        mainLayout.setCenter(centerSection);

        // Reload attendance data
        loadAttendanceData();
    }

    private VBox createTopSection() {
        VBox topSection = new VBox(15);
        topSection.setPadding(new Insets(30, 30, 20, 30));

        // Title
        Text title = new Text("My Attendance");
        title.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

        // Student info
        Label studentInfo = new Label(student.getName() + " (" + student.getUserId() + ")");
        studentInfo.setFont(Font.font("Tahoma", FontWeight.NORMAL, 16));
        studentInfo.setStyle("-fx-text-fill: #666;");

        // Year and Semester selection row
        HBox selectionRow = new HBox(15);
        selectionRow.setAlignment(Pos.CENTER_LEFT);

        // Year selector
        Label yearLabel = new Label("Year:");
        yearLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        yearDropdown = new ComboBox<>();
        yearDropdown.setPromptText("Select year");
        yearDropdown.setPrefWidth(120);
        yearDropdown.setOnAction(e -> loadAttendanceData());

        // Semester selector
        Label semesterLabel = new Label("Semester:");
        semesterLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        semesterDropdown = new ComboBox<>();
        semesterDropdown.getItems().addAll("Semester 1", "Semester 2");
        semesterDropdown.setPromptText("Select semester");
        semesterDropdown.setPrefWidth(150);
        semesterDropdown.setOnAction(e -> loadAttendanceData());

        // Mark Attendance button
        Button markAttendanceButton = new Button("Mark Attendance");
        markAttendanceButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");
        markAttendanceButton.setOnMouseEntered(e -> markAttendanceButton.setStyle("-fx-background-color: #229954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;"));
        markAttendanceButton.setOnMouseExited(e -> markAttendanceButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;"));
        markAttendanceButton.setOnAction(e -> showMarkAttendanceDialog());

        // Legend
        HBox legend = createLegend();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        selectionRow.getChildren().addAll(yearLabel, yearDropdown, semesterLabel, semesterDropdown, spacer, markAttendanceButton, legend);

        topSection.getChildren().addAll(title, studentInfo, selectionRow);
        return topSection;
    }

    private HBox createLegend() {
        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.setPadding(new Insets(5, 10, 5, 10));
        legend.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label legendTitle = new Label("Legend:");
        legendTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 12));

        Label presentLabel = createLegendItem("P = Present", "#90EE90");
        Label lateLabel = createLegendItem("L = Late", "#FFD700");
        Label absentLabel = createLegendItem("A = Absent", "#FFB6C1");

        legend.getChildren().addAll(legendTitle, presentLabel, lateLabel, absentLabel);
        return legend;
    }

    private Label createLegendItem(String text, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Tahoma", 11));
        label.setPadding(new Insets(3, 8, 3, 8));
        label.setStyle("-fx-background-color: " + color + "; -fx-text-fill: black; -fx-border-radius: 3; -fx-background-radius: 3;");
        return label;
    }

    private VBox createCenterSection() {
        VBox centerSection = new VBox(10);
        centerSection.setPadding(new Insets(0, 30, 30, 30));

        // Create table
        attendanceTable = new TableView<>();
        attendanceTable.setItems(attendanceData);
        attendanceTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);  // Match professor view

        // Create columns
        createTableColumns();

        // Placeholder text when no year/semester is selected
        Label placeholderLabel = new Label("Please select a year and semester from the dropdowns above");
        placeholderLabel.setFont(Font.font("Tahoma", 14));
        placeholderLabel.setStyle("-fx-text-fill: #999;");
        attendanceTable.setPlaceholder(placeholderLabel);

        centerSection.getChildren().add(attendanceTable);
        VBox.setVgrow(attendanceTable, Priority.ALWAYS);

        return centerSection;
    }

    private void createTableColumns() {
        // Course column
        TableColumn<StudentAttendanceData, String> courseColumn = new TableColumn<>("Course");
        courseColumn.setCellValueFactory(new PropertyValueFactory<>("studentName")); // Reusing field
        courseColumn.setPrefWidth(188);
        courseColumn.setMinWidth(150);
        courseColumn.setResizable(true);

        // Section column
        TableColumn<StudentAttendanceData, String> sectionColumn = new TableColumn<>("Section");
        sectionColumn.setCellValueFactory(new PropertyValueFactory<>("studentId")); // Reusing field
        sectionColumn.setPrefWidth(150);
        sectionColumn.setMinWidth(100);
        sectionColumn.setResizable(true);

        // Weeks parent column (equivalent to Sessions in professor view)
        TableColumn<StudentAttendanceData, String> weeksParentColumn = new TableColumn<>("Weeks");

        // Create 13 week sub-columns
        for (int week = 1; week <= 13; week++) {
            TableColumn<StudentAttendanceData, String> weekColumn = createWeekColumn(week);
            weeksParentColumn.getColumns().add(weekColumn);
        }

        // Totals parent column (with P, L, A sub-columns like professor view)
        TableColumn<StudentAttendanceData, String> totalsParentColumn = new TableColumn<>("Totals");

        TableColumn<StudentAttendanceData, String> totalPresentCol = new TableColumn<>("P");
        totalPresentCol.setCellValueFactory(cellData -> cellData.getValue().totalPresentProperty());
        totalPresentCol.setPrefWidth(50);
        totalPresentCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StudentAttendanceData, String> totalLateCol = new TableColumn<>("L");
        totalLateCol.setCellValueFactory(cellData -> cellData.getValue().totalLateProperty());
        totalLateCol.setPrefWidth(50);
        totalLateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StudentAttendanceData, String> totalAbsentCol = new TableColumn<>("A");
        totalAbsentCol.setCellValueFactory(cellData -> cellData.getValue().totalAbsentProperty());
        totalAbsentCol.setPrefWidth(50);
        totalAbsentCol.setStyle("-fx-alignment: CENTER;");

        totalsParentColumn.getColumns().add(totalPresentCol);
        totalsParentColumn.getColumns().add(totalLateCol);
        totalsParentColumn.getColumns().add(totalAbsentCol);

        attendanceTable.getColumns().add(courseColumn);
        attendanceTable.getColumns().add(sectionColumn);
        attendanceTable.getColumns().add(weeksParentColumn);
        attendanceTable.getColumns().add(totalsParentColumn);
    }

    private TableColumn<StudentAttendanceData, String> createWeekColumn(int weekNumber) {
        TableColumn<StudentAttendanceData, String> column = new TableColumn<>(String.valueOf(weekNumber));
        column.setPrefWidth(50);
        column.setStyle("-fx-alignment: CENTER;");

        // Use a custom cell value factory
        column.setCellValueFactory(cellData -> cellData.getValue().weekProperty(weekNumber));

        // Apply color styling - EXACTLY like ProfessorView
        column.setCellFactory(col -> new TableCell<StudentAttendanceData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Color coding - same as ProfessorView
                    switch (item) {
                        case "P":
                            setStyle("-fx-background-color: #90EE90; -fx-alignment: CENTER;"); // Light green
                            break;
                        case "L":
                            setStyle("-fx-background-color: #FFD700; -fx-alignment: CENTER;"); // Gold/Yellow
                            break;
                        case "A":
                            setStyle("-fx-background-color: #FFB6C1; -fx-alignment: CENTER;"); // Light red
                            break;
                        case "N/A":
                        case "-":
                            setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #999999; -fx-alignment: CENTER;"); // Gray
                            setText("-");
                            break;
                        default:
                            setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        return column;
    }

    /**
     * Get current semester based on current date
     * July-December = Semester 1
     * January-June = Semester 2
     */
    private String getCurrentSemester() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();

        if (month >= 7 && month <= 12) {
            return "Semester 1";
        } else {
            return "Semester 2";
        }
    }

    /**
     * Get current year
     */
    private int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    /**
     * Load year options and set current year/semester
     */
    public void loadYearSemesterOptions() {
        // Get all courses to extract unique years and semesters
        List<com.cs102.model.Course> allCourses = dbManager.findAllCourses();
        Set<Integer> years = new TreeSet<>(Collections.reverseOrder()); // Descending order

        // Extract years from course semester field (format: "2025-Semester 1")
        for (com.cs102.model.Course course : allCourses) {
            if (course.getSemester() != null && course.getSemester().contains("-")) {
                String[] parts = course.getSemester().split("-");
                try {
                    years.add(Integer.parseInt(parts[0]));
                } catch (NumberFormatException e) {
                    // Skip invalid year format
                }
            }
        }

        // If no years found, add current year
        if (years.isEmpty()) {
            years.add(getCurrentYear());
        }

        // Populate year dropdown
        ObservableList<String> yearStrings = FXCollections.observableArrayList();
        for (Integer year : years) {
            yearStrings.add(String.valueOf(year));
        }
        yearDropdown.setItems(yearStrings);

        // Set current year and semester
        String currentYear = String.valueOf(getCurrentYear());
        if (yearStrings.contains(currentYear)) {
            yearDropdown.setValue(currentYear);
        } else if (!yearStrings.isEmpty()) {
            yearDropdown.setValue(yearStrings.get(0)); // Most recent year
        }

        semesterDropdown.setValue(getCurrentSemester());

        // Load data for current selection
        loadAttendanceData();
    }

    /**
     * Load attendance data for the selected year and semester
     */
    private void loadAttendanceData() {
        String selectedYear = yearDropdown.getValue();
        String selectedSemester = semesterDropdown.getValue();

        if (selectedYear == null || selectedSemester == null) {
            return;
        }

        // Clear existing data
        attendanceData.clear();

        // Build semester string (format: "2025-Semester 1")
        String semesterFilter = selectedYear + "-" + selectedSemester;

        // Get student's enrolled courses
        List<com.cs102.model.Class> enrollments = dbManager.findEnrollmentsByUserId(student.getUserId());

        // Create a set of enrolled course-section pairs for quick lookup
        Set<String> enrolledCourseSections = new java.util.HashSet<>();
        for (com.cs102.model.Class enrollment : enrollments) {
            enrolledCourseSections.add(enrollment.getCourse() + "-" + enrollment.getSection());
        }

        // Get all courses for the selected semester
        List<com.cs102.model.Course> allCourses = dbManager.findAllCourses();

        for (com.cs102.model.Course course : allCourses) {
            // Filter by semester
            if (course.getSemester() == null || !course.getSemester().equals(semesterFilter)) {
                continue;
            }

            String courseName = course.getCourse();
            String section = course.getSection();

            // Only show courses the student is enrolled in
            if (!enrolledCourseSections.contains(courseName + "-" + section)) {
                continue;
            }

            // Get all sessions for this course/section
            List<Session> sessions = dbManager.findSessionsByCourseAndSection(courseName, section);

            if (sessions.isEmpty()) {
                continue;
            }

            // Sort sessions by date
            sessions.sort(Comparator.comparing(Session::getDate));

            // Get attendance records for this student in this course
            List<AttendanceRecord> attendanceRecords = dbManager.getAttendanceForStudentInCourse(
                    student.getUserId(), courseName);

            // Create a row for this course/section (reusing StudentAttendanceData)
            // Store course in "studentName" field and section in "studentId" field
            StudentAttendanceData rowData = new StudentAttendanceData(courseName, section);

            // Pack student's attended sessions to the left (no gaps)
            int weekCounter = 1;
            for (Session session : sessions) {
                if (weekCounter > 13) break;

                // Find attendance record for this session
                AttendanceRecord attendanceForSession = attendanceRecords.stream()
                        .filter(record -> record.getSessionId().equals(session.getId()))
                        .findFirst()
                        .orElse(null);

                if (attendanceForSession != null) {
                    String status = convertAttendanceStatus(attendanceForSession.getAttendance());
                    rowData.setWeekAttendance(weekCounter, status);
                    weekCounter++;
                }
            }

            // Add the row to the table
            attendanceData.add(rowData);
        }
    }

    /**
     * Convert full attendance status to single letter
     */
    private String convertAttendanceStatus(String status) {
        if (status == null) return "-";
        switch (status.toLowerCase()) {
            case "present":
                return "P";
            case "late":
                return "L";
            case "absent":
                return "A";
            default:
                return "-";
        }
    }

    private void showSettingsPage() {
        // Update navbar (without year/semester/legend section)
        HBox navbar = createNavbar();
        mainLayout.setTop(navbar);

        // Create settings content
        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 50, 30, 50));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #f5f5f5;");

        // Title
        Label titleLabel = new Label("Settings");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // Settings form container
        VBox formContainer = new VBox(15);
        formContainer.setPadding(new Insets(30));
        formContainer.setMaxWidth(600);
        formContainer.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Account Settings Section Header
        Label accountLabel = new Label("Account Settings");
        accountLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 16));
        accountLabel.setStyle("-fx-text-fill: #2c3e50;");

        // Email Section
        Label emailLabel = new Label("Email:");
        emailLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        TextField emailField = new TextField();
        emailField.setPromptText("Enter new email");
        emailField.setText(student.getEmail());
        emailField.setPrefHeight(35);
        emailField.setStyle("-fx-font-size: 14px;");

        // New Password Section
        Label newPasswordLabel = new Label("New Password:");
        newPasswordLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password (min 6 characters)");
        newPasswordField.setPrefHeight(35);
        newPasswordField.setStyle("-fx-font-size: 14px;");

        // Confirm Password Section
        Label confirmPasswordLabel = new Label("Confirm Password:");
        confirmPasswordLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        confirmPasswordField.setPrefHeight(35);
        confirmPasswordField.setStyle("-fx-font-size: 14px;");

        Label passwordHint = new Label("Leave password fields empty to keep current password.");
        passwordHint.setFont(Font.font("Tahoma", 11));
        passwordHint.setStyle("-fx-text-fill: #666;");

        // Status label for feedback
        Label statusLabel = new Label();
        statusLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 12));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(540);

        // Save button
        Button saveButton = new Button("Save Settings");
        saveButton.setPrefWidth(200);
        saveButton.setPrefHeight(40);
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        saveButton.setOnMouseEntered(e -> saveButton.setStyle("-fx-background-color: #229954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;"));

        saveButton.setOnAction(e -> {
            statusLabel.setText("");
            statusLabel.setStyle("");

            // Validate and save settings
            String validationResult = validateAndSaveSettings(
                emailField.getText().trim(),
                newPasswordField.getText(),
                confirmPasswordField.getText(),
                statusLabel
            );

            if (validationResult.equals("SUCCESS")) {
                statusLabel.setText("✓ Settings saved successfully!");
                statusLabel.setStyle("-fx-text-fill: #27ae60;");
                // Clear password fields
                newPasswordField.clear();
                confirmPasswordField.clear();
            } else {
                statusLabel.setText("✗ " + validationResult);
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        });

        // Separator for Face Recognition section
        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));

        // Face Recognition Section Header
        Label faceLabel = new Label("Face Recognition");
        faceLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 16));
        faceLabel.setStyle("-fx-text-fill: #2c3e50;");

        Label faceHint = new Label("Update your facial recognition data for attendance tracking.");
        faceHint.setFont(Font.font("Tahoma", 11));
        faceHint.setStyle("-fx-text-fill: #666;");

        // Redetect Face button
        Button redetectFaceButton = new Button("Redetect Face");
        redetectFaceButton.setPrefWidth(200);
        redetectFaceButton.setPrefHeight(40);
        redetectFaceButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        redetectFaceButton.setOnMouseEntered(e -> redetectFaceButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;"));
        redetectFaceButton.setOnMouseExited(e -> redetectFaceButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;"));

        redetectFaceButton.setOnAction(e -> {
            showFaceRedetectionDialog();
        });

        // Add all components to form (no late threshold for students)
        formContainer.getChildren().addAll(
            accountLabel,
            emailLabel, emailField,
            newPasswordLabel, newPasswordField,
            confirmPasswordLabel, confirmPasswordField,
            passwordHint,
            statusLabel,
            saveButton,
            separator,
            faceLabel,
            faceHint,
            redetectFaceButton
        );

        content.getChildren().addAll(titleLabel, formContainer);

        // Update the center content (same as ProfessorView)
        mainLayout.setCenter(content);
    }

    private void showFaceRedetectionDialog() {
        // Create a new stage for the face capture dialog
        javafx.stage.Stage faceDialog = new javafx.stage.Stage();
        faceDialog.setTitle("Redetect Face");
        faceDialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        faceDialog.initOwner(stage);

        // Create FaceCaptureView with callbacks
        FaceCaptureView faceCaptureView = new FaceCaptureView(
            faceDialog,
            (capturedFaces) -> {
                // Face capture complete - update face images
                updateFaceImages(capturedFaces);
                faceDialog.close();

                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Face Updated");
                alert.setHeaderText(null);
                alert.setContentText("Your facial recognition data has been successfully updated!");
                alert.showAndWait();
            },
            () -> {
                // User cancelled
                faceDialog.close();
            }
        );

        faceDialog.setScene(faceCaptureView.createScene());
        faceDialog.show();
    }

    private void updateFaceImages(java.util.List<byte[]> capturedFaces) {
        if (capturedFaces == null || capturedFaces.isEmpty()) {
            return;
        }

        System.out.println("Updating face images for student: " + student.getEmail());

        // Delete old face images
        authManager.deleteFaceImages(student);

        // Save new face images
        authManager.saveFaceImages(student, capturedFaces);

        System.out.println("Face images updated successfully - " + capturedFaces.size() + " images stored");
    }

    private void showMarkAttendanceDialog() {
        // Find current active session for this student
        Session currentSession = findCurrentActiveSession();

        if (currentSession == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Active Session");
            alert.setHeaderText(null);
            alert.setContentText("There is no active session at the moment.\nPlease check with your professor.");
            alert.showAndWait();
            return;
        }

        // Show live recognition dialog
        showLiveRecognitionForAttendance(currentSession);
    }

    private Session findCurrentActiveSession() {
        // Get all courses the student is enrolled in
        java.util.List<com.cs102.model.Class> enrollments = dbManager.findEnrollmentsByUserId(student.getUserId());

        if (enrollments.isEmpty()) {
            System.out.println("Student is not enrolled in any courses");
            return null;
        }

        // Get current time in Singapore timezone
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Singapore"));
        java.time.LocalDate today = now.toLocalDate();
        java.time.LocalTime currentTime = now.toLocalTime();

        System.out.println("=== FINDING ACTIVE SESSION ===");
        System.out.println("Current time: " + now);
        System.out.println("Student enrolled in " + enrollments.size() + " courses");

        // Check each enrolled course for an active session
        for (com.cs102.model.Class enrollment : enrollments) {
            String course = enrollment.getCourse();
            String section = enrollment.getSection();

            System.out.println("Checking course: " + course + ", section: " + section);

            // Get all sessions for this course/section
            java.util.List<Session> sessions = dbManager.findSessionsByCourseAndSection(course, section);

            for (Session session : sessions) {
                // Check if session is today
                if (!session.getDate().equals(today)) {
                    continue;
                }

                java.time.LocalTime sessionStart = session.getStartTime();
                java.time.LocalTime sessionEnd = session.getEndTime();

                System.out.println("  Session found: " + session.getDate() + " " + sessionStart + " - " + sessionEnd);

                // Check if current time is within session window
                if (currentTime.isAfter(sessionStart) && currentTime.isBefore(sessionEnd)) {
                    System.out.println("  ✓ Active session found!");
                    return session;
                }
            }
        }

        System.out.println("No active session found");
        return null;
    }

    private void showLiveRecognitionForAttendance(Session session) {
        // Create a new stage for the live recognition dialog
        javafx.stage.Stage recognitionDialog = new javafx.stage.Stage();
        recognitionDialog.setTitle("Mark Attendance - " + session.getCourse() + " " + session.getSection());
        recognitionDialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        recognitionDialog.initOwner(stage);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f5f5f5;");

        // Header
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Mark Attendance");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        Label courseLabel = new Label(session.getCourse() + " - " + session.getSection());
        courseLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 16));

        Label instructions = new Label("Please look at the camera to mark your attendance");
        instructions.setFont(Font.font("Tahoma", 14));
        instructions.setStyle("-fx-text-fill: #666;");

        Label statusLabel = new Label("Initializing camera...");
        statusLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        header.getChildren().addAll(titleLabel, courseLabel, instructions, statusLabel);

        // Camera view
        javafx.scene.image.ImageView cameraView = new javafx.scene.image.ImageView();
        cameraView.setFitWidth(640);
        cameraView.setFitHeight(480);
        cameraView.setPreserveRatio(true);
        cameraView.setStyle("-fx-border-color: #ddd; -fx-border-width: 2;");

        // Log list
        ListView<Label> logList = new ListView<>();
        logList.setPrefHeight(150);
        logList.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd;");

        // Center content with camera and log
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(cameraView, logList);

        // Bottom controls
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(15, 0, 0, 0));

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(150);
        cancelButton.setPrefHeight(40);
        cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        cancelButton.setOnAction(e -> recognitionDialog.close());

        controls.getChildren().add(cancelButton);

        layout.setTop(header);
        layout.setCenter(centerBox);
        layout.setBottom(controls);

        Scene scene = new Scene(layout, 800, 700);
        recognitionDialog.setScene(scene);

        // Start live recognition in background thread
        recognitionDialog.setOnShown(e -> {
            Thread recognitionThread = new Thread(() -> {
                runStudentLiveRecognition(session, cameraView, statusLabel, logList, recognitionDialog);
            });
            recognitionThread.setDaemon(true);
            recognitionThread.start();
        });

        recognitionDialog.showAndWait();
    }

    private void addLogEntry(ListView<Label> logList, String message, String color) {
        javafx.application.Platform.runLater(() -> {
            Label logLabel = new Label(message);
            logLabel.setStyle("-fx-text-fill: " + color + ";");
            logList.getItems().add(logLabel);
            logList.scrollTo(logList.getItems().size() - 1);
        });
    }

    private void runStudentLiveRecognition(Session session, javafx.scene.image.ImageView cameraView, Label statusLabel, ListView<Label> logList, javafx.stage.Stage dialog) {
        // Load OpenCV
        nu.pattern.OpenCV.loadLocally();
        addLogEntry(logList, "Initializing face recognition system...", "#3498db");

        // Initialize ArcFace recognizer
        com.cs102.recognition.ArcFaceRecognizer arcFace;
        try {
            arcFace = new com.cs102.recognition.ArcFaceRecognizer();
            addLogEntry(logList, "✓ ArcFace recognizer initialized", "#27ae60");
        } catch (Exception e) {
            System.err.println("Failed to initialize ArcFace: " + e.getMessage());
            addLogEntry(logList, "✗ Failed to initialize face recognition: " + e.getMessage(), "#e74c3c");
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Failed to initialize face recognition");
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            });
            return;
        }

        // Load student's face embeddings
        addLogEntry(logList, "Loading your face data...", "#3498db");
        java.util.List<com.cs102.model.FaceImage> faceImages = dbManager.findFaceImagesByUserId(student.getUserId());
        java.util.List<float[]> studentEmbeddings = new java.util.ArrayList<>();

        System.out.println("Loading student face embeddings for: " + student.getName());

        for (com.cs102.model.FaceImage faceImage : faceImages) {
            try {
                byte[] imageData = faceImage.getImageData();
                org.opencv.core.MatOfByte matOfByte = new org.opencv.core.MatOfByte(imageData);
                org.opencv.core.Mat faceMat = org.opencv.imgcodecs.Imgcodecs.imdecode(matOfByte, org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR);

                if (faceMat != null && !faceMat.empty()) {
                    org.opencv.core.Mat preprocessed = arcFace.preprocessFace(faceMat);
                    float[] embedding = arcFace.extractEmbedding(preprocessed);
                    studentEmbeddings.add(embedding);
                    faceMat.release();
                    preprocessed.release();
                }
            } catch (Exception e) {
                System.err.println("Failed to process face image: " + e.getMessage());
            }
        }

        if (studentEmbeddings.isEmpty()) {
            addLogEntry(logList, "✗ No face data found. Please register your face first.", "#e74c3c");
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("No face data found. Please register your face first.");
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            });
            return;
        }

        addLogEntry(logList, "✓ Loaded " + studentEmbeddings.size() + " face embeddings", "#27ae60");
        System.out.println("Loaded " + studentEmbeddings.size() + " embeddings for student");

        // Start camera
        addLogEntry(logList, "Starting camera...", "#3498db");
        org.opencv.videoio.VideoCapture camera = new org.opencv.videoio.VideoCapture(0);
        if (!camera.isOpened()) {
            addLogEntry(logList, "✗ Failed to open camera", "#e74c3c");
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Failed to open camera");
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            });
            return;
        }

        addLogEntry(logList, "✓ Camera active - Looking for your face...", "#27ae60");
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText("Camera active - Looking for your face...");
            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        });

        // Initialize YuNet face detector
        addLogEntry(logList, "Initializing face detector...", "#3498db");
        org.opencv.objdetect.FaceDetectorYN faceDetector = initializeFaceDetector((int)camera.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH),
                                                                                    (int)camera.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT));
        if (faceDetector == null) {
            addLogEntry(logList, "✗ Failed to initialize face detector", "#e74c3c");
            camera.release();
            return;
        }
        addLogEntry(logList, "✓ Face detector ready", "#27ae60");

        org.opencv.core.Mat frame = new org.opencv.core.Mat();
        boolean recognized = false;

        while (!recognized && camera.isOpened()) {
            if (camera.read(frame) && !frame.empty()) {
                // Detect faces using YuNet
                org.opencv.core.Mat faces = new org.opencv.core.Mat();
                faceDetector.detect(frame, faces);

                if (faces.rows() > 0) {
                    try {
                        // Get the largest face
                        int largestFaceIndex = 0;
                        float largestArea = 0;

                        for (int i = 0; i < faces.rows(); i++) {
                            float w = (float) faces.get(i, 2)[0];
                            float h = (float) faces.get(i, 3)[0];
                            float area = w * h;
                            if (area > largestArea) {
                                largestArea = area;
                                largestFaceIndex = i;
                            }
                        }

                        float x = (float) faces.get(largestFaceIndex, 0)[0];
                        float y = (float) faces.get(largestFaceIndex, 1)[0];
                        float w = (float) faces.get(largestFaceIndex, 2)[0];
                        float h = (float) faces.get(largestFaceIndex, 3)[0];

                        org.opencv.core.Rect faceRect = new org.opencv.core.Rect((int)x, (int)y, (int)w, (int)h);

                        // Extract and process face
                        org.opencv.core.Mat face = new org.opencv.core.Mat(frame, faceRect);
                        
                        // ===== ANTI-SPOOFING CHECK =====
                        FaceAntiSpoofingService antiSpoof = new FaceAntiSpoofingService();
                        FaceAntiSpoofingService.SpoofingAnalysisResult spoofResult = 
                            antiSpoof.analyzeFace(face, student.getEmail());
                        
                        if (!spoofResult.isLive()) {
                            System.err.println("⚠️ SPOOFING DETECTED: " + spoofResult.getDetails());
                            addLogEntry(logList, "⚠️ Fake face detected! Use real camera.", "#e74c3c");
                            
                            javafx.application.Platform.runLater(() -> {
                                statusLabel.setText("⚠️ Spoofing detected - Please use real camera");
                                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            });
                            
                            face.release();
                            faces.release();
                            continue; // Skip this frame
                        }
                        
                        System.out.println("✓ Liveness verified (Score: " + 
                            String.format("%.1f%%", spoofResult.getConfidenceScore()) + ")");
                        addLogEntry(logList, "✓ Liveness check passed", "#27ae60");
                        
                        org.opencv.core.Mat preprocessed = arcFace.preprocessFace(face);
                        float[] detectedEmbedding = arcFace.extractEmbedding(preprocessed);

                        // Compare with student's embeddings
                        double maxSimilarity = 0.0;
                        for (float[] storedEmbedding : studentEmbeddings) {
                            double similarity = com.cs102.recognition.ArcFaceRecognizer.cosineSimilarity(detectedEmbedding, storedEmbedding);
                            if (similarity > maxSimilarity) {
                                maxSimilarity = similarity;
                            }
                        }

                        double confidence = maxSimilarity * 100;
                        System.out.println("Face detected - Confidence: " + confidence + "%");
                        addLogEntry(logList, "Face detected - Confidence: " + String.format("%.1f", confidence) + "%", "#3498db");

                        // Draw rectangle
                        org.opencv.imgproc.Imgproc.rectangle(frame, faceRect.tl(), faceRect.br(), new org.opencv.core.Scalar(0, 255, 0), 2);

                        final double finalConfidence = confidence;

                    if (confidence >= 70) {
                        // High confidence - mark attendance immediately
                        recognized = true;
                        camera.release();

                        addLogEntry(logList, "✓ High confidence match! Marking attendance...", "#27ae60");
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText("✓ Recognized! Marking attendance...");
                            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        });

                        // Mark attendance
                        markStudentAttendance(session);
                        addLogEntry(logList, "✓ Attendance marked successfully", "#27ae60");

                        javafx.application.Platform.runLater(() -> {
                            dialog.close();

                            // Show success message
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Attendance Marked");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText("Your attendance has been marked successfully!");
                            successAlert.showAndWait();

                            // Reload attendance data to reflect changes
                            loadAttendanceData();
                        });

                    } else if (confidence >= 50) {
                        // Medium confidence - ask for confirmation
                        recognized = true;
                        camera.release();

                        addLogEntry(logList, "⚠ Medium confidence - requesting confirmation...", "#f39c12");
                        javafx.application.Platform.runLater(() -> {
                            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                            confirmAlert.setTitle("Confirm Identity");
                            confirmAlert.setHeaderText("Confidence: " + String.format("%.1f", finalConfidence) + "%");
                            confirmAlert.setContentText("Is this " + student.getName() + "?");

                            ButtonType yesButton = new ButtonType("Yes");
                            ButtonType noButton = new ButtonType("No");
                            confirmAlert.getButtonTypes().setAll(yesButton, noButton);

                            confirmAlert.showAndWait().ifPresent(response -> {
                                if (response == yesButton) {
                                    // User confirmed - mark attendance
                                    addLogEntry(logList, "✓ Identity confirmed by user", "#27ae60");
                                    markStudentAttendance(session);
                                    addLogEntry(logList, "✓ Attendance marked successfully", "#27ae60");
                                    dialog.close();

                                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                                    successAlert.setTitle("Attendance Marked");
                                    successAlert.setHeaderText(null);
                                    successAlert.setContentText("Your attendance has been marked successfully!");
                                    successAlert.showAndWait();

                                    // Reload attendance data to reflect changes
                                    loadAttendanceData();
                                } else {
                                    // User rejected - close and return
                                    addLogEntry(logList, "✗ Identity rejected by user", "#e74c3c");
                                    dialog.close();
                                }
                            });
                        });
                    }

                    } catch (ai.onnxruntime.OrtException e) {
                        System.err.println("Error during face recognition: " + e.getMessage());
                    }
                }

                // Display frame
                javafx.scene.image.Image image = mat2Image(frame);
                javafx.application.Platform.runLater(() -> cameraView.setImage(image));

                faces.release();
            }

            try {
                Thread.sleep(100); // Check every 100ms
            } catch (InterruptedException e) {
                break;
            }
        }

        camera.release();
    }

    private org.opencv.objdetect.FaceDetectorYN initializeFaceDetector(int width, int height) {
        try {
            String modelPath = downloadYuNetModel();

            if (modelPath == null) {
                System.err.println("Failed to get YuNet model");
                return null;
            }

            // Create YuNet face detector
            org.opencv.objdetect.FaceDetectorYN faceDetector = org.opencv.objdetect.FaceDetectorYN.create(
                modelPath,
                "",  // config (empty for ONNX)
                new org.opencv.core.Size(width, height),  // input size
                0.6f,  // score threshold
                0.3f,  // nms threshold
                5000   // top_k
            );

            System.out.println("YuNet face detector initialized successfully");
            return faceDetector;
        } catch (Exception e) {
            System.err.println("Error loading YuNet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String downloadYuNetModel() {
        try {
            // Try to load from resources first
            java.io.InputStream is = getClass().getClassLoader()
                .getResourceAsStream("face_detection_yunet_2023mar.onnx");

            if (is != null) {
                System.out.println("Loading YuNet model from resources...");
                java.io.File tempFile = java.io.File.createTempFile("yunet", ".onnx");
                tempFile.deleteOnExit();

                java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                is.close();

                System.out.println("YuNet model loaded from resources: " + tempFile.getAbsolutePath());
                return tempFile.getAbsolutePath();
            }

            // If not in resources, download from GitHub
            System.out.println("YuNet model not found in resources, downloading from GitHub...");
            String modelUrl = "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx";

            java.io.File tempFile = java.io.File.createTempFile("yunet", ".onnx");
            tempFile.deleteOnExit();

            // Download the model
            java.net.URL url = new java.net.URL(modelUrl);
            java.io.InputStream in = url.openStream();
            java.nio.file.Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            in.close();

            System.out.println("YuNet model downloaded successfully: " + tempFile.getAbsolutePath());
            return tempFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("Error loading YuNet model: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void markStudentAttendance(Session session) {
        try {
            // Get professor's late threshold from course
            Optional<com.cs102.model.Course> courseOpt = dbManager.findCourseByCourseAndSection(session.getCourse(), session.getSection());

            if (courseOpt.isEmpty()) {
                System.err.println("Course not found for session");
                return;
            }

            com.cs102.model.Course course = courseOpt.get();
            String professorId = course.getProfessorId();
            Optional<User> professorOpt = dbManager.findUserByUserId(professorId);

            if (professorOpt.isEmpty()) {
                System.err.println("Professor not found for session");
                return;
            }

            User professor = professorOpt.get();
            int lateThresholdMinutes = professor.getLateThreshold();

            // Calculate if student is late
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Singapore"));
            java.time.ZonedDateTime sessionStart = java.time.ZonedDateTime.of(session.getDate(), session.getStartTime(), java.time.ZoneId.of("Asia/Singapore"));
            java.time.ZonedDateTime lateThreshold = sessionStart.plusMinutes(lateThresholdMinutes);

            String status;
            if (now.isBefore(lateThreshold) || now.isEqual(lateThreshold)) {
                status = "Present";
            } else {
                status = "Late";
            }

            System.out.println("=== MARKING ATTENDANCE ===");
            System.out.println("Session start: " + sessionStart);
            System.out.println("Late threshold: " + lateThreshold);
            System.out.println("Check-in time: " + now);
            System.out.println("Status: " + status);

            // Convert ZonedDateTime to LocalDateTime for database
            java.time.LocalDateTime checkinTime = now.toLocalDateTime();

            // Check if attendance record already exists
            Optional<com.cs102.model.AttendanceRecord> existingRecord =
                dbManager.findAttendanceByUserIdAndSessionId(student.getUserId(), session.getId());

            if (existingRecord.isPresent()) {
                // Update existing record
                com.cs102.model.AttendanceRecord record = existingRecord.get();
                record.setAttendance(status);
                record.setCheckinTime(checkinTime);
                record.setMethod("Self");
                dbManager.saveAttendanceRecord(record);
                System.out.println("✓ Updated existing attendance record");
            } else {
                // Create new record
                com.cs102.model.AttendanceRecord newRecord = new com.cs102.model.AttendanceRecord();
                newRecord.setUserId(student.getUserId());
                newRecord.setSessionId(session.getId());
                newRecord.setAttendance(status);
                newRecord.setCheckinTime(checkinTime);
                newRecord.setMethod("Self");
                dbManager.saveAttendanceRecord(newRecord);
                System.out.println("✓ Created new attendance record");
            }

        } catch (Exception e) {
            System.err.println("Error marking attendance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private javafx.scene.image.Image mat2Image(org.opencv.core.Mat frame) {
        org.opencv.core.MatOfByte buffer = new org.opencv.core.MatOfByte();
        org.opencv.imgcodecs.Imgcodecs.imencode(".png", frame, buffer);
        return new javafx.scene.image.Image(new java.io.ByteArrayInputStream(buffer.toArray()));
    }

    private String validateAndSaveSettings(String email, String newPassword,
                                          String confirmPassword, Label statusLabel) {
        boolean emailProvided = !email.isEmpty();
        boolean passwordProvided = !newPassword.isEmpty() && !confirmPassword.isEmpty();

        boolean anyChanges = false;

        // Validate and update email if provided
        if (emailProvided) {
            if (!email.contains("@")) {
                return "Please enter a valid email address.";
            }

            if (!email.equals(student.getEmail())) {
                boolean emailUpdated = authManager.updateUserEmail(email);
                if (!emailUpdated) {
                    return "Failed to update email in authentication system.";
                }
                student.setEmail(email);
                anyChanges = true;
            }
        }

        // Check if only one password field is filled
        if (!newPassword.isEmpty() && confirmPassword.isEmpty()) {
            return "Please confirm your new password.";
        }
        if (newPassword.isEmpty() && !confirmPassword.isEmpty()) {
            return "Please enter your new password.";
        }

        // Validate and update password if both fields are provided
        if (passwordProvided) {
            if (newPassword.length() < 6) {
                return "Password must be at least 6 characters long.";
            }
            if (!newPassword.equals(confirmPassword)) {
                return "Passwords do not match.";
            }

            boolean passwordUpdated = authManager.updateUserPassword(newPassword);
            if (!passwordUpdated) {
                return "Failed to update password in authentication system.";
            }
            anyChanges = true;
        }

        // Save changes to database
        if (anyChanges) {
            dbManager.saveUser(student);
            return "SUCCESS";
        }

        return "No changes to save.";
    }
}
