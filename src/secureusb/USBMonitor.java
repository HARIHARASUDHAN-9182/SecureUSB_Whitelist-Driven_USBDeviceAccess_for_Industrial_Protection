package secureusb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * USB Monitor - Detects USB device connections
 * ENHANCED VERSION with real-time hot-plug detection
 */
public class USBMonitor {
    
    private final PolicyEngine policyEngine;
    private final SecureLogger logger;
    private final AccuracyTracker accuracyTracker;
    private ScheduledExecutorService scheduler;
    private boolean running = false;
    private boolean simulationMode = true;
    
    // Simulated devices for demo/fallback
    private List<USBDevice> simulatedDevices;
    private int simulationIndex = 0;
    
    // NEW: Current device snapshot for hot-plug detection
    private Map<String, USBDevice> currentDeviceMap = new ConcurrentHashMap<>();
    
    // NEW: Listeners for UI updates
    private List<DeviceChangeListener> deviceChangeListeners = new CopyOnWriteArrayList<>();
    
    // NEW: Track consecutive failures for auto-fallback
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES_BEFORE_FALLBACK = 3;
    
    public USBMonitor(PolicyEngine policyEngine, SecureLogger logger, AccuracyTracker accuracyTracker) {
        this.policyEngine = policyEngine;
        this.logger = logger;
        this.accuracyTracker = accuracyTracker;
        initializeSimulatedDevices();
    }
    
    /**
     * Initialize predefined simulated devices for demonstration/fallback
     */
    private void initializeSimulatedDevices() {
        simulatedDevices = new ArrayList<>();
        
        // Authorized devices
        simulatedDevices.add(new USBDevice("046D", "C31C", "Logitech Keyboard", "HID", true));
        simulatedDevices.add(new USBDevice("045E", "0745", "Microsoft Mouse", "HID", true));
        
        // Unauthorized devices
        simulatedDevices.add(new USBDevice("1234", "5678", "Unknown USB Storage", "Storage", false));
        simulatedDevices.add(new USBDevice("ABCD", "EF01", "Suspicious Drive", "Storage", false));
        
        // Composite devices (potential BadUSB)
        simulatedDevices.add(new USBDevice("9999", "8888", "BadUSB Composite", "HID+Storage", false));
        simulatedDevices.add(new USBDevice("AAAA", "BBBB", "Malicious Device", "HID+Storage", false));
    }
    
    /**
     * Start USB monitoring with periodic scanning every 5 seconds
     */
    public void startMonitoring() {
        if (running) {
            logger.log("USB monitoring already running");
            return;
        }
        
        running = true;
        consecutiveFailures = 0; // Reset failure counter
        scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (simulationMode) {
                    detectSimulatedDevices();
                } else {
                    scanAndDetectChanges();
                }
            } catch (Exception e) {
                logger.log("ERROR in monitoring loop: " + e.getMessage());
                e.printStackTrace();
                
                // Count consecutive failures
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_FAILURES_BEFORE_FALLBACK && !simulationMode) {
                    logger.log("⚠️ WARNING: " + consecutiveFailures + " consecutive failures - switching to SIMULATION mode");
                    simulationMode = true;
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        logger.log("✓ USB monitoring started (Mode: " + (simulationMode ? "SIMULATION" : "REAL-TIME WINDOWS") + ")");
        logger.log("✓ Periodic scanning: Every 5 seconds");
    }
    
    /**
     * Stop USB monitoring immediately
     */
    public void stopMonitoring() {
        if (!running) {
            return;
        }
        
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        currentDeviceMap.clear();
        logger.log("✓ USB monitoring stopped");
    }
    
    /**
     * Detect simulated USB devices (for demo/fallback)
     */
    private void detectSimulatedDevices() {
        if (simulatedDevices.isEmpty()) {
            return;
        }
        
        // Show all simulated devices at once (not cycling)
        Map<String, USBDevice> simulatedMap = new HashMap<>();
        for (USBDevice device : simulatedDevices) {
            simulatedMap.put(device.getIdentifier(), device);
        }
        
        // Detect changes
        for (String deviceId : simulatedMap.keySet()) {
            if (!currentDeviceMap.containsKey(deviceId)) {
                USBDevice newDevice = simulatedMap.get(deviceId);
                handleDeviceAdded(newDevice);
            }
        }
        
        currentDeviceMap = new ConcurrentHashMap<>(simulatedMap);
        notifyDeviceListChanged(new ArrayList<>(currentDeviceMap.values()));
    }
    
    /**
     * NEW: Scan for USB devices and detect hot-plug/unplug changes
     */
    private void scanAndDetectChanges() {
        logger.log("🔍 Starting USB scan...");
        
        // Get current USB devices from Windows
        List<USBDevice> currentScan = detectWindowsDevices();
        
        logger.log("📊 Scan result: Found " + currentScan.size() + " USB device(s)");
        
        // If scan returned devices, reset failure counter
        if (!currentScan.isEmpty()) {
            consecutiveFailures = 0;
        }
        
        // Create map for comparison
        Map<String, USBDevice> newDeviceMap = new HashMap<>();
        for (USBDevice device : currentScan) {
            newDeviceMap.put(device.getIdentifier(), device);
            logger.log("  → " + device.getName() + " (" + device.getIdentifier() + ")");
        }
        
        // Detect newly added devices (hot-plug)
        for (String deviceId : newDeviceMap.keySet()) {
            if (!currentDeviceMap.containsKey(deviceId)) {
                USBDevice newDevice = newDeviceMap.get(deviceId);
                handleDeviceAdded(newDevice);
            }
        }
        
        // Detect removed devices (unplug)
        for (String deviceId : currentDeviceMap.keySet()) {
            if (!newDeviceMap.containsKey(deviceId)) {
                USBDevice removedDevice = currentDeviceMap.get(deviceId);
                handleDeviceRemoved(removedDevice);
            }
        }
        
        // Update current snapshot
        currentDeviceMap = new ConcurrentHashMap<>(newDeviceMap);
        
        // Notify UI to refresh device list
        notifyDeviceListChanged(new ArrayList<>(currentDeviceMap.values()));
    }
    
    /**
     * NEW: Handle device addition (hot-plug)
     */
    private void handleDeviceAdded(USBDevice device) {
        logger.log("\n╔════════════════════════════════════╗");
        logger.log("║   NEW USB DEVICE CONNECTED         ║");
        logger.log("╚════════════════════════════════════╝");
        
        processDevice(device);
    }
    
    /**
     * NEW: Handle device removal (unplug)
     */
    private void handleDeviceRemoved(USBDevice device) {
        logger.log("\n┌────────────────────────────────────┐");
        logger.log("│   USB DEVICE DISCONNECTED          │");
        logger.log("│   Name: " + device.getName());
        logger.log("│   VID:PID: " + device.getIdentifier());
        logger.log("└────────────────────────────────────┘\n");
    }
    
    /**
     * FIXED: Detect USB devices on Windows using PowerShell
     * Multiple fallback strategies for maximum compatibility
     */
    private List<USBDevice> detectWindowsDevices() {
        List<USBDevice> devices = new ArrayList<>();
        
        // Try Method 1: Get-PnpDevice (Windows 8+)
        devices = tryGetPnpDevice();
        if (!devices.isEmpty()) {
            logger.log("✓ Method 1 (Get-PnpDevice) successful: " + devices.size() + " device(s)");
            return devices;
        }
        
        // Try Method 2: WMI Query
        devices = tryWmiQuery();
        if (!devices.isEmpty()) {
            logger.log("✓ Method 2 (WMI) successful: " + devices.size() + " device(s)");
            return devices;
        }
        
        // Try Method 3: Registry Query
        devices = tryRegistryQuery();
        if (!devices.isEmpty()) {
            logger.log("✓ Method 3 (Registry) successful: " + devices.size() + " device(s)");
            return devices;
        }
        
        logger.log("⚠️ All detection methods failed - no USB devices found");
        logger.log("💡 Suggestions:");
        logger.log("   1. Check if USB devices are actually connected");
        logger.log("   2. Try running as Administrator");
        logger.log("   3. Use Simulation Mode for demonstration");
        
        return devices;
    }
    
    /**
     * Method 1: Get-PnpDevice (most reliable on Windows 10/11)
     */
    private List<USBDevice> tryGetPnpDevice() {
        List<USBDevice> devices = new ArrayList<>();
        
        try {
            logger.log("🔍 Trying Method 1: Get-PnpDevice...");
            
            String psCommand = 
                "Get-PnpDevice -PresentOnly | " +
                "Where-Object { $_.InstanceId -match '^USB' -and $_.Status -eq 'OK' } | " +
                "ForEach-Object { " +
                "  $name = $_.FriendlyName -replace '[|]', '-'; " +
                "  $id = $_.InstanceId; " +
                "  Write-Output \"$name|$id\" " +
                "}";
            
            devices = executePowerShellCommand(psCommand);
            
        } catch (Exception e) {
            logger.log("Method 1 failed: " + e.getMessage());
        }
        
        return devices;
    }
    
    /**
     * Method 2: WMI Query (Windows 7+)
     */
    private List<USBDevice> tryWmiQuery() {
        List<USBDevice> devices = new ArrayList<>();
        
        try {
            logger.log("🔍 Trying Method 2: WMI Query...");
            
            String psCommand = 
                "Get-WmiObject Win32_USBControllerDevice | " +
                "ForEach-Object { " +
                "  $device = [wmi]$_.Dependent; " +
                "  if ($device.DeviceID -match 'USB.*VID_') { " +
                "    $name = $device.Name -replace '[|]', '-'; " +
                "    $id = $device.DeviceID; " +
                "    Write-Output \"$name|$id\" " +
                "  } " +
                "}";
            
            devices = executePowerShellCommand(psCommand);
            
        } catch (Exception e) {
            logger.log("Method 2 failed: " + e.getMessage());
        }
        
        return devices;
    }
    
    /**
     * Method 3: Registry Query (most compatible but requires admin)
     */
    private List<USBDevice> tryRegistryQuery() {
        List<USBDevice> devices = new ArrayList<>();
        
        try {
            logger.log("🔍 Trying Method 3: Registry Query...");
            
            String psCommand = 
                "Get-ItemProperty 'HKLM:\\SYSTEM\\CurrentControlSet\\Enum\\USB\\*\\*' -ErrorAction SilentlyContinue | " +
                "Where-Object { $_.FriendlyName -ne $null } | " +
                "ForEach-Object { " +
                "  $name = $_.FriendlyName -replace '[|]', '-'; " +
                "  $id = $_.PSChildName; " +
                "  if ($_.PSParentPath -match 'VID_[0-9A-F]{4}&PID_[0-9A-F]{4}') { " +
                "    $parent = Split-Path $_.PSParentPath -Leaf; " +
                "    Write-Output \"$name|USB\\$parent\\$id\" " +
                "  } " +
                "}";
            
            devices = executePowerShellCommand(psCommand);
            
        } catch (Exception e) {
            logger.log("Method 3 failed: " + e.getMessage());
        }
        
        return devices;
    }
    
    /**
     * Execute PowerShell command and parse output
     */
    private List<USBDevice> executePowerShellCommand(String psCommand) throws Exception {
        List<USBDevice> devices = new ArrayList<>();
        
        ProcessBuilder processBuilder = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NonInteractive",
            "-ExecutionPolicy", "Bypass",
            "-Command", psCommand
        );
        
        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        
        String line;
        int deviceCount = 0;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.isEmpty() || line.length() < 5) {
                continue;
            }
            
            logger.log("📝 PowerShell output: " + line);
            
            // Parse format: "DeviceName|USB\VID_XXXX&PID_YYYY\..."
            String[] parts = line.split("\\|", 2);
            
            if (parts.length == 2) {
                String friendlyName = parts[0].trim();
                String instanceId = parts[1].trim();
                
                if (friendlyName.isEmpty() || instanceId.isEmpty()) {
                    continue;
                }
                
                String vid = extractVID(instanceId);
                String pid = extractPID(instanceId);
                
                if (!vid.equals("0000") && !pid.equals("0000")) {
                    String deviceType = classifyDeviceType(friendlyName, instanceId);
                    boolean expectedAllowed = deviceType.equals("HID");
                    
                    USBDevice device = new USBDevice(vid, pid, friendlyName, deviceType, expectedAllowed);
                    devices.add(device);
                    deviceCount++;
                    
                    logger.log("✓ Parsed device: " + friendlyName + " (" + vid + ":" + pid + ")");
                }
            }
        }
        
        // Check for errors
        StringBuilder errors = new StringBuilder();
        String errorLine;
        while ((errorLine = errorReader.readLine()) != null) {
            String err = errorLine.trim();
            if (!err.isEmpty() && !err.contains("ParserError")) {
                errors.append(err).append(" ");
            }
        }
        
        if (errors.length() > 0) {
            logger.log("⚠️ PowerShell stderr: " + errors.toString().trim());
        }
        
        int exitCode = process.waitFor();
        reader.close();
        errorReader.close();
        
        if (deviceCount > 0) {
            logger.log("✓ Found " + deviceCount + " USB device(s)");
        }
        
        return devices;
    }
    
    /**
     * Extract VID from InstanceId
     */
    private String extractVID(String instanceId) {
        try {
            Pattern pattern = Pattern.compile("VID_([0-9A-Fa-f]{4})", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(instanceId);
            
            if (matcher.find()) {
                return matcher.group(1).toUpperCase();
            }
        } catch (Exception e) {
            logger.log("ERROR extracting VID from: " + instanceId);
        }
        
        return "0000";
    }
    
    /**
     * Extract PID from InstanceId
     */
    private String extractPID(String instanceId) {
        try {
            Pattern pattern = Pattern.compile("PID_([0-9A-Fa-f]{4})", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(instanceId);
            
            if (matcher.find()) {
                return matcher.group(1).toUpperCase();
            }
        } catch (Exception e) {
            logger.log("ERROR extracting PID from: " + instanceId);
        }
        
        return "0000";
    }
    
    /**
     * Classify device type
     */
    private String classifyDeviceType(String friendlyName, String instanceId) {
        String lowerName = friendlyName.toLowerCase();
        String lowerInstance = instanceId.toLowerCase();
        
        if (lowerName.contains("keyboard") || lowerName.contains("mouse") || 
            lowerName.contains("hid") || lowerName.contains("pointing")) {
            return "HID";
        }
        
        if (lowerName.contains("storage") || lowerName.contains("flash") || 
            lowerName.contains("drive") || lowerName.contains("disk") ||
            lowerName.contains("mass storage") || lowerName.contains("thumb")) {
            return "Storage";
        }
        
        if (lowerName.contains("network") || lowerName.contains("ethernet") || 
            lowerName.contains("wifi") || lowerName.contains("wireless")) {
            return "Network";
        }
        
        if (lowerInstance.contains("&mi_")) {
            if (lowerName.contains("keyboard") || lowerName.contains("mouse")) {
                return "HID";
            } else if (lowerName.contains("storage")) {
                return "HID+Storage";
            }
        }
        
        return "Unknown";
    }
    
    /**
     * Process detected USB device through policy engine
     */
    private void processDevice(USBDevice device) {
        logger.log("┌─────────────────────────────────────────┐");
        logger.log("│ Device: " + device.getName());
        logger.log("│ VID:PID: " + device.getVid() + ":" + device.getPid());
        logger.log("│ Type: " + device.getType());
        
        // Apply policy engine decision
        boolean allowed = policyEngine.evaluateDevice(device);
        
        // Store decision in device object
        device.setAllowed(allowed);
        
        // Track accuracy
        accuracyTracker.recordDecision(device.isExpectedAllowed(), allowed);
        
        if (allowed) {
            logger.log("│ Decision: ✓ ALLOWED");
            logger.log("│ Reason: Device whitelisted");
        } else {
            logger.log("│ Decision: ✗ BLOCKED");
            logger.log("│ Reason: Security policy violation");
        }
        
        logger.log("│ Accuracy: " + String.format("%.2f%%", accuracyTracker.getAccuracy()));
        logger.log("└─────────────────────────────────────────┘\n");
    }
    
    /**
     * NEW: Register listener for device list changes
     */
    public void addDeviceChangeListener(DeviceChangeListener listener) {
        deviceChangeListeners.add(listener);
    }
    
    /**
     * NEW: Remove listener
     */
    public void removeDeviceChangeListener(DeviceChangeListener listener) {
        deviceChangeListeners.remove(listener);
    }
    
    /**
     * NEW: Notify all listeners of device list update
     */
    private void notifyDeviceListChanged(List<USBDevice> devices) {
        logger.log("🔔 Notifying UI: " + devices.size() + " device(s)");
        
        for (DeviceChangeListener listener : deviceChangeListeners) {
            try {
                listener.onDeviceListChanged(devices);
            } catch (Exception e) {
                logger.log("ERROR notifying listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * NEW: Get current connected devices
     */
    public List<USBDevice> getCurrentDevices() {
        return new ArrayList<>(currentDeviceMap.values());
    }
    
    /**
     * NEW: Force immediate scan (for debug button)
     */
    public void forceScan() {
        if (!running) {
            logger.log("⚠️ Cannot scan - monitoring is not running");
            return;
        }
        
        logger.log("=== MANUAL SCAN TRIGGERED ===");
        
        if (simulationMode) {
            detectSimulatedDevices();
        } else {
            scanAndDetectChanges();
        }
    }
    
    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
        currentDeviceMap.clear();
        consecutiveFailures = 0;
        logger.log("✓ Switched to " + (simulationMode ? "SIMULATION" : "REAL-TIME WINDOWS") + " mode");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public boolean isSimulationMode() {
        return simulationMode;
    }
    
    public List<USBDevice> getSimulatedDevices() {
        return new ArrayList<>(simulatedDevices);
    }
}

/**
 * NEW: Interface for device change notifications
 */
interface DeviceChangeListener {
    void onDeviceListChanged(List<USBDevice> devices);
}

/**
 * USB Device representation
 */
class USBDevice {
    private String vid;
    private String pid;
    private String name;
    private String type;
    private boolean expectedAllowed;
    private boolean allowed;
    
    public USBDevice(String vid, String pid, String name, String type, boolean expectedAllowed) {
        this.vid = vid;
        this.pid = pid;
        this.name = name;
        this.type = type;
        this.expectedAllowed = expectedAllowed;
        this.allowed = false;
    }
    
    public String getVid() { return vid; }
    public String getPid() { return pid; }
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isExpectedAllowed() { return expectedAllowed; }
    
    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }
    
    public String getIdentifier() {
        return vid + ":" + pid;
    }
    
    public String getDisplayString() {
        String status = allowed ? "✓" : "✗";
        return status + " " + name + " (" + vid + ":" + pid + ") [" + type + "]";
    }
}