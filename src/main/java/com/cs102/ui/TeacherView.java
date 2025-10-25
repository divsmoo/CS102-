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

public class TeacherView {

    private Stage stage;
    private User teacher;
    private AuthenticationManager authManager;

    public TeacherView(Stage stage, User teacher, AuthenticationManager authManager) {
        this.stage = stage;
        this.teacher = teacher;
        this.authManager = authManager;
    }

    public Scene createScene() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        // Top Section - Header
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Text welcomeText = new Text("Teacher Dashboard");
        welcomeText.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

        Label teacherName = new Label("Welcome, " + teacher.getName());
        teacherName.setFont(Font.font("Tahoma", FontWeight.NORMAL, 18));

        Label emailLabel = new Label(teacher.getEmail());
        emailLabel.setFont(Font.font(14));
        emailLabel.setStyle("-fx-text-fill: gray;");

        header.getChildren().addAll(welcomeText, teacherName, emailLabel);

        // Center Section - Main Content
        VBox centerContent = new VBox(15);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(20));

        // Teacher-specific features
        Button viewAttendanceBtn = new Button("View All Attendance");
        viewAttendanceBtn.setPrefWidth(300);
        viewAttendanceBtn.setPrefHeight(50);
        viewAttendanceBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px;");
        viewAttendanceBtn.setOnAction(e -> {
            // TODO: Navigate to attendance viewing page
            System.out.println("View Attendance clicked");
        });

        Button manageSessionsBtn = new Button("Manage Sessions");
        manageSessionsBtn.setPrefWidth(300);
        manageSessionsBtn.setPrefHeight(50);
        manageSessionsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 16px;");
        manageSessionsBtn.setOnAction(e -> {
            // TODO: Navigate to session management page
            System.out.println("Manage Sessions clicked");
        });

        Button viewStudentsBtn = new Button("View Students");
        viewStudentsBtn.setPrefWidth(300);
        viewStudentsBtn.setPrefHeight(50);
        viewStudentsBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 16px;");
        viewStudentsBtn.setOnAction(e -> {
            // TODO: Navigate to student list page
            System.out.println("View Students clicked");
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
            viewAttendanceBtn,
            manageSessionsBtn,
            viewStudentsBtn,
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
