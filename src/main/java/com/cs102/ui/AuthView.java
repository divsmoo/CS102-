package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.model.User;
import com.cs102.model.UserRole;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Optional;

public class AuthView {

    private Stage stage;
    private AuthenticationManager authManager;

    public AuthView(Stage stage, AuthenticationManager authManager) {
        this.stage = stage;
        this.authManager = authManager;
    }

    public Scene createScene() {
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(25));

        // Title
        Text sceneTitle = new Text("Student Attendance System");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        // Tab Pane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Login Tab
        Tab loginTab = new Tab("Login");
        loginTab.setContent(createLoginForm());

        // Register Tab
        Tab registerTab = new Tab("Register");
        registerTab.setContent(createRegisterForm());

        tabPane.getTabs().addAll(loginTab, registerTab);

        mainLayout.getChildren().addAll(sceneTitle, tabPane);

        Scene scene = new Scene(mainLayout, 500, 550);
        return scene;
    }

    private GridPane createLoginForm() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));

        // Email field
        Label emailLabel = new Label("Email:");
        grid.add(emailLabel, 0, 0);
        TextField emailTextField = new TextField();
        emailTextField.setPromptText("Enter email");
        emailTextField.setPrefWidth(250);
        grid.add(emailTextField, 1, 0);

        // Password field
        Label pwLabel = new Label("Password:");
        grid.add(pwLabel, 0, 1);
        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter password");
        pwBox.setPrefWidth(250);
        grid.add(pwBox, 1, 1);

        // Message label
        Label messageLabel = new Label();
        grid.add(messageLabel, 1, 3);

        // Login button
        Button loginBtn = new Button("Login");
        loginBtn.setPrefWidth(250);
        loginBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        grid.add(loginBtn, 1, 2);

        // Login action
        loginBtn.setOnAction(e -> {
            String email = emailTextField.getText();
            String password = pwBox.getText();

            if (email.isEmpty() || password.isEmpty()) {
                showAlert(AlertType.ERROR, "Validation Error", "Please fill in all fields");
                return;
            }

            // Disable button during login
            loginBtn.setDisable(true);
            messageLabel.setText("Logging in...");
            messageLabel.setStyle("-fx-text-fill: blue;");

            // Run login in background thread
            new Thread(() -> {
                try {
                    Optional<User> userOpt = authManager.login(email, password);

                    javafx.application.Platform.runLater(() -> {
                        loginBtn.setDisable(false);
                        messageLabel.setText("");

                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            showAlert(AlertType.INFORMATION, "Login Successful",
                                "Welcome back, " + user.getName() + "!");
                            showMainScreen(user);
                        } else {
                            showAlert(AlertType.ERROR, "Login Failed",
                                "Invalid email or password.\nPlease check your credentials and try again.");
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        loginBtn.setDisable(false);
                        messageLabel.setText("");
                        showAlert(AlertType.ERROR, "Login Error",
                            "An error occurred during login:\n" + ex.getMessage());
                    });
                }
            }).start();
        });

        // Allow Enter key to login
        pwBox.setOnAction(e -> loginBtn.fire());

        return grid;
    }

    private GridPane createRegisterForm() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));

        int row = 0;

        // Name field
        Label nameLabel = new Label("Full Name:");
        grid.add(nameLabel, 0, row);
        TextField nameTextField = new TextField();
        nameTextField.setPromptText("Enter full name");
        nameTextField.setPrefWidth(250);
        grid.add(nameTextField, 1, row++);

        // Email field
        Label emailLabel = new Label("Email:");
        grid.add(emailLabel, 0, row);
        TextField emailTextField = new TextField();
        emailTextField.setPromptText("Enter email");
        emailTextField.setPrefWidth(250);
        grid.add(emailTextField, 1, row++);

        // Password field
        Label pwLabel = new Label("Password:");
        grid.add(pwLabel, 0, row);
        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter password (min 6 characters)");
        pwBox.setPrefWidth(250);
        grid.add(pwBox, 1, row++);

        // Confirm Password field
        Label confirmPwLabel = new Label("Confirm Password:");
        grid.add(confirmPwLabel, 0, row);
        PasswordField confirmPwBox = new PasswordField();
        confirmPwBox.setPromptText("Confirm password");
        confirmPwBox.setPrefWidth(250);
        grid.add(confirmPwBox, 1, row++);

        // Role selection
        Label roleLabel = new Label("Role:");
        grid.add(roleLabel, 0, row);
        ComboBox<UserRole> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(UserRole.STUDENT, UserRole.TEACHER);
        roleComboBox.setValue(UserRole.STUDENT);
        roleComboBox.setPrefWidth(250);
        grid.add(roleComboBox, 1, row++);

        // Message label
        Label messageLabel = new Label();
        grid.add(messageLabel, 1, row + 1);

        // Register button
        Button registerBtn = new Button("Register");
        registerBtn.setPrefWidth(250);
        registerBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        grid.add(registerBtn, 1, row);

        // Register action
        registerBtn.setOnAction(e -> {
            String name = nameTextField.getText();
            String email = emailTextField.getText();
            String password = pwBox.getText();
            String confirmPassword = confirmPwBox.getText();
            UserRole role = roleComboBox.getValue();

            // Validation
            if (name.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
                showAlert(AlertType.ERROR, "Validation Error", "Please fill in all fields");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showAlert(AlertType.ERROR, "Validation Error", "Passwords do not match");
                return;
            }

            if (password.length() < 6) {
                showAlert(AlertType.ERROR, "Validation Error",
                    "Password must be at least 6 characters");
                return;
            }

            // Show loading
            registerBtn.setDisable(true);
            messageLabel.setText("Creating account...");
            messageLabel.setStyle("-fx-text-fill: blue;");

            // Attempt registration in background thread
            new Thread(() -> {
                try {
                    Optional<User> userOpt = authManager.register(name, email, password, role);

                    javafx.application.Platform.runLater(() -> {
                        registerBtn.setDisable(false);
                        messageLabel.setText("");

                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            showAlert(AlertType.INFORMATION, "Registration Successful",
                                "Welcome, " + user.getName() + "!\nYour account has been created.");
                            // Auto-login: Show main screen
                            showMainScreen(user);
                        } else {
                            showAlert(AlertType.ERROR, "Registration Failed",
                                "Unable to create account.\nThe email may already be in use.");
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        registerBtn.setDisable(false);
                        messageLabel.setText("");
                        showAlert(AlertType.ERROR, "Registration Error",
                            "An error occurred during registration:\n" + ex.getMessage());
                    });
                }
            }).start();
        });

        return grid;
    }

    private void showMainScreen(User user) {
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(25));

        Text welcomeText = new Text("Welcome, " + user.getName() + "!");
        welcomeText.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));

        Label roleLabel = new Label("Role: " + user.getRole());
        roleLabel.setFont(Font.font(16));
        Label emailLabel = new Label("Email: " + user.getEmail());
        emailLabel.setFont(Font.font(16));

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
        logoutBtn.setOnAction(e -> stage.setScene(createScene()));

        mainLayout.getChildren().addAll(welcomeText, roleLabel, emailLabel, logoutBtn);

        Scene mainScene = new Scene(mainLayout, 500, 400);
        stage.setScene(mainScene);
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
