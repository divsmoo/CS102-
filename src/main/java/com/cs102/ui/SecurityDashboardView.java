package com.cs102.ui;

import com.cs102.model.SecurityEvent;
import com.cs102.model.Severity;
import com.cs102.service.IntrusionDetectionService;
import com.cs102.service.SecurityAlertListener;
import com.cs102.service.SecurityReportExporter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class SecurityDashboardView implements SecurityAlertListener {

    private final Stage stage;
    private final IntrusionDetectionService idsService;
    private final SecurityReportExporter reportExporter;
    private TableView<SecurityEvent> eventsTable;
    private Label alertLabel;

    public SecurityDashboardView(Stage stage, IntrusionDetectionService idsService) {
        this.stage = stage;
        this.idsService = idsService;
        this.reportExporter = new SecurityReportExporter();
        
        // Cleanup listener when stage is closed
        stage.setOnCloseRequest(e -> idsService.removeSecurityAlertListener(this));
    }

    @Override
    public void onSecurityAlert(SecurityEvent event) {
        System.out.println("📱 Dashboard received alert: " + event.getEventType() + " - " + event.getEmail());
        
        // Handle real-time security alerts on JavaFX thread
        Platform.runLater(() -> {
            // Add event to table (if table is initialized)
            if (eventsTable != null) {
                eventsTable.getItems().add(0, event);
                System.out.println("✅ Event added to table");
            } else {
                System.out.println("❌ Table is null!");
            }
            
            // Show alert notification for critical and high severity events
            if (event.getSeverity() == Severity.CRITICAL || event.getSeverity() == Severity.HIGH) {
                System.out.println("🚨 Showing pop-up alert");
                showRealTimeAlert(event);
            }
            
            // Update alert label
            updateAlertLabel(event);
        });
    }

    private void showRealTimeAlert(SecurityEvent event) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("🚨 Security Alert");
        alert.setHeaderText(event.getSeverity() + " - " + event.getEventType());
        alert.setContentText(
            "Email: " + event.getEmail() + "\n" +
            "Description: " + event.getDescription() + "\n" +
            "Time: " + event.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        alert.show();
    }

    private void updateAlertLabel(SecurityEvent event) {
        if (alertLabel != null) {
            String color = switch (event.getSeverity()) {
                case CRITICAL -> "#d32f2f";
                case HIGH -> "#f57c00";
                case MEDIUM -> "#fbc02d";
                case LOW -> "#388e3c";
            };
            
            alertLabel.setText("🔔 Latest: " + event.getEventType() + " - " + event.getEmail());
            alertLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        }
    }

    public Scene createScene() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        // Header
        VBox header = createHeader();
        mainLayout.setTop(header);

        // Statistics Panel
        HBox statsPanel = createStatisticsPanel();
        
        // Events Table
        eventsTable = createEventsTable();
        VBox tableContainer = new VBox(10);
        tableContainer.getChildren().addAll(
            new Label("Recent Security Events (Last 24 hours)"),
            eventsTable
        );
        VBox.setVgrow(eventsTable, Priority.ALWAYS);

        // Main content
        VBox centerContent = new VBox(20);
        centerContent.getChildren().addAll(statsPanel, tableContainer);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        mainLayout.setCenter(centerContent);

        // Bottom buttons
        HBox bottomButtons = createBottomButtons();
        mainLayout.setBottom(bottomButtons);

        // Load initial data
        refreshData();

        // Register for real-time alerts AFTER table is created
        idsService.addSecurityAlertListener(this);
        System.out.println("✅ Security Dashboard listener registered");

        Scene scene = new Scene(mainLayout, 1000, 700);
        return scene;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("🔒 Security Dashboard - Intrusion Detection System");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        Label subtitle = new Label("Monitor security events and potential threats");
        subtitle.setStyle("-fx-text-fill: gray;");

        // Real-time alert label
        alertLabel = new Label("🔔 Waiting for security events...");
        alertLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        header.getChildren().addAll(title, subtitle, alertLabel);
        return header;
    }

    private HBox createStatisticsPanel() {
        HBox statsPanel = new HBox(15);
        statsPanel.setAlignment(Pos.CENTER);
        statsPanel.setPadding(new Insets(10));

        Map<String, Object> stats = idsService.getSecurityStatistics();

        statsPanel.getChildren().addAll(
            createStatBox("Total Events", stats.get("totalEvents").toString(), "#2196F3"),
            createStatBox("Failed Logins", stats.get("failedLogins").toString(), "#FF9800"),
            createStatBox("Successful Logins", stats.get("successfulLogins").toString(), "#4CAF50"),
            createStatBox("Critical Events", stats.get("criticalEvents").toString(), "#f44336"),
            createStatBox("Locked Accounts", stats.get("lockedAccounts").toString(), "#9C27B0")
        );

        return statsPanel;
    }

    private VBox createStatBox(String label, String value, String color) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");
        box.setPrefWidth(150);

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        valueLabel.setStyle("-fx-text-fill: white;");

        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        box.getChildren().addAll(valueLabel, titleLabel);
        return box;
    }

    private TableView<SecurityEvent> createEventsTable() {
        TableView<SecurityEvent> table = new TableView<>();
        table.setPrefHeight(400);

        TableColumn<SecurityEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> {
            SecurityEvent event = cellData.getValue();
            String formattedTime = event.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return new javafx.beans.property.SimpleStringProperty(formattedTime);
        });
        // Custom comparator to sort by actual timestamp, not string
        timeCol.setComparator((time1, time2) -> {
            try {
                java.time.LocalDateTime dt1 = java.time.LocalDateTime.parse(time1, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                java.time.LocalDateTime dt2 = java.time.LocalDateTime.parse(time2, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return dt1.compareTo(dt2);
            } catch (Exception e) {
                return time1.compareTo(time2);
            }
        });
        timeCol.setPrefWidth(160);

        TableColumn<SecurityEvent, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(cellData -> {
            SecurityEvent event = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(event.getSeverity().toString());
        });
        severityCol.setCellFactory(col -> new TableCell<SecurityEvent, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    SecurityEvent event = getTableRow().getItem();
                    setText(event.getSeverity().toString());
                    switch (event.getSeverity()) {
                        case CRITICAL:
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold;");
                            break;
                        case HIGH:
                            setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #e65100;");
                            break;
                        case MEDIUM:
                            setStyle("-fx-background-color: #fff9c4; -fx-text-fill: #f57f17;");
                            break;
                        case LOW:
                            setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;");
                            break;
                    }
                }
            }
        });
        severityCol.setPrefWidth(90);

        TableColumn<SecurityEvent, String> typeCol = new TableColumn<>("Event Type");
        typeCol.setCellValueFactory(cellData -> {
            SecurityEvent event = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(event.getEventType().toString());
        });
        typeCol.setPrefWidth(220);

        TableColumn<SecurityEvent, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cellData -> {
            SecurityEvent event = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(event.getEmail());
        });
        emailCol.setPrefWidth(180);

        TableColumn<SecurityEvent, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cellData -> {
            SecurityEvent event = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(event.getDescription());
        });
        descCol.setPrefWidth(500);
        descCol.setCellFactory(column -> {
            return new TableCell<SecurityEvent, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                        setPrefHeight(Control.USE_COMPUTED_SIZE);
                    }
                }
            };
        });

        table.getColumns().addAll(timeCol, severityCol, typeCol, emailCol, descCol);
        
        // Set default sort to Time column descending (newest first)
        timeCol.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(timeCol);
        
        return table;
    }

    private HBox createBottomButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        refreshBtn.setPrefWidth(130);
        refreshBtn.setOnAction(e -> refreshData());

        Button exportCSVBtn = new Button("📊 Export CSV");
        exportCSVBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        exportCSVBtn.setPrefWidth(140);
        exportCSVBtn.setOnAction(e -> exportToCSV());

        Button exportReportBtn = new Button("📄 Export Report");
        exportReportBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-size: 14px;");
        exportReportBtn.setPrefWidth(150);
        exportReportBtn.setOnAction(e -> exportToReport());

        Button clearBtn = new Button("🗑️ Clear Old");
        clearBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px;");
        clearBtn.setPrefWidth(120);
        clearBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Clear Events");
            alert.setHeaderText("Clear events older than 7 days?");
            alert.setContentText("This action cannot be undone.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // TODO: Implement clear old events
                    showInfo("Feature coming soon!");
                }
            });
        });

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-font-size: 14px;");
        backBtn.setPrefWidth(120);
        backBtn.setOnAction(e -> stage.close());

        buttonBox.getChildren().addAll(refreshBtn, exportCSVBtn, exportReportBtn, clearBtn, backBtn);
        return buttonBox;
    }

    private void exportToCSV() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save CSV Report");
            fileChooser.setInitialFileName("security_report_" + 
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                List<SecurityEvent> events = idsService.getRecentEvents(24);
                String filename = reportExporter.exportToCSV(events, file.getAbsolutePath());
                showSuccess("CSV report exported successfully!\n\nFile: " + filename);
            }
        } catch (IOException e) {
            showError("Failed to export CSV: " + e.getMessage());
        }
    }

    private void exportToReport() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Security Report");
            fileChooser.setInitialFileName("security_report_" + 
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
            
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                List<SecurityEvent> events = idsService.getRecentEvents(24);
                Map<String, Object> stats = idsService.getSecurityStatistics();
                String filename = reportExporter.exportToTextReport(events, stats, file.getAbsolutePath());
                showSuccess("Security report exported successfully!\n\nFile: " + filename);
            }
        } catch (IOException e) {
            showError("Failed to export report: " + e.getMessage());
        }
    }

    private void refreshData() {
        List<SecurityEvent> events = idsService.getRecentEvents(24);
        eventsTable.getItems().clear();
        
        // Sort by timestamp descending (newest first)
        events.sort((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()));
        
        eventsTable.getItems().addAll(events);

        // Refresh statistics
        Map<String, Object> stats = idsService.getSecurityStatistics();
        System.out.println("📊 Security Statistics: " + stats);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("✅ Operation Successful");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("❌ Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
