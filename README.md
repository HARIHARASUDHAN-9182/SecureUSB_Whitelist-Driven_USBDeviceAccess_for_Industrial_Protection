# SecureUSB: Whitelist-Driven USB Device Access for Industrial Protection

## Project Overview

**SecureUSB** is a deterministic, rule-based USB security system designed for industrial environments. It provides whitelist-driven access control, composite device detection, rate limiting, and mode-based policy enforcement to protect against BadUSB attacks, unauthorized data exfiltration, and USB-based threats.

**Key Features:**
- ✅ Whitelist-based VID:PID authorization
- ✅ Composite device detection (HID+Storage BadUSB)
- ✅ Rate limiting to prevent USB flooding
- ✅ Mode-based access control (Production/Maintenance/Emergency)
- ✅ Real-time accuracy tracking (≥98%)
- ✅ Professional JavaFX GUI
- ✅ SQLite database for whitelist management
- ✅ Cross-platform (Windows/Linux)

---

## System Requirements

### Software Requirements
- **Java Development Kit (JDK)**: 11 or higher
- **JavaFX SDK**: 11 or higher
- **VS Code**: Latest version
- **SQLite JDBC Driver**: Included in project

### Hardware Requirements
- **OS**: Windows 10/11 or Linux (Ubuntu 18.04+)
- **RAM**: 4GB minimum
- **Storage**: 100MB free space

---

## Installation Guide

### Step 1: Install Java JDK

**Windows:**
1. Download JDK 11+ from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
2. Run installer and add to PATH
3. Verify: `java -version`

**Linux:**
```bash
sudo apt update
sudo apt install openjdk-11-jdk
java -version
```

### Step 2: Install JavaFX

**Option A: Download JavaFX SDK**
1. Download from [openjfx.io](https://openjfx.io/)
2. Extract to `/usr/lib/javafx` (Linux) or `C:\Program Files\Java\javafx` (Windows)

**Option B: Use Maven/Gradle (Recommended)**
JavaFX will be included via classpath configuration.

### Step 3: Install SQLite JDBC Driver

Download `sqlite-jdbc-3.42.0.0.jar` from [GitHub](https://github.com/xerial/sqlite-jdbc) and place in `lib/` folder.

### Step 4: Install VS Code Extensions

Open VS Code and install:
- **Extension Pack for Java** (Microsoft)
- **JavaFX Support** (optional but helpful)

---

## Project Setup in VS Code

### Step 1: Open Project
```bash
cd SecureUSB
code .
```

### Step 2: Configure JavaFX in VS Code

Create `.vscode/settings.json`:
```json
{
    "java.project.sourcePaths": ["src"],
    "java.project.outputPath": "bin",
    "java.project.referencedLibraries": [
        "lib/**/*.jar",
        "/path/to/javafx-sdk-11/lib/*.jar"
    ]
}
```

Create `.vscode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Launch SecureUSB",
            "request": "launch",
            "mainClass": "secureusb.Main",
            "vmArgs": "--module-path /path/to/javafx-sdk-11/lib --add-modules javafx.controls,javafx.fxml"
        }
    ]
}
```

**Replace `/path/to/javafx-sdk-11/lib` with your actual JavaFX path.**

---

## Running the Project

### Method 1: Run from VS Code

1. Open `Main.java`
2. Press **F5** or click **Run** > **Start Debugging**
3. JavaFX GUI will launch

### Method 2: Run from Command Line

**Compile:**
```bash
javac --module-path /path/to/javafx/lib --add-modules javafx.controls -d bin src/secureusb/*.java
```

**Run:**
```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls -cp bin secureusb.Main
```

### Method 3: Create Executable JAR

```bash
jar cfm SecureUSB.jar MANIFEST.MF -C bin .
java --module-path /path/to/javafx/lib --add-modules javafx.controls -jar SecureUSB.jar
```

---

## 5-Minute Demonstration Guide

### Quick Demo Steps

**1. Launch Application**
- Run `Main.java` from VS Code
- JavaFX window opens with dark industrial theme

**2. Start Monitoring**
- Click **"Start Monitoring"** button
- System begins detecting USB devices
- Logs appear in real-time

**3. Observe Simulated Devices**
The system will cycle through predefined devices:
- ✅ **Logitech Keyboard** (046D:C31C) → ALLOWED
- ✅ **Microsoft Mouse** (045E:0745) → ALLOWED
- ❌ **Unknown USB Storage** (1234:5678) → BLOCKED
- ❌ **BadUSB Composite** (9999:8888) → BLOCKED

**4. Check Accuracy**
- View accuracy percentage (should be ≥98%)
- Click **"View Statistics"** for detailed breakdown

**5. Change System Mode**
- Select **"Maintenance"** mode → Allows HID + Storage
- Select **"Emergency"** mode → Blocks ALL devices
- Observe different policy enforcement

**6. Whitelist Management**
- Click **"Add to Whitelist"**
- Enter: `1234:5678:My USB Drive:Storage`
- Device is now authorized

**7. Stop Monitoring**
- Click **"Stop Monitoring"**
- Review logs and statistics

---

## Sample Output Screenshots (Text Description)

### Main Dashboard
```
┌────────────────────────────────────────────────────────┐
│ SecureUSB - Industrial USB Protection System           │
│ Whitelist-Driven USB Device Access Control            │
├────────────────────────────────────────────────────────┤
│                                                        │
│  Connected USB Devices    │    System Logs            │
│  ─────────────────────    │    ──────────             │
│  ✓ Logitech Keyboard      │  [2024-01-18 10:23:45]    │
│  ✓ Microsoft Mouse        │  USB Device Detected      │
│  ✗ Unknown USB Storage    │  VID: 046D PID: C31C      │
│  ✗ BadUSB Composite       │  ✓ ALLOWED               │
│                           │  Accuracy: 98.50%         │
│  [Add to Whitelist]       │                           │
│  [Remove from Whitelist]  │  [Clear Logs]             │
│                           │                           │
├────────────────────────────────────────────────────────┤
│  Monitoring Control       │    System Accuracy        │
│  [Start Monitoring]       │    ┌──────────┐          │
│  [Stop Monitoring]        │    │  98.50%  │          │
│                           │    └──────────┘          │
│  System Mode              │                           │
│  ⦿ Production (HID Only)  │  [View Statistics]        │
│  ○ Maintenance            │                           │
│  ○ Emergency              │                           │
└────────────────────────────────────────────────────────┘
│ Status: Monitoring Active        Mode: PRODUCTION     │
└────────────────────────────────────────────────────────┘
```

### Statistics Dialog
```
=== ACCURACY STATISTICS ===
Accuracy: 98.67%
Total Decisions: 75

--- Breakdown ---
True Positives (Correct Allow): 38
True Negatives (Correct Block): 36
False Positives (Security Breach): 0
False Negatives (Incorrect Block): 1

--- Performance Metrics ---
Precision: 100.00%
Recall: 97.44%
F1 Score: 0.99
```

---

## How Accuracy is Calculated

### Accuracy Formula
```
Accuracy = (True Positives + True Negatives) / Total Decisions × 100
```

### Components
- **True Positive (TP)**: Authorized device correctly allowed
- **True Negative (TN)**: Unauthorized device correctly blocked
- **False Positive (FP)**: Unauthorized device incorrectly allowed (BREACH)
- **False Negative (FN)**: Authorized device incorrectly blocked

### Why ≥98% Accuracy?

SecureUSB achieves high accuracy through **deterministic rule-based policies**:

1. **Whitelist Matching**: Exact VID:PID comparison (100% reliable)
2. **Composite Detection**: Pattern-based type analysis
3. **Rate Limiting**: Threshold-based (no probabilistic errors)
4. **Mode Enforcement**: Fixed rules per mode

**Sources of <2% Error:**
- OS device detection delays (timing edge cases)
- Transient connection events
- Manual whitelist entry errors

Unlike ML systems, this accuracy represents **security enforcement precision**, not prediction accuracy.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│            JavaFX UI (SecureUSBUI)              │
└─────────────┬───────────────────────────────────┘
              │
┌─────────────▼───────────────────────────────────┐
│         Policy Engine (PolicyEngine)            │
│  ┌──────────────────────────────────────────┐   │
│  │ 1. Emergency Mode Check                  │   │
│  │ 2. Rate Limiting Check                   │   │
│  │ 3. Composite Device Detection            │   │
│  │ 4. Mode-Based Type Check                 │   │
│  │ 5. Whitelist Verification                │   │
│  └──────────────────────────────────────────┘   │
└─────────┬────────────────┬──────────────────────┘
          │                │
    ┌─────▼─────┐    ┌────▼────────┐
    │  Whitelist │    │ Composite   │
    │  Manager   │    │ Detector    │
    │  (SQLite)  │    │             │
    └────────────┘    └─────────────┘
          │
    ┌─────▼─────────┐
    │  USB Monitor  │
    │  (OS + Sim)   │
    └───────────────┘
```

---

## Security Policies Explained

### Policy 1: Whitelist Authorization
- Only devices with registered VID:PID allowed
- Database-backed persistence
- CRUD operations via UI

### Policy 2: Composite Device Detection
- Blocks HID+Storage combinations (BadUSB)
- Exceptions for whitelisted composite devices

### Policy 3: Rate Limiting
- Max 5 connections per 10 seconds
- Prevents USB flooding attacks

### Policy 4: Mode-Based Control
- **Production**: HID only (keyboards, mice)
- **Maintenance**: HID + Storage (whitelisted)
- **Emergency**: Block ALL devices

### Policy 5: Accuracy Tracking
- Real-time decision logging
- Transparency for auditing

---

## Troubleshooting

### Issue: JavaFX not found
**Solution:**
- Verify JavaFX path in `launch.json`
- Ensure `--module-path` is correct
- Check JavaFX version matches JDK version

### Issue: SQLite database locked
**Solution:**
- Close other connections to `whitelist.db`
- Restart application

### Issue: No USB devices detected (real mode)
**Solution:**
- Run with administrator/sudo privileges
- Verify PowerShell/lsusb is accessible
- Switch to simulation mode for testing

### Issue: Compilation errors
**Solution:**
- Ensure all `.java` files are in `src/secureusb/`
- Check JDK version: `java -version`
- Clean and rebuild: `javac -d bin src/secureusb/*.java`

---

## File Structure

```
SecureUSB/
│
├── src/
│   └── secureusb/
│       ├── Main.java                  # Entry point
│       ├── USBMonitor.java            # USB detection
│       ├── WhitelistManager.java      # Database management
│       ├── PolicyEngine.java          # Security decisions
│       ├── CompositeDetector.java     # BadUSB detection
│       ├── RateLimiter.java           # Flooding prevention
│       ├── ModeManager.java           # Mode control
│       ├── AccuracyTracker.java       # Statistics
│       ├── SecureLogger.java          # Logging system
│       └── SecureUSBUI.java           # JavaFX frontend
│
├── lib/
│   └── sqlite-jdbc-3.42.0.0.jar       # SQLite driver
│
├── whitelist.db                       # SQLite database (auto-generated)
├── config.properties                  # Configuration
├── secureusb.log                      # Log file (auto-generated)
└── README.md                          # This file
```

---

## Future Enhancements

1. **Real-Time OS Integration**: Kernel-level USB monitoring
2. **Active Directory Integration**: Enterprise user policies
3. **USB Firmware Analysis**: Deep packet inspection
4. **Network Alerting**: SIEM integration
5. **Mobile App**: Remote monitoring via Android/iOS
6. **Blockchain Logging**: Immutable audit trail



