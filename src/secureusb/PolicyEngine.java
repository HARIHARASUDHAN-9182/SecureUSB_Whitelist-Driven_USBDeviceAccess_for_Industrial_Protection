package secureusb;

/**
 * Policy Engine - Core security decision engine
 * Evaluates USB devices against multiple security policies
 * Deterministic rule-based approach (NO ML)
 */
public class PolicyEngine {
    
    private final WhitelistManager whitelistManager;
    private final CompositeDetector compositeDetector;
    private final RateLimiter rateLimiter;
    private final ModeManager modeManager;
    private final SecureLogger logger;
    
    public PolicyEngine(WhitelistManager whitelistManager, 
                       CompositeDetector compositeDetector,
                       RateLimiter rateLimiter,
                       ModeManager modeManager,
                       SecureLogger logger) {
        this.whitelistManager = whitelistManager;
        this.compositeDetector = compositeDetector;
        this.rateLimiter = rateLimiter;
        this.modeManager = modeManager;
        this.logger = logger;
    }
    
    /**
     * Evaluate USB device against security policies
     * Returns true if device is ALLOWED, false if BLOCKED
     * 
     * Policy Evaluation Order:
     * 1. Emergency Mode Check
     * 2. Rate Limiting Check
     * 3. Composite Device Check
     * 4. Mode-Based Type Check
     * 5. Whitelist Check
     */
    public boolean evaluateDevice(USBDevice device) {
        
        // POLICY 1: Emergency Mode - Block ALL devices
        if (modeManager.getCurrentMode() == SystemMode.EMERGENCY) {
            logger.log("POLICY VIOLATION: Emergency mode active - all devices blocked");
            return false;
        }
        
        // POLICY 2: Rate Limiting - Prevent USB flooding attacks
        if (!rateLimiter.allowConnection()) {
            logger.log("POLICY VIOLATION: Rate limit exceeded - potential flooding attack");
            return false;
        }
        
        // POLICY 3: Composite Device Detection - Block potential BadUSB
        if (compositeDetector.isCompositeDevice(device)) {
            if (!whitelistManager.isWhitelisted(device.getVid(), device.getPid())) {
                logger.log("POLICY VIOLATION: Unauthorized composite device - BadUSB risk");
                return false;
            }
        }
        
        // POLICY 4: Mode-Based Device Type Control
        SystemMode currentMode = modeManager.getCurrentMode();
        String deviceType = device.getType();
        
        if (currentMode == SystemMode.PRODUCTION) {
            // Production mode: HID only (keyboards, mice)
            if (!deviceType.equals("HID")) {
                logger.log("POLICY VIOLATION: Production mode allows HID only");
                return false;
            }
        } else if (currentMode == SystemMode.MAINTENANCE) {
            // Maintenance mode: HID + Storage (requires whitelist)
            if (!deviceType.equals("HID") && !deviceType.equals("Storage")) {
                logger.log("POLICY VIOLATION: Maintenance mode allows HID and Storage only");
                return false;
            }
        }
        
        // POLICY 5: Whitelist Check - Final authorization
        boolean isWhitelisted = whitelistManager.isWhitelisted(device.getVid(), device.getPid());
        
        if (!isWhitelisted) {
            logger.log("POLICY VIOLATION: Device not in whitelist");
            return false;
        }
        
        logger.log("POLICY PASSED: All security checks passed");
        return true;
    }
    
    /**
     * Get policy evaluation summary for UI display
     */
    public String getPolicySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Active Security Policies:\n");
        summary.append("1. Emergency Mode Enforcement\n");
        summary.append("2. Rate Limiting (Max ").append(rateLimiter.getMaxConnections())
               .append(" per ").append(rateLimiter.getTimeWindowSeconds()).append("s)\n");
        summary.append("3. Composite Device Detection\n");
        summary.append("4. Mode-Based Type Control (Current: ")
               .append(modeManager.getCurrentMode()).append(")\n");
        summary.append("5. VID:PID Whitelist Validation\n");
        return summary.toString();
    }
}