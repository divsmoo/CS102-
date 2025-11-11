package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.model.User;
import com.cs102.model.UserRole;
import com.cs102.service.IntrusionDetectionService;
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

import java.util.List;
import java.util.Optional;

public class AuthView {

    private Stage stage;
    private AuthenticationManager authManager;
    private IntrusionDetectionService idsService;
    
    // Store pending registration info for retry
    private String pendingUserId;
    private String pendingName;
    private String pendingEmail;
    private String pendingPassword;
    private UserRole pendingRole;

    public AuthView(Stage stage, AuthenticationManager authManager) {
        this.stage = stage;
        this.authManager = authManager;
        // Get IDS service from AuthenticationManager
        this.idsService = authManager.getIntrusionDetectionService();
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
        
        // Clear pending registration when switching to login tab
        loginTab.setOnSelectionChanged(e -> {
            if (loginTab.isSelected()) {
                clearPendingRegistration();
            }
        });

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
        
        // Show retry message if there's pending registration data
        if (pendingUserId != null) {
            Label retryLabel = new Label("⚠️ Face capture was cancelled. Your information has been saved below.");
            retryLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 12px;");
            grid.add(retryLabel, 0, row++, 2, 1);
            
            Label instructionLabel = new Label("Please verify your details and try again.");
            instructionLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            grid.add(instructionLabel, 0, row++, 2, 1);
        }

        // User ID field
        Label userIdLabel = new Label("User ID:");
        grid.add(userIdLabel, 0, row);
        TextField userIdTextField = new TextField();
        userIdTextField.setPromptText("e.g., S12345 (Student) or P12345 (Professor)");
        userIdTextField.setPrefWidth(250);
        if (pendingUserId != null) userIdTextField.setText(pendingUserId); // Restore value
        grid.add(userIdTextField, 1, row++);

        // Name field
        Label nameLabel = new Label("Full Name:");
        grid.add(nameLabel, 0, row);
        TextField nameTextField = new TextField();
        nameTextField.setPromptText("Enter full name");
        nameTextField.setPrefWidth(250);
        if (pendingName != null) nameTextField.setText(pendingName); // Restore value
        grid.add(nameTextField, 1, row++);

        // Email field
        Label emailLabel = new Label("Email:");
        grid.add(emailLabel, 0, row);
        TextField emailTextField = new TextField();
        emailTextField.setPromptText("Enter email");
        emailTextField.setPrefWidth(250);
        if (pendingEmail != null) emailTextField.setText(pendingEmail); // Restore value
        grid.add(emailTextField, 1, row++);

        // Password field
        Label pwLabel = new Label("Password:");
        grid.add(pwLabel, 0, row);
        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter password (min 6 characters)");
        pwBox.setPrefWidth(250);
        if (pendingPassword != null) pwBox.setText(pendingPassword); // Restore value
        grid.add(pwBox, 1, row++);

        // Confirm Password field
        Label confirmPwLabel = new Label("Confirm Password:");
        grid.add(confirmPwLabel, 0, row);
        PasswordField confirmPwBox = new PasswordField();
        confirmPwBox.setPromptText("Confirm password");
        confirmPwBox.setPrefWidth(250);
        if (pendingPassword != null) confirmPwBox.setText(pendingPassword); // Restore value
        grid.add(confirmPwBox, 1, row++);

        // Role selection
        Label roleLabel = new Label("Role:");
        grid.add(roleLabel, 0, row);
        ComboBox<UserRole> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(UserRole.STUDENT, UserRole.PROFESSOR, UserRole.ADMIN);
        roleComboBox.setValue(pendingRole != null ? pendingRole : UserRole.STUDENT); // Restore or default
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
            String userId = userIdTextField.getText().trim();
            String name = nameTextField.getText();
            String email = emailTextField.getText();
            String password = pwBox.getText();
            String confirmPassword = confirmPwBox.getText();
            UserRole role = roleComboBox.getValue();

            // Validation
            if (userId.isEmpty() || name.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
                showAlert(AlertType.ERROR, "Validation Error", "Please fill in all fields");
                return;
            }

            // Validate user ID format (alphanumeric)
            if (!userId.matches("[A-Za-z0-9]+")) {
                showAlert(AlertType.ERROR, "Validation Error",
                    "User ID must be alphanumeric (e.g., S12345");
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

            // Check if face capture is needed based on role
            if (role == UserRole.STUDENT) {
                // Save registration info for retry in case of cancellation
                pendingUserId = userId;
                pendingName = name;
                pendingEmail = email;
                pendingPassword = password;
                pendingRole = role;
                
                // Step 1: For STUDENTS, capture face images first
                FaceCaptureView faceCaptureView = new FaceCaptureView(
                    stage,
                    (capturedFaces) -> {
                        // Step 2: After face capture is complete, register the user
                        clearPendingRegistration(); // Clear saved data on success
                        registerUserWithFaces(userId, name, email, password, role, capturedFaces);
                    },
                    () -> {
                        // User cancelled face capture - keep pending data for retry
                        stage.setScene(createScene());
                    }
                );
                stage.setScene(faceCaptureView.createScene());
            } else {
                // For PROFESSORS, skip face capture and register directly
                registerUserWithFaces(userId, name, email, password, role, null);
            }
        });

        return grid;
    }

    private void registerUserWithFaces(String userId, String name, String email, String password, UserRole role, List<byte[]> capturedFaces) {
        // Attempt registration in background thread
        new Thread(() -> {
            try {
                Optional<User> userOpt = authManager.register(userId, name, email, password, role);

                javafx.application.Platform.runLater(() -> {
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // Store face images (only if capturedFaces is not null)
                        if (capturedFaces != null) {
                            storeFaceImages(user, capturedFaces);
                        }

                        String successMessage = user.getRole() == UserRole.STUDENT ?
                            "Welcome, " + user.getName() + "!\nYour account has been created with facial recognition." :
                            "Welcome, " + user.getName() + "!\nYour account has been created.";

                        showAlert(AlertType.INFORMATION, "Registration Successful", successMessage);
                        // Auto-login: Show main screen
                        showMainScreen(user);
                    } else {
                        // This should not happen anymore since we throw exceptions
                        showAlert(AlertType.ERROR, "Registration Failed",
                            "Unable to create account. An unexpected error occurred.");
                        stage.setScene(createScene());
                    }
                });
            } catch (RuntimeException ex) {
                // Catch and display the detailed error message
                javafx.application.Platform.runLater(() -> {
                    showAlert(AlertType.ERROR, "Registration Failed", ex.getMessage());
                    stage.setScene(createScene());
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    showAlert(AlertType.ERROR, "Registration Error",
                        "An unexpected error occurred:\n" + ex.getMessage());
                    stage.setScene(createScene());
                });
            }
        }).start();
    }

    private void storeFaceImages(User user, List<byte[]> capturedFaces) {
        // Store ALL captured face images in the database
        if (!capturedFaces.isEmpty()) {
            System.out.println("Storing " + capturedFaces.size() + " face images for user: " + user.getEmail());
            authManager.saveFaceImages(user, capturedFaces);

            // Also store the first image in the user's primary face_image field for backwards compatibility
            byte[] primaryFaceImage = capturedFaces.get(0);
            authManager.updateUserFaceImage(user, primaryFaceImage);

            System.out.println("Successfully stored all " + capturedFaces.size() + " face images");
        } else {
            System.out.println("No face images to store");
        }
    }

    private void showMainScreen(User user) {
        Scene dashboardScene;

        switch (user.getRole()) {
            case PROFESSOR:
                ProfessorView professorView = new ProfessorView(stage, user, authManager);
                professorView.setIdsService(idsService); // Inject IDS service
                dashboardScene = professorView.createScene();
                break;
            case STUDENT:
                StudentView studentView = new StudentView(stage, user, authManager);
                dashboardScene = studentView.createScene();
                break;
            case ADMIN:
                AdminView adminView = new AdminView(stage, user, authManager);
                dashboardScene = adminView.createScene();
                break;
            default:
                showAlert(AlertType.ERROR, "Invalid Role", "User role not recognized");
                return;
        }

        stage.setScene(dashboardScene);
    }

    private void clearPendingRegistration() {
        pendingUserId = null;
        pendingName = null;
        pendingEmail = null;
        pendingPassword = null;
        pendingRole = null;
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
