@echo off
REM SecureUSB Run Script for Windows

echo =========================================
echo   Launching SecureUSB...
echo =========================================

REM Configuration - UPDATE THIS PATH
set JAVAFX_PATH=C:\Program Files (x86)\Java\javafx-sdk-25.0.1\lib\

REM Run application
java --module-path "C:\Program Files (x86)\Java\javafx-sdk-25.0.1\lib\" --add-modules javafx.controls -cp "bin;lib/*" secureusb.Main

echo =========================================
echo   SecureUSB terminated
echo =========================================
pause