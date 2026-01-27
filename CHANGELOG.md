# Changelog

## 3.0.0
### Added
- Permission nodes with configurable default op-level fallbacks for start/vote/admin/exempt.
- Optional LuckPerms permissions bridge for Fabric and NeoForge.
- Admin commands to cancel, force, reload config, and view vote history.
- Dedicated `/votekick-admin` command entry point.
- Persistent vote history with retention and max-entry limits.
- Updated config defaults and documentation for permissions, admin controls, and history.

### Security
- Permissions checks now fail closed when external integrations error.
- Network payload string reads are length-bounded to prevent oversized allocations.
- Vote reasons are sanitized to block control/formatting injection in chat/UI/history.
- Per-player rate limiter for CastVote packets.

### CI
- Modrinth publishing job for tag pushes and manual dispatch.
