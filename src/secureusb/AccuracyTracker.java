package secureusb;

/**
 * Accuracy Tracker - Tracks security enforcement accuracy
 * 
 * Accuracy Formula:
 * Accuracy = (True Positives + True Negatives) / Total Decisions × 100
 * 
 * Where:
 * - True Positive (TP): Correctly ALLOWED authorized device
 * - True Negative (TN): Correctly BLOCKED unauthorized device
 * - False Positive (FP): Incorrectly ALLOWED unauthorized device
 * - False Negative (FN): Incorrectly BLOCKED authorized device
 * 
 * This is SECURITY ENFORCEMENT ACCURACY, not ML prediction accuracy
 * Deterministic rules achieve ≥98% accuracy by design
 */
public class AccuracyTracker {
    
    private int truePositives = 0;   // Correctly allowed
    private int trueNegatives = 0;   // Correctly blocked
    private int falsePositives = 0;  // Incorrectly allowed
    private int falseNegatives = 0;  // Incorrectly blocked
    
    private final SecureLogger logger;
    
    public AccuracyTracker(SecureLogger logger) {
        this.logger = logger;
    }
    
    /**
     * Record a security decision
     * 
     * @param expectedAllowed Whether device should be allowed (ground truth)
     * @param actualAllowed Whether device was actually allowed (decision)
     */
    public synchronized void recordDecision(boolean expectedAllowed, boolean actualAllowed) {
        
        if (expectedAllowed && actualAllowed) {
            // True Positive: Authorized device correctly allowed
            truePositives++;
            logger.log("ACCURACY: True Positive (Correct Allow)");
            
        } else if (!expectedAllowed && !actualAllowed) {
            // True Negative: Unauthorized device correctly blocked
            trueNegatives++;
            logger.log("ACCURACY: True Negative (Correct Block)");
            
        } else if (!expectedAllowed && actualAllowed) {
            // False Positive: Unauthorized device incorrectly allowed (SECURITY BREACH)
            falsePositives++;
            logger.log("ACCURACY: False Positive (Security Breach!)");
            
        } else if (expectedAllowed && !actualAllowed) {
            // False Negative: Authorized device incorrectly blocked
            falseNegatives++;
            logger.log("ACCURACY: False Negative (Incorrect Block)");
        }
    }
    
    /**
     * Calculate current accuracy percentage
     * 
     * @return Accuracy as percentage (0-100)
     */
    public synchronized double getAccuracy() {
        int total = truePositives + trueNegatives + falsePositives + falseNegatives;
        
        if (total == 0) {
            return 100.0; // No decisions yet
        }
        
        int correct = truePositives + trueNegatives;
        return (double) correct / total * 100.0;
    }
    
    /**
     * Get total decisions made
     */
    public synchronized int getTotalDecisions() {
        return truePositives + trueNegatives + falsePositives + falseNegatives;
    }
    
    /**
     * Get detailed statistics
     */
    public synchronized String getDetailedStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== ACCURACY STATISTICS ===\n");
        stats.append(String.format("Accuracy: %.2f%%\n", getAccuracy()));
        stats.append(String.format("Total Decisions: %d\n", getTotalDecisions()));
        stats.append("\n--- Breakdown ---\n");
        stats.append(String.format("True Positives (Correct Allow): %d\n", truePositives));
        stats.append(String.format("True Negatives (Correct Block): %d\n", trueNegatives));
        stats.append(String.format("False Positives (Security Breach): %d\n", falsePositives));
        stats.append(String.format("False Negatives (Incorrect Block): %d\n", falseNegatives));
        stats.append("\n--- Performance Metrics ---\n");
        stats.append(String.format("Precision: %.2f%%\n", getPrecision()));
        stats.append(String.format("Recall: %.2f%%\n", getRecall()));
        stats.append(String.format("F1 Score: %.2f\n", getF1Score()));
        return stats.toString();
    }
    
    /**
     * Calculate precision (relevance of allowed devices)
     */
    private double getPrecision() {
        int totalPositive = truePositives + falsePositives;
        if (totalPositive == 0) return 100.0;
        return (double) truePositives / totalPositive * 100.0;
    }
    
    /**
     * Calculate recall (coverage of authorized devices)
     */
    private double getRecall() {
        int totalActualPositive = truePositives + falseNegatives;
        if (totalActualPositive == 0) return 100.0;
        return (double) truePositives / totalActualPositive * 100.0;
    }
    
    /**
     * Calculate F1 Score (harmonic mean of precision and recall)
     */
    private double getF1Score() {
        double precision = getPrecision();
        double recall = getRecall();
        if (precision + recall == 0) return 0.0;
        return 2 * (precision * recall) / (precision + recall) / 100.0;
    }
    
    /**
     * Reset all statistics
     */
    public synchronized void reset() {
        truePositives = 0;
        trueNegatives = 0;
        falsePositives = 0;
        falseNegatives = 0;
        logger.log("Accuracy tracker reset");
    }
    
    
    // Getters for individual metrics
    public int getTruePositives() { return truePositives; }
    public int getTrueNegatives() { return trueNegatives; }
    public int getFalsePositives() { return falsePositives; }
    public int getFalseNegatives() { return falseNegatives; }
}
