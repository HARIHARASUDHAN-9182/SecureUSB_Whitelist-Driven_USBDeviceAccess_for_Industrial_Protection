package secureusb;

/**
 * Composite Device Detector
 * Detects USB devices with multiple interfaces (potential BadUSB attacks)
 * Example: A USB drive that also acts as a keyboard (HID + Storage)
 */
public class CompositeDetector {
    
    private final SecureLogger logger;
    
    public CompositeDetector(SecureLogger logger) {
        this.logger = logger;
    }
    
    /**
     * Check if device is a composite device (multiple USB classes)
     * Composite devices can be used for BadUSB attacks
     * 
     * @param device USB device to check
     * @return true if composite, false otherwise
     */
    public boolean isCompositeDevice(USBDevice device) {
        String deviceType = device.getType();
        
        // Check for multiple device classes in type string
        if (deviceType.contains("+")) {
            logger.log("COMPOSITE DETECTED: Device has multiple interfaces - " + deviceType);
            return true;
        }
        
        // Check for known composite patterns
        if (deviceType.contains("HID") && deviceType.contains("Storage")) {
            logger.log("COMPOSITE DETECTED: HID+Storage combination (BadUSB risk)");
            return true;
        }
        
        if (deviceType.contains("HID") && deviceType.contains("Network")) {
            logger.log("COMPOSITE DETECTED: HID+Network combination (BadUSB risk)");
            return true;
        }
        
        // Additional composite patterns can be added here
        
        return false;
    }
    
    /**
     * Get risk level for composite device
     * 
     * @param device USB device
     * @return risk level (HIGH, MEDIUM, LOW)
     */
    public String getRiskLevel(USBDevice device) {
        if (!isCompositeDevice(device)) {
            return "LOW";
        }
        
        String deviceType = device.getType();
        
        // HID+Storage is highest risk (classic BadUSB)
        if (deviceType.contains("HID") && deviceType.contains("Storage")) {
            return "HIGH";
        }
        
        // HID+Network is high risk (keystroke injection + network exfiltration)
        if (deviceType.contains("HID") && deviceType.contains("Network")) {
            return "HIGH";
        }
        
        // Other combinations are medium risk
        return "MEDIUM";
    }
    
    /**
     * Check if composite device should be blocked
     * Even if composite, whitelisted devices can be allowed
     * 
     * @param device USB device
     * @param isWhitelisted whether device is in whitelist
     * @return true if should be blocked
     */
    public boolean shouldBlockComposite(USBDevice device, boolean isWhitelisted) {
        if (!isCompositeDevice(device)) {
            return false;
        }
        
        // If device is whitelisted, allow it (authorized composite device)
        if (isWhitelisted) {
            logger.log("COMPOSITE ALLOWED: Device is whitelisted");
            return false;
        }
        
        // Block all non-whitelisted composite devices
        logger.log("COMPOSITE BLOCKED: Unauthorized composite device");
        return true;
    }
}