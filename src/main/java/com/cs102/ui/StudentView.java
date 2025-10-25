package com.cs102.ui;

import com.cs102.manager.AttendanceManager;
import com.cs102.manager.AuthenticationManager;
import com.cs102.model.AttendanceRecord;
import com.cs102.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class StudentView {

    private Stage stage;
    private User student;
    private AuthenticationManager authManager;
    private AttendanceManager attendanceManager;

    public StudentView(Stage stage, User student, AuthenticationManager authManager, AttendanceManager attendanceManager) {
        this.stage = stage;
        this.student = student;
        this.authManager = authManager;
        this.attendanceManager = attendanceManager;
    }

    public Scene createScene() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        // Top Section - Header
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Text welcomeText = new Text("Student Dashboard");
        welcomeText.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

        Label studentName = new Label("Welcome, " + student.getName());
        studentName.setFont(Font.font("Tahoma", FontWeight.NORMAL, 18));

        Label emailLabel = new Label(student.getEmail());
        emailLabel.setFont(Font.font(14));
        emailLabel.setStyle("-fx-text-fill: gray;");

        header.getChildren().addAll(welcomeText, studentName, emailLabel);

        // Center Section - Main Content
        VBox centerContent = new VBox(15);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(20));

        // Student-specific features
        Button checkInBtn = new Button("Check In");
        checkInBtn.setPrefWidth(300);
        checkInBtn.setPrefHeight(50);
        checkInBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px;");
        checkInBtn.setOnAction(e -> handleCheckIn());

        Button checkOutBtn = new Button("Check Out");
        checkOutBtn.setPrefWidth(300);
        checkOutBtn.setPrefHeight(50);
        checkOutBtn.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 16px;");
        checkOutBtn.setOnAction(e -> handleCheckOut());

        Button viewMyAttendanceBtn = new Button("View My Attendance");
        viewMyAttendanceBtn.setPrefWidth(300);
        viewMyAttendanceBtn.setPrefHeight(50);
        viewMyAttendanceBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 16px;");
        viewMyAttendanceBtn.setOnAction(e -> handleViewAttendance());

        Button settingsBtn = new Button("Settings");
        settingsBtn.setPrefWidth(300);
        settingsBtn.setPrefHeight(50);
        settingsBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-font-size: 16px;");
        settingsBtn.setOnAction(e -> {
            // TODO: Navigate to settings page
            System.out.println("Settings clicked");
        });

        centerContent.getChildren().addAll(
            checkInBtn,
            checkOutBtn,
            viewMyAttendanceBtn,
            settingsBtn
        );

        // Bottom Section - Logout
        VBox bottomSection = new VBox(10);
        bottomSection.setAlignment(Pos.CENTER);
        bottomSection.setPadding(new Insets(20, 0, 0, 0));

        Button logoutBtn = new Button("Logout");
        logoutBtn.setPrefWidth(200);
        logoutBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
        logoutBtn.setOnAction(e -> {
            // Navigate back to login screen
            AuthView authView = new AuthView(stage, authManager, attendanceManager);
            stage.setScene(authView.createScene());
        });

        bottomSection.getChildren().add(logoutBtn);

        // Assemble layout
        mainLayout.setTop(header);
        mainLayout.setCenter(centerContent);
        mainLayout.setBottom(bottomSection);

        Scene scene = new Scene(mainLayout, 600, 700);
        return scene;
    }

    /**
     * Handle check-in button click
     */
    private void handleCheckIn() {
        // Check if already checked in today
        if (attendanceManager.hasCheckedInToday(student)) {
            showAlert(Alert.AlertType.WARNING, "Already Checked In",
                "You have already checked in today. Please check out first before checking in again.");
            return;
        }

        // Perform check-in
        Optional<AttendanceRecord> result = attendanceManager.checkIn(student);

        if (result.isPresent()) {
            AttendanceRecord record = result.get();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timeStr = record.getCheckInTime().format(formatter);

            showAlert(Alert.AlertType.INFORMATION, "Check-In Successful",
                "You have successfully checked in!\n\n" +
                "Time: " + timeStr + "\n" +
                "Status: " + record.getStatus());
        } else {
            showAlert(Alert.AlertType.ERROR, "Check-In Failed",
                "Failed to check in. Please try again.");
        }
    }

    /**
     * Handle check-out button click
     */
    private void handleCheckOut() {
        // Check if user has checked in today
        if (!attendanceManager.hasCheckedInToday(student)) {
            showAlert(Alert.AlertType.WARNING, "Not Checked In",
                "You need to check in first before you can check out.");
            return;
        }

        // Perform check-out
        Optional<AttendanceRecord> result = attendanceManager.checkOut(student);

        if (result.isPresent()) {
            AttendanceRecord record = result.get();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String checkInTimeStr = record.getCheckInTime().format(formatter);
            String checkOutTimeStr = record.getCheckOutTime().format(formatter);

            showAlert(Alert.AlertType.INFORMATION, "Check-Out Successful",
                "You have successfully checked out!\n\n" +
                "Check-in: " + checkInTimeStr + "\n" +
                "Check-out: " + checkOutTimeStr + "\n" +
                "Status: " + record.getStatus());
        } else {
            showAlert(Alert.AlertType.ERROR, "Check-Out Failed",
                "Failed to check out. Please try again.");
        }
    }

    /**
     * Handle view attendance button click
     */
    private void handleViewAttendance() {
        List<AttendanceRecord> records = attendanceManager.getUserAttendanceHistory(student);

        if (records.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Attendance Records",
                "You don't have any attendance records yet.");
            return;
        }

        // Create a new window to display attendance records
        Stage attendanceStage = new Stage();
        attendanceStage.setTitle("My Attendance History");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Text title = new Text("Attendance History");
        title.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));

        // Create TableView for attendance records
        TableView<AttendanceRecord> table = new TableView<>();
        table.setPrefHeight(400);

        // Define columns
        TableColumn<AttendanceRecord, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<AttendanceRecord, String> checkInCol = new TableColumn<>("Check-In Time");
        checkInCol.setCellValueFactory(cellData -> {
            LocalDateTime checkIn = cellData.getValue().getCheckInTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return new javafx.beans.property.SimpleStringProperty(checkIn.format(formatter));
        });
        checkInCol.setPrefWidth(150);

        TableColumn<AttendanceRecord, String> checkOutCol = new TableColumn<>("Check-Out Time");
        checkOutCol.setCellValueFactory(cellData -> {
            LocalDateTime checkOut = cellData.getValue().getCheckOutTime();
            if (checkOut != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return new javafx.beans.property.SimpleStringProperty(checkOut.format(formatter));
            } else {
                return new javafx.beans.property.SimpleStringProperty("Not checked out");
            }
        });
        checkOutCol.setPrefWidth(150);

        TableColumn<AttendanceRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        table.getColumns().addAll(idCol, checkInCol, checkOutCol, statusCol);
        table.getItems().addAll(records);

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> attendanceStage.close());
        closeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");

        layout.getChildren().addAll(title, table, closeBtn);

        Scene scene = new Scene(layout, 500, 500);
        attendanceStage.setScene(scene);
        attendanceStage.show();
    }

    /**
     * Helper method to show alerts
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
