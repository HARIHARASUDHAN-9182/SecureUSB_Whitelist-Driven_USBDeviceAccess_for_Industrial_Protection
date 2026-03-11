package secureusb;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Rate Limiter - Prevents USB flooding attacks
 * Limits the number of USB connections within a time window
 * Deterministic threshold-based approach (NO ML)
 */
public class RateLimiter {
    
    private final int maxConnections;
    private final long timeWindowMs;
    private final Queue<Long> connectionTimestamps;
    private final SecureLogger logger;
    
    /**
     * Constructor
     * @param maxConnections Maximum allowed connections in time window
     * @param timeWindowSeconds Time window in seconds
     */
    public RateLimiter(int maxConnections, int timeWindowSeconds, SecureLogger logger) {
        this.maxConnections = maxConnections;
        this.timeWindowMs = timeWindowSeconds * 1000L;
        this.connectionTimestamps = new ConcurrentLinkedQueue<>();
        this.logger = logger;
    }
    
    /**
     * Check if a new connection should be allowed
     * Returns true if within rate limit, false if exceeded
     */
    public synchronized boolean allowConnection() {
        long currentTime = System.currentTimeMillis();
        
        // Remove timestamps outside the time window
        while (!connectionTimestamps.isEmpty()) {
            Long oldestTimestamp = connectionTimestamps.peek();
            if (currentTime - oldestTimestamp > timeWindowMs) {
                connectionTimestamps.poll();
            } else {
                break;
            }
        }
        
        // Check if rate limit exceeded
        if (connectionTimestamps.size() >= maxConnections) {
            logger.log("RATE LIMIT EXCEEDED: " + connectionTimestamps.size() + 
                      " connections in last " + (timeWindowMs / 1000) + " seconds");
            return false;
        }
        
        // Allow connection and record timestamp
        connectionTimestamps.offer(currentTime);
        return true;
    }
    
    /**
     * Get current connection count in time window
     */
    public int getCurrentConnectionCount() {
        long currentTime = System.currentTimeMillis();
        
        // Remove old timestamps
        while (!connectionTimestamps.isEmpty()) {
            Long oldestTimestamp = connectionTimestamps.peek();
            if (currentTime - oldestTimestamp > timeWindowMs) {
                connectionTimestamps.poll();
            } else {
                break;
            }
        }
        
        return connectionTimestamps.size();
    }
    
    /**
     * Reset rate limiter
     */
    public void reset() {
        connectionTimestamps.clear();
        logger.log("Rate limiter reset");
    }
    
    /**
     * Get maximum allowed connections
     */
    public int getMaxConnections() {
        return maxConnections;
    }
    
    /**
     * Get time window in seconds
     */
    public int getTimeWindowSeconds() {
        return (int)(timeWindowMs / 1000);
    }
    
    /**
     * Check if currently rate limited
     */
    public boolean isRateLimited() {
        return getCurrentConnectionCount() >= maxConnections;
    }
}