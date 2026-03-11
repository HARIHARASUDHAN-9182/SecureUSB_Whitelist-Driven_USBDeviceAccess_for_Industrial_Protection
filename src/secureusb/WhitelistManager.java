package secureusb;

import java.sql.*;
import java.util.*;

/**
 * Whitelist Manager - Manages authorized USB devices in SQLite database
 * ENHANCED: Added real-time whitelist retrieval for UI display
 */
public class WhitelistManager {
    
    private static final String DB_URL = "jdbc:sqlite:whitelist.db";
    private Connection connection;
    private SecureLogger logger;
    
    public WhitelistManager(SecureLogger logger) {
        this.logger = logger;
        initializeDatabase();
        loadDefaultWhitelist();
    }
    
    /**
     * Initialize SQLite database and create whitelist table
     */
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            
            String createTable = "CREATE TABLE IF NOT EXISTS whitelist (" +
                               "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                               "vid TEXT NOT NULL," +
                               "pid TEXT NOT NULL," +
                               "device_name TEXT NOT NULL," +
                               "device_type TEXT NOT NULL," +
                               "added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                               "UNIQUE(vid, pid))";
            
            Statement stmt = connection.createStatement();
            stmt.execute(createTable);
            stmt.close();
            
            logger.log("Database initialized successfully");
            
        } catch (SQLException e) {
            logger.log("ERROR initializing database: " + e.getMessage());
        }
    }
    
    /**
     * Load default authorized devices into whitelist
     */
    private void loadDefaultWhitelist() {
        try {
            String countQuery = "SELECT COUNT(*) as count FROM whitelist";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(countQuery);
            
            if (rs.next() && rs.getInt("count") == 0) {
                // Add default authorized devices
                addDevice("046D", "C31C", "Logitech Keyboard", "HID");
                addDevice("045E", "0745", "Microsoft Mouse", "HID");
                logger.log("Default whitelist loaded");
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            logger.log("ERROR loading default whitelist: " + e.getMessage());
        }
    }
    
    /**
     * Add device to whitelist
     * ENHANCED: Now returns true if newly added, false if already exists
     */
    public boolean addDevice(String vid, String pid, String name, String type) {
        try {
            String insertSQL = "INSERT OR IGNORE INTO whitelist (vid, pid, device_name, device_type) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(insertSQL);
            pstmt.setString(1, vid.toUpperCase());
            pstmt.setString(2, pid.toUpperCase());
            pstmt.setString(3, name);
            pstmt.setString(4, type);
            
            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();
            
            if (rowsAffected > 0) {
                logger.log("✓ Device added to whitelist: " + name + " (" + vid + ":" + pid + ")");
                return true;
            } else {
                logger.log("ℹ Device already in whitelist: " + vid + ":" + pid);
                return false;
            }
            
        } catch (SQLException e) {
            logger.log("ERROR adding device: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove device from whitelist
     */
    public boolean removeDevice(String vid, String pid) {
        try {
            String deleteSQL = "DELETE FROM whitelist WHERE vid = ? AND pid = ?";
            PreparedStatement pstmt = connection.prepareStatement(deleteSQL);
            pstmt.setString(1, vid.toUpperCase());
            pstmt.setString(2, pid.toUpperCase());
            
            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();
            
            if (rowsAffected > 0) {
                logger.log("✓ Device removed from whitelist: " + vid + ":" + pid);
                return true;
            } else {
                logger.log("ℹ Device not found in whitelist: " + vid + ":" + pid);
                return false;
            }
            
        } catch (SQLException e) {
            logger.log("ERROR removing device: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if device is whitelisted
     */
    public boolean isWhitelisted(String vid, String pid) {
        try {
            String query = "SELECT COUNT(*) as count FROM whitelist WHERE vid = ? AND pid = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, vid.toUpperCase());
            pstmt.setString(2, pid.toUpperCase());
            
            ResultSet rs = pstmt.executeQuery();
            boolean exists = rs.next() && rs.getInt("count") > 0;
            
            rs.close();
            pstmt.close();
            
            return exists;
            
        } catch (SQLException e) {
            logger.log("ERROR checking whitelist: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all whitelisted devices
     * ENHANCED: Now returns ordered by date (newest first)
     */
    public List<WhitelistEntry> getAllDevices() {
        List<WhitelistEntry> devices = new ArrayList<>();
        
        try {
            String query = "SELECT * FROM whitelist ORDER BY added_date DESC";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                WhitelistEntry entry = new WhitelistEntry(
                    rs.getInt("id"),
                    rs.getString("vid"),
                    rs.getString("pid"),
                    rs.getString("device_name"),
                    rs.getString("device_type"),
                    rs.getString("added_date")
                );
                devices.add(entry);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            logger.log("ERROR getting whitelist: " + e.getMessage());
        }
        
        return devices;
    }
    
    /**
     * Get whitelist count
     */
    public int getWhitelistCount() {
        try {
            String query = "SELECT COUNT(*) as count FROM whitelist";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            int count = rs.next() ? rs.getInt("count") : 0;
            
            rs.close();
            stmt.close();
            
            return count;
            
        } catch (SQLException e) {
            logger.log("ERROR getting count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * NEW: Check if device exists by VID:PID and return details
     */
    public WhitelistEntry getDeviceByVidPid(String vid, String pid) {
        try {
            String query = "SELECT * FROM whitelist WHERE vid = ? AND pid = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, vid.toUpperCase());
            pstmt.setString(2, pid.toUpperCase());
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                WhitelistEntry entry = new WhitelistEntry(
                    rs.getInt("id"),
                    rs.getString("vid"),
                    rs.getString("pid"),
                    rs.getString("device_name"),
                    rs.getString("device_type"),
                    rs.getString("added_date")
                );
                
                rs.close();
                pstmt.close();
                return entry;
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            logger.log("ERROR getting device: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.log("Database connection closed");
            }
        } catch (SQLException e) {
            logger.log("ERROR closing database: " + e.getMessage());
        }
    }
}

/**
 * Whitelist Entry representation
 */
class WhitelistEntry {
    private int id;
    private String vid;
    private String pid;
    private String deviceName;
    private String deviceType;
    private String addedDate;
    
    public WhitelistEntry(int id, String vid, String pid, String deviceName, 
                         String deviceType, String addedDate) {
        this.id = id;
        this.vid = vid;
        this.pid = pid;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.addedDate = addedDate;
    }
    
    public int getId() { return id; }
    public String getVid() { return vid; }
    public String getPid() { return pid; }
    public String getDeviceName() { return deviceName; }
    public String getDeviceType() { return deviceType; }
    public String getAddedDate() { return addedDate; }
    
    @Override
    public String toString() {
        return deviceName + " (" + vid + ":" + pid + ") - " + deviceType;
    }
}