package secureusb;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Secure Logger - Thread-safe logging system
 * Logs to both console and file with timestamps
 */
public class SecureLogger {
    
    private static final String LOG_FILE = "secureusb.log";
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final List<String> logMessages;
    private final List<LogListener> listeners;
    private PrintWriter fileWriter;
    
    public SecureLogger() {
        logMessages = new CopyOnWriteArrayList<>();
        listeners = new CopyOnWriteArrayList<>();
        initializeFileWriter();
    }
    
    /**
     * Initialize file writer for logging
     */
    private void initializeFileWriter() {
        try {
            fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true), true);
        } catch (IOException e) {
            System.err.println("ERROR: Could not create log file: " + e.getMessage());
        }
    }
    
    /**
     * Log a message with timestamp
     */
    public synchronized void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = "[" + timestamp + "] " + message;
        
        // Store in memory
        logMessages.add(logEntry);
        
        // Write to console
        System.out.println(logEntry);
        
        // Write to file
        if (fileWriter != null) {
            fileWriter.println(logEntry);
        }
        
        // Notify listeners (for UI update)
        notifyListeners(logEntry);
    }
    
    /**
     * Log error message
     */
    public void logError(String message) {
        log("ERROR: " + message);
    }
    
    /**
     * Log warning message
     */
    public void logWarning(String message) {
        log("WARNING: " + message);
    }
    
    /**
     * Log info message
     */
    public void logInfo(String message) {
        log("INFO: " + message);
    }
    
    /**
     * Get all log messages
     */
    public List<String> getLogMessages() {
        return new ArrayList<>(logMessages);
    }
    
    /**
     * Get recent log messages
     */
    public List<String> getRecentLogs(int count) {
        int size = logMessages.size();
        int start = Math.max(0, size - count);
        return new ArrayList<>(logMessages.subList(start, size));
    }
    
    /**
     * Clear all logs
     */
    public synchronized void clearLogs() {
        logMessages.clear();
        log("Logs cleared");
    }
    
    /**
     * Add log listener for UI updates
     */
    public void addListener(LogListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove log listener
     */
    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners of new log entry
     */
    private void notifyListeners(String logEntry) {
        for (LogListener listener : listeners) {
            listener.onNewLog(logEntry);
        }
    }
    
    /**
     * Close logger and file writer
     */
    public void close() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}

/**
 * Interface for log listeners (UI components)
 */
interface LogListener {
    void onNewLog(String logEntry);
}