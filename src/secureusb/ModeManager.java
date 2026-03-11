package secureusb;

/**
 * Mode Manager - Manages system operating modes
 * 
 * Modes:
 * - PRODUCTION: HID devices only (keyboards, mice)
 * - MAINTENANCE: HID + Storage devices (controlled access)
 * - EMERGENCY: Block ALL USB devices
 */
public class ModeManager {
    
    private SystemMode currentMode;
    private final SecureLogger logger;
    
    public ModeManager(SecureLogger logger) {
        this.currentMode = SystemMode.PRODUCTION; // Default to most restrictive
        this.logger = logger;
    }
    
    /**
     * Set system mode
     */
    public void setMode(SystemMode mode) {
        SystemMode previousMode = currentMode;
        currentMode = mode;
        
        logger.log("MODE CHANGED: " + previousMode + " → " + currentMode);
        logger.log(getModeDescription(currentMode));
    }
    
    /**
     * Get current system mode
     */
    public SystemMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Get mode description
     */
    public String getModeDescription(SystemMode mode) {
        switch (mode) {
            case PRODUCTION:
                return "PRODUCTION MODE: Only HID devices (keyboards, mice) allowed";
            case MAINTENANCE:
                return "MAINTENANCE MODE: HID and Storage devices allowed (whitelisted)";
            case EMERGENCY:
                return "EMERGENCY MODE: ALL USB devices blocked";
            default:
                return "UNKNOWN MODE";
        }
    }
    
    /**
     * Check if device type is allowed in current mode
     */
    public boolean isDeviceTypeAllowed(String deviceType) {
        switch (currentMode) {
            case PRODUCTION:
                return deviceType.equals("HID");
            case MAINTENANCE:
                return deviceType.equals("HID") || deviceType.equals("Storage");
            case EMERGENCY:
                return false; // Block all
            default:
                return false;
        }
    }
    
    /**
     * Get allowed device types for current mode
     */
    public String getAllowedDeviceTypes() {
        switch (currentMode) {
            case PRODUCTION:
                return "HID";
            case MAINTENANCE:
                return "HID, Storage";
            case EMERGENCY:
                return "NONE";
            default:
                return "NONE";
        }
    }
}

/**
 * System operating modes
 */
enum SystemMode {
    PRODUCTION,   // Most restrictive - HID only
    MAINTENANCE,  // Moderate - HID + Storage
    EMERGENCY     // Maximum security - Block all
}