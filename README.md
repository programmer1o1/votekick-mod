# VoteKick

Do you want players agreement? Bring out Democracy!

## Overview

VoteKick allows players to democratically remove disruptive players from the server through a simple voting system. When a vote is initiated, all players receive a clean, unobtrusive UI to cast their votes, with results tallied in real-time.

## How to Use

### Starting a Vote
```
/votekick <player> <reason>  (or /vk <player> <reason> for short)
```

### Casting Votes
- Press **F1** to vote YES (kick the player)
- Press **F2** to vote NO (keep the player)
- Or use commands: `/vote yes` or `/vote no`

### Checking Status
```
/vote status  (shows current vote details)
```

The vote will automatically pass or fail based on your server's configuration.

## Configuration

In `config/votekick.properties`:

```properties
# Duration of votes in seconds
vote_duration_seconds=30

# Cooldown between votes in seconds
cooldown_seconds=120

# Percentage of YES votes required (0.0-1.0)
vote_pass_percentage=0.6

# Minimum players required for voting
minimum_players=2

# Allow players to vote to kick themselves
allow_self_voting=false

# Whether target players are notified of vote start
notify_target_on_vote_start=true

# Require a reason when starting a vote kick
require_kick_reason=true
```

## Requirements

- Fabric API
- Required on both server and client

## Permissions

- Players cannot vote to kick server operators
- Players with permission level 2+ cannot be vote-kicked

## License

This mod is available under the MIT License.

---
