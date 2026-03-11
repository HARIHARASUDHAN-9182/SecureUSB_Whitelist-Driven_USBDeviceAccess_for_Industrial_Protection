package secureusb;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

/**
 * SecureUSB JavaFX User Interface
 * ENHANCED VERSION with real-time device updates and whitelist viewer
 */
public class SecureUSBUI extends Application implements LogListener, DeviceChangeListener {
    
    // Backend components
    private SecureLogger logger;
    private WhitelistManager whitelistManager;
    private CompositeDetector compositeDetector;
    private RateLimiter rateLimiter;
    private ModeManager modeManager;
    private PolicyEngine policyEngine;
    private AccuracyTracker accuracyTracker;
    private USBMonitor usbMonitor;
    
    // UI Components
    private Label statusLabel;
    private Label modeLabel;
    private Label deviceCountLabel; // FIXED: Store reference
    private Label accuracyLabel;
    private TextArea logArea;
    private ListView<String> deviceListView;
    private Button startButton;
    private Button stopButton;
    private ToggleGroup modeGroup;
    private ObservableList<String> deviceList;
    
    @Override
    public void start(Stage primaryStage) {
        initializeBackend();
        
        primaryStage.setTitle("SecureUSB - Industrial USB Protection System");
        
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #1e1e1e;");
        
        mainLayout.setTop(createHeader());
        mainLayout.setCenter(createCenterContent());
        mainLayout.setRight(createControlPanel());
        mainLayout.setBottom(createStatusBar());
        
        Scene scene = new Scene(mainLayout, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();
        
        logger.log("SecureUSB UI initialized");
    }
    
    /**
     * Initialize backend components
     */
    private void initializeBackend() {
        logger = new SecureLogger();
        logger.addListener(this);
        
        whitelistManager = new WhitelistManager(logger);
        compositeDetector = new CompositeDetector(logger);
        rateLimiter = new RateLimiter(5, 10, logger);
        modeManager = new ModeManager(logger);
        accuracyTracker = new AccuracyTracker(logger);
        
        policyEngine = new PolicyEngine(whitelistManager, compositeDetector, 
                                       rateLimiter, modeManager, logger);
        
        usbMonitor = new USBMonitor(policyEngine, logger, accuracyTracker);
        
        // FIXED: Register for device change notifications
        usbMonitor.addDeviceChangeListener(this);
    }
    
    /**
     * Create header section
     */
    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: #2d2d30;");
        
        Label title = new Label("SecureUSB");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#00d4ff"));
        
        Label subtitle = new Label("Whitelist-Driven USB Device Access Control");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#cccccc"));
        
        header.getChildren().addAll(title, subtitle);
        return header;
    }
    
    /**
     * Create center content (device list and logs)
     */
    private HBox createCenterContent() {
        HBox centerContent = new HBox(10);
        centerContent.setPadding(new Insets(20));
        
        VBox devicePanel = createDevicePanel();
        VBox logPanel = createLogPanel();
        
        centerContent.getChildren().addAll(devicePanel, logPanel);
        HBox.setHgrow(devicePanel, Priority.ALWAYS);
        HBox.setHgrow(logPanel, Priority.ALWAYS);
        
        return centerContent;
    }
    
    /**
     * Create device list panel
     * ENHANCED: Now updates automatically with real-time device changes
     */
    private VBox createDevicePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #252526; -fx-border-color: #3e3e42; -fx-border-width: 1;");
        
        Label heading = new Label("Connected USB Devices");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        heading.setTextFill(Color.web("#cccccc"));
        
        deviceList = FXCollections.observableArrayList();
        deviceListView = new ListView<>(deviceList);
        deviceListView.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #cccccc;");
        VBox.setVgrow(deviceListView, Priority.ALWAYS);
        
        // Add/Remove buttons
        HBox buttonBox = new HBox(10);
        Button addButton = createStyledButton("Add to Whitelist", "#4caf50");
        Button removeButton = createStyledButton("Remove from Whitelist", "#f44336");
        
        addButton.setOnAction(e -> addSelectedDeviceToWhitelist());
        removeButton.setOnAction(e -> removeSelectedDeviceFromWhitelist());
        
        buttonBox.getChildren().addAll(addButton, removeButton);
        
        panel.getChildren().addAll(heading, deviceListView, buttonBox);

        Button resetAccuracyButton = createStyledButton("Reset Accuracy", "#607d8b");
resetAccuracyButton.setOnAction(e -> {
    accuracyTracker.reset();
    logger.log("✓ Accuracy tracker reset");
    // showInfo("Reset Complete", "Accuracy has been reset to 100%");
});

panel.getChildren().add(resetAccuracyButton);
        
        return panel;
    }
    
    /**
     * Create log panel
     */
    private VBox createLogPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #252526; -fx-border-color: #3e3e42; -fx-border-width: 1;");
        
        Label heading = new Label("System Logs");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        heading.setTextFill(Color.web("#cccccc"));
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #00ff00; -fx-font-family: 'Courier New'; -fx-font-size: 12;");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        
        Button clearButton = createStyledButton("Clear Logs", "#ff9800");
        clearButton.setOnAction(e -> {
            logArea.clear();
            logger.clearLogs();
        });
        
        panel.getChildren().addAll(heading, logArea, clearButton);
        return panel;
    }
    
    /**
     * Create control panel
     * ENHANCED: Added "View Whitelist" button
     */
    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: #2d2d30;");
        
        // Monitoring controls
        Label monitorHeading = new Label("Monitoring Control");
        monitorHeading.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        monitorHeading.setTextFill(Color.web("#cccccc"));
        
        startButton = createStyledButton("Start Monitoring", "#4caf50");
        stopButton = createStyledButton("Stop Monitoring", "#f44336");
        stopButton.setDisable(true);
        
        startButton.setOnAction(e -> startMonitoring());
        stopButton.setOnAction(e -> stopMonitoring());
        
        // Mode selection
        Label modeHeading = new Label("System Mode");
        modeHeading.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        modeHeading.setTextFill(Color.web("#cccccc"));
        
        modeGroup = new ToggleGroup();
        RadioButton productionRadio = createModeRadio("Production (HID Only)", SystemMode.PRODUCTION);
        RadioButton maintenanceRadio = createModeRadio("Maintenance (HID+Storage)", SystemMode.MAINTENANCE);
        RadioButton emergencyRadio = createModeRadio("Emergency (Block All)", SystemMode.EMERGENCY);
        
        productionRadio.setSelected(true);
        
        // Accuracy display
        Label accuracyHeading = new Label("System Accuracy");
        accuracyHeading.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        accuracyHeading.setTextFill(Color.web("#cccccc"));
        
        accuracyLabel = new Label("100.00%");
        accuracyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        accuracyLabel.setTextFill(Color.web("#00ff00"));
        
        // Statistics and Whitelist buttons
        Button statsButton = createStyledButton("View Statistics", "#2196f3");
        statsButton.setOnAction(e -> showStatistics());
        
        // NEW: View Whitelist button
        Button whitelistButton = createStyledButton("View Whitelisted Devices", "#9c27b0");
        whitelistButton.setOnAction(e -> showWhitelistDialog());
        
        // NEW: Add mode toggle for demo
        CheckBox simulationCheckBox = new CheckBox("Use Simulation Mode (Demo)");
        simulationCheckBox.setTextFill(Color.web("#cccccc"));
        simulationCheckBox.setSelected(false);
        simulationCheckBox.setOnAction(e -> {
            boolean simMode = simulationCheckBox.isSelected();
            usbMonitor.setSimulationMode(simMode);
            if (simMode) {
                showInfo("Simulation Mode", "Now using demo devices for presentation.\nDevices will appear automatically.");
            }
        });
        
        panel.getChildren().addAll(
            monitorHeading, startButton, stopButton,
            simulationCheckBox, // Add checkbox here
            new Separator(),
            modeHeading, productionRadio, maintenanceRadio, emergencyRadio,
            new Separator(),
            accuracyHeading, accuracyLabel,
            statsButton,
            whitelistButton
        );
        
        return panel;
    }
    
    /**
     * Create status bar
     * FIXED: Properly store device count label reference
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #007acc;");
        
        statusLabel = new Label("Status: Ready");
        statusLabel.setTextFill(Color.WHITE);
        
        modeLabel = new Label("Mode: PRODUCTION");
        modeLabel.setTextFill(Color.WHITE);
        
        // FIXED: Store reference to device count label
        deviceCountLabel = new Label("Connected Devices: 0");
        deviceCountLabel.setTextFill(Color.WHITE);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        statusBar.getChildren().addAll(statusLabel, spacer, modeLabel, deviceCountLabel);
        
        return statusBar;
    }
    
    /**
     * Create styled button
     */
    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
        button.setPrefWidth(200);
        return button;
    }
    
    /**
     * Create mode radio button
     */
    private RadioButton createModeRadio(String text, SystemMode mode) {
        RadioButton radio = new RadioButton(text);
        radio.setToggleGroup(modeGroup);
        radio.setTextFill(Color.web("#cccccc"));
        radio.setOnAction(e -> {
            modeManager.setMode(mode);
            modeLabel.setText("Mode: " + mode);
        });
        return radio;
    }
    
    /**
     * Start USB monitoring
     */
    private void startMonitoring() {
        usbMonitor.startMonitoring();
        startButton.setDisable(true);
        stopButton.setDisable(false);
        statusLabel.setText("Status: Monitoring Active (Scan: Every 5s)");
        statusLabel.setTextFill(Color.web("#00ff00"));
        
        // Update accuracy periodically
        new Thread(() -> {
            while (usbMonitor.isRunning()) {
                Platform.runLater(() -> {
                    double accuracy = accuracyTracker.getAccuracy();
                    accuracyLabel.setText(String.format("%.2f%%", accuracy));
                    
                    if (accuracy >= 98.0) {
                        accuracyLabel.setTextFill(Color.web("#00ff00"));
                    } else if (accuracy >= 95.0) {
                        accuracyLabel.setTextFill(Color.web("#ffff00"));
                    } else {
                        accuracyLabel.setTextFill(Color.web("#ff0000"));
                    }
                });
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    
    /**
     * Stop USB monitoring
     */
    private void stopMonitoring() {
        usbMonitor.stopMonitoring();
        startButton.setDisable(false);
        stopButton.setDisable(true);
        statusLabel.setText("Status: Monitoring Stopped");
        statusLabel.setTextFill(Color.web("#ff9800"));
        
        // Clear device list
        deviceList.clear();
        deviceCountLabel.setText("Connected Devices: 0");
    }
    
    /**
     * NEW: Add selected device to whitelist
     * This implements the whitelist workflow
     */
    private void addSelectedDeviceToWhitelist() {
        String selected = deviceListView.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showError("Please select a device from the list");
            return;
        }
        
        // Parse device info from display string
        // Format: "✓/✗ DeviceName (VID:PID) [Type]"
        try {
            String vidPid = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
            String[] parts = vidPid.split(":");
            String vid = parts[0].trim();
            String pid = parts[1].trim();
            
            String name = selected.substring(selected.indexOf(" ") + 1, selected.indexOf("(")).trim();
            String type = selected.substring(selected.indexOf("[") + 1, selected.indexOf("]"));
            
            // Add to whitelist
            boolean success = whitelistManager.addDevice(vid, pid, name, type);
            
            if (success) {
                showInfo("Device Added to Whitelist", 
                        "Device: " + name + "\nVID:PID: " + vid + ":" + pid + "\n\nStatus: Now Authorized");
                
                // Refresh device list to show updated status
                refreshDeviceList();
            } else {
                showInfo("Already Whitelisted", "This device is already in the whitelist.");
            }
            
        } catch (Exception e) {
            showError("Failed to parse device information: " + e.getMessage());
        }
    }
    
    /**
     * NEW: Remove selected device from whitelist
     */
    private void removeSelectedDeviceFromWhitelist() {
        String selected = deviceListView.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showError("Please select a device from the list");
            return;
        }
        
        try {
            String vidPid = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
            String[] parts = vidPid.split(":");
            String vid = parts[0].trim();
            String pid = parts[1].trim();
            
            boolean success = whitelistManager.removeDevice(vid, pid);
            
            if (success) {
                showInfo("Device Removed", "Device has been removed from whitelist.\nVID:PID: " + vid + ":" + pid);
                refreshDeviceList();
            } else {
                showInfo("Not Found", "Device is not in the whitelist.");
            }
            
        } catch (Exception e) {
            showError("Failed to parse device information: " + e.getMessage());
        }
    }
    
    /**
     * NEW: Refresh device list (re-evaluate with updated whitelist)
     */
    private void refreshDeviceList() {
        List<USBDevice> devices = usbMonitor.getCurrentDevices();
        onDeviceListChanged(devices);
    }
    
    /**
     * NEW: Show whitelist dialog
     * Displays all whitelisted devices from database
     */
    private void showWhitelistDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Whitelisted Devices");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #2d2d30;");
        
        Label title = new Label("Authorized USB Devices");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#00d4ff"));
        
        // Get whitelist from database
        List<WhitelistEntry> whitelistEntries = whitelistManager.getAllDevices();
        
        if (whitelistEntries.isEmpty()) {
            Label emptyLabel = new Label("No devices in whitelist");
            emptyLabel.setTextFill(Color.web("#cccccc"));
            content.getChildren().addAll(title, emptyLabel);
        } else {
            TableView<WhitelistEntry> table = new TableView<>();
            table.setStyle("-fx-background-color: #1e1e1e;");
            
            TableColumn<WhitelistEntry, String> nameCol = new TableColumn<>("Device Name");
            nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDeviceName()));
            nameCol.setPrefWidth(250);
            
            TableColumn<WhitelistEntry, String> vidCol = new TableColumn<>("VID");
            vidCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getVid()));
            vidCol.setPrefWidth(80);
            
            TableColumn<WhitelistEntry, String> pidCol = new TableColumn<>("PID");
            pidCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPid()));
            pidCol.setPrefWidth(80);
            
            TableColumn<WhitelistEntry, String> typeCol = new TableColumn<>("Type");
            typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDeviceType()));
            typeCol.setPrefWidth(100);
            
            TableColumn<WhitelistEntry, String> dateCol = new TableColumn<>("Date Added");
            dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAddedDate()));
            dateCol.setPrefWidth(150);
            
            table.getColumns().addAll(nameCol, vidCol, pidCol, typeCol, dateCol);
            table.getItems().addAll(whitelistEntries);
            
            Label countLabel = new Label("Total Whitelisted: " + whitelistEntries.size());
            countLabel.setTextFill(Color.web("#cccccc"));
            
            content.getChildren().addAll(title, table, countLabel);
        }
        
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #007acc; -fx-text-fill: white;");
        closeButton.setOnAction(e -> dialog.close());
        
        content.getChildren().add(closeButton);
        
        Scene dialogScene = new Scene(content, 700, 500);
        dialog.setScene(dialogScene);
        dialog.show();
    }
    
    /**
     * Show statistics dialog
     */
    private void showStatistics() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Statistics");
        alert.setHeaderText("Security Enforcement Statistics");
        alert.setContentText(accuracyTracker.getDetailedStats());
        alert.showAndWait();
    }
    
    /**
     * Show error dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Handle new log entries from backend
     */
    @Override
    public void onNewLog(String logEntry) {
        Platform.runLater(() -> {
            logArea.appendText(logEntry + "\n");
            // Auto-scroll to bottom
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    /**
     * FIXED: Handle device list changes from USB monitor
     * This implements real-time device list updates
     */
    @Override
    public void onDeviceListChanged(List<USBDevice> devices) {
        Platform.runLater(() -> {
            // Clear current list
            deviceList.clear();
            
            // Add all devices with proper formatting
            for (USBDevice device : devices) {
                deviceList.add(device.getDisplayString());
            }
            
            // FIXED: Update device count label
            if (deviceCountLabel != null) {
                deviceCountLabel.setText("Connected Devices: " + devices.size());
            }
            
            // Log the update for debugging
            logger.log("UI Updated: " + devices.size() + " device(s) in list");
        });
    }
    
    /**
     * Shutdown application
     */
    private void shutdown() {
        if (usbMonitor.isRunning()) {
            usbMonitor.stopMonitoring();
        }
        whitelistManager.close();
        logger.close();
        Platform.exit();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}