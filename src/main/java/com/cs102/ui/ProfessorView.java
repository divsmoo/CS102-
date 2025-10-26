package com.cs102.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cs102.manager.AuthenticationManager;
import com.cs102.manager.DatabaseManager;
import com.cs102.model.AttendanceRecord;
import com.cs102.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.opencv.objdetect.CascadeClassifier;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

public class ProfessorView {

    private Stage stage;
    private User professor;
    private AuthenticationManager authManager;
    private DatabaseManager databaseManager;

    private BorderPane mainLayout;
    private String currentPage = "Home";

    // UI Components
    private ComboBox<String> yearDropdown;
    private ComboBox<String> semesterDropdown;
    private ComboBox<String> courseDropdown;
    private ComboBox<String> sectionDropdown;
    private TableView<AttendanceRow> attendanceTable;
    private Label loadingLabel;

    // Thread management for data loading
    private Thread currentLoadingThread;

    public ProfessorView(Stage stage, User professor, AuthenticationManager authManager) {
        this.stage = stage;
        this.professor = professor;
        this.authManager = authManager;
        this.databaseManager = authManager.getDatabaseManager();
    }

    public Scene createScene() {
        mainLayout = new BorderPane();
        mainLayout.setTop(createNavigationBar());

        // Show Home page by default
        showHomePage();

        Scene scene = new Scene(mainLayout, 1200, 800);
        return scene;
    }

    private HBox createNavigationBar() {
        HBox navbar = new HBox(20);
        navbar.setPadding(new Insets(15, 20, 15, 20));
        navbar.setStyle("-fx-background-color: #2c3e50;");
        navbar.setAlignment(Pos.CENTER_LEFT);

        // Navigation buttons
        Button homeBtn = createNavButton("Home");
        Button classesBtn = createNavButton("Classes");
        Button sessionsBtn = createNavButton("Sessions");
        Button liveRecognitionBtn = createNavButton("Live Recognition");
        Button settingsBtn = createNavButton("Settings");

        // Spacer to push logout to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Logout button
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20;");
        logoutBtn.setOnAction(e -> {
            AuthView authView = new AuthView(stage, authManager);
            stage.setScene(authView.createScene());
        });

        navbar.getChildren().addAll(homeBtn, classesBtn, sessionsBtn, liveRecognitionBtn, settingsBtn, spacer, logoutBtn);
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

        btn.setOnAction(e -> navigateTo(text));

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
            case "Classes":
                showClassesPage();
                break;
            case "Sessions":
                showSessionsPage();
                break;
            case "Live Recognition":
                showLiveRecognitionPage();
                break;
            case "Settings":
                showSettingsPage();
                break;
        }

        // Refresh navbar to update active state
        mainLayout.setTop(createNavigationBar());
    }

    private void showHomePage() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        // Title and Loading indicator row
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Attendance Overview");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Loading indicator in top-right
        loadingLabel = new Label("Loading attendance data...");
        loadingLabel.setFont(Font.font("Tahoma", FontWeight.NORMAL, 14));
        loadingLabel.setStyle("-fx-text-fill: #3498db;");
        loadingLabel.setVisible(false);

        titleRow.getChildren().addAll(titleLabel, spacer, loadingLabel);

        // First Row: Year, Semester, Course and Section Dropdowns
        HBox dropdownRow = new HBox(20);
        dropdownRow.setAlignment(Pos.CENTER_LEFT);

        Label yearLabel = new Label("Year:");
        yearLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        yearDropdown = new ComboBox<>();
        yearDropdown.setPrefWidth(100);
        yearDropdown.setPromptText("Select Year");

        Label semesterLabel = new Label("Semester:");
        semesterLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        semesterDropdown = new ComboBox<>();
        semesterDropdown.setPrefWidth(130);
        semesterDropdown.setPromptText("Select Semester");

        Label courseLabel = new Label("Course:");
        courseLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        courseDropdown = new ComboBox<>();
        courseDropdown.setPrefWidth(150);
        courseDropdown.setPromptText("Select Course");

        Label sectionLabel = new Label("Section:");
        sectionLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        sectionDropdown = new ComboBox<>();
        sectionDropdown.setPrefWidth(100);
        sectionDropdown.setPromptText("Select Section");

        dropdownRow.getChildren().addAll(yearLabel, yearDropdown, semesterLabel, semesterDropdown, courseLabel, courseDropdown, sectionLabel, sectionDropdown);

        // Load courses for this professor
        loadCourseDropdown();

        // Attendance Table
        attendanceTable = createAttendanceTable();

        content.getChildren().addAll(titleRow, dropdownRow, attendanceTable);

        mainLayout.setCenter(content);
    }

    private void loadCourseDropdown() {
        // Get all courses taught by this professor
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

        // Populate Year dropdown (always show all years)
        ObservableList<String> years = FXCollections.observableArrayList();
        years.add("All");
        Set<String> uniqueYears = professorCourses.stream()
            .map(c -> c.getSemester() != null && c.getSemester().contains("-") ? c.getSemester().split("-")[0] : "")
            .filter(y -> !y.isEmpty())
            .collect(Collectors.toSet());
        years.addAll(uniqueYears.stream().sorted().collect(Collectors.toList()));
        yearDropdown.setItems(years);
        yearDropdown.setValue("All");

        // Initialize cascading dropdowns
        updateHomeSemesterDropdown(professorCourses);
        updateHomeCourseDropdown(professorCourses);
        updateHomeSectionDropdown(professorCourses);

        // Add filter listeners for cascading behavior
        yearDropdown.setOnAction(e -> {
            updateHomeSemesterDropdown(professorCourses);
            updateHomeCourseDropdown(professorCourses);
            updateHomeSectionDropdown(professorCourses);
            loadAttendanceData();
        });
        semesterDropdown.setOnAction(e -> {
            updateHomeCourseDropdown(professorCourses);
            updateHomeSectionDropdown(professorCourses);
            loadAttendanceData();
        });
        courseDropdown.setOnAction(e -> {
            updateHomeSectionDropdown(professorCourses);
            loadAttendanceData();
        });
        sectionDropdown.setOnAction(e -> loadAttendanceData());

        // Trigger initial data load
        loadAttendanceData();
    }

    private void updateHomeSemesterDropdown(List<Course> professorCourses) {
        String selectedYear = yearDropdown.getValue();

        // Filter semesters based on selected year
        ObservableList<String> semesterOptions = FXCollections.observableArrayList();
        semesterOptions.add("All");

        Set<String> uniqueSemesters = professorCourses.stream()
            .filter(c -> {
                if (c.getSemester() == null || !c.getSemester().contains("-")) return false;
                String year = c.getSemester().split("-")[0];
                return selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
            })
            .map(c -> c.getSemester().split("-", 2)[1])
            .collect(Collectors.toSet());

        semesterOptions.addAll(uniqueSemesters.stream().sorted().collect(Collectors.toList()));

        // Remember current selection
        String currentSelection = semesterDropdown.getValue();
        semesterDropdown.setItems(semesterOptions);

        // Restore selection if still valid, otherwise set to "All"
        if (currentSelection != null && semesterOptions.contains(currentSelection)) {
            semesterDropdown.setValue(currentSelection);
        } else {
            semesterDropdown.setValue("All");
        }
    }

    private void updateHomeCourseDropdown(List<Course> professorCourses) {
        String selectedYear = yearDropdown.getValue();
        String selectedSemester = semesterDropdown.getValue();

        // Filter courses based on selected year and semester
        ObservableList<String> courseOptions = FXCollections.observableArrayList();
        courseOptions.add("All");

        Set<String> uniqueCourses = professorCourses.stream()
            .filter(c -> {
                if (c.getSemester() == null || !c.getSemester().contains("-")) return false;
                String[] parts = c.getSemester().split("-", 2);
                String year = parts[0];
                String semester = parts.length > 1 ? parts[1] : "";

                boolean yearMatch = selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
                boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All") || semester.equals(selectedSemester);

                return yearMatch && semesterMatch;
            })
            .map(Course::getCourse)
            .collect(Collectors.toSet());

        courseOptions.addAll(uniqueCourses.stream().sorted().collect(Collectors.toList()));

        // Remember current selection
        String currentSelection = courseDropdown.getValue();
        courseDropdown.setItems(courseOptions);

        // Restore selection if still valid, otherwise set to "All"
        if (currentSelection != null && courseOptions.contains(currentSelection)) {
            courseDropdown.setValue(currentSelection);
        } else {
            courseDropdown.setValue("All");
        }
    }

    private void updateHomeSectionDropdown(List<Course> professorCourses) {
        String selectedYear = yearDropdown.getValue();
        String selectedSemester = semesterDropdown.getValue();
        String selectedCourse = courseDropdown.getValue();

        // Filter sections based on selected year, semester, and course
        ObservableList<String> sectionOptions = FXCollections.observableArrayList();
        sectionOptions.add("All");

        Set<String> uniqueSections = professorCourses.stream()
            .filter(c -> {
                if (c.getSemester() == null || !c.getSemester().contains("-")) return false;
                String[] parts = c.getSemester().split("-", 2);
                String year = parts[0];
                String semester = parts.length > 1 ? parts[1] : "";

                boolean yearMatch = selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
                boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All") || semester.equals(selectedSemester);
                boolean courseMatch = selectedCourse == null || selectedCourse.equals("All") || c.getCourse().equals(selectedCourse);

                return yearMatch && semesterMatch && courseMatch;
            })
            .map(Course::getSection)
            .collect(Collectors.toSet());

        sectionOptions.addAll(uniqueSections.stream().sorted().collect(Collectors.toList()));

        // Remember current selection
        String currentSelection = sectionDropdown.getValue();
        sectionDropdown.setItems(sectionOptions);

        // Restore selection if still valid, otherwise set to "All"
        if (currentSelection != null && sectionOptions.contains(currentSelection)) {
            sectionDropdown.setValue(currentSelection);
        } else {
            sectionDropdown.setValue("All");
        }
    }

    private TableView<AttendanceRow> createAttendanceTable() {
        TableView<AttendanceRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Student Name Column
        TableColumn<AttendanceRow, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentName()));
        nameCol.setPrefWidth(150);

        // Student ID Column
        TableColumn<AttendanceRow, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentId()));
        idCol.setPrefWidth(120);

        // Sessions Column (parent for all session sub-columns)
        TableColumn<AttendanceRow, String> sessionsCol = new TableColumn<>("Sessions");

        // Totals Column (parent for P, L, A)
        TableColumn<AttendanceRow, String> totalsCol = new TableColumn<>("Totals");

        TableColumn<AttendanceRow, String> totalPresentCol = new TableColumn<>("P");
        totalPresentCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalPresent())));
        totalPresentCol.setPrefWidth(50);

        TableColumn<AttendanceRow, String> totalLateCol = new TableColumn<>("L");
        totalLateCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalLate())));
        totalLateCol.setPrefWidth(50);

        TableColumn<AttendanceRow, String> totalAbsentCol = new TableColumn<>("A");
        totalAbsentCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalAbsent())));
        totalAbsentCol.setPrefWidth(50);

        totalsCol.getColumns().addAll(totalPresentCol, totalLateCol, totalAbsentCol);

        // Percentages Column (parent for P%, L%, A%)
        TableColumn<AttendanceRow, String> percentagesCol = new TableColumn<>("Percentages");

        TableColumn<AttendanceRow, String> percentPresentCol = new TableColumn<>("P");
        percentPresentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentPresent() + "%"));
        percentPresentCol.setPrefWidth(60);

        TableColumn<AttendanceRow, String> percentLateCol = new TableColumn<>("L");
        percentLateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentLate() + "%"));
        percentLateCol.setPrefWidth(60);

        TableColumn<AttendanceRow, String> percentAbsentCol = new TableColumn<>("A");
        percentAbsentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentAbsent() + "%"));
        percentAbsentCol.setPrefWidth(60);

        percentagesCol.getColumns().addAll(percentPresentCol, percentLateCol, percentAbsentCol);

        table.getColumns().addAll(nameCol, idCol, sessionsCol, totalsCol, percentagesCol);

        VBox.setVgrow(table, Priority.ALWAYS);

        return table;
    }

    private void loadAttendanceData() {
        String selectedYear = yearDropdown.getValue();
        String selectedSemester = semesterDropdown.getValue();
        String selectedCourse = courseDropdown.getValue();
        String selectedSection = sectionDropdown.getValue();

        if (selectedYear == null || selectedSemester == null || selectedCourse == null || selectedSection == null) {
            return;
        }

        // Cancel previous loading thread if still running
        if (currentLoadingThread != null && currentLoadingThread.isAlive()) {
            currentLoadingThread.interrupt();
            System.out.println("Cancelled previous loading thread");
        }

        System.out.println("Loading attendance for Year: " + selectedYear + ", Semester: " + selectedSemester + ", Course: " + selectedCourse + ", Section: " + selectedSection);

        // Show loading indicator and hide table
        javafx.application.Platform.runLater(() -> {
            loadingLabel.setVisible(true);
            attendanceTable.setVisible(false);
        });

        // Run in background thread to avoid freezing UI
        currentLoadingThread = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // Check if thread was interrupted
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Loading cancelled");
                    return;
                }

                // OPTIMIZATION: Fetch all sessions for the selected year/semester/course/section
                List<Session> sessions = getSessionsForFilter(selectedYear, selectedSemester, selectedCourse, selectedSection);
                System.out.println("Fetched " + sessions.size() + " sessions");

                if (Thread.currentThread().isInterrupted()) return;

                // OPTIMIZATION: Fetch all enrollments for the selected year/semester/course/section
                List<com.cs102.model.Class> enrollments = getEnrollmentsForFilter(selectedYear, selectedSemester, selectedCourse, selectedSection);
                System.out.println("Fetched " + enrollments.size() + " enrollments");

                if (Thread.currentThread().isInterrupted()) return;

                // Extract all unique user IDs and create enrollment map
                Set<String> userIds = enrollments.stream()
                    .map(com.cs102.model.Class::getUserId)
                    .collect(Collectors.toSet());

                // Map userId to enrollment for section lookup
                Map<String, com.cs102.model.Class> enrollmentByUserId = enrollments.stream()
                    .collect(Collectors.toMap(
                        com.cs102.model.Class::getUserId,
                        e -> e,
                        (e1, e2) -> e1 // In case of duplicate, keep first
                    ));

                // OPTIMIZATION: Bulk fetch all user profiles at once
                Map<String, User> usersById = new HashMap<>();
                for (String userId : userIds) {
                    if (Thread.currentThread().isInterrupted()) return;
                    Optional<User> userOpt = databaseManager.findUserByUserId(userId);
                    userOpt.ifPresent(user -> usersById.put(userId, user));
                }
                System.out.println("Fetched " + usersById.size() + " user profiles");

                if (Thread.currentThread().isInterrupted()) return;

                // OPTIMIZATION: Bulk fetch all attendance records for all sessions
                Map<String, Map<UUID, AttendanceRecord>> attendanceByUserAndSession = new HashMap<>();
                for (Session session : sessions) {
                    if (Thread.currentThread().isInterrupted()) return;
                    List<AttendanceRecord> sessionRecords = databaseManager.findAttendanceBySessionId(session.getId());
                    for (AttendanceRecord record : sessionRecords) {
                        attendanceByUserAndSession
                            .computeIfAbsent(record.getUserId(), k -> new HashMap<>())
                            .put(session.getId(), record);
                    }
                }
                System.out.println("Fetched attendance records for " + sessions.size() + " sessions");

                if (Thread.currentThread().isInterrupted()) return;

                // Build attendance data rows (fast - all data is in memory)
                ObservableList<AttendanceRow> rows = FXCollections.observableArrayList();

                for (String userId : userIds) {
                    if (Thread.currentThread().isInterrupted()) return;

                    User user = usersById.get(userId);
                    if (user == null) continue;

                    // Get enrollment info for course and section
                    com.cs102.model.Class enrollment = enrollmentByUserId.get(userId);
                    String course = enrollment != null ? enrollment.getCourse() : "";
                    String section = enrollment != null ? enrollment.getSection() : "";

                    // Get year and semester from the course - we'll find it from one of the sessions
                    String year = "";
                    String semester = "";
                    if (!sessions.isEmpty() && sessions.get(0) != null) {
                        // Get the first session's course to find the semester info
                        Session firstSession = sessions.stream()
                            .filter(s -> s.getCourse().equals(course) && s.getSection().equals(section))
                            .findFirst()
                            .orElse(sessions.get(0));

                        // Find the course object to get semester
                        List<Course> userCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());
                        Course userCourse = userCourses.stream()
                            .filter(c -> c.getCourse().equals(course) && c.getSection().equals(section))
                            .findFirst()
                            .orElse(null);

                        if (userCourse != null && userCourse.getSemester() != null && userCourse.getSemester().contains("-")) {
                            String[] parts = userCourse.getSemester().split("-", 2);
                            year = parts[0];
                            semester = parts.length > 1 ? parts[1] : "";
                        }
                    }

                    AttendanceRow row = new AttendanceRow(user.getName(), userId, course, section, year, semester);

                    // For each session, get attendance from cache
                    for (Session session : sessions) {
                        // Check if student is enrolled in this session's course/section
                        boolean isEnrolledInSession = course.equals(session.getCourse()) &&
                                                     section.equals(session.getSection());

                        if (!isEnrolledInSession) {
                            // Student not enrolled in this session - mark as N/A (will be grayed out)
                            row.addSessionAttendance(session.getSessionId(), "N/A");
                        } else {
                            Map<UUID, AttendanceRecord> userAttendance = attendanceByUserAndSession.get(userId);
                            if (userAttendance != null && userAttendance.containsKey(session.getId())) {
                                AttendanceRecord record = userAttendance.get(session.getId());
                                row.addSessionAttendance(session.getSessionId(), record.getAttendance());
                            } else {
                                // Student is enrolled but didn't attend - mark as Absent
                                row.addSessionAttendance(session.getSessionId(), "Absent");
                            }
                        }
                    }

                    rows.add(row);
                }

                if (Thread.currentThread().isInterrupted()) return;

                // Sort rows by Year → Semester → Course → Section
                rows.sort((r1, r2) -> {
                    // First by year
                    int yearCompare = r1.getYear().compareTo(r2.getYear());
                    if (yearCompare != 0) {
                        return yearCompare;
                    }
                    // Then by semester
                    int semesterCompare = r1.getSemester().compareTo(r2.getSemester());
                    if (semesterCompare != 0) {
                        return semesterCompare;
                    }
                    // Then by course
                    int courseCompare = r1.getCourse().compareTo(r2.getCourse());
                    if (courseCompare != 0) {
                        return courseCompare;
                    }
                    // Finally by section
                    return r1.getSection().compareTo(r2.getSection());
                });

                long endTime = System.currentTimeMillis();
                System.out.println("Loaded " + rows.size() + " students in " + (endTime - startTime) + "ms");

                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    rebuildTableWithSessions(sessions);
                    attendanceTable.setItems(rows);
                    loadingLabel.setVisible(false);
                    attendanceTable.setVisible(true);
                });

            } catch (Exception e) {
                // Only log if not interrupted
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("Error loading attendance data: " + e.getMessage());
                    e.printStackTrace();
                }
                // Hide loading indicator on error
                javafx.application.Platform.runLater(() -> {
                    loadingLabel.setVisible(false);
                    attendanceTable.setVisible(true);
                });
            }
        });
        currentLoadingThread.start();
    }

    private List<Session> getSessionsForFilter(String year, String semester, String course, String section) {
        // Get all professor courses first
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

        // Filter courses by year and semester
        List<Course> filteredCourses = professorCourses.stream()
            .filter(c -> {
                if (c.getSemester() == null || !c.getSemester().contains("-")) return false;
                String[] parts = c.getSemester().split("-", 2);
                String courseYear = parts[0];
                String courseSemester = parts.length > 1 ? parts[1] : "";

                boolean yearMatch = "All".equals(year) || courseYear.equals(year);
                boolean semesterMatch = "All".equals(semester) || courseSemester.equals(semester);
                boolean courseMatch = "All".equals(course) || c.getCourse().equals(course);
                boolean sectionMatch = "All".equals(section) || c.getSection().equals(section);

                return yearMatch && semesterMatch && courseMatch && sectionMatch;
            })
            .collect(Collectors.toList());

        // Get all sessions for filtered courses
        return filteredCourses.stream()
            .flatMap(c -> databaseManager.findSessionsByCourseAndSection(c.getCourse(), c.getSection()).stream())
            .sorted(Comparator.comparing(Session::getDate).thenComparing(Session::getStartTime))
            .collect(Collectors.toList());
    }

    private List<com.cs102.model.Class> getEnrollmentsForFilter(String year, String semester, String course, String section) {
        // Get all professor courses first
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

        // Filter courses by year and semester
        List<Course> filteredCourses = professorCourses.stream()
            .filter(c -> {
                if (c.getSemester() == null || !c.getSemester().contains("-")) return false;
                String[] parts = c.getSemester().split("-", 2);
                String courseYear = parts[0];
                String courseSemester = parts.length > 1 ? parts[1] : "";

                boolean yearMatch = "All".equals(year) || courseYear.equals(year);
                boolean semesterMatch = "All".equals(semester) || courseSemester.equals(semester);
                boolean courseMatch = "All".equals(course) || c.getCourse().equals(course);
                boolean sectionMatch = "All".equals(section) || c.getSection().equals(section);

                return yearMatch && semesterMatch && courseMatch && sectionMatch;
            })
            .collect(Collectors.toList());

        // Get all enrollments for filtered courses
        return filteredCourses.stream()
            .flatMap(c -> databaseManager.findEnrollmentsByCourseAndSection(c.getCourse(), c.getSection()).stream())
            .collect(Collectors.toList());
    }

    private void rebuildTableWithSessions(List<Session> sessions) {
        // Clear existing columns
        attendanceTable.getColumns().clear();

        // Student Name Column (reduced width)
        TableColumn<AttendanceRow, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentName()));
        nameCol.setPrefWidth(100);
        nameCol.setMinWidth(100);

        // Student ID Column (reduced width)
        TableColumn<AttendanceRow, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentId()));
        idCol.setPrefWidth(80);
        idCol.setMinWidth(80);

        // Year Column
        TableColumn<AttendanceRow, String> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getYear()));
        yearCol.setPrefWidth(60);
        yearCol.setMinWidth(60);

        // Semester Column
        TableColumn<AttendanceRow, String> semesterCol = new TableColumn<>("Semester");
        semesterCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSemester()));
        semesterCol.setPrefWidth(100);
        semesterCol.setMinWidth(100);

        // Course Column
        TableColumn<AttendanceRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourse()));
        courseCol.setPrefWidth(70);
        courseCol.setMinWidth(70);

        // Section Column
        TableColumn<AttendanceRow, String> sectionCol = new TableColumn<>("Section");
        sectionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSection()));
        sectionCol.setPrefWidth(60);
        sectionCol.setMinWidth(60);

        attendanceTable.getColumns().addAll(nameCol, idCol, yearCol, semesterCol, courseCol, sectionCol);

        // Sessions Column (parent for all session sub-columns)
        TableColumn<AttendanceRow, String> sessionsCol = new TableColumn<>("Sessions");

        // Add a column for each session (using date as header)
        for (Session session : sessions) {
            // Format date as "MM/dd" for compact display
            String dateHeader = session.getDate() != null ?
                String.format("%02d/%02d", session.getDate().getMonthValue(), session.getDate().getDayOfMonth()) :
                "N/A";

            TableColumn<AttendanceRow, String> sessionCol = new TableColumn<>(dateHeader);
            sessionCol.setPrefWidth(60);
            sessionCol.setMinWidth(60);

            sessionCol.setCellValueFactory(data -> {
                String status = data.getValue().getSessionAttendance().get(session.getSessionId());
                return new SimpleStringProperty(status != null ? getStatusAbbreviation(status) : "A");
            });

            // Add cell factory for color coding
            sessionCol.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        // Color coding
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

            sessionsCol.getColumns().add(sessionCol);
        }

        attendanceTable.getColumns().add(sessionsCol);

        // Totals Column (parent for P, L, A)
        TableColumn<AttendanceRow, String> totalsCol = new TableColumn<>("Totals");

        TableColumn<AttendanceRow, String> totalPresentCol = new TableColumn<>("P");
        totalPresentCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalPresent())));
        totalPresentCol.setPrefWidth(50);
        totalPresentCol.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        TableColumn<AttendanceRow, String> totalLateCol = new TableColumn<>("L");
        totalLateCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalLate())));
        totalLateCol.setPrefWidth(50);
        totalLateCol.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        TableColumn<AttendanceRow, String> totalAbsentCol = new TableColumn<>("A");
        totalAbsentCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalAbsent())));
        totalAbsentCol.setPrefWidth(50);
        totalAbsentCol.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        totalsCol.getColumns().addAll(totalPresentCol, totalLateCol, totalAbsentCol);
        attendanceTable.getColumns().add(totalsCol);

        // Percentages Column (parent for P%, L%, A%)
        TableColumn<AttendanceRow, String> percentagesCol = new TableColumn<>("Percentages");

        TableColumn<AttendanceRow, String> percentPresentCol = new TableColumn<>("P");
        percentPresentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentPresent() + "%"));
        percentPresentCol.setPrefWidth(60);
        percentPresentCol.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        TableColumn<AttendanceRow, String> percentLateCol = new TableColumn<>("L");
        percentLateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentLate() + "%"));
        percentLateCol.setPrefWidth(60);
        percentLateCol.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        TableColumn<AttendanceRow, String> percentAbsentCol = new TableColumn<>("A");
        percentAbsentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentAbsent() + "%"));
        percentAbsentCol.setPrefWidth(60);
        percentAbsentCol.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        percentagesCol.getColumns().addAll(percentPresentCol, percentLateCol, percentAbsentCol);
        attendanceTable.getColumns().add(percentagesCol);
    }

    private String getStatusAbbreviation(String status) {
        switch (status) {
            case "Present":
                return "P";
            case "Late":
                return "L";
            case "Absent":
                return "A";
            case "N/A":
                return "-";
            default:
                return "A";
        }
    }

    private void showClassesPage() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        // Title
        Label titleLabel = new Label("Classes Overview");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        // Year, Semester, Course and Section Filter Dropdowns with Buttons
        HBox filterRow = new HBox(20);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        Label yearLabel = new Label("Year:");
        yearLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        ComboBox<String> yearFilterDropdown = new ComboBox<>();
        yearFilterDropdown.setPrefWidth(100);
        yearFilterDropdown.setPromptText("All");

        Label semesterLabel = new Label("Semester:");
        semesterLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        ComboBox<String> semesterFilterDropdown = new ComboBox<>();
        semesterFilterDropdown.setPrefWidth(130);
        semesterFilterDropdown.setPromptText("All");

        Label courseLabel = new Label("Course:");
        courseLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        ComboBox<String> courseFilterDropdown = new ComboBox<>();
        courseFilterDropdown.setPrefWidth(150);
        courseFilterDropdown.setPromptText("All");

        Label sectionLabel = new Label("Section:");
        sectionLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        ComboBox<String> sectionFilterDropdown = new ComboBox<>();
        sectionFilterDropdown.setPrefWidth(100);
        sectionFilterDropdown.setPromptText("All");

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Buttons
        Button addClassBtn = new Button("Add Class");
        addClassBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 16;");
        addClassBtn.setOnAction(e -> handleAddClass());

        Button deleteClassBtn = new Button("Delete Class");
        deleteClassBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 16;");
        deleteClassBtn.setOnAction(e -> handleDeleteClass());

        filterRow.getChildren().addAll(yearLabel, yearFilterDropdown, semesterLabel, semesterFilterDropdown,
                                       courseLabel, courseFilterDropdown, sectionLabel, sectionFilterDropdown,
                                       spacer, addClassBtn, deleteClassBtn);

        // Classes Table
        TableView<ClassRow> classesTable = createClassesTable();

        // Load professor's courses
        loadClassesData(classesTable, yearFilterDropdown, semesterFilterDropdown, courseFilterDropdown, sectionFilterDropdown);

        content.getChildren().addAll(titleLabel, filterRow, classesTable);
        mainLayout.setCenter(content);
    }

    private TableView<ClassRow> createClassesTable() {
        TableView<ClassRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Course Column
        TableColumn<ClassRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourse()));
        courseCol.setPrefWidth(200);

        // Section Column
        TableColumn<ClassRow, String> sectionCol = new TableColumn<>("Section");
        sectionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSection()));
        sectionCol.setPrefWidth(100);

        // Year Column
        TableColumn<ClassRow, String> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getYear()));
        yearCol.setPrefWidth(80);
        yearCol.setCellFactory(col -> new TableCell<ClassRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // Semester Column
        TableColumn<ClassRow, String> semesterCol = new TableColumn<>("Semester");
        semesterCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSemester()));
        semesterCol.setPrefWidth(120);
        semesterCol.setCellFactory(col -> new TableCell<ClassRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // No. of Students Column
        TableColumn<ClassRow, String> studentsCol = new TableColumn<>("No. of Students");
        studentsCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getNumStudents())));
        studentsCol.setPrefWidth(150);
        studentsCol.setCellFactory(col -> new TableCell<ClassRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // No. of Sessions Column
        TableColumn<ClassRow, String> sessionsCol = new TableColumn<>("No. of Sessions");
        sessionsCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getNumSessions())));
        sessionsCol.setPrefWidth(150);
        sessionsCol.setCellFactory(col -> new TableCell<ClassRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // Edit Classlist Column (Button Column)
        TableColumn<ClassRow, Void> editCol = new TableColumn<>("");
        editCol.setPrefWidth(150);
        editCol.setCellFactory(col -> new TableCell<ClassRow, Void>() {
            private final Button editBtn = new Button("Edit Classlist");

            {
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 12;");
                editBtn.setOnAction(e -> {
                    ClassRow classRow = getTableView().getItems().get(getIndex());
                    handleEditClasslist(classRow);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(editBtn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(courseCol, sectionCol, yearCol, semesterCol, studentsCol, sessionsCol, editCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        return table;
    }

    private void loadClassesData(TableView<ClassRow> table, ComboBox<String> yearFilter, ComboBox<String> semesterFilter,
                                 ComboBox<String> courseFilter, ComboBox<String> sectionFilter) {
        // Get all courses taught by this professor
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

        ObservableList<ClassRow> rows = FXCollections.observableArrayList();

        for (Course course : professorCourses) {
            // Count students enrolled
            List<com.cs102.model.Class> enrollments = databaseManager.findEnrollmentsByCourseAndSection(
                course.getCourse(), course.getSection());
            int numStudents = enrollments.size();

            // Count sessions
            List<Session> sessions = databaseManager.findSessionsByCourseAndSection(
                course.getCourse(), course.getSection());
            int numSessions = sessions.size();

            // Parse semester string (e.g., "2025-Semester 1" -> year="2025", semester="Semester 1")
            String semesterString = course.getSemester();
            String year = "";
            String semester = "";
            if (semesterString != null && semesterString.contains("-")) {
                String[] parts = semesterString.split("-", 2);
                year = parts[0];
                semester = parts.length > 1 ? parts[1] : "";
            }

            ClassRow row = new ClassRow(course.getCourse(), course.getSection(), year, semester, numStudents, numSessions);
            rows.add(row);
        }

        table.setItems(rows);

        // Populate year filter dropdown (always show all years)
        ObservableList<String> yearOptions = FXCollections.observableArrayList();
        yearOptions.add("All");
        Set<String> uniqueYears = professorCourses.stream()
            .map(c -> c.getSemester() != null && c.getSemester().contains("-") ? c.getSemester().split("-")[0] : "")
            .filter(y -> !y.isEmpty())
            .collect(Collectors.toSet());
        yearOptions.addAll(uniqueYears.stream().sorted().collect(Collectors.toList()));
        yearFilter.setItems(yearOptions);
        yearFilter.setValue("All");

        // Initialize cascading dropdowns
        updateSemesterFilterDropdown(yearFilter, semesterFilter, professorCourses);
        updateCourseFilterDropdown(yearFilter, semesterFilter, courseFilter, professorCourses);
        updateSectionFilterDropdown(yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);

        // Add filter listeners for cascading behavior
        yearFilter.setOnAction(e -> {
            updateSemesterFilterDropdown(yearFilter, semesterFilter, professorCourses);
            updateCourseFilterDropdown(yearFilter, semesterFilter, courseFilter, professorCourses);
            updateSectionFilterDropdown(yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
            filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
        });
        semesterFilter.setOnAction(e -> {
            updateCourseFilterDropdown(yearFilter, semesterFilter, courseFilter, professorCourses);
            updateSectionFilterDropdown(yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
            filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
        });
        courseFilter.setOnAction(e -> {
            updateSectionFilterDropdown(yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
            filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
        });
        sectionFilter.setOnAction(e -> filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses));
    }

    private void updateSemesterFilterDropdown(ComboBox<String> yearFilter, ComboBox<String> semesterFilter,
                                              List<Course> professorCourses) {
        String selectedYear = yearFilter.getValue();

        // Filter semesters based on selected year
        ObservableList<String> semesterOptions = FXCollections.observableArrayList();
        semesterOptions.add("All");

        Set<String> uniqueSemesters = professorCourses.stream()
            .filter(c -> {
                if (c.getSemester() == null || !c.getSemester().contains("-")) return false;
                String year = c.getSemester().split("-")[0];
                return selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
            })
            .map(c -> c.getSemester().split("-", 2)[1])
            .collect(Collectors.toSet());

        semesterOptions.addAll(uniqueSemesters.stream().sorted().collect(Collectors.toList()));

        // Remember current selection
        String currentSelection = semesterFilter.getValue();
        semesterFilter.setItems(semesterOptions);

        // Restore selection if still valid, otherwise set to "All"
        if (currentSelection != null && semesterOptions.contains(currentSelection)) {
            semesterFilter.setValue(currentSelection);
        } else {
            semesterFilter.setValue("All");
        }
    }

    private void updateCourseFilterDropdown(ComboBox<String> yearFilter, ComboBox<String> semesterFilter,
                                            ComboBox<String> courseFilter, List<Course> professorCourses) {
        String selectedYear = yearFilter.getValue();
        String selectedSemester = semesterFilter.getValue();

        // Filter courses based on selected year and semester
        ObservableList<String> courseOptions = FXCollections.observableArrayList();
        courseOptions.add("All");

        Set<String> uniqueCourses = professorCourses.stream()
            .filter(c -> {
                if (c.getSemester() == null || !c.getSemester().contains("-")) return false;
                String[] parts = c.getSemester().split("-", 2);
                String year = parts[0];
                String semester = parts.length > 1 ? parts[1] : "";

                boolean yearMatch = selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
                boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All") || semester.equals(selectedSemester);

                return yearMatch && semesterMatch;
            })
            .map(Course::getCourse)
            .collect(Collectors.toSet());

        courseOptions.addAll(uniqueCourses.stream().sorted().collect(Collectors.toList()));

        // Remember current selection
        String currentSelection = courseFilter.getValue();
        courseFilter.setItems(courseOptions);

        // Restore selection if still valid, otherwise set to "All"
        if (currentSelection != null && courseOptions.contains(currentSelection)) {
            courseFilter.setValue(currentSelection);
        } else {
            courseFilter.setValue("All");
        }
    }

    private void updateSectionFilterDropdown(ComboBox<String> yearFilter, ComboBox<String> semesterFilter,
                                             ComboBox<String> courseFilter, ComboBox<String> sectionFilter,
                                             List<Course> professorCourses) {
        String selectedYear = yearFilter.getValue();
        String selectedSemester = semesterFilter.getValue();
        String selectedCourse = courseFilter.getValue();

        // Filter courses based on current year, semester, and course selections
        ObservableList<String> sectionOptions = FXCollections.observableArrayList();
        sectionOptions.add("All");

        Set<String> uniqueSections = professorCourses.stream()
            .filter(c -> {
                // Parse year and semester from course
                String year = "";
                String semester = "";
                if (c.getSemester() != null && c.getSemester().contains("-")) {
                    String[] parts = c.getSemester().split("-", 2);
                    year = parts[0];
                    semester = parts.length > 1 ? parts[1] : "";
                }

                // Apply filters
                boolean yearMatch = selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
                boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All") || semester.equals(selectedSemester);
                boolean courseMatch = selectedCourse == null || selectedCourse.equals("All") || c.getCourse().equals(selectedCourse);

                return yearMatch && semesterMatch && courseMatch;
            })
            .map(Course::getSection)
            .collect(Collectors.toSet());

        sectionOptions.addAll(uniqueSections.stream().sorted().collect(Collectors.toList()));

        // Remember current selection
        String currentSelection = sectionFilter.getValue();

        sectionFilter.setItems(sectionOptions);

        // Restore selection if still valid, otherwise set to "All"
        if (currentSelection != null && sectionOptions.contains(currentSelection)) {
            sectionFilter.setValue(currentSelection);
        } else {
            sectionFilter.setValue("All");
        }
    }

    private void filterClassesTable(TableView<ClassRow> table, ComboBox<String> yearFilter, ComboBox<String> semesterFilter,
                                    ComboBox<String> courseFilter, ComboBox<String> sectionFilter, List<Course> allCourses) {
        String selectedYear = yearFilter.getValue();
        String selectedSemester = semesterFilter.getValue();
        String selectedCourse = courseFilter.getValue();
        String selectedSection = sectionFilter.getValue();

        ObservableList<ClassRow> filteredRows = FXCollections.observableArrayList();

        for (Course course : allCourses) {
            // Parse semester string to get year and semester
            String semesterString = course.getSemester();
            String year = "";
            String semester = "";
            if (semesterString != null && semesterString.contains("-")) {
                String[] parts = semesterString.split("-", 2);
                year = parts[0];
                semester = parts.length > 1 ? parts[1] : "";
            }

            // Apply filters
            boolean yearMatch = selectedYear.equals("All") || year.equals(selectedYear);
            boolean semesterMatch = selectedSemester.equals("All") || semester.equals(selectedSemester);
            boolean courseMatch = selectedCourse.equals("All") || course.getCourse().equals(selectedCourse);
            boolean sectionMatch = selectedSection.equals("All") || course.getSection().equals(selectedSection);

            if (yearMatch && semesterMatch && courseMatch && sectionMatch) {
                // Count students enrolled
                List<com.cs102.model.Class> enrollments = databaseManager.findEnrollmentsByCourseAndSection(
                    course.getCourse(), course.getSection());
                int numStudents = enrollments.size();

                // Count sessions
                List<Session> sessions = databaseManager.findSessionsByCourseAndSection(
                    course.getCourse(), course.getSection());
                int numSessions = sessions.size();

                ClassRow row = new ClassRow(course.getCourse(), course.getSection(), year, semester, numStudents, numSessions);
                filteredRows.add(row);
            }
        }

        table.setItems(filteredRows);
    }

    private void handleAddClass() {
        // Create dialog for adding a new class
        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle("Add Class");
        dialog.setHeaderText("Create a new class/course");

        // Set button types
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField courseField = new TextField();
        courseField.setPromptText("e.g., CS102");
        TextField sectionField = new TextField();
        sectionField.setPromptText("e.g., G1");

        // Year dropdown - generate years from current year to 10 years in the future
        ComboBox<Integer> yearDropdown = new ComboBox<>();
        int currentYear = java.time.Year.now().getValue();
        ObservableList<Integer> years = FXCollections.observableArrayList();
        for (int i = currentYear - 5; i <= currentYear + 10; i++) {
            years.add(i);
        }
        yearDropdown.setItems(years);
        yearDropdown.setValue(currentYear);
        yearDropdown.setPrefWidth(150);

        // Semester dropdown
        ComboBox<String> semesterDropdown = new ComboBox<>();
        ObservableList<String> semesters = FXCollections.observableArrayList("Semester 1", "Semester 2");
        semesterDropdown.setItems(semesters);
        semesterDropdown.setValue("Semester 1");
        semesterDropdown.setPrefWidth(150);

        grid.add(new Label("Course:"), 0, 0);
        grid.add(courseField, 1, 0);
        grid.add(new Label("Section:"), 0, 1);
        grid.add(sectionField, 1, 1);
        grid.add(new Label("Year:"), 0, 2);
        grid.add(yearDropdown, 1, 2);
        grid.add(new Label("Semester:"), 0, 3);
        grid.add(semesterDropdown, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Convert result when Add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                // Combine year and semester into a single string (e.g., "2024-Semester 1")
                String semesterString = yearDropdown.getValue() + "-" + semesterDropdown.getValue();
                return new Course(courseField.getText().trim(), sectionField.getText().trim(),
                                professor.getUserId(), semesterString);
            }
            return null;
        });

        Optional<Course> result = dialog.showAndWait();
        result.ifPresent(course -> {
            if (course.getCourse().isEmpty() || course.getSection().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Course and Section cannot be empty.");
                return;
            }

            // Check if course already exists
            Optional<Course> existing = databaseManager.findCourseByCourseAndSection(course.getCourse(), course.getSection());
            if (existing.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Course Exists", "This course and section already exist.");
                return;
            }

            // Save to database
            databaseManager.saveCourse(course);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Class created successfully!");

            // Refresh the page
            showClassesPage();
        });
    }

    private void handleDeleteClass() {
        // Create dialog for deleting a class
        Dialog<Course.CourseId> dialog = new Dialog<>();
        dialog.setTitle("Delete Class");
        dialog.setHeaderText("Select a class to delete");

        // Set button types
        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Get professor's courses
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());
        ObservableList<String> courseOptions = FXCollections.observableArrayList();
        professorCourses.forEach(c -> courseOptions.add(c.getCourse() + " - " + c.getSection()));

        ComboBox<String> classDropdown = new ComboBox<>(courseOptions);
        classDropdown.setPromptText("Select class to delete");
        classDropdown.setPrefWidth(200);

        grid.add(new Label("Class:"), 0, 0);
        grid.add(classDropdown, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Convert result when Delete button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == deleteButtonType && classDropdown.getValue() != null) {
                String[] parts = classDropdown.getValue().split(" - ");
                if (parts.length == 2) {
                    return new Course.CourseId(parts[0], parts[1]);
                }
            }
            return null;
        });

        Optional<Course.CourseId> result = dialog.showAndWait();
        result.ifPresent(courseId -> {
            // Confirm deletion
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Deletion");
            confirmAlert.setHeaderText("Delete " + courseId.getCourse() + " - " + courseId.getSection() + "?");
            confirmAlert.setContentText("This will delete the course, all enrollments, and all sessions. This action cannot be undone.");

            Optional<ButtonType> confirmation = confirmAlert.showAndWait();
            if (confirmation.isPresent() && confirmation.get() == ButtonType.OK) {
                // Delete course
                databaseManager.deleteCourse(courseId.getCourse(), courseId.getSection());
                showAlert(Alert.AlertType.INFORMATION, "Success", "Class deleted successfully!");

                // Refresh the page
                showClassesPage();
            }
        });
    }

    private void handleEditClasslist(ClassRow classRow) {
        // Create dialog for editing class enrollment list
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Classlist");
        dialog.setHeaderText("Manage students for " + classRow.getCourse() + " - " + classRow.getSection());

        // Set button types
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        content.setPrefHeight(500);

        // Student list table
        TableView<User> studentTable = new TableView<>();
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<User, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserId()));
        idCol.setPrefWidth(150);

        TableColumn<User, Void> removeCol = new TableColumn<>("Action");
        removeCol.setPrefWidth(100);
        removeCol.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button removeBtn = new Button("Remove");

            {
                removeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8;");
                removeBtn.setOnAction(e -> {
                    User student = getTableView().getItems().get(getIndex());
                    databaseManager.deleteEnrollment(classRow.getCourse(), classRow.getSection(), student.getUserId());
                    loadEnrolledStudents(studentTable, classRow);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(removeBtn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        studentTable.getColumns().addAll(nameCol, idCol, removeCol);

        // Load enrolled students
        loadEnrolledStudents(studentTable, classRow);

        // Add student section
        HBox addStudentRow = new HBox(10);
        addStudentRow.setAlignment(Pos.CENTER_LEFT);

        TextField studentIdField = new TextField();
        studentIdField.setPromptText("Enter Student ID (e.g., S12345)");
        studentIdField.setPrefWidth(250);

        Button addStudentBtn = new Button("Add Student");
        addStudentBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 12;");
        addStudentBtn.setOnAction(e -> {
            String studentId = studentIdField.getText().trim();
            if (studentId.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a student ID.");
                return;
            }

            // Check if student exists
            Optional<User> studentOpt = databaseManager.findUserByUserId(studentId);
            if (studentOpt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Student Not Found", "No student with ID " + studentId + " exists.");
                return;
            }

            User student = studentOpt.get();
            if (!"Student".equals(student.getRole())) {
                showAlert(Alert.AlertType.ERROR, "Invalid User", "User " + studentId + " is not a student.");
                return;
            }

            // Check if already enrolled
            List<com.cs102.model.Class> enrollments = databaseManager.findEnrollmentsByCourseAndSection(
                classRow.getCourse(), classRow.getSection());
            boolean alreadyEnrolled = enrollments.stream()
                .anyMatch(e2 -> e2.getUserId().equals(studentId));

            if (alreadyEnrolled) {
                showAlert(Alert.AlertType.WARNING, "Already Enrolled",
                    "Student " + studentId + " is already enrolled in this class.");
                return;
            }

            // Add enrollment
            com.cs102.model.Class enrollment = new com.cs102.model.Class(
                classRow.getCourse(), classRow.getSection(), studentId);
            databaseManager.saveClassEnrollment(enrollment);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Student added successfully!");
            studentIdField.clear();
            loadEnrolledStudents(studentTable, classRow);
        });

        addStudentRow.getChildren().addAll(studentIdField, addStudentBtn);

        content.getChildren().addAll(new Label("Enrolled Students:"), studentTable, new Label("Add New Student:"), addStudentRow);
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait();

        // Refresh classes page after dialog closes
        showClassesPage();
    }

    private void loadEnrolledStudents(TableView<User> table, ClassRow classRow) {
        List<com.cs102.model.Class> enrollments = databaseManager.findEnrollmentsByCourseAndSection(
            classRow.getCourse(), classRow.getSection());

        ObservableList<User> students = FXCollections.observableArrayList();
        for (com.cs102.model.Class enrollment : enrollments) {
            Optional<User> userOpt = databaseManager.findUserByUserId(enrollment.getUserId());
            userOpt.ifPresent(students::add);
        }

        table.setItems(students);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSessionsPage() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);

        Label label = new Label("Sessions Page - Coming Soon");
        label.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        content.getChildren().add(label);
        mainLayout.setCenter(content);
    }

    private void showLiveRecognitionPage() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_LEFT);

        // Title
        Label titleLabel = new Label("Live Face Recognition Check-In");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        // Get current year and semester
        java.time.LocalDate today = java.time.LocalDate.now();
        int currentYear = today.getYear();
        String currentSemester = (today.getMonthValue() >= 7) ? "Semester 1" : "Semester 2";

        // Get all courses for the professor
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

        // Filter courses by current year and semester
        List<Course> currentCourses = professorCourses.stream()
            .filter(c -> {
                if (c.getSemester() == null || !c.getSemester().contains("-")) return false;
                String[] parts = c.getSemester().split("-", 2);
                String year = parts[0];
                String semester = parts[1];
                return year.equals(String.valueOf(currentYear)) && semester.equals(currentSemester);
            })
            .collect(Collectors.toList());

        // Dropdowns row
        HBox dropdownRow = new HBox(15);
        dropdownRow.setAlignment(Pos.CENTER_LEFT);

        Label courseLabel = new Label("Course:");
        ComboBox<String> liveRecCourseDropdown = new ComboBox<>();
        liveRecCourseDropdown.setPromptText("Select Course");
        liveRecCourseDropdown.setPrefWidth(200);

        Label sectionLabel = new Label("Section:");
        ComboBox<String> liveRecSectionDropdown = new ComboBox<>();
        liveRecSectionDropdown.setPromptText("Select Section");
        liveRecSectionDropdown.setPrefWidth(150);

        // Populate course dropdown
        Set<String> uniqueCourses = currentCourses.stream()
            .map(Course::getCourse)
            .collect(Collectors.toSet());
        ObservableList<String> courseOptions = FXCollections.observableArrayList(uniqueCourses);
        liveRecCourseDropdown.setItems(courseOptions);

        // Update section dropdown when course changes
        liveRecCourseDropdown.setOnAction(e -> {
            String selectedCourse = liveRecCourseDropdown.getValue();
            if (selectedCourse != null) {
                Set<String> sections = currentCourses.stream()
                    .filter(c -> c.getCourse().equals(selectedCourse))
                    .map(Course::getSection)
                    .collect(Collectors.toSet());
                liveRecSectionDropdown.setItems(FXCollections.observableArrayList(sections));
            } else {
                liveRecSectionDropdown.setItems(FXCollections.observableArrayList());
            }
            liveRecSectionDropdown.setValue(null);
        });

        // Spacer to push button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Start Check In button
        Button startCheckInBtn = new Button("Start Check In");
        startCheckInBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 30; -fx-cursor: hand;");
        startCheckInBtn.setOnAction(e -> {
            String selectedCourse = liveRecCourseDropdown.getValue();
            String selectedSection = liveRecSectionDropdown.getValue();

            // Validation
            if (selectedCourse == null || selectedSection == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Selection Required");
                alert.setHeaderText(null);
                alert.setContentText("Please select both Course and Section before starting check-in.");
                alert.showAndWait();
                return;
            }

            // Check if there's an active session
            java.time.LocalDate currentDate = java.time.LocalDate.now();
            java.time.LocalTime currentTime = java.time.LocalTime.now();

            // Get all sessions for the course and section, then filter by today's date
            List<Session> sessions = databaseManager.findSessionsByCourseAndSection(selectedCourse, selectedSection);
            Optional<Session> sessionOpt = sessions.stream()
                .filter(s -> s.getDate().equals(currentDate))
                .findFirst();

            if (sessionOpt.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Active Session");
                alert.setHeaderText(null);
                alert.setContentText("No session found for today. Please create a session before starting check-in.");
                alert.showAndWait();
                return;
            }

            Session session = sessionOpt.get();

            // Check if current time is within session time
            if (currentTime.isBefore(session.getStartTime()) || currentTime.isAfter(session.getEndTime())) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Session Not Active");
                alert.setHeaderText(null);
                alert.setContentText("Current time is outside the session time window.\n" +
                    "Session time: " + session.getStartTime() + " - " + session.getEndTime() + "\n" +
                    "Current time: " + currentTime);
                alert.showAndWait();
                return;
            }

            // All validations passed - start live recognition
            startLiveRecognition(selectedCourse, selectedSection, session);
        });

        dropdownRow.getChildren().addAll(courseLabel, liveRecCourseDropdown, sectionLabel, liveRecSectionDropdown, spacer, startCheckInBtn);

        content.getChildren().addAll(titleLabel, dropdownRow);
        mainLayout.setCenter(content);
    }

    private void startLiveRecognition(String course, String section, Session session) {
        // Create new VBox for live recognition view
        VBox liveRecContent = new VBox(15);
        liveRecContent.setPadding(new Insets(30));
        liveRecContent.setAlignment(Pos.TOP_CENTER);

        // Title with session info
        Label titleLabel = new Label("Live Face Recognition - " + course + " Section " + section);
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));

        // Status label (shows latest check-in result)
        Label statusLabel = new Label("Initializing camera...");
        statusLabel.setFont(Font.font(18));
        statusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold; -fx-padding: 10;");
        statusLabel.setMinHeight(50);
        statusLabel.setAlignment(Pos.CENTER);

        // Camera feed ImageView (matching FaceCaptureView dimensions)
        ImageView cameraView = new ImageView();
        cameraView.setFitWidth(640);
        cameraView.setFitHeight(480);
        cameraView.setPreserveRatio(true);
        cameraView.setStyle("-fx-border-color: black; -fx-border-width: 2;");

        // Stop button
        Button stopBtn = new Button("Stop Check In");
        stopBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 30; -fx-cursor: hand;");

        liveRecContent.getChildren().addAll(titleLabel, statusLabel, cameraView, stopBtn);
        mainLayout.setCenter(liveRecContent);

        // Start camera and face recognition in background thread
        Thread recognitionThread = new Thread(() -> {
            try {
                runLiveRecognition(course, section, session, cameraView, statusLabel, stopBtn);
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("An error occurred during face recognition: " + e.getMessage());
                    alert.showAndWait();
                    showLiveRecognitionPage();
                });
            }
        });
        recognitionThread.setDaemon(true);
        recognitionThread.start();

        // Stop button handler
        stopBtn.setOnAction(e -> {
            recognitionThread.interrupt();
            showLiveRecognitionPage();
        });
    }

    private void runLiveRecognition(String course, String section, Session session,
                                     ImageView cameraView, Label statusLabel, Button stopBtn) {
        // Load OpenCV
        nu.pattern.OpenCV.loadLocally();

        // Initialize face detector
        CascadeClassifier faceDetector = initializeFaceDetector();
        if (faceDetector == null || faceDetector.empty()) {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Error: Failed to load face detector");
                statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            });
            return;
        }

        // Get all students enrolled in this course and section
        List<com.cs102.model.Class> enrollments = databaseManager.findEnrollmentsByCourseAndSection(course, section);
        Map<String, User> studentMap = new HashMap<>();
        Map<String, List<org.opencv.core.Mat>> studentFaceHistograms = new HashMap<>();

        for (com.cs102.model.Class enrollment : enrollments) {
            String userId = enrollment.getUserId();
            User student = databaseManager.findUserByUserId(userId).orElse(null);
            if (student != null) {
                studentMap.put(userId, student);

                // Load face images for this student and compute histograms
                List<FaceImage> faceImages = databaseManager.findFaceImagesByUserId(userId);
                List<org.opencv.core.Mat> histograms = new ArrayList<>();
                for (FaceImage faceImage : faceImages) {
                    byte[] imageData = faceImage.getImageData();
                    org.opencv.core.MatOfByte matOfByte = new org.opencv.core.MatOfByte(imageData);
                    // Images are already grayscale 224x224, decode and compute histogram
                    org.opencv.core.Mat faceMat = org.opencv.imgcodecs.Imgcodecs.imdecode(matOfByte, org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE);
                    if (faceMat != null && !faceMat.empty()) {
                        // Compute histogram for this training image
                        org.opencv.core.Mat hist = computeHistogram(faceMat);
                        histograms.add(hist);
                        faceMat.release();
                    }
                }
                if (!histograms.isEmpty()) {
                    studentFaceHistograms.put(userId, histograms);
                    System.out.println("Loaded " + histograms.size() + " histograms for student: " + student.getName());
                }
            }
        }

        javafx.application.Platform.runLater(() -> {
            statusLabel.setText("Loaded " + studentFaceHistograms.size() + " students. Ready to scan faces.");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        });

        // Open camera
        org.opencv.videoio.VideoCapture camera = new org.opencv.videoio.VideoCapture(0);
        if (!camera.isOpened()) {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Error: Failed to open camera");
                statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            });
            return;
        }

        // Configure camera for maximum FPS and performance (matching FaceCaptureView)
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 60.0); // Request 60 FPS (camera will use max available)
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH, 640);
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT, 480);
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_BUFFERSIZE, 1); // Minimize buffer latency

        // Get actual FPS the camera can provide
        double actualFps = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FPS);
        System.out.println("Camera configured - Requested: 60 FPS, Actual: " + actualFps + " FPS");

        javafx.application.Platform.runLater(() -> {
            statusLabel.setText("Camera active. Position faces in view.");
            statusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
        });

        // Main recognition loop with FPS tracking
        org.opencv.core.Mat frame = new org.opencv.core.Mat();
        Set<String> recentlyCheckedIn = new HashSet<>(); // Track recently checked-in students
        int frameCount = 0;
        long startTime = System.currentTimeMillis();
        long lastFpsReport = startTime;

        while (!Thread.currentThread().isInterrupted() && camera.isOpened()) {
            if (!camera.read(frame) || frame.empty()) {
                continue;
            }

            frameCount++;

            // Report FPS every second
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFpsReport >= 1000) {
                long elapsed = currentTime - startTime;
                double fps = frameCount / (elapsed / 1000.0);
                System.out.println("Recognition Camera FPS: " + String.format("%.2f", fps) + " (Frame #" + frameCount + ")");
                lastFpsReport = currentTime;
            }

            // Detect faces
            org.opencv.core.MatOfRect faceDetections = new org.opencv.core.MatOfRect();
            faceDetector.detectMultiScale(frame, faceDetections);

            // Process each detected face
            for (org.opencv.core.Rect faceRect : faceDetections.toArray()) {
                // Extract face region
                org.opencv.core.Mat face = frame.submat(faceRect);

                // Preprocess face for recognition (simplified - just grayscale and resize)
                org.opencv.core.Mat processedFace = preprocessFaceForRecognition(face);

                // Recognize face using histogram comparison
                RecognitionResult result = recognizeFace(processedFace, studentFaceHistograms, studentMap);

                if (result != null) {
                    // Determine box color and handle check-in
                    org.opencv.core.Scalar boxColor;
                    String statusMessage;

                    if (result.confidence >= 70.0) {
                        boxColor = new org.opencv.core.Scalar(0, 255, 0); // Green
                        statusMessage = result.studentName + " Checked In!";

                        // Check in student (only if not recently checked in)
                        if (!recentlyCheckedIn.contains(result.userId)) {
                            checkInStudent(result.userId, session);
                            recentlyCheckedIn.add(result.userId);

                            final String finalMessage = statusMessage;
                            javafx.application.Platform.runLater(() -> {
                                statusLabel.setText(finalMessage);
                                statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            });
                        }
                    } else {
                        boxColor = new org.opencv.core.Scalar(0, 0, 255); // Red
                        statusMessage = result.studentName + " Low Match, please try again!";

                        final String finalMessage = statusMessage;
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText(finalMessage);
                            statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        });
                    }

                    // Draw bounding box
                    org.opencv.imgproc.Imgproc.rectangle(frame,
                        new org.opencv.core.Point(faceRect.x, faceRect.y),
                        new org.opencv.core.Point(faceRect.x + faceRect.width, faceRect.y + faceRect.height),
                        boxColor, 3);

                    // Draw student name and confidence
                    String label = result.studentName + " (" + String.format("%.1f", result.confidence) + "%)";
                    org.opencv.imgproc.Imgproc.putText(frame, label,
                        new org.opencv.core.Point(faceRect.x, faceRect.y - 10),
                        org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, boxColor, 2);
                } else {
                    // Unknown face - red box
                    org.opencv.imgproc.Imgproc.rectangle(frame,
                        new org.opencv.core.Point(faceRect.x, faceRect.y),
                        new org.opencv.core.Point(faceRect.x + faceRect.width, faceRect.y + faceRect.height),
                        new org.opencv.core.Scalar(0, 0, 255), 3);
                    org.opencv.imgproc.Imgproc.putText(frame, "Unknown",
                        new org.opencv.core.Point(faceRect.x, faceRect.y - 10),
                        org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new org.opencv.core.Scalar(0, 0, 255), 2);
                }

                processedFace.release();
                face.release();
            }

            // Convert frame to JavaFX Image and display
            final javafx.scene.image.Image fxImage = mat2Image(frame);
            javafx.application.Platform.runLater(() -> {
                cameraView.setImage(fxImage);
            });
        }

        // Cleanup
        camera.release();
        for (List<org.opencv.core.Mat> histograms : studentFaceHistograms.values()) {
            for (org.opencv.core.Mat hist : histograms) {
                hist.release();
            }
        }
    }

    private CascadeClassifier initializeFaceDetector() {
        try {
            java.io.InputStream is = getClass().getClassLoader()
                .getResourceAsStream("haarcascade_frontalface_default.xml");

            if (is == null) {
                System.err.println("Could not find haarcascade_frontalface_default.xml in resources");
                return null;
            }

            java.io.File tempFile = java.io.File.createTempFile("haarcascade", ".xml");
            tempFile.deleteOnExit();

            java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();

            CascadeClassifier detector = new CascadeClassifier(tempFile.getAbsolutePath());

            if (detector.empty()) {
                System.err.println("Failed to load cascade classifier from: " + tempFile.getAbsolutePath());
                return null;
            }

            return detector;
        } catch (Exception e) {
            System.err.println("Error loading Haar Cascade: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private org.opencv.core.Mat preprocessFaceForRecognition(org.opencv.core.Mat face) {
        // Simple preprocessing: just convert to grayscale and resize (matching demo code)
        org.opencv.core.Mat grayFace = new org.opencv.core.Mat();
        org.opencv.imgproc.Imgproc.cvtColor(face, grayFace, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY);

        // Resize to match training images (200x200)
        org.opencv.core.Mat resizedFace = new org.opencv.core.Mat();
        org.opencv.imgproc.Imgproc.resize(grayFace, resizedFace, new org.opencv.core.Size(200, 200));

        grayFace.release();
        return resizedFace;
    }

    private RecognitionResult recognizeFace(org.opencv.core.Mat queryFace,
                                           Map<String, List<org.opencv.core.Mat>> studentFaceHistograms,
                                           Map<String, User> studentMap) {
        // Compute histogram for the query face
        org.opencv.core.Mat queryHist = computeHistogram(queryFace);

        double bestCorrelation = 0;
        String bestMatchUserId = null;

        // Compare with all students using histogram correlation
        for (Map.Entry<String, List<org.opencv.core.Mat>> entry : studentFaceHistograms.entrySet()) {
            String userId = entry.getKey();
            List<org.opencv.core.Mat> trainedHistograms = entry.getValue();

            // Compare with all training histograms for this student
            for (org.opencv.core.Mat trainedHist : trainedHistograms) {
                // Use correlation method (higher = better match)
                double correlation = org.opencv.imgproc.Imgproc.compareHist(queryHist, trainedHist,
                    org.opencv.imgproc.Imgproc.HISTCMP_CORREL);

                if (correlation > bestCorrelation) {
                    bestCorrelation = correlation;
                    bestMatchUserId = userId;
                }
            }
        }

        queryHist.release();

        if (bestMatchUserId != null) {
            // Convert correlation to confidence percentage
            // Correlation ranges from -1 to 1, but typically 0 to 1 for similar images
            // Using 0.7 (70%) as threshold like in the demo
            double confidence = bestCorrelation * 100.0;

            User student = studentMap.get(bestMatchUserId);

            // Debug logging
            System.out.println("Recognition: " + student.getName() + " - Correlation: " +
                             String.format("%.3f", bestCorrelation) + " - Confidence: " +
                             String.format("%.1f", confidence) + "%");

            return new RecognitionResult(bestMatchUserId, student.getName(), confidence);
        }

        return null;
    }

    // Compute histogram for a grayscale image (matching demo code)
    private org.opencv.core.Mat computeHistogram(org.opencv.core.Mat image) {
        org.opencv.core.Mat hist = new org.opencv.core.Mat();
        org.opencv.core.MatOfInt histSize = new org.opencv.core.MatOfInt(256);
        org.opencv.core.MatOfFloat ranges = new org.opencv.core.MatOfFloat(0f, 256f);
        org.opencv.core.MatOfInt channels = new org.opencv.core.MatOfInt(0);

        List<org.opencv.core.Mat> images = new ArrayList<>();
        images.add(image);

        org.opencv.imgproc.Imgproc.calcHist(images, channels, new org.opencv.core.Mat(),
                                            hist, histSize, ranges);
        org.opencv.core.Core.normalize(hist, hist, 0, 1, org.opencv.core.Core.NORM_MINMAX);

        return hist;
    }

    private void checkInStudent(String userId, Session session) {
        try {
            // Check if student already checked in
            Optional<AttendanceRecord> existingRecord = databaseManager.findAttendanceByUserIdAndSessionId(userId, session.getId());
            if (existingRecord.isEmpty()) {
                AttendanceRecord record = new AttendanceRecord();
                record.setUserId(userId);
                record.setSessionId(session.getId());
                record.setAttendance("Present");
                record.setMethod("Auto");
                record.setCheckinTime(java.time.LocalDateTime.now());

                databaseManager.saveAttendanceRecord(record);
                System.out.println("Checked in student: " + userId + " at " + record.getCheckinTime());
            }
        } catch (Exception e) {
            System.err.println("Error checking in student: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private javafx.scene.image.Image mat2Image(org.opencv.core.Mat frame) {
        org.opencv.core.MatOfByte buffer = new org.opencv.core.MatOfByte();
        org.opencv.imgcodecs.Imgcodecs.imencode(".png", frame, buffer);
        return new javafx.scene.image.Image(new java.io.ByteArrayInputStream(buffer.toArray()));
    }

    // Inner class for recognition results
    private static class RecognitionResult {
        String userId;
        String studentName;
        double confidence;

        RecognitionResult(String userId, String studentName, double confidence) {
            this.userId = userId;
            this.studentName = studentName;
            this.confidence = confidence;
        }
    }

    private void showSettingsPage() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);

        Label label = new Label("Settings Page - Coming Soon");
        label.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        content.getChildren().add(label);
        mainLayout.setCenter(content);
    }

    // Inner class to represent a row in the classes table
    public static class ClassRow {
        private String course;
        private String section;
        private String year;
        private String semester;
        private int numStudents;
        private int numSessions;

        public ClassRow(String course, String section, String year, String semester, int numStudents, int numSessions) {
            this.course = course;
            this.section = section;
            this.year = year;
            this.semester = semester;
            this.numStudents = numStudents;
            this.numSessions = numSessions;
        }

        public String getCourse() {
            return course;
        }

        public String getSection() {
            return section;
        }

        public String getYear() {
            return year;
        }

        public String getSemester() {
            return semester;
        }

        public int getNumStudents() {
            return numStudents;
        }

        public int getNumSessions() {
            return numSessions;
        }
    }

    // Inner class to represent a row in the attendance table
    public static class AttendanceRow {
        private String studentName;
        private String studentId;
        private String course;
        private String section;
        private String year;
        private String semester;
        private Map<String, String> sessionAttendance; // sessionId -> attendance status
        private int totalPresent;
        private int totalLate;
        private int totalAbsent;

        public AttendanceRow(String studentName, String studentId, String course, String section, String year, String semester) {
            this.studentName = studentName;
            this.studentId = studentId;
            this.course = course;
            this.section = section;
            this.year = year;
            this.semester = semester;
            this.sessionAttendance = new HashMap<>();
            this.totalPresent = 0;
            this.totalLate = 0;
            this.totalAbsent = 0;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getCourse() {
            return course;
        }

        public String getSection() {
            return section;
        }

        public String getYear() {
            return year;
        }

        public String getSemester() {
            return semester;
        }

        public Map<String, String> getSessionAttendance() {
            return sessionAttendance;
        }

        public void addSessionAttendance(String sessionId, String status) {
            sessionAttendance.put(sessionId, status);

            // Update totals
            switch (status) {
                case "Present":
                    totalPresent++;
                    break;
                case "Late":
                    totalLate++;
                    break;
                case "Absent":
                    totalAbsent++;
                    break;
            }
        }

        public int getTotalPresent() {
            return totalPresent;
        }

        public int getTotalLate() {
            return totalLate;
        }

        public int getTotalAbsent() {
            return totalAbsent;
        }

        public String getPercentPresent() {
            int total = totalPresent + totalLate + totalAbsent;
            if (total == 0) return "0";
            return String.format("%.1f", (totalPresent * 100.0) / total);
        }

        public String getPercentLate() {
            int total = totalPresent + totalLate + totalAbsent;
            if (total == 0) return "0";
            return String.format("%.1f", (totalLate * 100.0) / total);
        }

        public String getPercentAbsent() {
            int total = totalPresent + totalLate + totalAbsent;
            if (total == 0) return "0";
            return String.format("%.1f", (totalAbsent * 100.0) / total);
        }
    }
}
