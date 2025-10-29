package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.manager.DatabaseManager;
import com.cs102.model.AttendanceRecord;
import com.cs102.model.Session;
import com.cs102.model.StudentAttendanceData;
import com.cs102.model.User;
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

import java.util.*;

public class StudentAttendanceView {

    private Stage stage;
    private User student;
    private AuthenticationManager authManager;
    private DatabaseManager dbManager;

    private ComboBox<String> courseDropdown;
    private TableView<StudentAttendanceData> attendanceTable;
    private ObservableList<StudentAttendanceData> attendanceData;

    public StudentAttendanceView(Stage stage, User student, AuthenticationManager authManager, DatabaseManager dbManager) {
        this.stage = stage;
        this.student = student;
        this.authManager = authManager;
        this.dbManager = dbManager;
        this.attendanceData = FXCollections.observableArrayList();
    }

    public Scene createScene() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // Top Section - Header with course selection
        VBox topSection = createTopSection();
        mainLayout.setTop(topSection);

        // Center Section - Attendance Table
        VBox centerSection = createCenterSection();
        mainLayout.setCenter(centerSection);

        // Bottom Section - Back button
        HBox bottomSection = createBottomSection();
        mainLayout.setBottom(bottomSection);

        Scene scene = new Scene(mainLayout, 1400, 800);
        return scene;
    }

    private VBox createTopSection() {
        VBox topSection = new VBox(15);
        topSection.setPadding(new Insets(0, 0, 20, 0));

        // Title
        Text title = new Text("My Attendance");
        title.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

        // Student info
        Label studentInfo = new Label(student.getName() + " (" + student.getUserId() + ")");
        studentInfo.setFont(Font.font("Tahoma", FontWeight.NORMAL, 16));
        studentInfo.setStyle("-fx-text-fill: #666;");

        // Course selection row
        HBox courseSelectionRow = new HBox(15);
        courseSelectionRow.setAlignment(Pos.CENTER_LEFT);

        Label courseLabel = new Label("Course:");
        courseLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        courseDropdown = new ComboBox<>();
        courseDropdown.setPromptText("Select a course");
        courseDropdown.setPrefWidth(200);
        courseDropdown.setOnAction(e -> loadAttendanceData());

        // Legend
        HBox legend = createLegend();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        courseSelectionRow.getChildren().addAll(courseLabel, courseDropdown, spacer, legend);

        topSection.getChildren().addAll(title, studentInfo, courseSelectionRow);
        return topSection;
    }

    private HBox createLegend() {
        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.setPadding(new Insets(5, 10, 5, 10));
        legend.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label legendTitle = new Label("Legend:");
        legendTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 12));

        Label presentLabel = createLegendItem("P = Present", "#4CAF50");
        Label lateLabel = createLegendItem("L = Late", "#FFC107");
        Label absentLabel = createLegendItem("A = Absent", "#F44336");

        legend.getChildren().addAll(legendTitle, presentLabel, lateLabel, absentLabel);
        return legend;
    }

    private Label createLegendItem(String text, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Tahoma", 11));
        label.setPadding(new Insets(3, 8, 3, 8));
        label.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3;");
        return label;
    }

    private VBox createCenterSection() {
        VBox centerSection = new VBox(10);
        centerSection.setPadding(new Insets(10, 0, 10, 0));

        // Create table
        attendanceTable = new TableView<>();
        attendanceTable.setItems(attendanceData);
        attendanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        attendanceTable.setStyle("-fx-background-color: white;");

        // Create columns
        createTableColumns();

        // Placeholder text when no course is selected
        Label placeholderLabel = new Label("Please select a course from the dropdown above");
        placeholderLabel.setFont(Font.font("Tahoma", 14));
        placeholderLabel.setStyle("-fx-text-fill: #999;");
        attendanceTable.setPlaceholder(placeholderLabel);

        centerSection.getChildren().add(attendanceTable);
        VBox.setVgrow(attendanceTable, Priority.ALWAYS);

        return centerSection;
    }

    private void createTableColumns() {
        // Student Name column
        TableColumn<StudentAttendanceData, String> nameColumn = new TableColumn<>("Student Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        nameColumn.setPrefWidth(150);
        nameColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        // Student ID column
        TableColumn<StudentAttendanceData, String> idColumn = new TableColumn<>("Student ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        idColumn.setPrefWidth(100);
        idColumn.setStyle("-fx-alignment: CENTER;");

        attendanceTable.getColumns().add(nameColumn);
        attendanceTable.getColumns().add(idColumn);

        // Create 13 week columns
        for (int week = 1; week <= 13; week++) {
            TableColumn<StudentAttendanceData, String> weekColumn = createWeekColumn(week);
            attendanceTable.getColumns().add(weekColumn);
        }
    }

    private TableColumn<StudentAttendanceData, String> createWeekColumn(int weekNumber) {
        TableColumn<StudentAttendanceData, String> column = new TableColumn<>("Week " + weekNumber);
        column.setPrefWidth(70);
        column.setStyle("-fx-alignment: CENTER;");

        // Use a custom cell value factory
        column.setCellValueFactory(cellData -> cellData.getValue().weekProperty(weekNumber));

        // Apply color styling based on attendance status
        column.setCellFactory(col -> new TableCell<StudentAttendanceData, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null || status.equals("-")) {
                    setText("-");
                    setStyle("-fx-background-color: white; -fx-text-fill: #ccc;");
                } else {
                    setText(status);
                    switch (status) {
                        case "P":
                            setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                            break;
                        case "L":
                            setStyle("-fx-background-color: #FFC107; -fx-text-fill: white; -fx-font-weight: bold;");
                            break;
                        case "A":
                            setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-background-color: white; -fx-text-fill: black;");
                    }
                }
            }
        });

        return column;
    }

    private HBox createBottomSection() {
        HBox bottomSection = new HBox(10);
        bottomSection.setAlignment(Pos.CENTER);
        bottomSection.setPadding(new Insets(20, 0, 0, 0));

        Button backButton = new Button("Back to Dashboard");
        backButton.setPrefWidth(200);
        backButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        backButton.setOnAction(e -> {
            // Navigate back to student dashboard
            StudentView studentView = new StudentView(stage, student, authManager);
            stage.setScene(studentView.createScene());
        });

        bottomSection.getChildren().add(backButton);
        return bottomSection;
    }

    /**
     * Load enrolled courses into the dropdown
     */
    public void loadEnrolledCourses() {
        List<String> courses = dbManager.getEnrolledCoursesForStudent(student.getUserId());
        courseDropdown.setItems(FXCollections.observableArrayList(courses));
    }

    /**
     * Load attendance data for the selected course
     */
    private void loadAttendanceData() {
        String selectedCourse = courseDropdown.getValue();
        if (selectedCourse == null || selectedCourse.isEmpty()) {
            return;
        }

        // Clear existing data
        attendanceData.clear();

        // Get all sessions for this course, sorted by date
        // First, try to find sessions for the student's enrolled section
        List<com.cs102.model.Class> enrollments = dbManager.findEnrollmentsByUserId(student.getUserId());

        List<Session> sessions = new ArrayList<>();
        for (com.cs102.model.Class enrollment : enrollments) {
            if (enrollment.getCourse().equals(selectedCourse)) {
                List<Session> courseSessions = dbManager.findSessionsByCourseAndSection(
                        enrollment.getCourse(), enrollment.getSection());
                sessions.addAll(courseSessions);
            }
        }

        // Sort sessions by date
        sessions.sort(Comparator.comparing(Session::getDate));

        // Create a map of sessionId to week number
        Map<UUID, Integer> sessionToWeekMap = new HashMap<>();
        for (int i = 0; i < Math.min(sessions.size(), 13); i++) {
            sessionToWeekMap.put(sessions.get(i).getId(), i + 1);
        }

        // Get attendance records for this student in this course
        List<AttendanceRecord> attendanceRecords = dbManager.getAttendanceForStudentInCourse(
                student.getUserId(), selectedCourse);

        // Create a single row for this student
        StudentAttendanceData studentData = new StudentAttendanceData(
                student.getName(),
                student.getUserId()
        );

        // Fill in the attendance data for each week
        for (AttendanceRecord record : attendanceRecords) {
            Integer weekNumber = sessionToWeekMap.get(record.getSessionId());
            if (weekNumber != null && weekNumber >= 1 && weekNumber <= 13) {
                String status = convertAttendanceStatus(record.getAttendance());
                studentData.setWeekAttendance(weekNumber, status);
            }
        }

        // Add the row to the table
        attendanceData.add(studentData);
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
}
