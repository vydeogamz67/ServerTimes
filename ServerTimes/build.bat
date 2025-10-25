@echo off
echo Building ServerTimes Plugin...
echo.

REM Check if Maven is installed
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH.
    echo Please install Maven from https://maven.apache.org/download.cgi
    echo Or download a pre-built JAR file instead.
    pause
    exit /b 1
)

echo Maven found, building plugin...
mvn clean package

if %errorlevel% equ 0 (
    echo.
    echo SUCCESS: Plugin built successfully!
    echo The JAR file is located at: target\ServerTimes-1.0.0.jar
    echo Copy this file to your server's plugins folder.
) else (
    echo.
    echo ERROR: Build failed. Check the output above for errors.
)

echo.
pause