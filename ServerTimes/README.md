# ServerTimes - Minecraft Server Schedule Plugin

A Minecraft plugin for managing server opening and closing times with configurable schedules for each day of the week.

## Features

- **Daily Schedules**: Set different opening hours for each day of the week
- **Multiple Sessions**: Support for multiple time sessions per day (e.g., morning and evening sessions)
- **Flexible Time Formats**: Supports both 12-hour (9pm) and 24-hour (21:00) time formats
- **Graceful Shutdown**: Warns players before server closure
- **Bypass Permissions**: Allow certain players to stay online during closed hours
- **Automatic Player Management**: Prevents new joins and kicks players when server closes
- **Easy Configuration**: Simple YAML configuration and in-game commands

## Requirements

- Minecraft Server 1.21.4
- Java 21 or higher
- Paper/Spigot server (recommended)

## Installation

1. **Download or Build the Plugin**:
   - If you have Maven installed: `mvn clean package`
   - The compiled JAR will be in the `target/` directory

2. **Install the Plugin**:
   - Copy `ServerTimes-1.0.0.jar` to your server's `plugins/` folder
   - Restart your server

3. **Configure Permissions** (optional):
   - Add permissions to your permission plugin or use default OP permissions

## Configuration

The plugin creates a `config.yml` file in `plugins/ServerTimes/` with the following structure:

```yaml
# ServerTimes Configuration
schedule:
  monday:
    - "21:00-22:00"  # 9 PM to 10 PM
  tuesday: []
  wednesday: []
  thursday: []
  friday: []
  saturday: []
  sunday:
    - "21:00-24:00"  # 9 PM to midnight
    - "19:00-20:00"  # 7 PM to 8 PM

messages:
  server_closed: "§cThe server is currently closed. Please check the schedule and come back during open hours!"
  join_denied: "§cThe server is currently closed. Please check the schedule and come back during open hours!"
  server_opening: "§aThe server is now open! Welcome!"
  server_closing_warning: "§eThe server will close in {minutes} minute(s)!"

settings:
  warning_minutes: 5
  graceful_shutdown: true
```

### Time Formats Supported

- **24-hour format**: `21:00`, `9:30`, `23:45`
- **12-hour format**: `9pm`, `9:30pm`, `11:45am`
- **Compact format**: `2130`, `930` (for 21:30, 9:30)

## Commands

All commands require the `servertimes.admin` permission (default: OP).

### Basic Commands

- `/servertimes help` - Show help message
- `/servertimes status` - Check current server status and today's schedule
- `/servertimes list [day]` - List schedule for all days or a specific day
- `/servertimes reload` - Reload configuration from file

### Schedule Management

- `/servertimes set <day> <start-time> <end-time>` - Set server times for a day (replaces existing)
- `/servertimes add <day> <start-time> <end-time>` - Add additional session for a day
- `/servertimes remove <day> [session-number]` - Remove session(s) for a day

### Examples

```
# Set Monday to be open from 9 PM to 10 PM
/servertimes set monday 9pm 10pm

# Add a morning session on Sunday (9 AM to 12 PM)
/servertimes add sunday 9am 12pm

# Add an evening session on Sunday (7 PM to 8 PM)
/servertimes add sunday 7pm 8pm

# Remove the first session on Sunday
/servertimes remove sunday 1

# Remove all sessions for Tuesday (close the server all day)
/servertimes remove tuesday

# Check current status
/servertimes status

# List all schedules
/servertimes list

# List only Monday's schedule
/servertimes list monday
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `servertimes.admin` | Access to all ServerTimes commands | OP |
| `servertimes.bypass` | Can stay online when server is closed | false |

### Permission Examples

For LuckPerms:
```
/lp group admin permission set servertimes.admin true
/lp user VIPPlayer permission set servertimes.bypass true
```

## How It Works

1. **Scheduler**: The plugin checks every 30 seconds if the server should be open or closed
2. **Warnings**: Players receive warnings before the server closes (configurable)
3. **Graceful Shutdown**: Players are warned, then kicked after a brief delay
4. **Join Prevention**: New players cannot join when the server is closed
5. **Bypass System**: Players with bypass permission can join and stay online anytime

## Configuration Tips

### Multiple Sessions Per Day
You can have multiple sessions per day for different time zones or play groups:

```yaml
sunday:
  - "09:00-12:00"  # Morning session
  - "19:00-22:00"  # Evening session
```

### Midnight Crossover
Sessions can cross midnight:

```yaml
friday:
  - "22:00-02:00"  # 10 PM Friday to 2 AM Saturday
```

### Closed Days
Leave the day array empty to keep the server closed all day:

```yaml
tuesday: []
```

## Troubleshooting

### Common Issues

1. **Plugin not loading**: Check server logs for errors, ensure Java 21+
2. **Commands not working**: Verify you have `servertimes.admin` permission
3. **Times not working**: Check time format, use `/servertimes status` to debug
4. **Players not getting kicked**: Check if they have bypass permission

### Debug Commands

- `/servertimes status` - Shows current server state and active sessions
- `/servertimes list` - Shows all configured schedules
- Check server console for plugin messages

## Building from Source

1. **Prerequisites**:
   - Java 21 JDK
   - Maven 3.6+

2. **Build**:
   ```bash
   git clone <repository-url>
   cd ServerTimes
   mvn clean package
   ```

3. **Output**: The compiled JAR will be in `target/ServerTimes-1.0.0.jar`

## Support

- Check the server console for error messages
- Verify your time formats using `/servertimes status`
- Test with `/servertimes list` to see your current configuration
- Use `/servertimes reload` after making manual config changes

## License

This plugin is provided as-is for educational and server management purposes.