@echo off
REM SecureUSB Build Script for Windows

echo =========================================
echo   SecureUSB Build Script
echo =========================================

REM Configuration - UPDATE THIS PATH
set JAVAFX_PATH=C:\Program Files (x86)\Java\javafx-sdk-25.0.1\lib\

REM Create directories
echo Creating build directories...
if not exist bin mkdir bin

REM Compile Java sources
echo Compiling Java sources...
javac --module-path "C:\Program Files (x86)\Java\javafx-sdk-25.0.1\lib\" --add-modules javafx.controls -d bin -cp "lib/*" src\secureusb\*.java

if %errorlevel% equ 0 (
    echo [SUCCESS] Compilation successful!
) else (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

REM Create executable JAR
echo Creating JAR file...
cd bin
jar cfm ..\SecureUSB.jar ..\MANIFEST.MF secureusb\*.class
cd ..

echo [SUCCESS] Build complete!
echo.
echo To run: run.bat
echo =========================================
pause