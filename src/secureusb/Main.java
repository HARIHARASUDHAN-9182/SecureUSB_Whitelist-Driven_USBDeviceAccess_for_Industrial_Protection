package secureusb;

import javafx.application.Application;

/**
 * SecureUSB: Whitelist-Driven USB Device Access Control
 * Entry point for the application
 * 
 * @author SecureUSB Development Team
 * @version 1.0
 */
public class Main {
    
    public static void main(String[] args) {
        // Initialize database and configuration
        System.out.println("===========================================");
        System.out.println("  SecureUSB - Industrial USB Protection");
        System.out.println("  Version 1.0");
        System.out.println("===========================================\n");
        
        // Launch JavaFX GUI
        Application.launch(SecureUSBUI.class, args);
    }
}