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
import com.cs102.model.Course;
import com.cs102.model.Session;
import com.cs102.model.User;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

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
                        Map<UUID, AttendanceRecord> userAttendance = attendanceByUserAndSession.get(userId);
                        if (userAttendance != null && userAttendance.containsKey(session.getId())) {
                            AttendanceRecord record = userAttendance.get(session.getId());
                            row.addSessionAttendance(session.getSessionId(), record.getAttendance());
                        } else {
                            row.addSessionAttendance(session.getSessionId(), "Absent");
                        }
                    }

                    rows.add(row);
                }

                if (Thread.currentThread().isInterrupted()) return;

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
            default:
                return "A";
        }
    }

    private void showClassesPage() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        // Title
        Label titleLabel = new Label("CLASSES OVERVIEW");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        // Course and Section Filter Dropdowns with Buttons
        HBox filterRow = new HBox(20);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        Label courseLabel = new Label("Course:");
        courseLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        ComboBox<String> courseFilterDropdown = new ComboBox<>();
        courseFilterDropdown.setPrefWidth(200);
        courseFilterDropdown.setPromptText("Courses under prof");

        Label sectionLabel = new Label("Section:");
        sectionLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        ComboBox<String> sectionFilterDropdown = new ComboBox<>();
        sectionFilterDropdown.setPrefWidth(150);
        sectionFilterDropdown.setPromptText("Sections under prof");

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

        filterRow.getChildren().addAll(courseLabel, courseFilterDropdown, sectionLabel, sectionFilterDropdown,
                                       spacer, addClassBtn, deleteClassBtn);

        // Classes Table
        TableView<ClassRow> classesTable = createClassesTable();

        // Load professor's courses
        loadClassesData(classesTable, courseFilterDropdown, sectionFilterDropdown);

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
        sectionCol.setPrefWidth(150);

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

        table.getColumns().addAll(courseCol, sectionCol, studentsCol, sessionsCol, editCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        return table;
    }

    private void loadClassesData(TableView<ClassRow> table, ComboBox<String> courseFilter, ComboBox<String> sectionFilter) {
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

            ClassRow row = new ClassRow(course.getCourse(), course.getSection(), numStudents, numSessions);
            rows.add(row);
        }

        table.setItems(rows);

        // Populate course filter dropdown
        ObservableList<String> courseOptions = FXCollections.observableArrayList();
        courseOptions.add("Courses under prof");
        Set<String> uniqueCourses = professorCourses.stream()
            .map(Course::getCourse)
            .collect(Collectors.toSet());
        courseOptions.addAll(uniqueCourses);
        courseFilter.setItems(courseOptions);
        courseFilter.setValue("Courses under prof");

        // Populate section filter dropdown
        ObservableList<String> sectionOptions = FXCollections.observableArrayList();
        sectionOptions.add("Sections under prof");
        Set<String> uniqueSections = professorCourses.stream()
            .map(Course::getSection)
            .collect(Collectors.toSet());
        sectionOptions.addAll(uniqueSections);
        sectionFilter.setItems(sectionOptions);
        sectionFilter.setValue("Sections under prof");

        // Add filter listeners
        courseFilter.setOnAction(e -> filterClassesTable(table, courseFilter, sectionFilter, professorCourses));
        sectionFilter.setOnAction(e -> filterClassesTable(table, courseFilter, sectionFilter, professorCourses));
    }

    private void filterClassesTable(TableView<ClassRow> table, ComboBox<String> courseFilter,
                                    ComboBox<String> sectionFilter, List<Course> allCourses) {
        String selectedCourse = courseFilter.getValue();
        String selectedSection = sectionFilter.getValue();

        ObservableList<ClassRow> filteredRows = FXCollections.observableArrayList();

        for (Course course : allCourses) {
            boolean courseMatch = selectedCourse.equals("Courses under prof") || course.getCourse().equals(selectedCourse);
            boolean sectionMatch = selectedSection.equals("Sections under prof") || course.getSection().equals(selectedSection);

            if (courseMatch && sectionMatch) {
                // Count students enrolled
                List<com.cs102.model.Class> enrollments = databaseManager.findEnrollmentsByCourseAndSection(
                    course.getCourse(), course.getSection());
                int numStudents = enrollments.size();

                // Count sessions
                List<Session> sessions = databaseManager.findSessionsByCourseAndSection(
                    course.getCourse(), course.getSection());
                int numSessions = sessions.size();

                ClassRow row = new ClassRow(course.getCourse(), course.getSection(), numStudents, numSessions);
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
        TextField semesterField = new TextField();
        semesterField.setPromptText("e.g., Fall 2024");

        grid.add(new Label("Course:"), 0, 0);
        grid.add(courseField, 1, 0);
        grid.add(new Label("Section:"), 0, 1);
        grid.add(sectionField, 1, 1);
        grid.add(new Label("Semester:"), 0, 2);
        grid.add(semesterField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Convert result when Add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new Course(courseField.getText().trim(), sectionField.getText().trim(),
                                professor.getUserId(), semesterField.getText().trim());
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

    // Inner class to represent a row in the classes table
    public static class ClassRow {
        private String course;
        private String section;
        private int numStudents;
        private int numSessions;

        public ClassRow(String course, String section, int numStudents, int numSessions) {
            this.course = course;
            this.section = section;
            this.numStudents = numStudents;
            this.numSessions = numSessions;
        }

        public String getCourse() {
            return course;
        }

        public String getSection() {
            return section;
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
