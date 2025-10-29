package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class SettingsView {

    private final Stage stage;
    private final User teacher;
    private final AuthenticationManager authManager;

    public SettingsView(Stage stage, User teacher, AuthenticationManager authManager) {
        this.stage = stage;
        this.teacher = teacher;
        this.authManager = authManager;
    }

    public Scene createScene() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(12));

        // --- Top: Title + improved nav bar ---
        VBox topBox = new VBox(8);
        topBox.setAlignment(Pos.CENTER);

        Text title = new Text("Settings (Professor)");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 46));
        title.getStyleClass().add("page-title");

        HBox nav = new HBox(12);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(8, 0, 8, 0));

        // create nav buttons using helper
        Button bHome = createNavButton("Home");
        Button bClasses = createNavButton("Classes");
        Button bSessions = createNavButton("Sessions");
        Button bLive = createNavButton("Live Recognition");
        Button bSettings = createNavButton("Settings");

        // mark Settings as active appearance
        bSettings.getStyleClass().add("nav-active");

        // handlers - Home goes to TeacherView, others show placeholders (replace later)
        bHome.setOnAction(e -> {
            TeacherView tv = new TeacherView(stage, teacher, authManager);
            stage.setScene(tv.createScene());
        });

        bClasses.setOnAction(e -> showPlaceholder("Classes"));
        bSessions.setOnAction(e -> showPlaceholder("Sessions"));
        bLive.setOnAction(e -> showPlaceholder("Live Recognition"));
        bSettings.setOnAction(e -> {
            // already on settings; maybe do small visual feedback
        });

        nav.getChildren().addAll(bHome, bClasses, bSessions, bLive, bSettings);
        topBox.getChildren().addAll(title, nav);

        // --- Center: Form ---
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30, 40, 30, 40));

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHalignment(javafx.geometry.HPos.RIGHT);
        c1.setMinWidth(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setMinWidth(380);
        form.getColumnConstraints().addAll(c1, c2);

        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField();
        emailField.setPrefWidth(420);

        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();
        passField.setPrefWidth(420);
        passField.setPromptText("Leave blank to keep current password");

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        nameField.setPrefWidth(420);

        form.add(emailLabel, 0, 0);
        form.add(emailField, 1, 0);

        form.add(passLabel, 0, 1);
        form.add(passField, 1, 1);

        form.add(nameLabel, 0, 2);
        form.add(nameField, 1, 2);

        // Prefill with current user data
        emailField.setText(teacher.getEmail() != null ? teacher.getEmail() : "");
        nameField.setText(teacher.getName() != null ? teacher.getName() : "");

        // Buttons row
        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("pill-outline");
        backBtn.setOnAction(e -> {
            TeacherView tv = new TeacherView(stage, teacher, authManager);
            stage.setScene(tv.createScene());
        });

        Button saveBtn = new Button("Save Settings");
        saveBtn.getStyleClass().add("pill-primary");
        saveBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String name = nameField.getText().trim();
            String password = passField.getText();

            if (email.isEmpty() || name.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", 
                         "Email and Name are required.");
                return;
            }

            try {
                boolean success = true;
                StringBuilder message = new StringBuilder();

                // Update profile (name and email) in database
                boolean profileUpdated = authManager.updateUserProfile(teacher, name, email);
                
                if (profileUpdated) {
                    message.append("Profile updated successfully.\n");
                } else {
                    success = false;
                    message.append("Failed to update profile.\n");
                }

                // Update password if provided
                if (password != null && !password.isEmpty()) {
                    boolean passwordUpdated = authManager.updatePassword(email, password);
                    
                    if (passwordUpdated) {
                        message.append("Password updated successfully.");
                        passField.clear(); // Clear password field after successful update
                    } else {
                        success = false;
                        message.append("Failed to update password.");
                    }
                }

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                             message.toString());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                             message.toString());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", 
                         "An error occurred while saving settings: " + ex.getMessage());
            }
        });

        HBox buttons = new HBox(18, backBtn, saveBtn);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(18, 0, 6, 0));

        VBox centerBox = new VBox(8, form, buttons);
        centerBox.setAlignment(Pos.CENTER);

        mainLayout.setTop(topBox);
        mainLayout.setCenter(centerBox);

        Scene scene = new Scene(mainLayout, 1000, 580);

        // load CSS if present (non-fatal)
        try {
            String css = getClass().getResource("/ui/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ex) {
            // stylesheet missing is fine
        }

        return scene;
    }

    // helper to create visually consistent nav buttons
    private Button createNavButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMinWidth(160);
        b.setMinHeight(52);
        return b;
    }

    // Helper method to show alerts
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    // placeholder scenes for pages not implemented yet
    private void showPlaceholder(String pageName) {
        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        Text header = new Text(pageName);
        header.setFont(Font.font("Tahoma", FontWeight.BOLD, 32));
        Label info = new Label(pageName + " page is not implemented yet.\nThis is a placeholder - replace with your real view.");
        info.setWrapText(true);
        info.setMaxWidth(700);

        Button back = new Button("Back to Settings");
        back.setOnAction(e -> {
            SettingsView sv = new SettingsView(stage, teacher, authManager);
            stage.setScene(sv.createScene());
        });

        root.getChildren().addAll(header, info, back);
        Scene scene = new Scene(root, 900, 500);
        try {
            String css = getClass().getResource("/ui/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ex) {}
        stage.setScene(scene);
    }
}