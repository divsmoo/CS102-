/**
 * EXAMPLE: How to integrate BackupManager into ProfessorView.java
 *
 * This example shows how to add a "Backup" button to the professor's
 * Settings page or create a new Backup page in the navigation.
 */

// ==============================================================
// STEP 1: Add BackupManager to ProfessorView class
// ==============================================================

public class ProfessorView {

    private Stage stage;
    private User professor;
    private AuthenticationManager authManager;
    private DatabaseManager databaseManager;
    private BackupManager backupManager;  // <-- ADD THIS

    public ProfessorView(Stage stage, User professor, AuthenticationManager authManager) {
        this.stage = stage;
        this.professor = professor;
        this.authManager = authManager;
        this.databaseManager = authManager.getDatabaseManager();
        this.backupManager = authManager.getBackupManager();  // <-- ADD THIS (need to add getter to AuthManager)
    }

    // ... existing code ...
}


// ==============================================================
// STEP 2: Add backup section to Settings Page
// ==============================================================

private void showSettingsPage() {
    VBox settingsPage = new VBox(20);
    settingsPage.setPadding(new Insets(30));
    settingsPage.setAlignment(Pos.TOP_CENTER);

    // Title
    Label title = new Label("Settings");
    title.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

    // ... existing settings sections ...

    // ========== BACKUP SECTION ==========
    VBox backupSection = createBackupSection();

    settingsPage.getChildren().addAll(title, /* existing sections */, backupSection);
    mainLayout.setCenter(new ScrollPane(settingsPage));
}

private VBox createBackupSection() {
    VBox section = new VBox(15);
    section.setPadding(new Insets(20));
    section.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");
    section.setMaxWidth(600);

    // Section title
    Label sectionTitle = new Label("Database Backup");
    sectionTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));

    // Info text
    Label infoLabel = new Label(
        "Create manual backups of attendance data. Backups are stored locally and can be used for recovery."
    );
    infoLabel.setWrapText(true);
    infoLabel.setStyle("-fx-text-fill: #666;");

    // Backup statistics
    BackupManager.BackupStats stats = backupManager.getBackupStats();
    Label statsLabel = new Label(String.format(
        "Backups: %d | Total Size: %s | Last: %s",
        stats.backupCount,
        stats.getTotalSizeMB(),
        stats.lastBackupName != null ? stats.lastBackupName : "Never"
    ));
    statsLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");

    // Buttons
    HBox buttonBox = new HBox(10);
    buttonBox.setAlignment(Pos.CENTER_LEFT);

    Button fullBackupBtn = new Button("Create Full Backup");
    fullBackupBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
    fullBackupBtn.setOnAction(e -> handleFullBackup());

    Button attendanceBackupBtn = new Button("Backup Attendance Only");
    attendanceBackupBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
    attendanceBackupBtn.setOnAction(e -> handleAttendanceBackup());

    Button cleanupBtn = new Button("Cleanup Old Backups");
    cleanupBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px;");
    cleanupBtn.setOnAction(e -> handleCleanupBackups());

    buttonBox.getChildren().addAll(fullBackupBtn, attendanceBackupBtn, cleanupBtn);

    // Progress indicator (hidden by default)
    ProgressIndicator progressIndicator = new ProgressIndicator();
    progressIndicator.setVisible(false);
    progressIndicator.setPrefSize(30, 30);

    Label statusLabel = new Label();
    statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");

    section.getChildren().addAll(
        sectionTitle,
        infoLabel,
        statsLabel,
        buttonBox,
        progressIndicator,
        statusLabel
    );

    return section;
}


// ==============================================================
// STEP 3: Implement backup handlers
// ==============================================================

private void handleFullBackup() {
    // Show confirmation dialog
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Create Full Backup");
    confirmAlert.setHeaderText("Create a full database backup?");
    confirmAlert.setContentText(
        "This will backup all users, courses, sessions, and attendance records.\n" +
        "Backup will be saved to the ./backups directory."
    );

    confirmAlert.showAndWait().ifPresent(response -> {
        if (response == ButtonType.OK) {
            // Run backup in background thread to avoid freezing UI
            new Thread(() -> {
                try {
                    System.out.println("Creating full backup...");
                    Path backupPath = backupManager.createFullBackup();

                    // Show success message on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Backup Complete");
                        successAlert.setHeaderText("Full backup created successfully!");
                        successAlert.setContentText("Backup location: " + backupPath);
                        successAlert.showAndWait();

                        // Refresh settings page to update stats
                        showSettingsPage();
                    });

                } catch (IOException ex) {
                    javafx.application.Platform.runLater(() -> {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Backup Failed");
                        errorAlert.setHeaderText("Failed to create backup");
                        errorAlert.setContentText(ex.getMessage());
                        errorAlert.showAndWait();
                    });
                }
            }).start();
        }
    });
}

private void handleAttendanceBackup() {
    new Thread(() -> {
        try {
            System.out.println("Creating attendance backup...");
            Path backupPath = backupManager.createAttendanceBackup();

            javafx.application.Platform.runLater(() -> {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Backup Complete");
                successAlert.setHeaderText("Attendance backup created!");
                successAlert.setContentText("Backup location: " + backupPath);
                successAlert.showAndWait();
                showSettingsPage();
            });

        } catch (IOException ex) {
            javafx.application.Platform.runLater(() -> {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Backup Failed");
                errorAlert.setContentText(ex.getMessage());
                errorAlert.showAndWait();
            });
        }
    }).start();
}

private void handleCleanupBackups() {
    // Show input dialog for how many backups to keep
    TextInputDialog dialog = new TextInputDialog("5");
    dialog.setTitle("Cleanup Old Backups");
    dialog.setHeaderText("How many recent backups to keep?");
    dialog.setContentText("Keep:");

    dialog.showAndWait().ifPresent(input -> {
        try {
            int keepCount = Integer.parseInt(input);

            backupManager.cleanupOldBackups(keepCount);

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Cleanup Complete");
            successAlert.setHeaderText("Old backups deleted successfully");
            successAlert.setContentText("Kept " + keepCount + " most recent backups");
            successAlert.showAndWait();

            showSettingsPage();

        } catch (NumberFormatException ex) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setContentText("Please enter a valid number");
            errorAlert.showAndWait();
        } catch (IOException ex) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setContentText("Cleanup failed: " + ex.getMessage());
            errorAlert.showAndWait();
        }
    });
}


// ==============================================================
// STEP 4: Add getter to AuthenticationManager
// ==============================================================

// In AuthenticationManager.java, add:
public class AuthenticationManager {

    @Autowired
    private DatabaseManager databaseManager;

    @Autowired
    private BackupManager backupManager;  // <-- ADD THIS

    // Existing getters...
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    // ADD THIS GETTER
    public BackupManager getBackupManager() {
        return backupManager;
    }
}


// ==============================================================
// ALTERNATIVE: Create a dedicated Backup page in navigation
// ==============================================================

// Add to navigation bar
Button backupBtn = createNavButton("Backup");

// Add click handler
backupBtn.setOnAction(e -> {
    currentPage = "Backup";
    updateNavButtonStyles();
    showBackupPage();  // Create this method similar to showHomePage()
});

private void showBackupPage() {
    VBox backupPage = new VBox(30);
    backupPage.setPadding(new Insets(40));
    backupPage.setAlignment(Pos.TOP_CENTER);
    backupPage.setStyle("-fx-background-color: #f5f5f5;");

    // Title
    Label title = new Label("Database Backup & Recovery");
    title.setFont(Font.font("Tahoma", FontWeight.BOLD, 28));

    // Create backup cards
    HBox backupCards = new HBox(20);
    backupCards.setAlignment(Pos.CENTER);

    VBox fullBackupCard = createBackupCard(
        "Full Backup",
        "Backs up all tables including users, courses, sessions, and attendance",
        "#4CAF50",
        () -> handleFullBackup()
    );

    VBox attendanceBackupCard = createBackupCard(
        "Attendance Only",
        "Quick backup of attendance records only",
        "#2196F3",
        () -> handleAttendanceBackup()
    );

    backupCards.getChildren().addAll(fullBackupCard, attendanceBackupCard);

    // Backup history table
    TableView<BackupInfo> backupTable = createBackupHistoryTable();

    backupPage.getChildren().addAll(title, backupCards, backupTable);
    mainLayout.setCenter(new ScrollPane(backupPage));
}

private VBox createBackupCard(String title, String description, String color, Runnable action) {
    VBox card = new VBox(15);
    card.setPadding(new Insets(30));
    card.setAlignment(Pos.CENTER);
    card.setStyle(
        "-fx-background-color: white; " +
        "-fx-border-color: " + color + "; " +
        "-fx-border-width: 2; " +
        "-fx-border-radius: 10; " +
        "-fx-background-radius: 10; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);"
    );
    card.setPrefSize(300, 200);

    Label titleLabel = new Label(title);
    titleLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));
    titleLabel.setStyle("-fx-text-fill: " + color + ";");

    Label descLabel = new Label(description);
    descLabel.setWrapText(true);
    descLabel.setAlignment(Pos.CENTER);
    descLabel.setMaxWidth(250);
    descLabel.setStyle("-fx-text-fill: #666;");

    Button actionBtn = new Button("Create Backup");
    actionBtn.setStyle(
        "-fx-background-color: " + color + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10 30;"
    );
    actionBtn.setOnAction(e -> action.run());

    card.getChildren().addAll(titleLabel, descLabel, actionBtn);
    return card;
}
