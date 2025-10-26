package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.manager.DatabaseManager;
import com.cs102.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

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

        // First Row: Course and Section Dropdowns
        HBox dropdownRow = new HBox(20);
        dropdownRow.setAlignment(Pos.CENTER_LEFT);

        Label courseLabel = new Label("Course:");
        courseLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        courseDropdown = new ComboBox<>();
        courseDropdown.setPrefWidth(200);
        courseDropdown.setPromptText("Select Course");

        Label sectionLabel = new Label("Section:");
        sectionLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        sectionDropdown = new ComboBox<>();
        sectionDropdown.setPrefWidth(150);
        sectionDropdown.setPromptText("Select Section");

        dropdownRow.getChildren().addAll(courseLabel, courseDropdown, sectionLabel, sectionDropdown);

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

        ObservableList<String> courses = FXCollections.observableArrayList();
        courses.add("All");

        // Get unique course names
        Set<String> uniqueCourses = professorCourses.stream()
            .map(Course::getCourse)
            .collect(Collectors.toSet());

        courses.addAll(uniqueCourses);
        courseDropdown.setItems(courses);
        courseDropdown.setValue("All");

        // When course changes, update sections dropdown
        courseDropdown.setOnAction(e -> {
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
        sectionDropdown.setValue("All");

        sectionDropdown.setOnAction(e -> loadAttendanceData());

        // Trigger initial data load only on first call
        if (isInitialLoad) {
            loadAttendanceData();
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
        String selectedCourse = courseDropdown.getValue();
        String selectedSection = sectionDropdown.getValue();

        if (selectedCourse == null || selectedSection == null) {
            return;
        }

        // Cancel previous loading thread if still running
        if (currentLoadingThread != null && currentLoadingThread.isAlive()) {
            currentLoadingThread.interrupt();
            System.out.println("Cancelled previous loading thread");
        }

        System.out.println("Loading attendance for Course: " + selectedCourse + ", Section: " + selectedSection);

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

                // OPTIMIZATION: Fetch all sessions for the selected course/section
                List<Session> sessions = getSessionsForFilter(selectedCourse, selectedSection);
                System.out.println("Fetched " + sessions.size() + " sessions");

                if (Thread.currentThread().isInterrupted()) return;

                // OPTIMIZATION: Fetch all enrollments for the selected course/section
                List<com.cs102.model.Class> enrollments = getEnrollmentsForFilter(selectedCourse, selectedSection);
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

                    AttendanceRow row = new AttendanceRow(user.getName(), userId, course, section);

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

                // Sort rows by course then section
                rows.sort((r1, r2) -> {
                    int courseCompare = r1.getCourse().compareTo(r2.getCourse());
                    if (courseCompare != 0) {
                        return courseCompare;
                    }
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

    private List<Session> getSessionsForFilter(String course, String section) {
        if ("All".equals(course) && "All".equals(section)) {
            // Get all sessions for all courses taught by this professor
            List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());
            return professorCourses.stream()
                .flatMap(c -> databaseManager.findSessionsByCourseAndSection(c.getCourse(), c.getSection()).stream())
                .sorted(Comparator.comparing(Session::getDate).thenComparing(Session::getStartTime))
                .collect(Collectors.toList());
        } else if ("All".equals(section)) {
            // Get all sessions for the selected course (all sections)
            List<Course> courseSections = databaseManager.findCoursesByProfessorId(professor.getUserId()).stream()
                .filter(c -> c.getCourse().equals(course))
                .collect(Collectors.toList());
            return courseSections.stream()
                .flatMap(c -> databaseManager.findSessionsByCourseAndSection(c.getCourse(), c.getSection()).stream())
                .sorted(Comparator.comparing(Session::getDate).thenComparing(Session::getStartTime))
                .collect(Collectors.toList());
        } else {
            // Get sessions for specific course and section
            return databaseManager.findSessionsByCourseAndSection(course, section).stream()
                .sorted(Comparator.comparing(Session::getDate).thenComparing(Session::getStartTime))
                .collect(Collectors.toList());
        }
    }

    private List<com.cs102.model.Class> getEnrollmentsForFilter(String course, String section) {
        if ("All".equals(course) && "All".equals(section)) {
            // Get all enrollments for all courses taught by this professor
            List<Course> professorCourses = databaseManager.findCoursesByProfessorId(professor.getUserId());
            return professorCourses.stream()
                .flatMap(c -> databaseManager.findEnrollmentsByCourseAndSection(c.getCourse(), c.getSection()).stream())
                .collect(Collectors.toList());
        } else if ("All".equals(section)) {
            // Get all enrollments for the selected course (all sections)
            List<Course> courseSections = databaseManager.findCoursesByProfessorId(professor.getUserId()).stream()
                .filter(c -> c.getCourse().equals(course))
                .collect(Collectors.toList());
            return courseSections.stream()
                .flatMap(c -> databaseManager.findEnrollmentsByCourseAndSection(c.getCourse(), c.getSection()).stream())
                .collect(Collectors.toList());
        } else {
            // Get enrollments for specific course and section
            return databaseManager.findEnrollmentsByCourseAndSection(course, section);
        }
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

        attendanceTable.getColumns().addAll(nameCol, idCol, courseCol, sectionCol);

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
        content.setAlignment(Pos.CENTER);

        Label label = new Label("Classes Page - Coming Soon");
        label.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        content.getChildren().add(label);
        mainLayout.setCenter(content);
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
        content.setAlignment(Pos.CENTER);

        Label label = new Label("Live Recognition Page - Coming Soon");
        label.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        content.getChildren().add(label);
        mainLayout.setCenter(content);
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

    // Inner class to represent a row in the attendance table
    public static class AttendanceRow {
        private String studentName;
        private String studentId;
        private String course;
        private String section;
        private Map<String, String> sessionAttendance; // sessionId -> attendance status
        private int totalPresent;
        private int totalLate;
        private int totalAbsent;

        public AttendanceRow(String studentName, String studentId, String course, String section) {
            this.studentName = studentName;
            this.studentId = studentId;
            this.course = course;
            this.section = section;
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
