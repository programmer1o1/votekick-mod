# VoteKick

A democratic voting system that allows players to collectively remove disruptive players from the server through a clean, fair voting process.

## Features

- **Simple Voting Interface** - Unobtrusive UI with F1/F2 keybinds or chat commands
- **Anti-Abuse Protection** - Prevents harassment and vote spam with cooldowns and immunity periods
- **Configurable Client UI** - Adjustable scaling, positioning, and visual options
- **Real-time Vote Tracking** - Live progress bars and vote counts
- **Flexible Configuration** - Extensive server-side settings for fine-tuning

## Usage

### Starting a Vote
```
/votekick <player> <reason>
/vk <player> <reason>
```

### Voting
- Press **F1** for YES (kick player)
- Press **F2** for NO (keep player)
- Commands: `/vote yes` or `/vote no`

### Status
```
/vote status
```

## Configuration

### Server Configuration
Located in `config/votekick.properties`:

```properties
# Basic Settings
vote_duration_seconds=30
vote_pass_percentage=0.6
minimum_players=2
cooldown_seconds=120

# Protection System
new_player_protection_enabled=true
post_kick_protection_enabled=true
harassment_detection_enabled=true
vote_threshold_modifiers_enabled=true

# Customization
require_kick_reason=true
allow_self_voting=false
```

### Client Configuration
Customize your voting experience through:
- **ModMenu Integration** - Graphical configuration screen
- **Properties File** - `config/votekick-client.properties` for manual editing

Options include UI scaling, panel positioning, sound controls, and animation settings.

## Anti-Abuse Features

- **New Player Protection** - Grace period for first-time joiners
- **Post-Kick Immunity** - Temporary protection after being kicked
- **Harassment Detection** - Automatic protection for repeatedly targeted players
- **Vote Cooldowns** - Prevents spam voting and target harassment
- **Threshold Modifiers** - Requires more votes to kick frequently kicked players

## Requirements

- **Fabric API** (required)
- **ModMenu** (optional, for GUI configuration)
- Must be installed on both client and server

## Permissions

- Server operators and players with permission level 2+ cannot be vote-kicked
- All other players can participate in voting (unless protected)

## Compatibility

- Minecraft 1.20.1
- Fabric Loader 0.14.0+
