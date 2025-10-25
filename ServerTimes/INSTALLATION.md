# Quick Installation Guide

## For Server Administrators

### Option 1: Use Pre-built JAR (Recommended)
1. Download the `ServerTimes-1.0.0.jar` file
2. Copy it to your server's `plugins/` folder
3. Restart your server
4. Configure using `/servertimes` commands

### Option 2: Build from Source

#### Prerequisites
- Java 21+ (JDK, not just JRE)
- Apache Maven 3.6+

#### Installing Maven on Windows

**Method 1: Using Chocolatey (Easiest)**
1. Install Chocolatey if you don't have it:
   - Open PowerShell as Administrator
   - Run: `Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))`
2. Install Maven: `choco install maven`
3. Restart your terminal

**Method 2: Manual Installation**
1. Download Maven from: https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Apache\maven`
3. Add environment variables:
   - `MAVEN_HOME` = `C:\Program Files\Apache\maven`
   - Add `%MAVEN_HOME%\bin` to your PATH
4. Restart your terminal

**Verify Installation**
```cmd
mvn --version
```

#### Building the Plugin
1. Run `build.bat` (Windows) or `mvn clean package` (any OS)
2. Copy `target/ServerTimes-1.0.0.jar` to your server's `plugins/` folder
3. Restart your server

## Quick Setup

1. **Set your first schedule**:
   ```
   /servertimes set monday 9pm 10pm
   ```

2. **Check if it's working**:
   ```
   /servertimes status
   ```

3. **Add more days**:
   ```
   /servertimes add sunday 2pm 6pm
   /servertimes add friday 8pm 11pm
   ```

4. **Give bypass permission to VIPs** (optional):
   ```
   /lp user PlayerName permission set servertimes.bypass true
   ```

## Default Permissions
- **servertimes.admin**: Required for all commands (default: OP only)
- **servertimes.bypass**: Can join/stay when server is closed (default: nobody)

## Need Help?
- Use `/servertimes help` in-game
- Check the full README.md for detailed documentation
- Look at server console for any error messages