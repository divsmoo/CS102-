package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.manager.DatabaseManager;
import com.cs102.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AdminView {

    private Stage stage;
    private User admin;
    private AuthenticationManager authManager;
    private DatabaseManager dbManager;
    private BorderPane mainLayout;
    private String currentPage = "Dashboard";

    public AdminView(Stage stage, User admin, AuthenticationManager authManager) {
        this.stage = stage;
        this.admin = admin;
        this.authManager = authManager;
        this.dbManager = authManager.getDatabaseManager();
    }

    public Scene createScene() {
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // Navigation bar
        mainLayout.setTop(createNavbar());

        // Show dashboard by default
        showDashboard();

        return new Scene(mainLayout, 1400, 900);
    }

    private HBox createNavbar() {
        HBox navbar = new HBox(20);
        navbar.setPadding(new Insets(15, 30, 15, 30));
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 2 0;");

        // App title
        Label appTitle = new Label("Admin Dashboard");
        appTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));
        appTitle.setStyle("-fx-text-fill: white;");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Navigation buttons
        Button dashboardBtn = createNavButton("Dashboard");
        Button analyticsBtn = createNavButton("Analytics");
        Button reportsBtn = createNavButton("Reports");
        Button usersBtn = createNavButton("Users");

        // Logout button
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand;"));
        logoutBtn.setOnAction(e -> {
            AuthView authView = new AuthView(stage, authManager);
            stage.setScene(authView.createScene());
        });

        navbar.getChildren().addAll(appTitle, spacer, dashboardBtn, analyticsBtn, reportsBtn, usersBtn, logoutBtn);
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
        mainLayout.setTop(createNavbar());

        switch (page) {
            case "Dashboard":
                showDashboard();
                break;
            case "Analytics":
                showAnalytics();
                break;
            case "Reports":
                showReports();
                break;
            case "Users":
                showUsers();
                break;
        }
    }

    // ========== DASHBOARD PAGE ==========
    private void showDashboard() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #f5f5f5;");

        // Title
        Label titleLabel = new Label("System Overview");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

        // Stats cards
        HBox statsCards = createStatsCards();

        // Recent activity
        VBox recentActivity = createRecentActivitySection();

        // Quick actions
        HBox quickActions = createQuickActions();

        content.getChildren().addAll(titleLabel, statsCards, recentActivity, quickActions);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5; -fx-background-color: #f5f5f5;");

        mainLayout.setCenter(scrollPane);
    }

    private HBox createStatsCards() {
        HBox cards = new HBox(20);
        cards.setAlignment(Pos.CENTER);

        // Total Students
        int totalStudents = dbManager.findUsersByRole(UserRole.STUDENT).size();
        VBox studentCard = createStatCard("Total Students", String.valueOf(totalStudents), "#3498db");

        // Total Professors
        int totalProfessors = dbManager.findUsersByRole(UserRole.PROFESSOR).size();
        VBox professorCard = createStatCard("Total Professors", String.valueOf(totalProfessors), "#9b59b6");

        // Total Courses
        int totalCourses = dbManager.findAllCourses().size();
        VBox courseCard = createStatCard("Total Courses", String.valueOf(totalCourses), "#e67e22");

        // Total Sessions Today
        List<Session> allSessions = getAllSessions();
        long sessionsToday = allSessions.stream()
                .filter(s -> s.getDate().equals(LocalDate.now()))
                .count();
        VBox sessionCard = createStatCard("Sessions Today", String.valueOf(sessionsToday), "#27ae60");

        cards.getChildren().addAll(studentCard, professorCard, courseCard, sessionCard);
        return cards;
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(250);
        card.setPrefHeight(120);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Tahoma", FontWeight.NORMAL, 14));
        titleLabel.setStyle("-fx-text-fill: #7f8c8d;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 36));
        valueLabel.setStyle("-fx-text-fill: " + color + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createRecentActivitySection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label sectionTitle = new Label("Recent Sessions");
        sectionTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));

        // Get recent sessions (last 10)
        List<Session> recentSessions = getAllSessions().stream()
                .sorted(Comparator.comparing(Session::getDate).reversed()
                        .thenComparing(Session::getStartTime, Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toList());

        VBox sessionsList = new VBox(10);
        for (Session session : recentSessions) {
            HBox sessionItem = createSessionItem(session);
            sessionsList.getChildren().add(sessionItem);
        }

        ScrollPane scrollPane = new ScrollPane(sessionsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        section.getChildren().addAll(sectionTitle, scrollPane);
        return section;
    }

    private HBox createSessionItem(Session session) {
    HBox item = new HBox(15);
    item.setPadding(new Insets(10));
    item.setAlignment(Pos.CENTER_LEFT);
    item.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-radius: 5;");

    Label courseLabel = new Label(session.getCourse() + " - " + session.getSection());
    courseLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 13));
    courseLabel.setStyle("-fx-text-fill: #2c3e50;");  // ADD THIS LINE
    courseLabel.setPrefWidth(150);

    Label dateLabel = new Label(session.getDate().toString());
    dateLabel.setFont(Font.font("Tahoma", 12));
    dateLabel.setStyle("-fx-text-fill: #34495e;");  // ADD THIS LINE
    dateLabel.setPrefWidth(100);

    Label timeLabel = new Label(session.getStartTime() + " - " + session.getEndTime());
    timeLabel.setFont(Font.font("Tahoma", 12));
    timeLabel.setStyle("-fx-text-fill: #34495e;");  // ADD THIS LINE
    timeLabel.setPrefWidth(120);

    // Get attendance stats
    List<AttendanceRecord> records = dbManager.findAttendanceBySessionId(session.getId());
    long present = records.stream().filter(r -> "Present".equals(r.getAttendance())).count();
    long late = records.stream().filter(r -> "Late".equals(r.getAttendance())).count();
    long absent = records.stream().filter(r -> "Absent".equals(r.getAttendance())).count();

    Label statsLabel = new Label(String.format("P: %d  L: %d  A: %d", present, late, absent));
    statsLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 13));  // INCREASE FONT SIZE
    statsLabel.setStyle("-fx-text-fill: #e74c3c;");  // CHANGE TO RED FOR BETTER VISIBILITY

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    item.getChildren().addAll(courseLabel, dateLabel, timeLabel, spacer, statsLabel);
    return item;
}

    private HBox createQuickActions() {
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(20, 0, 0, 0));

        Button exportBtn = createActionButton("Export All Data", "#27ae60");
        exportBtn.setOnAction(e -> exportAllData());

        Button viewReportsBtn = createActionButton("View Reports", "#3498db");
        viewReportsBtn.setOnAction(e -> navigateTo("Reports"));

        Button manageUsersBtn = createActionButton("Manage Users", "#9b59b6");
        manageUsersBtn.setOnAction(e -> navigateTo("Users"));

        actions.getChildren().addAll(exportBtn, viewReportsBtn, manageUsersBtn);
        return actions;
    }

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setPrefHeight(45);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    // ========== ANALYTICS PAGE ==========
    private void showAnalytics() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        Label titleLabel = new Label("Analytics & Insights");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

        // Charts row 1
        HBox chartsRow1 = new HBox(20);
        chartsRow1.setAlignment(Pos.CENTER);

        BarChart<String, Number> attendanceChart = createAttendanceBarChart();
        PieChart studentDistChart = createStudentDistributionChart();

        chartsRow1.getChildren().addAll(attendanceChart, studentDistChart);

        // Charts row 2
        HBox chartsRow2 = new HBox(20);
        chartsRow2.setAlignment(Pos.CENTER);

        LineChart<String, Number> trendChart = createAttendanceTrendChart();
        chartsRow2.getChildren().add(trendChart);

        content.getChildren().addAll(titleLabel, chartsRow1, chartsRow2);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5; -fx-background-color: #f5f5f5;");

        mainLayout.setCenter(scrollPane);
    }

    private BarChart<String, Number> createAttendanceBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Course");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Students");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Attendance by Course");
        barChart.setPrefSize(600, 400);

        XYChart.Series<String, Number> presentSeries = new XYChart.Series<>();
        presentSeries.setName("Present");
        XYChart.Series<String, Number> lateSeries = new XYChart.Series<>();
        lateSeries.setName("Late");
        XYChart.Series<String, Number> absentSeries = new XYChart.Series<>();
        absentSeries.setName("Absent");

        // Get data for each course
        List<Course> courses = dbManager.findAllCourses();
        Map<String, int[]> courseStats = new HashMap<>(); // [present, late, absent]

        for (Course course : courses) {
            String key = course.getCourse() + "-" + course.getSection();
            List<Session> sessions = dbManager.findSessionsByCourseAndSection(course.getCourse(), course.getSection());
            
            int totalPresent = 0, totalLate = 0, totalAbsent = 0;
            
            for (Session session : sessions) {
                List<AttendanceRecord> records = dbManager.findAttendanceBySessionId(session.getId());
                totalPresent += records.stream().filter(r -> "Present".equals(r.getAttendance())).count();
                totalLate += records.stream().filter(r -> "Late".equals(r.getAttendance())).count();
                totalAbsent += records.stream().filter(r -> "Absent".equals(r.getAttendance())).count();
            }
            
            courseStats.put(key, new int[]{totalPresent, totalLate, totalAbsent});
        }

        // Add data to chart (top 10 courses)
        courseStats.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                    e2.getValue()[0] + e2.getValue()[1] + e2.getValue()[2],
                    e1.getValue()[0] + e1.getValue()[1] + e1.getValue()[2]))
                .limit(10)
                .forEach(entry -> {
                    String courseName = entry.getKey();
                    int[] stats = entry.getValue();
                    presentSeries.getData().add(new XYChart.Data<>(courseName, stats[0]));
                    lateSeries.getData().add(new XYChart.Data<>(courseName, stats[1]));
                    absentSeries.getData().add(new XYChart.Data<>(courseName, stats[2]));
                });

        barChart.getData().addAll(presentSeries, lateSeries, absentSeries);
        return barChart;
    }

    private PieChart createStudentDistributionChart() {
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Overall Attendance Distribution");
        pieChart.setPrefSize(500, 400);

        // Calculate overall stats
        List<AttendanceRecord> allRecords = dbManager.findAllAttendanceRecords();
        long present = allRecords.stream().filter(r -> "Present".equals(r.getAttendance())).count();
        long late = allRecords.stream().filter(r -> "Late".equals(r.getAttendance())).count();
        long absent = allRecords.stream().filter(r -> "Absent".equals(r.getAttendance())).count();

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Present (" + present + ")", present),
            new PieChart.Data("Late (" + late + ")", late),
            new PieChart.Data("Absent (" + absent + ")", absent)
        );

        pieChart.setData(pieChartData);
        return pieChart;
    }

    private LineChart<String, Number> createAttendanceTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Attendance Rate (%)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Attendance Trend (Last 30 Days)");
        lineChart.setPrefSize(1200, 400);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendance Rate");

        // Get sessions from last 30 days
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Session> recentSessions = getAllSessions().stream()
                .filter(s -> s.getDate().isAfter(thirtyDaysAgo))
                .sorted(Comparator.comparing(Session::getDate))
                .collect(Collectors.toList());

        // Group by date and calculate average attendance rate
        Map<LocalDate, List<Session>> sessionsByDate = recentSessions.stream()
                .collect(Collectors.groupingBy(Session::getDate));

        sessionsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LocalDate date = entry.getKey();
                    List<Session> sessions = entry.getValue();
                    
                    double totalRate = 0;
                    int count = 0;
                    
                    for (Session session : sessions) {
                        List<AttendanceRecord> records = dbManager.findAttendanceBySessionId(session.getId());
                        if (!records.isEmpty()) {
                            long present = records.stream().filter(r -> "Present".equals(r.getAttendance())).count();
                            double rate = (present * 100.0) / records.size();
                            totalRate += rate;
                            count++;
                        }
                    }
                    
                    if (count > 0) {
                        double avgRate = totalRate / count;
                        series.getData().add(new XYChart.Data<>(date.toString(), avgRate));
                    }
                });

        lineChart.getData().add(series);
        return lineChart;
    }

    // ========== REPORTS PAGE ==========
    private void showReports() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        Label titleLabel = new Label("Reports & Exports");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

        // Report options
        VBox reportOptions = new VBox(15);
        reportOptions.setPadding(new Insets(20));
        reportOptions.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; " +
                              "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label optionsTitle = new Label("Generate Reports");
        optionsTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));

        Button exportAllBtn = new Button("Export All Attendance Data");
        styleReportButton(exportAllBtn);
        exportAllBtn.setOnAction(e -> exportAllData());

        Button exportByDateBtn = new Button("Export by Date Range");
        styleReportButton(exportByDateBtn);
        exportByDateBtn.setOnAction(e -> exportByDateRange());

        Button exportByCourseBtn = new Button("Export by Course");
        styleReportButton(exportByCourseBtn);
        exportByCourseBtn.setOnAction(e -> exportByCourse());

        Button exportStudentSummaryBtn = new Button("Export Student Summary");
        styleReportButton(exportStudentSummaryBtn);
        exportStudentSummaryBtn.setOnAction(e -> exportStudentSummary());

        reportOptions.getChildren().addAll(optionsTitle, exportAllBtn, exportByDateBtn, exportByCourseBtn, exportStudentSummaryBtn);

        content.getChildren().addAll(titleLabel, reportOptions);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5; -fx-background-color: #f5f5f5;");

        mainLayout.setCenter(scrollPane);
    }

    private void styleReportButton(Button btn) {
        btn.setPrefWidth(300);
        btn.setPrefHeight(45);
        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;"));
    }

    // ========== USERS PAGE ==========
    private void showUsers() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        Label titleLabel = new Label("User Management");
        titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

        // Filter tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab studentsTab = new Tab("Students");
        studentsTab.setContent(createUsersTable(UserRole.STUDENT));

        Tab professorsTab = new Tab("Professors");
        professorsTab.setContent(createUsersTable(UserRole.PROFESSOR));

        tabPane.getTabs().addAll(studentsTab, professorsTab);

        content.getChildren().addAll(titleLabel, tabPane);

        mainLayout.setCenter(content);
    }

    private VBox createUsersTable(UserRole role) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));

        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // User ID Column
        TableColumn<User, String> idCol = new TableColumn<>("User ID");
        idCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUserId()));
        idCol.setPrefWidth(150);

        // Name Column
        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        nameCol.setPrefWidth(200);

        // Email Column
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        emailCol.setPrefWidth(250);

        // Stats Column (only for students)
        if (role == UserRole.STUDENT) {
            TableColumn<User, String> statsCol = new TableColumn<>("Attendance Stats");
            statsCol.setCellValueFactory(cellData -> {
                User user = cellData.getValue();
                List<AttendanceRecord> records = dbManager.findAttendanceByUserId(user.getUserId());
                long present = records.stream().filter(r -> "Present".equals(r.getAttendance())).count();
                long late = records.stream().filter(r -> "Late".equals(r.getAttendance())).count();
                long absent = records.stream().filter(r -> "Absent".equals(r.getAttendance())).count();
                return new javafx.beans.property.SimpleStringProperty(
                    String.format("P: %d  L: %d  A: %d", present, late, absent));
            });
            statsCol.setPrefWidth(200);
            table.getColumns().addAll(idCol, nameCol, emailCol, statsCol);
        } else {
            // For professors, show courses taught
            TableColumn<User, String> coursesCol = new TableColumn<>("Courses Taught");
            coursesCol.setCellValueFactory(cellData -> {
                User user = cellData.getValue();
                List<Course> courses = dbManager.findCoursesByProfessorId(user.getUserId());
                String courseList = courses.stream()
                    .map(c -> c.getCourse() + "-" + c.getSection())
                    .collect(Collectors.joining(", "));
                return new javafx.beans.property.SimpleStringProperty(courseList);
            });
            coursesCol.setPrefWidth(300);
            table.getColumns().addAll(idCol, nameCol, emailCol, coursesCol);
        }

        // Load data
        List<User> users = dbManager.findUsersByRole(role);
        table.getItems().addAll(users);

        container.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);

        return container;
    }

    // ========== EXPORT FUNCTIONS ==========
    private void exportAllData() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export All Attendance Data");
        fileChooser.setInitialFileName("all_attendance_data.csv");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        java.io.File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                writer.println("Session ID,Course,Section,Date,Start Time,End Time,Student ID,Student Name,Attendance,Check-in Time,Method,Notes");

                List<Session> allSessions = getAllSessions();
                for (Session session : allSessions) {
                    List<AttendanceRecord> records = dbManager.findAttendanceBySessionId(session.getId());
                    for (AttendanceRecord record : records) {
                        Optional<User> studentOpt = dbManager.findUserByUserId(record.getUserId());
                        String studentName = studentOpt.isPresent() ? studentOpt.get().getName() : "Unknown";
                        String checkinTime = record.getCheckinTime() != null ? 
                            record.getCheckinTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";

                        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            session.getSessionId(),
                            session.getCourse(),
                            session.getSection(),
                            session.getDate(),
                            session.getStartTime(),
                            session.getEndTime(),
                            record.getUserId(),
                            studentName,
                            record.getAttendance(),
                            checkinTime,
                            record.getMethod(),
                            record.getNotes() != null ? record.getNotes().replace(",", ";") : "");
                    }
                }

                showAlert(Alert.AlertType.INFORMATION, "Success", "Data exported successfully to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to export data: " + e.getMessage());
            }
        }
    }

    private void exportByDateRange() {
        // Create dialog for date range selection
        Dialog<LocalDate[]> dialog = new Dialog<>();
        dialog.setTitle("Export by Date Range");
        dialog.setHeaderText("Select date range");

        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setValue(LocalDate.now());

        grid.add(new Label("Start Date:"), 0, 0);
        grid.add(startDatePicker, 1, 0);
        grid.add(new Label("End Date:"), 0, 1);
        grid.add(endDatePicker, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType) {
                return new LocalDate[]{startDatePicker.getValue(), endDatePicker.getValue()};
            }
            return null;
        });

        Optional<LocalDate[]> result = dialog.showAndWait();
        result.ifPresent(dates -> {
            LocalDate startDate = dates[0];
            LocalDate endDate = dates[1];

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Attendance by Date Range");
            fileChooser.setInitialFileName("attendance_" + startDate + "_to_" + endDate + ".csv");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

            java.io.File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                    writer.println("Session ID,Course,Section,Date,Student ID,Student Name,Attendance,Check-in Time");

                    List<Session> sessions = getAllSessions().stream()
                        .filter(s -> !s.getDate().isBefore(startDate) && !s.getDate().isAfter(endDate))
                        .collect(Collectors.toList());

                    for (Session session : sessions) {
                        List<AttendanceRecord> records = dbManager.findAttendanceBySessionId(session.getId());
                        for (AttendanceRecord record : records) {
                            Optional<User> studentOpt = dbManager.findUserByUserId(record.getUserId());
                            String studentName = studentOpt.isPresent() ? studentOpt.get().getName() : "Unknown";
                            String checkinTime = record.getCheckinTime() != null ? 
                                record.getCheckinTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";

                            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                                session.getSessionId(),
                                session.getCourse(),
                                session.getSection(),
                                session.getDate(),
                                record.getUserId(),
                                studentName,
                                record.getAttendance(),
                                checkinTime);
                        }
                    }

                    showAlert(Alert.AlertType.INFORMATION, "Success", "Data exported successfully!");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to export data: " + e.getMessage());
                }
            }
        });
    }

    private void exportByCourse() {
        // Create dialog for course selection
        List<Course> courses = dbManager.findAllCourses();
        List<String> courseOptions = courses.stream()
            .map(c -> c.getCourse() + " - " + c.getSection())
            .collect(Collectors.toList());

        ChoiceDialog<String> dialog = new ChoiceDialog<>(courseOptions.get(0), courseOptions);
        dialog.setTitle("Export by Course");
        dialog.setHeaderText("Select a course");
        dialog.setContentText("Course:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(selected -> {
            String[] parts = selected.split(" - ");
            String course = parts[0];
            String section = parts[1];

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Course Attendance");
            fileChooser.setInitialFileName(course + "_" + section + "_attendance.csv");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

            java.io.File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                    writer.println("Session ID,Date,Student ID,Student Name,Attendance,Check-in Time");

                    List<Session> sessions = dbManager.findSessionsByCourseAndSection(course, section);
                    for (Session session : sessions) {
                        List<AttendanceRecord> records = dbManager.findAttendanceBySessionId(session.getId());
                        for (AttendanceRecord record : records) {
                            Optional<User> studentOpt = dbManager.findUserByUserId(record.getUserId());
                            String studentName = studentOpt.isPresent() ? studentOpt.get().getName() : "Unknown";
                            String checkinTime = record.getCheckinTime() != null ? 
                                record.getCheckinTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";

                            writer.printf("%s,%s,%s,%s,%s,%s%n",
                                session.getSessionId(),
                                session.getDate(),
                                record.getUserId(),
                                studentName,
                                record.getAttendance(),
                                checkinTime);
                        }
                    }

                    showAlert(Alert.AlertType.INFORMATION, "Success", "Data exported successfully!");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to export data: " + e.getMessage());
                }
            }
        });
    }

    private void exportStudentSummary() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Student Summary");
        fileChooser.setInitialFileName("student_summary.csv");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        java.io.File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                writer.println("Student ID,Student Name,Total Sessions,Present,Late,Absent,Attendance Rate");

                List<User> students = dbManager.findUsersByRole(UserRole.STUDENT);
                for (User student : students) {
                    List<AttendanceRecord> records = dbManager.findAttendanceByUserId(student.getUserId());
                    long total = records.size();
                    long present = records.stream().filter(r -> "Present".equals(r.getAttendance())).count();
                    long late = records.stream().filter(r -> "Late".equals(r.getAttendance())).count();
                    long absent = records.stream().filter(r -> "Absent".equals(r.getAttendance())).count();
                    double rate = total > 0 ? (present * 100.0 / total) : 0;

                    writer.printf("%s,%s,%d,%d,%d,%d,%.2f%%%n",
                        student.getUserId(),
                        student.getName(),
                        total,
                        present,
                        late,
                        absent,
                        rate);
                }

                showAlert(Alert.AlertType.INFORMATION, "Success", "Student summary exported successfully!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to export data: " + e.getMessage());
            }
        }
    }

    // ========== HELPER METHODS ==========
    private List<Session> getAllSessions() {
        List<Course> courses = dbManager.findAllCourses();
        List<Session> allSessions = new ArrayList<>();
        for (Course course : courses) {
            allSessions.addAll(dbManager.findSessionsByCourseAndSection(course.getCourse(), course.getSection()));
        }
        return allSessions;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}