package com.cs102.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cs102.manager.AuthenticationManager;
import com.cs102.manager.DatabaseManager;
import com.cs102.model.AttendanceRecord;
import com.cs102.model.Course;
import com.cs102.model.FaceImage;
import com.cs102.model.Session;
import com.cs102.model.User;
import com.cs102.model.UserRole;

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
import org.opencv.objdetect.FaceDetectorYN;

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

    // Shared filter state for both Home and Classes pages
    private String savedYear = "All";
    private String savedSemester = "All";
    private String savedCourse = "All";
    private String savedSection = "All";

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
        navbar.setPadding(new Insets(15, 30, 15, 30));
        navbar.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 2 0;");
        navbar.setAlignment(Pos.CENTER_LEFT);

        // App title/logo
        Label appTitle = new Label("Professor Portal");
        appTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));
        appTitle.setStyle("-fx-text-fill: white;");

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Navigation buttons
        Button homeBtn = createNavButton("Home");
        Button classesBtn = createNavButton("Classes");
        Button sessionsBtn = createNavButton("Sessions");
        Button liveRecognitionBtn = createNavButton("Live Recognition");
        Button settingsBtn = createNavButton("Settings");

        // Logout button
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
                "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;"));
        logoutBtn.setOnAction(e -> {
            AuthView authView = new AuthView(stage, authManager);
            stage.setScene(authView.createScene());
        });

        navbar.getChildren().addAll(appTitle, spacer, homeBtn, classesBtn, sessionsBtn, liveRecognitionBtn, settingsBtn,
                logoutBtn);
        return navbar;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");

        btn.setOnMouseEntered(e -> {
            if (!currentPage.equals(text)) {
                btn.setStyle(
                        "-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");
            }
        });

        btn.setOnMouseExited(e -> {
            if (!currentPage.equals(text)) {
                btn.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");
            }
        });

        btn.setOnAction(e -> navigateTo(text));

        if (currentPage.equals(text)) {
            btn.setStyle(
                    "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");
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

        dropdownRow.getChildren().addAll(yearLabel, yearDropdown, semesterLabel, semesterDropdown, courseLabel,
                courseDropdown, sectionLabel, sectionDropdown);

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
        // Restore saved value or default to "All"
        if (years.contains(savedYear)) {
            yearDropdown.setValue(savedYear);
        } else {
            yearDropdown.setValue("All");
            savedYear = "All";
        }

        // Populate Semester dropdown
        ObservableList<String> semesters = FXCollections.observableArrayList();
        semesters.add("All");
        Set<String> uniqueSemesters = professorCourses.stream()
                .map(c -> c.getSemester() != null && c.getSemester().contains("-") ? c.getSemester().split("-", 2)[1]
                        : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        semesters.addAll(uniqueSemesters.stream().sorted().collect(Collectors.toList()));
        semesterDropdown.setItems(semesters);
        // Restore saved value or default to "All"
        if (semesters.contains(savedSemester)) {
            semesterDropdown.setValue(savedSemester);
        } else {
            semesterDropdown.setValue("All");
            savedSemester = "All";
        }

        // Populate Course dropdown
        ObservableList<String> courses = FXCollections.observableArrayList();
        courses.add("All");
        Set<String> uniqueCourses = professorCourses.stream()
                .map(Course::getCourse)
                .collect(Collectors.toSet());
        courses.addAll(uniqueCourses);
        courseDropdown.setItems(courses);
        // Restore saved value or default to "All"
        if (courses.contains(savedCourse)) {
            courseDropdown.setValue(savedCourse);
        } else {
            courseDropdown.setValue("All");
            savedCourse = "All";
        }

        // When any filter changes, save state and reload data
        yearDropdown.setOnAction(e -> {
            savedYear = yearDropdown.getValue();
            loadAttendanceData();
        });
        semesterDropdown.setOnAction(e -> {
            savedSemester = semesterDropdown.getValue();
            loadAttendanceData();
        });
        courseDropdown.setOnAction(e -> {
            savedCourse = courseDropdown.getValue();
            String selectedCourse = courseDropdown.getValue();
            loadSectionDropdown(selectedCourse, professorCourses, false);
            loadAttendanceData();
        });

        // Initialize sections dropdown (with initial load flag)
        loadSectionDropdown("All", professorCourses, true);
    }

    private void loadSectionDropdown(String selectedCourse, List<Course> professorCourses, boolean isInitialLoad) {
        ObservableList<String> sections = FXCollections.observableArrayList();
        sections.add("All");

        if ("All".equals(selectedCourse)) {
            // Show all sections from all courses
            Set<String> uniqueSections = professorCourses.stream()
                    .map(Course::getSection)
                    .collect(Collectors.toSet());
            sections.addAll(uniqueSections);
        } else {
            // Show only sections for selected course
            Set<String> courseSections = professorCourses.stream()
                    .filter(c -> c.getCourse().equals(selectedCourse))
                    .map(Course::getSection)
                    .collect(Collectors.toSet());
            sections.addAll(courseSections);
        }

        sectionDropdown.setItems(sections);
        // Restore saved value or default to "All"
        if (sections.contains(savedSection)) {
            sectionDropdown.setValue(savedSection);
        } else {
            sectionDropdown.setValue("All");
            savedSection = "All";
        }

        sectionDropdown.setOnAction(e -> {
            savedSection = sectionDropdown.getValue();
            loadAttendanceData();
        });

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
                    if (c.getSemester() == null || !c.getSemester().contains("-"))
                        return false;
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
                    if (c.getSemester() == null || !c.getSemester().contains("-"))
                        return false;
                    String[] parts = c.getSemester().split("-", 2);
                    String year = parts[0];
                    String semester = parts.length > 1 ? parts[1] : "";

                    boolean yearMatch = selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
                    boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All")
                            || semester.equals(selectedSemester);

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
                    if (c.getSemester() == null || !c.getSemester().contains("-"))
                        return false;
                    String[] parts = c.getSemester().split("-", 2);
                    String year = parts[0];
                    String semester = parts.length > 1 ? parts[1] : "";

                    boolean yearMatch = selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
                    boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All")
                            || semester.equals(selectedSemester);
                    boolean courseMatch = selectedCourse == null || selectedCourse.equals("All")
                            || c.getCourse().equals(selectedCourse);

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
        // Use UNCONSTRAINED to allow horizontal scrolling
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Student Name Column
        TableColumn<AttendanceRow, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentName()));
        nameCol.setPrefWidth(120);
        nameCol.setMinWidth(100);
        nameCol.setResizable(true);

        // Student ID Column
        TableColumn<AttendanceRow, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentId()));
        idCol.setPrefWidth(100);
        idCol.setMinWidth(80);
        idCol.setResizable(true);

        // Sessions Column (parent for all session sub-columns)
        TableColumn<AttendanceRow, String> sessionsCol = new TableColumn<>("Sessions");

        // Totals Column (parent for P, L, A)
        TableColumn<AttendanceRow, String> totalsCol = new TableColumn<>("Totals");

        TableColumn<AttendanceRow, String> totalPresentCol = new TableColumn<>("P");
        totalPresentCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalPresent())));
        totalPresentCol.setPrefWidth(50);

        TableColumn<AttendanceRow, String> totalLateCol = new TableColumn<>("L");
        totalLateCol
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalLate())));
        totalLateCol.setPrefWidth(50);

        TableColumn<AttendanceRow, String> totalAbsentCol = new TableColumn<>("A");
        totalAbsentCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalAbsent())));
        totalAbsentCol.setPrefWidth(50);

        totalsCol.getColumns().addAll(totalPresentCol, totalLateCol, totalAbsentCol);

        // Percentages Column (parent for P%, L%, A%)
        TableColumn<AttendanceRow, String> percentagesCol = new TableColumn<>("Percentages");

        TableColumn<AttendanceRow, String> percentPresentCol = new TableColumn<>("P");
        percentPresentCol
                .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentPresent() + "%"));
        percentPresentCol.setPrefWidth(60);

        TableColumn<AttendanceRow, String> percentLateCol = new TableColumn<>("L");
        percentLateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentLate() + "%"));
        percentLateCol.setPrefWidth(60);

        TableColumn<AttendanceRow, String> percentAbsentCol = new TableColumn<>("A");
        percentAbsentCol
                .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentAbsent() + "%"));
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

        System.out.println("Loading attendance for Year: " + selectedYear + ", Semester: " + selectedSemester
                + ", Course: " + selectedCourse + ", Section: " + selectedSection);

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

                // OPTIMIZATION: Fetch all sessions for the selected
                // year/semester/course/section
                List<Session> sessions = getSessionsForFilter(selectedYear, selectedSemester, selectedCourse,
                        selectedSection);
                System.out.println("Fetched " + sessions.size() + " sessions");

                if (Thread.currentThread().isInterrupted())
                    return;

                // OPTIMIZATION: Fetch all enrollments for the selected
                // year/semester/course/section
                List<com.cs102.model.Class> enrollments = getEnrollmentsForFilter(selectedYear, selectedSemester,
                        selectedCourse, selectedSection);
                System.out.println("Fetched " + enrollments.size() + " enrollments");

                if (Thread.currentThread().isInterrupted())
                    return;

                // Extract all unique user IDs for bulk fetch
                Set<String> userIds = enrollments.stream()
                        .map(com.cs102.model.Class::getUserId)
                        .collect(Collectors.toSet());

                // OPTIMIZATION: Bulk fetch all user profiles at once
                Map<String, User> usersById = new HashMap<>();
                for (String userId : userIds) {
                    if (Thread.currentThread().isInterrupted())
                        return;
                    Optional<User> userOpt = databaseManager.findUserByUserId(userId);
                    userOpt.ifPresent(user -> usersById.put(userId, user));
                }
                System.out.println("Fetched " + usersById.size() + " user profiles");

                if (Thread.currentThread().isInterrupted())
                    return;

                // OPTIMIZATION: Bulk fetch all attendance records for all sessions
                Map<String, Map<UUID, AttendanceRecord>> attendanceByUserAndSession = new HashMap<>();
                for (Session session : sessions) {
                    if (Thread.currentThread().isInterrupted())
                        return;
                    List<AttendanceRecord> sessionRecords = databaseManager.findAttendanceBySessionId(session.getId());
                    for (AttendanceRecord record : sessionRecords) {
                        attendanceByUserAndSession
                                .computeIfAbsent(record.getUserId(), k -> new HashMap<>())
                                .put(session.getId(), record);
                    }
                }
                System.out.println("Fetched attendance records for " + sessions.size() + " sessions");

                if (Thread.currentThread().isInterrupted())
                    return;

                // Build attendance data rows (fast - all data is in memory)
                // IMPORTANT: Create separate row for each enrollment (course + section
                // combination)
                ObservableList<AttendanceRow> rows = FXCollections.observableArrayList();

                for (com.cs102.model.Class enrollment : enrollments) {
                    if (Thread.currentThread().isInterrupted())
                        return;

                    String userId = enrollment.getUserId();
                    User user = usersById.get(userId);
                    if (user == null)
                        continue;

                    // Get enrollment info for course and section
                    String course = enrollment.getCourse();
                    String section = enrollment.getSection();

                    // Get year and semester from the course - we'll find it from one of the
                    // sessions
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

                        if (userCourse != null && userCourse.getSemester() != null
                                && userCourse.getSemester().contains("-")) {
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

                if (Thread.currentThread().isInterrupted())
                    return;

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
                    if (c.getSemester() == null || !c.getSemester().contains("-"))
                        return false;
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

    private List<com.cs102.model.Class> getEnrollmentsForFilter(String year, String semester, String course,
            String section) {
        // Get all professor courses first
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

        // Filter courses by year and semester
        List<Course> filteredCourses = professorCourses.stream()
                .filter(c -> {
                    if (c.getSemester() == null || !c.getSemester().contains("-"))
                        return false;
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
        yearCol.setPrefWidth(50);
        yearCol.setMinWidth(50);
        yearCol.setResizable(true);

        // Semester Column
        TableColumn<AttendanceRow, String> semesterCol = new TableColumn<>("Semester");
        semesterCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSemester()));
        semesterCol.setPrefWidth(90);
        semesterCol.setMinWidth(90);
        semesterCol.setResizable(true);

        // Course Column
        TableColumn<AttendanceRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourse()));
        courseCol.setPrefWidth(60);
        courseCol.setMinWidth(60);
        courseCol.setResizable(true);

        // Section Column
        TableColumn<AttendanceRow, String> sectionCol = new TableColumn<>("Section");
        sectionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSection()));
        sectionCol.setPrefWidth(60);
        sectionCol.setMinWidth(60);
        sectionCol.setResizable(true);

        attendanceTable.getColumns().addAll(nameCol, idCol, yearCol, semesterCol, courseCol, sectionCol);

        // Weeks Column (parent for week 1-13 sub-columns)
        TableColumn<AttendanceRow, String> weeksCol = new TableColumn<>("Weeks");

        // Create a map of session to week number (based on chronological order)
        Map<String, Integer> sessionToWeekMap = new HashMap<>();
        for (int i = 0; i < Math.min(sessions.size(), 13); i++) {
            sessionToWeekMap.put(sessions.get(i).getSessionId(), i + 1);
        }

        // Add 13 week columns
        for (int week = 1; week <= 13; week++) {
            final int weekNumber = week;
            TableColumn<AttendanceRow, String> weekCol = new TableColumn<>(String.valueOf(week));
            weekCol.setPrefWidth(50);
            weekCol.setMinWidth(50);
            weekCol.setResizable(true);

            weekCol.setCellValueFactory(data -> {
                // Find the session that corresponds to this week
                String statusForWeek = "A";
                for (Map.Entry<String, String> entry : data.getValue().getSessionAttendance().entrySet()) {
                    Integer sessionWeek = sessionToWeekMap.get(entry.getKey());
                    if (sessionWeek != null && sessionWeek == weekNumber) {
                        statusForWeek = getStatusAbbreviation(entry.getValue());
                        break;
                    }
                }
                return new SimpleStringProperty(statusForWeek);
            });

            // Add cell factory for color coding
            weekCol.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
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
                                setStyle(
                                        "-fx-background-color: #E0E0E0; -fx-text-fill: #999999; -fx-alignment: CENTER;"); // Gray
                                setText("-");
                                break;
                            default:
                                setStyle("-fx-alignment: CENTER;");
                        }
                    }
                }
            });

            weeksCol.getColumns().add(weekCol);
        }

        attendanceTable.getColumns().add(weeksCol);

        // Totals Column (parent for P, L, A)
        TableColumn<AttendanceRow, String> totalsCol = new TableColumn<>("Totals");

        TableColumn<AttendanceRow, String> totalPresentCol = new TableColumn<>("P");
        totalPresentCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalPresent())));
        totalPresentCol.setPrefWidth(40);
        totalPresentCol.setMinWidth(40);
        totalPresentCol.setResizable(true);
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
        totalLateCol
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalLate())));
        totalLateCol.setPrefWidth(40);
        totalLateCol.setMinWidth(40);
        totalLateCol.setResizable(true);
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
        totalAbsentCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalAbsent())));
        totalAbsentCol.setPrefWidth(40);
        totalAbsentCol.setMinWidth(40);
        totalAbsentCol.setResizable(true);
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
        percentPresentCol
                .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentPresent() + "%"));
        percentPresentCol.setPrefWidth(50);
        percentPresentCol.setMinWidth(50);
        percentPresentCol.setResizable(true);
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
        percentLateCol.setPrefWidth(50);
        percentLateCol.setMinWidth(50);
        percentLateCol.setResizable(true);
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
        percentAbsentCol
                .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentAbsent() + "%"));
        percentAbsentCol.setPrefWidth(50);
        percentAbsentCol.setMinWidth(50);
        percentAbsentCol.setResizable(true);
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
        addClassBtn.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 16;");
        addClassBtn.setOnAction(e -> handleAddClass());

        Button deleteClassBtn = new Button("Delete Class");
        deleteClassBtn.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 16;");
        deleteClassBtn.setOnAction(e -> handleDeleteClass());

        filterRow.getChildren().addAll(yearLabel, yearFilterDropdown, semesterLabel, semesterFilterDropdown,
                courseLabel, courseFilterDropdown, sectionLabel, sectionFilterDropdown,
                spacer, addClassBtn, deleteClassBtn);

        // Classes Table
        TableView<ClassRow> classesTable = createClassesTable();

        // Load professor's courses
        loadClassesData(classesTable, yearFilterDropdown, semesterFilterDropdown, courseFilterDropdown,
                sectionFilterDropdown);

        content.getChildren().addAll(titleLabel, filterRow, classesTable);
        mainLayout.setCenter(content);
    }

    private TableView<ClassRow> createClassesTable() {
        TableView<ClassRow> table = new TableView<>();
        // Use constrained resize policy to prevent extra column
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Course Column
        TableColumn<ClassRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourse()));
        courseCol.setPrefWidth(120);
        courseCol.setMaxWidth(180);

        // Section Column
        TableColumn<ClassRow, String> sectionCol = new TableColumn<>("Section");
        sectionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSection()));
        sectionCol.setPrefWidth(100);
        sectionCol.setMaxWidth(120);

        // Year Column
        TableColumn<ClassRow, String> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getYear()));
        yearCol.setPrefWidth(80);
        yearCol.setMaxWidth(100);
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
        semesterCol.setMaxWidth(150);
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
        TableColumn<ClassRow, String> studentsCol = new TableColumn<>("Students");
        studentsCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getNumStudents())));
        studentsCol.setPrefWidth(100);
        studentsCol.setMaxWidth(120);
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
        TableColumn<ClassRow, String> sessionsCol = new TableColumn<>("Sessions");
        sessionsCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getNumSessions())));
        sessionsCol.setPrefWidth(100);
        sessionsCol.setMaxWidth(120);
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

        // Edit Classlist Column (Button Column) - this will flex to fill remaining
        // space
        TableColumn<ClassRow, Void> editCol = new TableColumn<>("");
        editCol.setPrefWidth(150);
        editCol.setCellFactory(col -> new TableCell<ClassRow, Void>() {
            private final Button editBtn = new Button("Edit Classlist");

            {
                editBtn.setStyle(
                        "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 12;");
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

    private void loadClassesData(TableView<ClassRow> table, ComboBox<String> yearFilter,
            ComboBox<String> semesterFilter,
            ComboBox<String> courseFilter, ComboBox<String> sectionFilter) {
        // Get all courses taught by this professor
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

        // Populate year filter dropdown (always show all years)
        ObservableList<String> yearOptions = FXCollections.observableArrayList();
        yearOptions.add("All");
        Set<String> uniqueYears = professorCourses.stream()
                .map(c -> c.getSemester() != null && c.getSemester().contains("-") ? c.getSemester().split("-")[0] : "")
                .filter(y -> !y.isEmpty())
                .collect(Collectors.toSet());
        yearOptions.addAll(uniqueYears.stream().sorted().collect(Collectors.toList()));
        yearFilter.setItems(yearOptions);

        // Set listeners FIRST before setting values to avoid premature filtering
        yearFilter.setOnAction(e -> {
            savedYear = yearFilter.getValue();
            updateSemesterFilterDropdown(yearFilter, semesterFilter, professorCourses);
            updateCourseFilterDropdown(yearFilter, semesterFilter, courseFilter, professorCourses);
            updateSectionFilterDropdown(yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
            filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
        });
        semesterFilter.setOnAction(e -> {
            savedSemester = semesterFilter.getValue();
            updateCourseFilterDropdown(yearFilter, semesterFilter, courseFilter, professorCourses);
            updateSectionFilterDropdown(yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
            filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
        });
        courseFilter.setOnAction(e -> {
            savedCourse = courseFilter.getValue();
            updateSectionFilterDropdown(yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
            filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
        });
        sectionFilter.setOnAction(e -> {
            savedSection = sectionFilter.getValue();
            filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
        });

        // Restore saved year value if valid, otherwise "All"
        if (yearOptions.contains(savedYear)) {
            yearFilter.setValue(savedYear);
        } else {
            yearFilter.setValue("All");
            savedYear = "All";
        }

        // Initialize cascading dropdowns (this will restore saved values)
        updateSemesterFilterDropdown(yearFilter, semesterFilter, professorCourses);
        updateCourseFilterDropdown(yearFilter, semesterFilter, courseFilter, professorCourses);
        updateSectionFilterDropdown(yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);

        // Apply initial filter to show data based on restored filter values
        filterClassesTable(table, yearFilter, semesterFilter, courseFilter, sectionFilter, professorCourses);
    }

    private void updateSemesterFilterDropdown(ComboBox<String> yearFilter, ComboBox<String> semesterFilter,
            List<Course> professorCourses) {
        String selectedYear = yearFilter.getValue();

        // Filter semesters based on selected year
        ObservableList<String> semesterOptions = FXCollections.observableArrayList();
        semesterOptions.add("All");

        Set<String> uniqueSemesters = professorCourses.stream()
                .filter(c -> {
                    if (c.getSemester() == null || !c.getSemester().contains("-"))
                        return false;
                    String year = c.getSemester().split("-")[0];
                    return selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
                })
                .map(c -> c.getSemester().split("-", 2)[1])
                .collect(Collectors.toSet());

        semesterOptions.addAll(uniqueSemesters.stream().sorted().collect(Collectors.toList()));

        semesterFilter.setItems(semesterOptions);

        // Restore saved semester value if valid, otherwise set to "All"
        if (semesterOptions.contains(savedSemester)) {
            semesterFilter.setValue(savedSemester);
        } else {
            semesterFilter.setValue("All");
            savedSemester = "All";
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
                    if (c.getSemester() == null || !c.getSemester().contains("-"))
                        return false;
                    String[] parts = c.getSemester().split("-", 2);
                    String year = parts[0];
                    String semester = parts.length > 1 ? parts[1] : "";

                    boolean yearMatch = selectedYear == null || selectedYear.equals("All") || year.equals(selectedYear);
                    boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All")
                            || semester.equals(selectedSemester);

                    return yearMatch && semesterMatch;
                })
                .map(Course::getCourse)
                .collect(Collectors.toSet());

        courseOptions.addAll(uniqueCourses.stream().sorted().collect(Collectors.toList()));

        courseFilter.setItems(courseOptions);

        // Restore saved course value if valid, otherwise set to "All"
        if (courseOptions.contains(savedCourse)) {
            courseFilter.setValue(savedCourse);
        } else {
            courseFilter.setValue("All");
            savedCourse = "All";
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
                    boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All")
                            || semester.equals(selectedSemester);
                    boolean courseMatch = selectedCourse == null || selectedCourse.equals("All")
                            || c.getCourse().equals(selectedCourse);

                    return yearMatch && semesterMatch && courseMatch;
                })
                .map(Course::getSection)
                .collect(Collectors.toSet());

        sectionOptions.addAll(uniqueSections.stream().sorted().collect(Collectors.toList()));

        sectionFilter.setItems(sectionOptions);

        // Restore saved section value if valid, otherwise set to "All"
        if (sectionOptions.contains(savedSection)) {
            sectionFilter.setValue(savedSection);
        } else {
            sectionFilter.setValue("All");
            savedSection = "All";
        }
    }

    private void filterClassesTable(TableView<ClassRow> table, ComboBox<String> yearFilter,
            ComboBox<String> semesterFilter,
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

                ClassRow row = new ClassRow(course.getCourse(), course.getSection(), year, semester, numStudents,
                        numSessions);
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
            Optional<Course> existing = databaseManager.findCourseByCourseAndSection(course.getCourse(),
                    course.getSection());
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
            confirmAlert.setContentText(
                    "This will delete the course, all enrollments, and all sessions. This action cannot be undone.");

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
                removeBtn.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8;");
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

        // Create header row with "Enrolled Students:" label and Import CSV button
        HBox enrolledStudentsHeader = new HBox();
        enrolledStudentsHeader.setAlignment(Pos.CENTER_LEFT);

        Label enrolledStudentsLabel = new Label("Enrolled Students:");
        enrolledStudentsLabel.setFont(javafx.scene.text.Font.font("Tahoma", javafx.scene.text.FontWeight.BOLD, 14));

        Region spacerRegion = new Region();
        HBox.setHgrow(spacerRegion, Priority.ALWAYS);

        Button importCsvBtn = new Button("Import CSV");
        importCsvBtn.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 12;");
        importCsvBtn.setOnAction(e -> handleImportCsv(classRow, studentTable));

        enrolledStudentsHeader.getChildren().addAll(enrolledStudentsLabel, spacerRegion, importCsvBtn);

        // Add student section
        HBox addStudentRow = new HBox(10);
        addStudentRow.setAlignment(Pos.CENTER_LEFT);

        TextField studentIdField = new TextField();
        studentIdField.setPromptText("Enter Student ID (e.g., S12345)");
        studentIdField.setPrefWidth(250);

        Button addStudentBtn = new Button("Add Student");
        addStudentBtn.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 12;");
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
            if (student.getRole() != UserRole.STUDENT) {
                showAlert(Alert.AlertType.ERROR, "Invalid User",
                        "User " + studentId + " has role '" + student.getRole() + "', not 'STUDENT'.");
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

        content.getChildren().addAll(enrolledStudentsHeader, studentTable, new Label("Add New Student:"),
                addStudentRow);
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

    private void handleImportCsv(ClassRow classRow, TableView<User> studentTable) {
        // Create file chooser
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        // Show open dialog
        java.io.File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return; // User cancelled
        }

        // Read and process CSV file
        List<String> studentIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String studentId = line.trim();

                // Skip empty lines
                if (studentId.isEmpty()) {
                    continue;
                }

                // Skip header line if it contains "student" or "id" (case insensitive)
                if (lineNumber == 1 && (studentId.toLowerCase().contains("student") ||
                        studentId.toLowerCase().contains("id"))) {
                    continue;
                }

                // If line has multiple columns (comma-separated), take the first column
                if (studentId.contains(",")) {
                    studentId = studentId.split(",")[0].trim();
                }

                studentIds.add(studentId);
            }

            // Now process each student ID
            for (String studentId : studentIds) {
                // Check if student exists
                Optional<User> studentOpt = databaseManager.findUserByUserId(studentId);
                if (studentOpt.isEmpty()) {
                    errors.add("Student ID " + studentId + " not found");
                    errorCount++;
                    continue;
                }

                User student = studentOpt.get();
                if (student.getRole() != UserRole.STUDENT) {
                    errors.add(studentId + " has role '" + student.getRole() + "', not 'STUDENT'");
                    errorCount++;
                    continue;
                }

                // Check if already enrolled
                List<com.cs102.model.Class> enrollments = databaseManager.findEnrollmentsByCourseAndSection(
                        classRow.getCourse(), classRow.getSection());
                boolean alreadyEnrolled = enrollments.stream()
                        .anyMatch(e -> e.getUserId().equals(studentId));

                if (alreadyEnrolled) {
                    skipCount++;
                    continue;
                }

                // Add enrollment
                com.cs102.model.Class enrollment = new com.cs102.model.Class(
                        classRow.getCourse(), classRow.getSection(), studentId);
                databaseManager.saveClassEnrollment(enrollment);
                successCount++;
            }

            // Refresh the student table
            loadEnrolledStudents(studentTable, classRow);

            // Show summary
            StringBuilder message = new StringBuilder();
            message.append("Import completed!\n\n");
            message.append("Successfully added: ").append(successCount).append("\n");
            message.append("Already enrolled (skipped): ").append(skipCount).append("\n");
            message.append("Errors: ").append(errorCount).append("\n");

            if (!errors.isEmpty() && errors.size() <= 10) {
                message.append("\nError details:\n");
                for (String error : errors) {
                    message.append("- ").append(error).append("\n");
                }
            } else if (errors.size() > 10) {
                message.append("\nShowing first 10 errors:\n");
                for (int i = 0; i < 10; i++) {
                    message.append("- ").append(errors.get(i)).append("\n");
                }
                message.append("... and ").append(errors.size() - 10).append(" more errors");
            }

            Alert alert = new Alert(successCount > 0 ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
            alert.setTitle("Import Results");
            alert.setHeaderText(null);
            alert.setContentText(message.toString());
            alert.showAndWait();

        } catch (java.io.IOException e) {
            showAlert(Alert.AlertType.ERROR, "File Error",
                    "Failed to read CSV file: " + e.getMessage());
        }
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
        content.setAlignment(Pos.TOP_LEFT);

        // Title
        Label titleLabel = new Label("Sessions Overview");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        // Filters HBox
        HBox filtersBox = new HBox(20);
        filtersBox.setAlignment(Pos.CENTER_LEFT);

        // Course Label and ComboBox
        Label courseLabel = new Label("Course:");
        courseLabel.setFont(Font.font("Tahoma", FontWeight.NORMAL, 14));
        ComboBox<String> courseComboBox = new ComboBox<>();
        courseComboBox.setPromptText("Select Course");
        courseComboBox.setPrefWidth(180);

        // Section Label and ComboBox
        Label sectionLabel = new Label("Section:");
        sectionLabel.setFont(Font.font("Tahoma", FontWeight.NORMAL, 14));
        ComboBox<String> sectionComboBox = new ComboBox<>();
        sectionComboBox.setPromptText("Select Section");
        sectionComboBox.setPrefWidth(180);

        // Create Session Button
        Button createSessionBtn = new Button("Create Session");
        createSessionBtn.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        createSessionBtn.setOnMouseEntered(e -> createSessionBtn.setStyle(
                "-fx-background-color: #229954; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;"));
        createSessionBtn.setOnMouseExited(e -> createSessionBtn.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;"));
        createSessionBtn
                .setOnAction(e -> showCreateSessionDialog(courseComboBox.getValue(), sectionComboBox.getValue()));

        filtersBox.getChildren().addAll(courseLabel, courseComboBox, sectionLabel, sectionComboBox, createSessionBtn);

        // TableView for Sessions
        TableView<SessionRow> sessionTable = new TableView<>();
        sessionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Session ID Column
        TableColumn<SessionRow, String> sessionIdCol = new TableColumn<>("All Sessions IDs");
        sessionIdCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().sessionId));
        sessionIdCol.setPrefWidth(150);

        // Course Column
        TableColumn<SessionRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().course));
        courseCol.setPrefWidth(80);

        // Section Column
        TableColumn<SessionRow, String> sectionCol = new TableColumn<>("Section");
        sectionCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().section));
        sectionCol.setPrefWidth(80);

        // Date Column
        TableColumn<SessionRow, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().date));
        dateCol.setPrefWidth(100);

        // Start Time Column
        TableColumn<SessionRow, String> startTimeCol = new TableColumn<>("Start Time");
        startTimeCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().startTime));
        startTimeCol.setPrefWidth(90);

        // End Time Column
        TableColumn<SessionRow, String> endTimeCol = new TableColumn<>("End Time");
        endTimeCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().endTime));
        endTimeCol.setPrefWidth(90);

        // Statistics Column (Present/Late/Absent with bars)
        TableColumn<SessionRow, SessionRow> statsCol = new TableColumn<>("Statistics");
        statsCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        statsCol.setCellFactory(col -> new TableCell<SessionRow, SessionRow>() {
            @Override
            protected void updateItem(SessionRow sessionRow, boolean empty) {
                super.updateItem(sessionRow, empty);
                if (empty || sessionRow == null) {
                    setGraphic(null);
                } else {
                    VBox statsBox = new VBox(5);
                    statsBox.setAlignment(Pos.CENTER_LEFT);

                    // Present Bar
                    HBox presentBox = createStatBar("P", sessionRow.presentCount, "#27ae60",
                            sessionRow.getTotalStudents());
                    // Late Bar
                    HBox lateBox = createStatBar("L", sessionRow.lateCount, "#f39c12", sessionRow.getTotalStudents());
                    // Absent Bar
                    HBox absentBox = createStatBar("A", sessionRow.absentCount, "#e74c3c",
                            sessionRow.getTotalStudents());

                    statsBox.getChildren().addAll(presentBox, lateBox, absentBox);
                    setGraphic(statsBox);
                }
            }
        });
        statsCol.setPrefWidth(200);

        // Actions Column (View Report and Export CSV buttons)
        TableColumn<SessionRow, SessionRow> actionsCol = new TableColumn<>("Actions");
        actionsCol
                .setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        actionsCol.setCellFactory(col -> new TableCell<SessionRow, SessionRow>() {
            @Override
            protected void updateItem(SessionRow sessionRow, boolean empty) {
                super.updateItem(sessionRow, empty);
                if (empty || sessionRow == null) {
                    setGraphic(null);
                } else {
                    HBox actionsBox = new HBox(10);
                    actionsBox.setAlignment(Pos.CENTER);

                    Button viewReportBtn = new Button("View Report");
                    viewReportBtn.setStyle(
                            "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-cursor: hand;");
                    viewReportBtn.setOnMouseEntered(e -> viewReportBtn.setStyle(
                            "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-cursor: hand;"));
                    viewReportBtn.setOnMouseExited(e -> viewReportBtn.setStyle(
                            "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-cursor: hand;"));
                    viewReportBtn.setOnAction(e -> showSessionReport(sessionRow));

                    Button exportCsvBtn = new Button("Export CSV");
                    exportCsvBtn.setStyle(
                            "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-cursor: hand;");
                    exportCsvBtn.setOnMouseEntered(e -> exportCsvBtn.setStyle(
                            "-fx-background-color: #229954; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-cursor: hand;"));
                    exportCsvBtn.setOnMouseExited(e -> exportCsvBtn.setStyle(
                            "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-cursor: hand;"));
                    exportCsvBtn.setOnAction(e -> exportSessionToCSV(sessionRow));

                    actionsBox.getChildren().addAll(viewReportBtn, exportCsvBtn);
                    setGraphic(actionsBox);
                }
            }
        });
        actionsCol.setPrefWidth(220);

        sessionTable.getColumns().addAll(sessionIdCol, courseCol, sectionCol, dateCol, startTimeCol, endTimeCol,
                statsCol, actionsCol);

        // Load professor's courses into ComboBox
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

        // Populate course dropdown with unique course codes
        Set<String> uniqueCourses = new java.util.HashSet<>();
        for (Course course : professorCourses) {
            uniqueCourses.add(course.getCourse());
        }
        courseComboBox.getItems().add("All");
        courseComboBox.getItems().addAll(uniqueCourses.stream().sorted().collect(java.util.stream.Collectors.toList()));
        courseComboBox.setValue("All");

        // When course is selected, update sections
        courseComboBox.setOnAction(e -> {
            String selectedCourse = courseComboBox.getValue();
            sectionComboBox.getItems().clear();
            sectionComboBox.getItems().add("All");
            if (selectedCourse != null && !selectedCourse.equals("All")) {
                List<String> sections = professorCourses.stream()
                        .filter(c -> c.getCourse().equals(selectedCourse))
                        .map(Course::getSection)
                        .sorted()
                        .collect(java.util.stream.Collectors.toList());
                sectionComboBox.getItems().addAll(sections);
            }
            sectionComboBox.setValue("All");

            // Pass null for "All" selection
            String courseFilter = (selectedCourse != null && selectedCourse.equals("All")) ? null : selectedCourse;
            loadSessionsData(sessionTable, courseFilter, null);
        });

        // When section is selected, reload table
        sectionComboBox.setOnAction(e -> {
            String selectedCourse = courseComboBox.getValue();
            String selectedSection = sectionComboBox.getValue();

            // Pass null for "All" selection
            String courseFilter = (selectedCourse != null && selectedCourse.equals("All")) ? null : selectedCourse;
            String sectionFilter = (selectedSection != null && selectedSection.equals("All")) ? null : selectedSection;

            loadSessionsData(sessionTable, courseFilter, sectionFilter);
        });

        // Initial load - show all sessions
        loadSessionsData(sessionTable, null, null);

        content.getChildren().addAll(titleLabel, filtersBox, sessionTable);
        mainLayout.setCenter(content);
    }

    private HBox createStatBar(String label, int count, String color, int total) {
        HBox barBox = new HBox(5);
        barBox.setAlignment(Pos.CENTER_LEFT);

        Label countLabel = new Label(label + ": " + count);
        countLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 11));
        countLabel.setPrefWidth(40);

        // Progress bar
        double percentage = total > 0 ? (double) count / total : 0;
        javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar(percentage);
        progressBar.setPrefWidth(80);
        progressBar.setStyle("-fx-accent: " + color + ";");

        Label percentLabel = new Label(String.format("%.0f%%", percentage * 100));
        percentLabel.setFont(Font.font("Tahoma", FontWeight.NORMAL, 10));
        percentLabel.setPrefWidth(40);

        barBox.getChildren().addAll(countLabel, progressBar, percentLabel);
        return barBox;
    }

    private void loadSessionsData(TableView<SessionRow> table, String courseFilter, String sectionFilter) {
        new Thread(() -> {
            try {
                List<Session> sessions;

                // Get all professor's courses first
                List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());

                if (courseFilter != null && sectionFilter != null) {
                    // Load sessions for specific course/section
                    sessions = databaseManager.findSessionsByCourseAndSection(courseFilter, sectionFilter);
                } else if (courseFilter != null) {
                    // Load sessions for all sections of this course
                    sessions = new java.util.ArrayList<>();
                    for (Course course : professorCourses) {
                        if (course.getCourse().equals(courseFilter)) {
                            sessions.addAll(databaseManager.findSessionsByCourseAndSection(course.getCourse(),
                                    course.getSection()));
                        }
                    }
                } else {
                    // Load all sessions for all professor's courses
                    sessions = new java.util.ArrayList<>();
                    for (Course course : professorCourses) {
                        sessions.addAll(databaseManager.findSessionsByCourseAndSection(course.getCourse(),
                                course.getSection()));
                    }
                }

                // Build SessionRow objects with attendance statistics
                List<SessionRow> sessionRows = new java.util.ArrayList<>();
                for (Session session : sessions) {
                    List<AttendanceRecord> records = databaseManager.findAttendanceBySessionId(session.getId());

                    int presentCount = 0;
                    int lateCount = 0;
                    int absentCount = 0;

                    for (AttendanceRecord record : records) {
                        String status = record.getAttendance();
                        if ("Present".equalsIgnoreCase(status)) {
                            presentCount++;
                        } else if ("Late".equalsIgnoreCase(status)) {
                            lateCount++;
                        } else if ("Absent".equalsIgnoreCase(status)) {
                            absentCount++;
                        }
                    }

                    SessionRow row = new SessionRow(
                            session.getSessionId(),
                            session.getCourse(),
                            session.getSection(),
                            session.getDate().toString(),
                            session.getStartTime() != null ? session.getStartTime().toString() : "",
                            session.getEndTime() != null ? session.getEndTime().toString() : "",
                            presentCount,
                            lateCount,
                            absentCount,
                            session.getId());
                    sessionRows.add(row);
                }

                // Update table on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    table.getItems().clear();
                    table.getItems().addAll(sessionRows);
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load sessions: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showLiveRecognitionPage() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_LEFT);

        // Title
        Label titleLabel = new Label("Live Face Recognition Check-In");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        // Get current date for session filtering
        java.time.LocalDate today = java.time.LocalDate.now();

        // Get all courses for the professor (no semester filtering - show all courses)
        List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());
        List<Course> currentCourses = professorCourses; // Show all courses, not filtered by semester

        // First row: Course and Section dropdowns
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
        ObservableList<String> courseOptions = FXCollections.observableArrayList("All");
        courseOptions.addAll(uniqueCourses.stream().sorted().collect(Collectors.toList()));
        liveRecCourseDropdown.setItems(courseOptions);
        liveRecCourseDropdown.setValue("All");

        Button loadSessionsBtn = new Button("Load Sessions");
        loadSessionsBtn.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");

        dropdownRow.getChildren().addAll(courseLabel, liveRecCourseDropdown, sectionLabel, liveRecSectionDropdown,
                loadSessionsBtn);

        // Sessions table
        TableView<Session> sessionsTable = new TableView<>();
        sessionsTable.setPrefHeight(300);
        sessionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Session, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCourse()));
        courseCol.setPrefWidth(120);

        TableColumn<Session, String> sectionCol = new TableColumn<>("Section");
        sectionCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSection()));
        sectionCol.setPrefWidth(80);

        TableColumn<Session, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDate().toString()));
        dateCol.setPrefWidth(100);

        TableColumn<Session, String> startTimeCol = new TableColumn<>("Start Time");
        startTimeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStartTime().toString()));
        startTimeCol.setPrefWidth(90);

        TableColumn<Session, String> endTimeCol = new TableColumn<>("End Time");
        endTimeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getEndTime().toString()));
        endTimeCol.setPrefWidth(90);

        TableColumn<Session, String> sessionIdCol = new TableColumn<>("Session ID");
        sessionIdCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSessionId()));
        sessionIdCol.setPrefWidth(180);

        TableColumn<Session, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            Session session = cellData.getValue();
            java.time.LocalDate sessionDate = session.getDate();
            java.time.LocalTime sessionStart = session.getStartTime();
            java.time.LocalTime sessionEnd = session.getEndTime();
            java.time.LocalTime now = java.time.LocalTime.now();

            String status;
            if (sessionDate.equals(today) && !now.isBefore(sessionStart) && !now.isAfter(sessionEnd)) {
                status = "Active Now";
            } else if (sessionDate.isBefore(today) || (sessionDate.equals(today) && now.isAfter(sessionEnd))) {
                status = "Past";
            } else {
                status = "Upcoming";
            }
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        statusCol.setPrefWidth(100);

        sessionsTable.getColumns().addAll(courseCol, sectionCol, dateCol, startTimeCol, endTimeCol, sessionIdCol,
                statusCol);

        // Start Check In button (below table)
        HBox buttonRow = new HBox(15);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        Button startCheckInBtn = new Button("Start Check In");
        startCheckInBtn.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 30; -fx-cursor: hand;");
        startCheckInBtn.setDisable(true);

        buttonRow.getChildren().add(startCheckInBtn);

        // Helper method to load active sessions
        Runnable loadActiveSessionsRunnable = () -> {
            String selectedCourse = liveRecCourseDropdown.getValue();
            String selectedSection = liveRecSectionDropdown.getValue();

            // Get current date and time
            java.time.LocalDate currentDate = java.time.LocalDate.now();
            java.time.LocalTime currentTime = java.time.LocalTime.now();

            List<Session> allSessions;

            // Convert "All" to null for filtering
            String courseFilter = (selectedCourse != null && selectedCourse.equals("All")) ? null : selectedCourse;
            String sectionFilter = (selectedSection != null && selectedSection.equals("All")) ? null : selectedSection;

            // If course and section are selected, filter by them
            if (courseFilter != null && sectionFilter != null) {
                allSessions = databaseManager.findSessionsByCourseAndSection(courseFilter, sectionFilter);
            } else if (courseFilter != null) {
                // If only course is selected, get all sessions for that course across all
                // sections
                allSessions = currentCourses.stream()
                        .filter(c -> c.getCourse().equals(courseFilter))
                        .flatMap(c -> databaseManager.findSessionsByCourseAndSection(c.getCourse(), c.getSection())
                                .stream())
                        .collect(Collectors.toList());
            } else {
                // No filters - get all sessions for all professor's courses
                allSessions = currentCourses.stream()
                        .flatMap(c -> databaseManager.findSessionsByCourseAndSection(c.getCourse(), c.getSection())
                                .stream())
                        .collect(Collectors.toList());
            }

            // Filter to show only active sessions (current time is within session time
            // range)
            List<Session> activeSessions = allSessions.stream()
                    .filter(s -> {
                        // Session must be today
                        if (!s.getDate().equals(currentDate))
                            return false;
                        // Current time must be within session time range
                        return !currentTime.isBefore(s.getStartTime()) && !currentTime.isAfter(s.getEndTime());
                    })
                    .collect(Collectors.toList());

            if (activeSessions.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Active Sessions");
                alert.setHeaderText(null);
                alert.setContentText("No sessions are currently active.\n\n" +
                        "Active sessions must be scheduled for today and the current time must be within the session's time range.");
                alert.showAndWait();
            }

            sessionsTable.setItems(FXCollections.observableArrayList(activeSessions));
            startCheckInBtn.setDisable(true);
        };

        // Update section dropdown when course changes
        liveRecCourseDropdown.setOnAction(e -> {
            String selectedCourse = liveRecCourseDropdown.getValue();
            liveRecSectionDropdown.getItems().clear();
            liveRecSectionDropdown.getItems().add("All");

            if (selectedCourse != null && !selectedCourse.equals("All")) {
                Set<String> sections = currentCourses.stream()
                        .filter(c -> c.getCourse().equals(selectedCourse))
                        .map(Course::getSection)
                        .sorted()
                        .collect(Collectors.toSet());
                liveRecSectionDropdown.getItems().addAll(sections);
            }
            liveRecSectionDropdown.setValue("All");
        });

        // Load sessions button action - shows only active sessions based on current
        // date and time
        loadSessionsBtn.setOnAction(e -> loadActiveSessionsRunnable.run());

        // Enable Start Check In button when a session is selected
        sessionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            startCheckInBtn.setDisable(newSelection == null);
        });

        // Start Check In button action
        startCheckInBtn.setOnAction(e -> {
            Session selectedSession = sessionsTable.getSelectionModel().getSelectedItem();

            if (selectedSession == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Session Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a session from the table to start check-in.");
                alert.showAndWait();
                return;
            }

            // Get course and section from the selected session
            String sessionCourse = selectedSession.getCourse();
            String sessionSection = selectedSession.getSection();

            // All validations passed - start live recognition
            startLiveRecognition(sessionCourse, sessionSection, selectedSession);
        });

        content.getChildren().addAll(titleLabel, dropdownRow, sessionsTable, buttonRow);
        mainLayout.setCenter(content);

        // Load all active sessions on page load
        loadActiveSessionsRunnable.run();
    }

    private void startLiveRecognition(String course, String section, Session session) {
        // Create main layout with BorderPane
        BorderPane liveRecLayout = new BorderPane();
        liveRecLayout.setPadding(new Insets(20));

        // Top section - Title, Stop Button, and Recognition Log
        BorderPane topSection = new BorderPane();

        // Left side: Title and Stop button
        HBox leftBox = new HBox(20);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Live Face Recognition - " + course + " Section " + section);
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));

        Button stopBtn = new Button("Stop Check In");
        stopBtn.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");

        leftBox.getChildren().addAll(titleLabel, stopBtn);
        topSection.setLeft(leftBox);

        // Recognition log box (top-right, smaller)
        VBox logBox = new VBox(5);
        logBox.setPadding(new Insets(10));
        logBox.setPrefWidth(280);
        logBox.setPrefHeight(120);
        logBox.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.95); -fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label logTitle = new Label("Recognition Log");
        logTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 11));

        ListView<Label> logList = new ListView<>();
        logList.setPrefHeight(85);
        logList.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 10px;");

        logBox.getChildren().addAll(logTitle, logList);
        topSection.setRight(logBox);
        BorderPane.setAlignment(logBox, Pos.TOP_RIGHT);

        liveRecLayout.setTop(topSection);
        BorderPane.setMargin(topSection, new Insets(0, 0, 20, 0));

        // Center section - Large camera view (10% smaller height)
        VBox cameraBox = new VBox();
        cameraBox.setAlignment(Pos.CENTER);

        ImageView cameraView = new ImageView();
        cameraView.setFitWidth(900);
        cameraView.setFitHeight(675);
        cameraView.setPreserveRatio(true);
        cameraView.setStyle("-fx-border-color: black; -fx-border-width: 2;");

        cameraBox.getChildren().add(cameraView);
        liveRecLayout.setCenter(cameraBox);

        mainLayout.setCenter(liveRecLayout);

        // Start camera and face recognition in background thread
        Thread recognitionThread = new Thread(() -> {
            try {
                runLiveRecognition(course, section, session, cameraView, logList, stopBtn);
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
            ImageView cameraView, ListView<Label> logList, Button stopBtn) {
        // Load OpenCV
        nu.pattern.OpenCV.loadLocally();

        // Initialize ArcFace recognizer
        com.cs102.recognition.ArcFaceRecognizer arcFace;
        try {
            arcFace = new com.cs102.recognition.ArcFaceRecognizer();
        } catch (Exception e) {
            System.err.println("Failed to initialize ArcFace: " + e.getMessage());
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("ArcFace Error");
                alert.setHeaderText("Failed to load ArcFace model");
                alert.setContentText(
                        "Error: " + e.getMessage() + "\n\nPlease check your internet connection and try again.");
                alert.showAndWait();
            });
            return;
        }

        // Get all students enrolled in this course and section
        List<com.cs102.model.Class> enrollments = databaseManager.findEnrollmentsByCourseAndSection(course, section);
        Map<String, User> studentMap = new HashMap<>();
        Map<String, float[][]> studentFaceEmbeddings = new HashMap<>();

        System.out.println("Loading student face embeddings...");

        for (com.cs102.model.Class enrollment : enrollments) {
            String userId = enrollment.getUserId();
            User student = databaseManager.findUserByUserId(userId).orElse(null);
            if (student != null) {
                studentMap.put(userId, student);

                // Load face images for this student and compute ArcFace embeddings
                List<FaceImage> faceImages = databaseManager.findFaceImagesByUserId(userId);
                List<float[]> embeddings = new ArrayList<>();

                for (FaceImage faceImage : faceImages) {
                    try {
                        byte[] imageData = faceImage.getImageData();
                        org.opencv.core.MatOfByte matOfByte = new org.opencv.core.MatOfByte(imageData);

                        // Decode image (should be 112x112 RGB from registration)
                        org.opencv.core.Mat faceMat = org.opencv.imgcodecs.Imgcodecs.imdecode(matOfByte,
                                org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR);

                        if (faceMat != null && !faceMat.empty()) {
                            // Preprocess for ArcFace
                            org.opencv.core.Mat preprocessed = arcFace.preprocessFace(faceMat);

                            // Extract embedding
                            float[] embedding = arcFace.extractEmbedding(preprocessed);
                            embeddings.add(embedding);

                            // Clean up
                            faceMat.release();
                            preprocessed.release();
                        }
                    } catch (Exception e) {
                        System.err.println(
                                "Failed to process face image for " + student.getName() + ": " + e.getMessage());
                    }
                }

                if (!embeddings.isEmpty()) {
                    // Convert List<float[]> to float[][]
                    float[][] embeddingsArray = embeddings.toArray(new float[0][]);
                    studentFaceEmbeddings.put(userId, embeddingsArray);
                    System.out.println("Loaded " + embeddings.size() + " embeddings for student: " + student.getName());
                }
            }
        }

        System.out.println("✓ Loaded " + studentFaceEmbeddings.size() + " students for ArcFace recognition");

        // Store ArcFace instance for use in detection thread
        final com.cs102.recognition.ArcFaceRecognizer finalArcFace = arcFace;

        // Open camera
        org.opencv.videoio.VideoCapture camera = new org.opencv.videoio.VideoCapture(0);
        if (!camera.isOpened()) {
            System.err.println("Failed to open camera");
            return;
        }

        // Configure camera for maximum FPS and performance (matching FaceCaptureView)
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 60.0); // Request 60 FPS (camera will use max available)
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH, 640);
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT, 480);
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_BUFFERSIZE, 1); // Minimize buffer latency

        // Try common resolutions in order of preference for live recognition
        // Lower resolution = higher FPS and less lag
        int[][] resolutions = {
                { 1280, 720 }, // HD 720p - best balance for recognition
                { 960, 540 }, // qHD - good performance
                { 640, 480 }, // VGA - fallback
        };

        int selectedWidth = 640;
        int selectedHeight = 480;

        for (int[] res : resolutions) {
            camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH, res[0]);
            camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT, res[1]);

            double actualWidth = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH);
            double actualHeight = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT);

            // Check if camera accepted this resolution (within 10% tolerance)
            if (Math.abs(actualWidth - res[0]) < res[0] * 0.1 &&
                    Math.abs(actualHeight - res[1]) < res[1] * 0.1) {
                selectedWidth = (int) actualWidth;
                selectedHeight = (int) actualHeight;
                System.out.println("  ✓ Camera supports " + selectedWidth + "x" + selectedHeight);
                break;
            }
        }

        // Try to maximize FPS (try 60, 30, then accept whatever camera provides)
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 60.0);
        double actualFps = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FPS);

        if (actualFps < 30) {
            camera.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 30.0);
            actualFps = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FPS);
        }

        // Optimize buffer settings for minimal latency
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_BUFFERSIZE, 1);

        System.out.println("Camera optimized for this computer:");
        System.out.println("  Resolution: " + selectedWidth + "x" + selectedHeight);
        System.out.println("  FPS: " + String.format("%.1f", actualFps));
        System.out.println("  Detection: continuous (separate thread)");

        // Main recognition loop with FPS tracking
        org.opencv.core.Mat frame = new org.opencv.core.Mat();
        Set<String> recentlyCheckedIn = new HashSet<>(); // Track recently checked-in students
        Map<String, Double> highestConfidence = new HashMap<>(); // Track highest confidence per student
        Map<String, Label> studentLogLabels = new HashMap<>(); // Track log labels for each student (to remove/replace)
        int frameCount = 0;
        long startTime = System.currentTimeMillis();
        long lastFpsReport = startTime;

        // Initialize YuNet face detector with camera resolution
        final FaceDetectorYN finalFaceDetector = initializeFaceDetector(selectedWidth, selectedHeight);
        if (finalFaceDetector == null) {
            System.err.println("Failed to load YuNet face detector");
            camera.release();
            return;
        }

        // Shared state for detection thread
        final org.opencv.core.Mat latestFrame = new org.opencv.core.Mat();
        final org.opencv.core.Mat detectionResults = new org.opencv.core.Mat();
        final Object frameLock = new Object();
        final Map<org.opencv.core.Rect, RecognitionResult> cachedResults = new HashMap<>();

        // Separate thread for continuous face detection and recognition (doesn't block
        // rendering)
        Thread detectionThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && camera.isOpened()) {
                org.opencv.core.Mat frameToProcess = new org.opencv.core.Mat();
                synchronized (frameLock) {
                    if (!latestFrame.empty()) {
                        latestFrame.copyTo(frameToProcess);
                    }
                }

                if (!frameToProcess.empty()) {
                    // Detect faces using YuNet
                    org.opencv.core.Mat faces = new org.opencv.core.Mat();
                    finalFaceDetector.detect(frameToProcess, faces);

                    System.out.println("DEBUG: Detected " + faces.rows() + " faces");

                    Map<org.opencv.core.Rect, RecognitionResult> newResults = new HashMap<>();

                    // Process each detected face
                    for (int i = 0; i < faces.rows(); i++) {
                        float x = (float) faces.get(i, 0)[0];
                        float y = (float) faces.get(i, 1)[0];
                        float w = (float) faces.get(i, 2)[0];
                        float h = (float) faces.get(i, 3)[0];

                        org.opencv.core.Rect faceRect = new org.opencv.core.Rect((int) x, (int) y, (int) w, (int) h);

                        try {
                            org.opencv.core.Mat face = frameToProcess.submat(faceRect).clone();

                            // Preprocess for ArcFace
                            org.opencv.core.Mat preprocessed = finalArcFace.preprocessFace(face);

                            // Extract embedding
                            float[] queryEmbedding = finalArcFace.extractEmbedding(preprocessed);

                            // Find best match using ArcFace
                            com.cs102.recognition.ArcFaceRecognizer.MatchResult match = finalArcFace
                                    .findBestMatch(queryEmbedding, studentFaceEmbeddings, 0.5);

                            System.out.println("DEBUG: Extracted embedding for face at (" + x + "," + y + ")");

                            if (match != null) {
                                System.out.println("DEBUG: Match found! " + match.userId + " with confidence "
                                        + match.getConfidencePercentage() + "%");
                                User student = studentMap.get(match.userId);
                                if (student != null) {
                                    RecognitionResult result = new RecognitionResult(
                                            match.userId,
                                            student.getName(),
                                            match.getConfidencePercentage());
                                    newResults.put(faceRect, result);
                                    System.out.println("Detection Thread: Recognized " + result.studentName +
                                            " with confidence " + String.format("%.1f", result.confidence) + "%");
                                }
                            }

                            face.release();
                            preprocessed.release();
                        } catch (Exception e) {
                            System.err.println("ERROR: Failed to process face - " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    // Update cached results
                    synchronized (frameLock) {
                        cachedResults.clear();
                        cachedResults.putAll(newResults);
                        faces.copyTo(detectionResults);
                    }

                    faces.release();
                    frameToProcess.release();
                }

                // Small sleep to avoid maxing out CPU
                try {
                    Thread.sleep(100); // Run detection ~10 times per second
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "FaceRecognition-Thread");
        detectionThread.setDaemon(true);
        detectionThread.start();

        // Main rendering loop - display ALL frames without dropping
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
                System.out.println(
                        "Recognition Camera FPS: " + String.format("%.2f", fps) + " (Frame #" + frameCount + ")");
                lastFpsReport = currentTime;
            }

            // Update latest frame for detection thread
            synchronized (frameLock) {
                frame.copyTo(latestFrame);
            }

            // Draw results from detection thread
            synchronized (frameLock) {
                for (Map.Entry<org.opencv.core.Rect, RecognitionResult> entry : cachedResults.entrySet()) {
                    org.opencv.core.Rect faceRect = entry.getKey();
                    RecognitionResult result = entry.getValue();
                    // Update highest confidence for this student
                    double currentHighest = highestConfidence.getOrDefault(result.userId, 0.0);
                    double displayConfidence = result.confidence;

                    // Track if this is first time seeing this student
                    boolean isFirstDetection = !studentLogLabels.containsKey(result.userId);

                    // Only update if new confidence is higher
                    if (result.confidence > currentHighest) {
                        highestConfidence.put(result.userId, result.confidence);
                        displayConfidence = result.confidence;
                    } else {
                        // Use the previously recorded highest confidence
                        displayConfidence = currentHighest;
                    }

                    // Determine box color and handle check-in
                    org.opencv.core.Scalar boxColor;

                    if (displayConfidence >= 70.0) {
                        boxColor = new org.opencv.core.Scalar(0, 255, 0); // Green

                        // Check in student (only if not recently checked in)
                        if (!recentlyCheckedIn.contains(result.userId)) {
                            System.out.println("Attempting to check in student: " + result.studentName + " (ID: "
                                    + result.userId + ") with confidence: " + displayConfidence + "%");
                            checkInStudent(result.userId, session);
                            recentlyCheckedIn.add(result.userId);
                            System.out.println("Student " + result.studentName + " added to recentlyCheckedIn set");

                            // If there was a previous failure message, remove it
                            Label oldLabel = studentLogLabels.get(result.userId);

                            // Create success log entry
                            String timestamp = java.time.LocalTime.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            String logMessage = "[" + timestamp + "] ✓ " + result.studentName + " Checked In! ("
                                    + String.format("%.1f", displayConfidence) + "%)";
                            Label successLabel = new Label(logMessage);
                            successLabel
                                    .setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 11px;");

                            final Label oldLabelFinal = oldLabel;
                            final Label successLabelFinal = successLabel;
                            javafx.application.Platform.runLater(() -> {
                                // Remove old failure message if exists
                                if (oldLabelFinal != null) {
                                    logList.getItems().remove(oldLabelFinal);
                                }
                                // Add success message
                                logList.getItems().add(successLabelFinal);
                                logList.scrollTo(successLabelFinal);
                            });

                            studentLogLabels.put(result.userId, successLabel);
                        }
                    } else {
                        boxColor = new org.opencv.core.Scalar(0, 0, 255); // Red

                        // Only log error if first time seeing this student
                        if (isFirstDetection) {
                            String timestamp = java.time.LocalTime.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            String logMessage = "[" + timestamp + "] ✗ " + result.studentName + " Low Match ("
                                    + String.format("%.1f", displayConfidence) + "%)";
                            Label failureLabel = new Label(logMessage);
                            failureLabel
                                    .setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 11px;");

                            final Label failureLabelFinal = failureLabel;
                            javafx.application.Platform.runLater(() -> {
                                logList.getItems().add(failureLabelFinal);
                                logList.scrollTo(failureLabelFinal);
                            });

                            studentLogLabels.put(result.userId, failureLabel);
                        }
                    }

                    // Draw bounding box
                    org.opencv.imgproc.Imgproc.rectangle(frame,
                            new org.opencv.core.Point(faceRect.x, faceRect.y),
                            new org.opencv.core.Point(faceRect.x + faceRect.width, faceRect.y + faceRect.height),
                            boxColor, 3);

                    // Draw student name and HIGHEST confidence recorded
                    String label = result.studentName + " (" + String.format("%.1f", displayConfidence) + "%)";
                    org.opencv.imgproc.Imgproc.putText(frame, label,
                            new org.opencv.core.Point(faceRect.x, faceRect.y - 10),
                            org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, boxColor, 2);
                } // end for loop
            } // end synchronized

            // Display EVERY frame - no dropping
            final javafx.scene.image.Image fxImage = mat2Image(frame);
            javafx.application.Platform.runLater(() -> {
                cameraView.setImage(fxImage);
            });
        }

        // Release the frame after loop ends
        frame.release();

        // Cleanup
        camera.release();

        // Close ArcFace session
        if (finalArcFace != null) {
            finalArcFace.close();
        }
    }

    private FaceDetectorYN initializeFaceDetector(int width, int height) {
        try {
            String modelPath = downloadYuNetModel();

            if (modelPath == null) {
                System.err.println("Failed to get YuNet model");
                return null;
            }

            // Create YuNet face detector
            FaceDetectorYN detector = FaceDetectorYN.create(
                    modelPath,
                    "", // config (empty for ONNX)
                    new org.opencv.core.Size(width, height), // input size
                    0.6f, // score threshold
                    0.3f, // nms threshold
                    5000 // top_k
            );

            System.out.println("YuNet face detector initialized successfully for live recognition");
            return detector;
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

    // OLD HISTOGRAM-BASED METHODS REMOVED - NOW USING ARCFACE
    // See ArcFaceRecognizer class for new recognition implementation

    private void checkInStudent(String userId, Session session) {
        try {
            System.out.println("checkInStudent called for userId: " + userId + ", sessionId: " + session.getId());

            // Calculate attendance status based on check-in time using Singapore timezone
            java.time.ZoneId singaporeZone = java.time.ZoneId.of("Asia/Singapore");
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(singaporeZone);
            java.time.ZonedDateTime sessionStart = java.time.ZonedDateTime.of(
                session.getDate(),
                session.getStartTime(),
                singaporeZone
            );
            java.time.ZonedDateTime lateThreshold = sessionStart.plusMinutes(15);

            System.out.println("Current time (Singapore): " + now);
            System.out.println("Session start time (Singapore): " + sessionStart);
            System.out.println("Late threshold (15 min after start): " + lateThreshold);

            String attendanceStatus;
            if (now.isAfter(lateThreshold)) {
                attendanceStatus = "Late";
                System.out.println("Student checking in after 15-minute threshold: LATE");
            } else {
                attendanceStatus = "Present";
                System.out.println("Student checking in within 15 minutes: PRESENT");
            }

            // Convert to LocalDateTime for database storage
            java.time.LocalDateTime checkinTime = now.toLocalDateTime();

            // Check if student already checked in
            Optional<AttendanceRecord> existingRecord = databaseManager.findAttendanceByUserIdAndSessionId(userId,
                    session.getId());

            if (existingRecord.isEmpty()) {
                // No record exists - create new one (shouldn't happen if trigger is working)
                System.out.println("No existing record found. Creating new attendance record...");
                AttendanceRecord record = new AttendanceRecord();
                record.setUserId(userId);
                record.setSessionId(session.getId());
                record.setAttendance(attendanceStatus);
                record.setMethod("Auto");
                record.setCheckinTime(checkinTime);

                databaseManager.saveAttendanceRecord(record);
                System.out.println("✓ Successfully checked in student: " + userId + " as " + attendanceStatus + " at " + record.getCheckinTime());
            } else {
                // Record exists - update it with check-in time
                AttendanceRecord record = existingRecord.get();
                String previousStatus = record.getAttendance();

                // Only update if not already checked in
                if (record.getCheckinTime() == null) {
                    System.out
                            .println("Updating existing record from '" + previousStatus + "' to '" + attendanceStatus + "'...");
                    record.setCheckinTime(checkinTime);
                    record.setAttendance(attendanceStatus);

                    databaseManager.saveAttendanceRecord(record); // save() works for both insert and update
                    System.out
                            .println("✓ Successfully checked in student: " + userId + " as " + attendanceStatus + " at " + record.getCheckinTime());
                } else {
                    System.out.println("Student " + userId + " already checked in at: " + record.getCheckinTime());
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR checking in student: " + e.getMessage());
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

    private void showCreateSessionDialog(String preSelectedCourse, String preSelectedSection) {
        // Validate that course and section are selected
        if (preSelectedCourse == null || preSelectedSection == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required",
                    "Please select both a Course and Section before creating a session.");
            return;
        }

        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("Start Session");
        dialog.setHeaderText(null);

        // Create content
        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 40, 30, 40));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: #d3d3d3;");

        // Create GridPane for proper alignment
        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);

        // Get today's date
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy");

        // Course (read-only)
        Label courseLabel = new Label("Course:");
        courseLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        courseLabel.setAlignment(Pos.CENTER_RIGHT);
        courseLabel.setPrefWidth(100);
        Label courseValue = new Label(preSelectedCourse);
        courseValue.setFont(Font.font("Tahoma", FontWeight.NORMAL, 14));
        formGrid.add(courseLabel, 0, 0);
        formGrid.add(courseValue, 1, 0);

        // Section (read-only)
        Label sectionLabel = new Label("Section:");
        sectionLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        sectionLabel.setAlignment(Pos.CENTER_RIGHT);
        sectionLabel.setPrefWidth(100);
        Label sectionValue = new Label(preSelectedSection);
        sectionValue.setFont(Font.font("Tahoma", FontWeight.NORMAL, 14));
        formGrid.add(sectionLabel, 0, 1);
        formGrid.add(sectionValue, 1, 1);

        // Date (read-only)
        Label dateLabel = new Label("Date:");
        dateLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        dateLabel.setAlignment(Pos.CENTER_RIGHT);
        dateLabel.setPrefWidth(100);
        Label dateValue = new Label(today.format(dateFormatter));
        dateValue.setFont(Font.font("Tahoma", FontWeight.NORMAL, 14));
        formGrid.add(dateLabel, 0, 2);
        formGrid.add(dateValue, 1, 2);

        // Time Start (editable)
        Label startTimeLabel = new Label("Time Start:");
        startTimeLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        startTimeLabel.setAlignment(Pos.CENTER_RIGHT);
        startTimeLabel.setPrefWidth(100);
        TextField startTimeField = new TextField();
        startTimeField.setPromptText("HH:MM");
        startTimeField.setPrefWidth(200);
        startTimeField.setStyle("-fx-font-size: 13px;");
        formGrid.add(startTimeLabel, 0, 3);
        formGrid.add(startTimeField, 1, 3);

        // Time End (editable)
        Label endTimeLabel = new Label("Time End:");
        endTimeLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        endTimeLabel.setAlignment(Pos.CENTER_RIGHT);
        endTimeLabel.setPrefWidth(100);
        TextField endTimeField = new TextField();
        endTimeField.setPromptText("HH:MM");
        endTimeField.setPrefWidth(200);
        endTimeField.setStyle("-fx-font-size: 13px;");
        formGrid.add(endTimeLabel, 0, 4);
        formGrid.add(endTimeField, 1, 4);

        // Start Session button
        Button startSessionBtn = new Button("Start Session");
        startSessionBtn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; " +
                "-fx-font-size: 13px; -fx-padding: 10 30; -fx-cursor: hand; " +
                "-fx-background-radius: 20; -fx-border-radius: 20;");
        startSessionBtn.setOnMouseEntered(
                e -> startSessionBtn.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; " +
                        "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 30; -fx-cursor: hand; " +
                        "-fx-background-radius: 20; -fx-border-radius: 20;"));
        startSessionBtn
                .setOnMouseExited(e -> startSessionBtn.setStyle("-fx-background-color: white; -fx-text-fill: black; " +
                        "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 30; -fx-cursor: hand; " +
                        "-fx-background-radius: 20; -fx-border-radius: 20;"));

        startSessionBtn.setOnAction(e -> {
            try {
                String startTimeStr = startTimeField.getText().trim();
                String endTimeStr = endTimeField.getText().trim();

                // Validate inputs
                if (startTimeStr.isEmpty() || endTimeStr.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Please enter both start and end times!");
                    return;
                }

                // Parse times
                java.time.LocalTime startTime = java.time.LocalTime.parse(startTimeStr);
                java.time.LocalTime endTime = java.time.LocalTime.parse(endTimeStr);

                // Validate time order
                if (!endTime.isAfter(startTime)) {
                    showAlert(Alert.AlertType.ERROR, "Error", "End time must be after start time!");
                    return;
                }

                // Create session ID
                String sessionId = preSelectedCourse + "-" + preSelectedSection + "-" + today.toString();

                // Check if session already exists
                if (databaseManager.existsBySessionId(sessionId)) {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "A session already exists for this course and section today!");
                    return;
                }

                // Create new session
                Session session = new Session();
                session.setId(java.util.UUID.randomUUID());
                session.setSessionId(sessionId);
                session.setCourse(preSelectedCourse);
                session.setSection(preSelectedSection);
                session.setDate(today);
                session.setStartTime(startTime);
                session.setEndTime(endTime);
                session.setCreatedAt(java.time.LocalDateTime.now());

                // Save session to database
                databaseManager.saveSession(session);

                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Session started successfully!\n" +
                                "Students can now check in using facial recognition from " +
                                startTime + " to " + endTime + ".");

                dialog.close();

                // Refresh the sessions page
                showSessionsPage();

            } catch (java.time.format.DateTimeParseException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid time format! Use HH:MM (e.g., 09:00)");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to start session: " + ex.getMessage());
            }
        });

        content.getChildren().addAll(formGrid, startSessionBtn);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(450, 380);

        // Remove default button styling
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle("-fx-font-size: 12px;");

        dialog.showAndWait();
    }

    private void showSessionReport(SessionRow sessionRow) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Session Report - Edit Attendance");
        dialog.setHeaderText("Attendance Report for " + sessionRow.getSessionId());

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Session info
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(10);
        infoGrid.add(new Label("Course:"), 0, 0);
        infoGrid.add(new Label(sessionRow.getCourse()), 1, 0);
        infoGrid.add(new Label("Section:"), 0, 1);
        infoGrid.add(new Label(sessionRow.getSection()), 1, 1);
        infoGrid.add(new Label("Date:"), 0, 2);
        infoGrid.add(new Label(sessionRow.getDate()), 1, 2);
        infoGrid.add(new Label("Time:"), 0, 3);
        infoGrid.add(new Label(sessionRow.getStartTime() + " - " + sessionRow.getEndTime()), 1, 3);

        // Track modified records (for auto-setting method to Manual)
        java.util.Set<AttendanceRecord> modifiedRecords = new java.util.HashSet<>();

        // Editable Attendance table
        TableView<AttendanceRecord> attendanceTable = new TableView<>();
        attendanceTable.setEditable(true);
        attendanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        attendanceTable.setPrefHeight(400);

        // Student ID Column (read-only)
        TableColumn<AttendanceRecord, String> studentIdCol = new TableColumn<>("Student ID");
        studentIdCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUserId()));
        studentIdCol.setPrefWidth(100);

        // Student Name Column (read-only)
        TableColumn<AttendanceRecord, String> studentNameCol = new TableColumn<>("Student Name");
        studentNameCol.setCellValueFactory(cellData -> {
            Optional<User> studentOpt = databaseManager.findUserByUserId(cellData.getValue().getUserId());
            return new javafx.beans.property.SimpleStringProperty(
                    studentOpt.isPresent() ? studentOpt.get().getName() : "Unknown");
        });
        studentNameCol.setPrefWidth(150);

        // Attendance Status Column (editable with ComboBox)
        TableColumn<AttendanceRecord, String> statusCol = new TableColumn<>("Attendance");
        statusCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAttendance()));
        statusCol.setCellFactory(col -> new TableCell<AttendanceRecord, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>();
            private String originalValue = null;
            private boolean updating = false;

            {
                comboBox.getItems().addAll("Present", "Late", "Absent");
                comboBox.setOnAction(event -> {
                    if (updating) return; // Ignore programmatic updates

                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        AttendanceRecord record = getTableView().getItems().get(getIndex());
                        String newValue = comboBox.getValue();

                        System.out.println("ComboBox changed - User: " + record.getUserId() +
                                         ", Original: " + originalValue + ", New: " + newValue);

                        // Mark as modified
                        record.setAttendance(newValue);
                        modifiedRecords.add(record);
                        System.out.println("Added to modifiedRecords. Total modified: " + modifiedRecords.size());

                        commitEdit(newValue);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                updating = true; // Set flag before setValue to ignore the event
                if (empty) {
                    setGraphic(null);
                    originalValue = null;
                } else {
                    originalValue = item;
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                }
                updating = false; // Clear flag
            }
        });
        statusCol.setPrefWidth(120);

        // Check-in Time Column (editable)
        TableColumn<AttendanceRecord, String> checkinTimeCol = new TableColumn<>("Check In Time");
        checkinTimeCol.setCellValueFactory(cellData -> {
            java.time.LocalDateTime time = cellData.getValue().getCheckinTime();
            return new javafx.beans.property.SimpleStringProperty(
                    time != null ? time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) : "");
        });
        checkinTimeCol.setCellFactory(col -> new TableCell<AttendanceRecord, String>() {
            private final TextField textField = new TextField();
            {
                textField.setPromptText("HH:mm:ss");
                textField.setOnAction(event -> {
                    try {
                        AttendanceRecord record = getTableView().getItems().get(getIndex());
                        String value = textField.getText().trim();

                        System.out.println("Check-in time changed - User: " + record.getUserId() +
                                         ", New value: " + value);

                        if (!value.isEmpty()) {
                            java.time.LocalTime time = java.time.LocalTime.parse(value);
                            java.time.LocalDateTime dateTime = java.time.LocalDate.parse(sessionRow.getDate())
                                    .atTime(time);
                            record.setCheckinTime(dateTime);
                        } else {
                            record.setCheckinTime(null);
                        }
                        modifiedRecords.add(record); // Mark as modified
                        System.out.println("Added to modifiedRecords. Total modified: " + modifiedRecords.size());

                        commitEdit(value);
                    } catch (Exception e) {
                        System.err.println("Error parsing time: " + e.getMessage());
                        textField.setStyle("-fx-border-color: red;");
                    }
                });
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        textField.fireEvent(new javafx.event.ActionEvent());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item != null ? item : "");
                    textField.setStyle("");
                    setGraphic(textField);
                }
            }
        });
        checkinTimeCol.setPrefWidth(110);

        // Notes Column (editable)
        TableColumn<AttendanceRecord, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getNotes() != null ? cellData.getValue().getNotes() : ""));
        notesCol.setCellFactory(col -> new TableCell<AttendanceRecord, String>() {
            private final TextField textField = new TextField();
            {
                textField.setPromptText("Add notes...");
                textField.setOnAction(event -> {
                    AttendanceRecord record = getTableView().getItems().get(getIndex());
                    String newNotes = textField.getText();

                    System.out.println("Notes changed - User: " + record.getUserId() +
                                     ", New notes: " + newNotes);

                    record.setNotes(newNotes);
                    modifiedRecords.add(record); // Mark as modified
                    System.out.println("Added to modifiedRecords. Total modified: " + modifiedRecords.size());

                    commitEdit(newNotes);
                });
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        textField.fireEvent(new javafx.event.ActionEvent());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item != null ? item : "");
                    setGraphic(textField);
                }
            }
        });
        notesCol.setPrefWidth(250);

        attendanceTable.getColumns().addAll(studentIdCol, studentNameCol, statusCol, checkinTimeCol, notesCol);

        // Load attendance records
        List<AttendanceRecord> records = databaseManager.findAttendanceBySessionId(sessionRow.getSessionUUID());
        attendanceTable.getItems().addAll(records);

        // Save button
        Button saveButton = new Button("Save Report");
        saveButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(
                "-fx-background-color: #229954; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;"));
        saveButton.setOnAction(e -> {
            try {
                System.out.println("=== SAVING ATTENDANCE RECORDS ===");
                System.out.println("Total records to save: " + attendanceTable.getItems().size());
                System.out.println("Modified records count: " + modifiedRecords.size());

                // Save all attendance records
                int savedCount = 0;
                for (AttendanceRecord record : attendanceTable.getItems()) {
                    // If record was modified, set method to Manual
                    if (modifiedRecords.contains(record)) {
                        System.out.println("Marking record as Manual - User: " + record.getUserId() +
                                         ", Attendance: " + record.getAttendance());
                        record.setMethod("Manual");
                    }
                    record.setUpdatedAt(java.time.LocalDateTime.now());

                    AttendanceRecord saved = databaseManager.saveAttendanceRecord(record);
                    System.out.println("Saved record - User: " + saved.getUserId() +
                                     ", Attendance: " + saved.getAttendance() +
                                     ", Method: " + saved.getMethod() +
                                     ", ID: " + saved.getId());
                    savedCount++;
                }

                System.out.println("Successfully saved " + savedCount + " records");
                showAlert(Alert.AlertType.INFORMATION, "Success",
                         "Attendance records saved successfully! (" + savedCount + " records)");
                dialog.close();
                // Refresh sessions page to update statistics
                showSessionsPage();
            } catch (Exception ex) {
                System.err.println("ERROR saving attendance records:");
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save attendance records: " + ex.getMessage());
            }
        });

        HBox buttonBox = new HBox(saveButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(infoGrid, new javafx.scene.control.Separator(), attendanceTable, buttonBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(900, 650);

        dialog.showAndWait();
    }

    private void exportSessionToCSV(SessionRow sessionRow) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Session to CSV");
        fileChooser.setInitialFileName(sessionRow.getSessionId() + "_attendance.csv");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        java.io.File file = fileChooser.showSaveDialog(mainLayout.getScene().getWindow());
        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                // Write header
                writer.println("Student ID,Student Name,Status,Check-in Time,Method,Notes");

                // Get attendance records
                List<AttendanceRecord> records = databaseManager.findAttendanceBySessionId(sessionRow.getSessionUUID());

                // Write data
                for (AttendanceRecord record : records) {
                    Optional<User> studentOpt = databaseManager.findUserByUserId(record.getUserId());
                    String studentName = studentOpt.isPresent() ? studentOpt.get().getName() : "Unknown";
                    String checkinTime = record.getCheckinTime() != null ? record.getCheckinTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
                    String method = record.getMethod() != null ? record.getMethod() : "";
                    String notes = record.getNotes() != null ? record.getNotes().replace(",", ";") : "";

                    writer.printf("%s,%s,%s,%s,%s,%s%n",
                            record.getUserId(),
                            studentName,
                            record.getAttendance(),
                            checkinTime,
                            method,
                            notes);
                }

                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Session exported successfully to:\n" + file.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to export session: " + e.getMessage());
            }
        }
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

        public AttendanceRow(String studentName, String studentId, String course, String section, String year,
                String semester) {
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
            if (total == 0)
                return "0";
            return String.format("%.1f", (totalPresent * 100.0) / total);
        }

        public String getPercentLate() {
            int total = totalPresent + totalLate + totalAbsent;
            if (total == 0)
                return "0";
            return String.format("%.1f", (totalLate * 100.0) / total);
        }

        public String getPercentAbsent() {
            int total = totalPresent + totalLate + totalAbsent;
            if (total == 0)
                return "0";
            return String.format("%.1f", (totalAbsent * 100.0) / total);
        }
    }

    // Inner class to represent a row in the session table
    public static class SessionRow {
        private String sessionId;
        private String course;
        private String section;
        private String date;
        private String startTime;
        private String endTime;
        private int presentCount;
        private int lateCount;
        private int absentCount;
        private java.util.UUID sessionUUID;

        public SessionRow(String sessionId, String course, String section, String date, String startTime,
                String endTime,
                int presentCount, int lateCount, int absentCount, java.util.UUID sessionUUID) {
            this.sessionId = sessionId;
            this.course = course;
            this.section = section;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.presentCount = presentCount;
            this.lateCount = lateCount;
            this.absentCount = absentCount;
            this.sessionUUID = sessionUUID;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getCourse() {
            return course;
        }

        public String getSection() {
            return section;
        }

        public String getDate() {
            return date;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public int getPresentCount() {
            return presentCount;
        }

        public int getLateCount() {
            return lateCount;
        }

        public int getAbsentCount() {
            return absentCount;
        }

        public java.util.UUID getSessionUUID() {
            return sessionUUID;
        }

        public int getTotalStudents() {
            return presentCount + lateCount + absentCount;
        }
    }
}
