package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class StudentView {

    private Stage stage;
    private User student;
    private AuthenticationManager authManager;

    public StudentView(Stage stage, User student, AuthenticationManager authManager) {
        this.stage = stage;
        this.student = student;
        this.authManager = authManager;
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
        checkInBtn.setOnAction(e -> {
            // TODO: Navigate to check-in page (facial recognition)
            System.out.println("Check In clicked");
        });

        Button checkOutBtn = new Button("Check Out");
        checkOutBtn.setPrefWidth(300);
        checkOutBtn.setPrefHeight(50);
        checkOutBtn.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 16px;");
        checkOutBtn.setOnAction(e -> {
            // TODO: Navigate to check-out page
            System.out.println("Check Out clicked");
        });

        Button viewMyAttendanceBtn = new Button("View My Attendance");
        viewMyAttendanceBtn.setPrefWidth(300);
        viewMyAttendanceBtn.setPrefHeight(50);
        viewMyAttendanceBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 16px;");
        viewMyAttendanceBtn.setOnAction(e -> {
            // Navigate to attendance view page
            StudentAttendanceView attendanceView = new StudentAttendanceView(
                stage, student, authManager, authManager.getDatabaseManager());
            attendanceView.loadEnrolledCourses(); // Load courses into dropdown
            stage.setScene(attendanceView.createScene());
        });

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
            AuthView authView = new AuthView(stage, authManager);
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
}
