// VoteKickConfig.java
package sierra.thing.votekick.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

import java.util.Properties;

public class VoteKickConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    // basic vote settings
    private static final int DEFAULT_VOTE_DURATION = 30;
    private static final int DEFAULT_COOLDOWN = 120;
    private static final double DEFAULT_PASS_PERCENTAGE = 0.6;
    private static final int DEFAULT_MIN_PLAYERS = 2;
    private static final boolean DEFAULT_REQUIRE_REASON = true;
    private static final int DEFAULT_MAX_REASON_LENGTH = 100;
    private static final int DEFAULT_TARGET_COOLDOWN = 300;

    // protection system defaults
    private static final boolean DEFAULT_NEW_PLAYER_PROTECTION = true;
    private static final int DEFAULT_NEW_PLAYER_DURATION = 300; // 5 minutes
    private static final boolean DEFAULT_POST_KICK_PROTECTION = true;
    private static final int DEFAULT_POST_KICK_DURATION = 600; // 10 minutes
    private static final int DEFAULT_EXTENDED_KICK_DURATION = 1800; // 30 minutes
    private static final boolean DEFAULT_HARASSMENT_DETECTION = true;
    private static final int DEFAULT_HARASSMENT_KICK_THRESHOLD = 3;
    private static final int DEFAULT_HARASSMENT_TIME_WINDOW = 3600; // 1 hour
    private static final boolean DEFAULT_VOTE_THRESHOLD_MODIFIERS = true;
    private static final double DEFAULT_LIGHT_MODIFIER = 1.25; // 25% more votes
    private static final double DEFAULT_HEAVY_MODIFIER = 1.5; // 50% more votes
    private static final int DEFAULT_LIGHT_MODIFIER_THRESHOLD = 3;
    private static final int DEFAULT_HEAVY_MODIFIER_THRESHOLD = 5;
    private static final int DEFAULT_DATA_CLEANUP_DAYS = 30;

    // permissions + history defaults
    private static final boolean DEFAULT_PERMISSIONS_ENABLED = true;
    private static final int DEFAULT_PERMISSION_START_LEVEL = 0;
    private static final int DEFAULT_PERMISSION_VOTE_LEVEL = 0;
    private static final int DEFAULT_PERMISSION_ADMIN_LEVEL = 2;
    private static final int DEFAULT_PERMISSION_EXEMPT_LEVEL = 2;
    private static final boolean DEFAULT_HISTORY_ENABLED = true;
    private static final int DEFAULT_HISTORY_MAX_ENTRIES = 200;
    private static final int DEFAULT_HISTORY_RETENTION_DAYS = 90;

    // limits
    private static final int MIN_VOTE_DURATION = 5;
    private static final int MAX_VOTE_DURATION = 300;
    private static final int MIN_COOLDOWN = 0;
    private static final int MAX_COOLDOWN = 3600;
    private static final int MIN_REASON_LENGTH = 10;
    private static final int MAX_REASON_LENGTH = 500;
    private static final int MIN_HISTORY_ENTRIES = 10;
    private static final int MAX_HISTORY_ENTRIES = 10000;
    private static final int MIN_HISTORY_RETENTION_DAYS = 1;
    private static final int MAX_HISTORY_RETENTION_DAYS = 3650;
    private static final int MIN_PERMISSION_LEVEL = 0;
    private static final int MAX_PERMISSION_LEVEL = 4;

    // basic vote config
    private final int voteDurationSeconds;
    private final int cooldownSeconds;
    private final double votePassPercentage;
    private final int minimumPlayers;
    private final boolean allowSelfVoting;
    private final boolean notifyPlayerOnVoteStart;
    private final boolean requireKickReason;
    private final int maxReasonLength;
    private final int targetCooldownSeconds;

    // protection system config
    private final boolean newPlayerProtectionEnabled;
    private final int newPlayerProtectionDuration;
    private final boolean postKickProtectionEnabled;
    private final int postKickProtectionDuration;
    private final int extendedKickProtectionDuration;
    private final boolean harassmentDetectionEnabled;
    private final int harassmentKickThreshold;
    private final int harassmentTimeWindow;
    private final boolean voteThresholdModifiersEnabled;
    private final double lightVoteModifier;
    private final double heavyVoteModifier;
    private final int lightModifierThreshold;
    private final int heavyModifierThreshold;
    private final int dataCleanupDays;

    // permissions + history config
    private final boolean permissionsEnabled;
    private final int permissionStartDefaultLevel;
    private final int permissionVoteDefaultLevel;
    private final int permissionAdminDefaultLevel;
    private final int permissionExemptDefaultLevel;
    private final boolean historyEnabled;
    private final int historyMaxEntries;
    private final int historyRetentionDays;

    public VoteKickConfig() {
        this.voteDurationSeconds = DEFAULT_VOTE_DURATION;
        this.cooldownSeconds = DEFAULT_COOLDOWN;
        this.votePassPercentage = DEFAULT_PASS_PERCENTAGE;
        this.minimumPlayers = DEFAULT_MIN_PLAYERS;
        this.allowSelfVoting = false;
        this.notifyPlayerOnVoteStart = true;
        this.requireKickReason = DEFAULT_REQUIRE_REASON;
        this.maxReasonLength = DEFAULT_MAX_REASON_LENGTH;
        this.targetCooldownSeconds = DEFAULT_TARGET_COOLDOWN;

        this.newPlayerProtectionEnabled = DEFAULT_NEW_PLAYER_PROTECTION;
        this.newPlayerProtectionDuration = DEFAULT_NEW_PLAYER_DURATION;
        this.postKickProtectionEnabled = DEFAULT_POST_KICK_PROTECTION;
        this.postKickProtectionDuration = DEFAULT_POST_KICK_DURATION;
        this.extendedKickProtectionDuration = DEFAULT_EXTENDED_KICK_DURATION;
        this.harassmentDetectionEnabled = DEFAULT_HARASSMENT_DETECTION;
        this.harassmentKickThreshold = DEFAULT_HARASSMENT_KICK_THRESHOLD;
        this.harassmentTimeWindow = DEFAULT_HARASSMENT_TIME_WINDOW;
        this.voteThresholdModifiersEnabled = DEFAULT_VOTE_THRESHOLD_MODIFIERS;
        this.lightVoteModifier = DEFAULT_LIGHT_MODIFIER;
        this.heavyVoteModifier = DEFAULT_HEAVY_MODIFIER;
        this.lightModifierThreshold = DEFAULT_LIGHT_MODIFIER_THRESHOLD;
        this.heavyModifierThreshold = DEFAULT_HEAVY_MODIFIER_THRESHOLD;
        this.dataCleanupDays = DEFAULT_DATA_CLEANUP_DAYS;

        this.permissionsEnabled = DEFAULT_PERMISSIONS_ENABLED;
        this.permissionStartDefaultLevel = DEFAULT_PERMISSION_START_LEVEL;
        this.permissionVoteDefaultLevel = DEFAULT_PERMISSION_VOTE_LEVEL;
        this.permissionAdminDefaultLevel = DEFAULT_PERMISSION_ADMIN_LEVEL;
        this.permissionExemptDefaultLevel = DEFAULT_PERMISSION_EXEMPT_LEVEL;
        this.historyEnabled = DEFAULT_HISTORY_ENABLED;
        this.historyMaxEntries = DEFAULT_HISTORY_MAX_ENTRIES;
        this.historyRetentionDays = DEFAULT_HISTORY_RETENTION_DAYS;
    }

    public VoteKickConfig(Properties props) {
        // basic vote settings
        int duration = DEFAULT_VOTE_DURATION;
        try {
            duration = Integer.parseInt(props.getProperty("vote_duration_seconds", String.valueOf(DEFAULT_VOTE_DURATION)));
            if (duration < MIN_VOTE_DURATION || duration > MAX_VOTE_DURATION) {
                LOGGER.warn("Invalid vote_duration_seconds ({}), using default: {}", duration, DEFAULT_VOTE_DURATION);
                duration = DEFAULT_VOTE_DURATION;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid vote_duration_seconds format, using default: {}", DEFAULT_VOTE_DURATION);
        }
        this.voteDurationSeconds = duration;

        int cooldown = DEFAULT_COOLDOWN;
        try {
            cooldown = Integer.parseInt(props.getProperty("cooldown_seconds", String.valueOf(DEFAULT_COOLDOWN)));
            if (cooldown < MIN_COOLDOWN || cooldown > MAX_COOLDOWN) {
                LOGGER.warn("Invalid cooldown_seconds ({}), using default: {}", cooldown, DEFAULT_COOLDOWN);
                cooldown = DEFAULT_COOLDOWN;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid cooldown_seconds format, using default: {}", DEFAULT_COOLDOWN);
        }
        this.cooldownSeconds = cooldown;

        int targetCooldown = DEFAULT_TARGET_COOLDOWN;
        try {
            targetCooldown = Integer.parseInt(props.getProperty("target_cooldown_seconds", String.valueOf(DEFAULT_TARGET_COOLDOWN)));
            if (targetCooldown < 0 || targetCooldown > MAX_COOLDOWN) {
                LOGGER.warn("Invalid target_cooldown_seconds ({}), using default: {}", targetCooldown, DEFAULT_TARGET_COOLDOWN);
                targetCooldown = DEFAULT_TARGET_COOLDOWN;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid target_cooldown_seconds format, using default: {}", DEFAULT_TARGET_COOLDOWN);
        }
        this.targetCooldownSeconds = targetCooldown;

        double percentage = DEFAULT_PASS_PERCENTAGE;
        try {
            percentage = Double.parseDouble(props.getProperty("vote_pass_percentage", String.valueOf(DEFAULT_PASS_PERCENTAGE)));
            if (percentage <= 0.0 || percentage > 1.0) {
                LOGGER.warn("Invalid vote_pass_percentage ({}), using default: {}", percentage, DEFAULT_PASS_PERCENTAGE);
                percentage = DEFAULT_PASS_PERCENTAGE;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid vote_pass_percentage format, using default: {}", DEFAULT_PASS_PERCENTAGE);
        }
        this.votePassPercentage = percentage;

        int minPlayers = DEFAULT_MIN_PLAYERS;
        try {
            minPlayers = Integer.parseInt(props.getProperty("minimum_players", String.valueOf(DEFAULT_MIN_PLAYERS)));
            if (minPlayers < 2) {
                LOGGER.warn("Invalid minimum_players ({}), using default: {}", minPlayers, DEFAULT_MIN_PLAYERS);
                minPlayers = DEFAULT_MIN_PLAYERS;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid minimum_players format, using default: {}", DEFAULT_MIN_PLAYERS);
        }
        this.minimumPlayers = minPlayers;

        this.allowSelfVoting = Boolean.parseBoolean(props.getProperty("allow_self_voting", "false"));
        this.notifyPlayerOnVoteStart = Boolean.parseBoolean(props.getProperty("notify_target_on_vote_start", "true"));
        this.requireKickReason = Boolean.parseBoolean(props.getProperty("require_kick_reason", "true"));

        int maxLength = DEFAULT_MAX_REASON_LENGTH;
        try {
            maxLength = Integer.parseInt(props.getProperty("max_reason_length", String.valueOf(DEFAULT_MAX_REASON_LENGTH)));
            if (maxLength < MIN_REASON_LENGTH || maxLength > MAX_REASON_LENGTH) {
                LOGGER.warn("Invalid max_reason_length ({}), using default: {}", maxLength, DEFAULT_MAX_REASON_LENGTH);
                maxLength = DEFAULT_MAX_REASON_LENGTH;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid max_reason_length format, using default: {}", DEFAULT_MAX_REASON_LENGTH);
        }
        this.maxReasonLength = maxLength;

        // protection system settings
        this.newPlayerProtectionEnabled = Boolean.parseBoolean(props.getProperty("new_player_protection_enabled", "true"));

        int newPlayerDuration = DEFAULT_NEW_PLAYER_DURATION;
        try {
            newPlayerDuration = Integer.parseInt(props.getProperty("new_player_protection_duration", String.valueOf(DEFAULT_NEW_PLAYER_DURATION)));
            if (newPlayerDuration < 0) {
                newPlayerDuration = DEFAULT_NEW_PLAYER_DURATION;
            }
        } catch (NumberFormatException e) {
            newPlayerDuration = DEFAULT_NEW_PLAYER_DURATION;
        }
        this.newPlayerProtectionDuration = newPlayerDuration;

        this.postKickProtectionEnabled = Boolean.parseBoolean(props.getProperty("post_kick_protection_enabled", "true"));

        int postKickDuration = DEFAULT_POST_KICK_DURATION;
        try {
            postKickDuration = Integer.parseInt(props.getProperty("post_kick_protection_duration", String.valueOf(DEFAULT_POST_KICK_DURATION)));
            if (postKickDuration < 0) {
                postKickDuration = DEFAULT_POST_KICK_DURATION;
            }
        } catch (NumberFormatException e) {
            postKickDuration = DEFAULT_POST_KICK_DURATION;
        }
        this.postKickProtectionDuration = postKickDuration;

        int extendedDuration = DEFAULT_EXTENDED_KICK_DURATION;
        try {
            extendedDuration = Integer.parseInt(props.getProperty("extended_kick_protection_duration", String.valueOf(DEFAULT_EXTENDED_KICK_DURATION)));
            if (extendedDuration < 0) {
                extendedDuration = DEFAULT_EXTENDED_KICK_DURATION;
            }
        } catch (NumberFormatException e) {
            extendedDuration = DEFAULT_EXTENDED_KICK_DURATION;
        }
        this.extendedKickProtectionDuration = extendedDuration;

        this.harassmentDetectionEnabled = Boolean.parseBoolean(props.getProperty("harassment_detection_enabled", "true"));

        int harassmentThreshold = DEFAULT_HARASSMENT_KICK_THRESHOLD;
        try {
            harassmentThreshold = Integer.parseInt(props.getProperty("harassment_kick_threshold", String.valueOf(DEFAULT_HARASSMENT_KICK_THRESHOLD)));
            if (harassmentThreshold < 1) {
                harassmentThreshold = DEFAULT_HARASSMENT_KICK_THRESHOLD;
            }
        } catch (NumberFormatException e) {
            harassmentThreshold = DEFAULT_HARASSMENT_KICK_THRESHOLD;
        }
        this.harassmentKickThreshold = harassmentThreshold;

        int harassmentWindow = DEFAULT_HARASSMENT_TIME_WINDOW;
        try {
            harassmentWindow = Integer.parseInt(props.getProperty("harassment_time_window", String.valueOf(DEFAULT_HARASSMENT_TIME_WINDOW)));
            if (harassmentWindow < 60) {
                harassmentWindow = DEFAULT_HARASSMENT_TIME_WINDOW;
            }
        } catch (NumberFormatException e) {
            harassmentWindow = DEFAULT_HARASSMENT_TIME_WINDOW;
        }
        this.harassmentTimeWindow = harassmentWindow;

        this.voteThresholdModifiersEnabled = Boolean.parseBoolean(props.getProperty("vote_threshold_modifiers_enabled", "true"));

        double lightModifier = DEFAULT_LIGHT_MODIFIER;
        try {
            lightModifier = Double.parseDouble(props.getProperty("light_vote_modifier", String.valueOf(DEFAULT_LIGHT_MODIFIER)));
            if (lightModifier < 1.0 || lightModifier > 3.0) {
                lightModifier = DEFAULT_LIGHT_MODIFIER;
            }
        } catch (NumberFormatException e) {
            lightModifier = DEFAULT_LIGHT_MODIFIER;
        }
        this.lightVoteModifier = lightModifier;

        double heavyModifier = DEFAULT_HEAVY_MODIFIER;
        try {
            heavyModifier = Double.parseDouble(props.getProperty("heavy_vote_modifier", String.valueOf(DEFAULT_HEAVY_MODIFIER)));
            if (heavyModifier < 1.0 || heavyModifier > 5.0) {
                heavyModifier = DEFAULT_HEAVY_MODIFIER;
            }
        } catch (NumberFormatException e) {
            heavyModifier = DEFAULT_HEAVY_MODIFIER;
        }
        this.heavyVoteModifier = heavyModifier;

        int lightThreshold = DEFAULT_LIGHT_MODIFIER_THRESHOLD;
        try {
            lightThreshold = Integer.parseInt(props.getProperty("light_modifier_threshold", String.valueOf(DEFAULT_LIGHT_MODIFIER_THRESHOLD)));
            if (lightThreshold < 1) {
                lightThreshold = DEFAULT_LIGHT_MODIFIER_THRESHOLD;
            }
        } catch (NumberFormatException e) {
            lightThreshold = DEFAULT_LIGHT_MODIFIER_THRESHOLD;
        }
        this.lightModifierThreshold = lightThreshold;

        int heavyThreshold = DEFAULT_HEAVY_MODIFIER_THRESHOLD;
        try {
            heavyThreshold = Integer.parseInt(props.getProperty("heavy_modifier_threshold", String.valueOf(DEFAULT_HEAVY_MODIFIER_THRESHOLD)));
            if (heavyThreshold < 1) {
                heavyThreshold = DEFAULT_HEAVY_MODIFIER_THRESHOLD;
            }
        } catch (NumberFormatException e) {
            heavyThreshold = DEFAULT_HEAVY_MODIFIER_THRESHOLD;
        }
        this.heavyModifierThreshold = heavyThreshold;

        int cleanupDays = DEFAULT_DATA_CLEANUP_DAYS;
        try {
            cleanupDays = Integer.parseInt(props.getProperty("data_cleanup_days", String.valueOf(DEFAULT_DATA_CLEANUP_DAYS)));
            if (cleanupDays < 1) {
                cleanupDays = DEFAULT_DATA_CLEANUP_DAYS;
            }
        } catch (NumberFormatException e) {
            cleanupDays = DEFAULT_DATA_CLEANUP_DAYS;
        }
        this.dataCleanupDays = cleanupDays;

        this.permissionsEnabled = Boolean.parseBoolean(props.getProperty("permissions_enabled", "true"));

        int startLevel = DEFAULT_PERMISSION_START_LEVEL;
        try {
            startLevel = Integer.parseInt(props.getProperty("permissions_start_default_level", String.valueOf(DEFAULT_PERMISSION_START_LEVEL)));
            if (startLevel < MIN_PERMISSION_LEVEL || startLevel > MAX_PERMISSION_LEVEL) {
                startLevel = DEFAULT_PERMISSION_START_LEVEL;
            }
        } catch (NumberFormatException e) {
            startLevel = DEFAULT_PERMISSION_START_LEVEL;
        }
        this.permissionStartDefaultLevel = startLevel;

        int voteLevel = DEFAULT_PERMISSION_VOTE_LEVEL;
        try {
            voteLevel = Integer.parseInt(props.getProperty("permissions_vote_default_level", String.valueOf(DEFAULT_PERMISSION_VOTE_LEVEL)));
            if (voteLevel < MIN_PERMISSION_LEVEL || voteLevel > MAX_PERMISSION_LEVEL) {
                voteLevel = DEFAULT_PERMISSION_VOTE_LEVEL;
            }
        } catch (NumberFormatException e) {
            voteLevel = DEFAULT_PERMISSION_VOTE_LEVEL;
        }
        this.permissionVoteDefaultLevel = voteLevel;

        int adminLevel = DEFAULT_PERMISSION_ADMIN_LEVEL;
        try {
            adminLevel = Integer.parseInt(props.getProperty("permissions_admin_default_level", String.valueOf(DEFAULT_PERMISSION_ADMIN_LEVEL)));
            if (adminLevel < MIN_PERMISSION_LEVEL || adminLevel > MAX_PERMISSION_LEVEL) {
                adminLevel = DEFAULT_PERMISSION_ADMIN_LEVEL;
            }
        } catch (NumberFormatException e) {
            adminLevel = DEFAULT_PERMISSION_ADMIN_LEVEL;
        }
        this.permissionAdminDefaultLevel = adminLevel;

        int exemptLevel = DEFAULT_PERMISSION_EXEMPT_LEVEL;
        try {
            exemptLevel = Integer.parseInt(props.getProperty("permissions_exempt_default_level", String.valueOf(DEFAULT_PERMISSION_EXEMPT_LEVEL)));
            if (exemptLevel < MIN_PERMISSION_LEVEL || exemptLevel > MAX_PERMISSION_LEVEL) {
                exemptLevel = DEFAULT_PERMISSION_EXEMPT_LEVEL;
            }
        } catch (NumberFormatException e) {
            exemptLevel = DEFAULT_PERMISSION_EXEMPT_LEVEL;
        }
        this.permissionExemptDefaultLevel = exemptLevel;

        this.historyEnabled = Boolean.parseBoolean(props.getProperty("vote_history_enabled", "true"));

        int historyEntries = DEFAULT_HISTORY_MAX_ENTRIES;
        try {
            historyEntries = Integer.parseInt(props.getProperty("vote_history_max_entries", String.valueOf(DEFAULT_HISTORY_MAX_ENTRIES)));
            if (historyEntries < MIN_HISTORY_ENTRIES || historyEntries > MAX_HISTORY_ENTRIES) {
                historyEntries = DEFAULT_HISTORY_MAX_ENTRIES;
            }
        } catch (NumberFormatException e) {
            historyEntries = DEFAULT_HISTORY_MAX_ENTRIES;
        }
        this.historyMaxEntries = historyEntries;

        int historyRetention = DEFAULT_HISTORY_RETENTION_DAYS;
        try {
            historyRetention = Integer.parseInt(props.getProperty("vote_history_retention_days", String.valueOf(DEFAULT_HISTORY_RETENTION_DAYS)));
            if (historyRetention < MIN_HISTORY_RETENTION_DAYS || historyRetention > MAX_HISTORY_RETENTION_DAYS) {
                historyRetention = DEFAULT_HISTORY_RETENTION_DAYS;
            }
        } catch (NumberFormatException e) {
            historyRetention = DEFAULT_HISTORY_RETENTION_DAYS;
        }
        this.historyRetentionDays = historyRetention;
    }

    public void updateProperties(Properties props) {
        props.setProperty("vote_duration_seconds", Integer.toString(voteDurationSeconds));
        props.setProperty("cooldown_seconds", Integer.toString(cooldownSeconds));
        props.setProperty("vote_pass_percentage", Double.toString(votePassPercentage));
        props.setProperty("minimum_players", Integer.toString(minimumPlayers));
        props.setProperty("allow_self_voting", Boolean.toString(allowSelfVoting));
        props.setProperty("notify_target_on_vote_start", Boolean.toString(notifyPlayerOnVoteStart));
        props.setProperty("require_kick_reason", Boolean.toString(requireKickReason));
        props.setProperty("max_reason_length", Integer.toString(maxReasonLength));
        props.setProperty("target_cooldown_seconds", Integer.toString(targetCooldownSeconds));

        props.setProperty("new_player_protection_enabled", Boolean.toString(newPlayerProtectionEnabled));
        props.setProperty("new_player_protection_duration", Integer.toString(newPlayerProtectionDuration));
        props.setProperty("post_kick_protection_enabled", Boolean.toString(postKickProtectionEnabled));
        props.setProperty("post_kick_protection_duration", Integer.toString(postKickProtectionDuration));
        props.setProperty("extended_kick_protection_duration", Integer.toString(extendedKickProtectionDuration));
        props.setProperty("harassment_detection_enabled", Boolean.toString(harassmentDetectionEnabled));
        props.setProperty("harassment_kick_threshold", Integer.toString(harassmentKickThreshold));
        props.setProperty("harassment_time_window", Integer.toString(harassmentTimeWindow));
        props.setProperty("vote_threshold_modifiers_enabled", Boolean.toString(voteThresholdModifiersEnabled));
        props.setProperty("light_vote_modifier", Double.toString(lightVoteModifier));
        props.setProperty("heavy_vote_modifier", Double.toString(heavyVoteModifier));
        props.setProperty("light_modifier_threshold", Integer.toString(lightModifierThreshold));
        props.setProperty("heavy_modifier_threshold", Integer.toString(heavyModifierThreshold));
        props.setProperty("data_cleanup_days", Integer.toString(dataCleanupDays));

        props.setProperty("permissions_enabled", Boolean.toString(permissionsEnabled));
        props.setProperty("permissions_start_default_level", Integer.toString(permissionStartDefaultLevel));
        props.setProperty("permissions_vote_default_level", Integer.toString(permissionVoteDefaultLevel));
        props.setProperty("permissions_admin_default_level", Integer.toString(permissionAdminDefaultLevel));
        props.setProperty("permissions_exempt_default_level", Integer.toString(permissionExemptDefaultLevel));
        props.setProperty("vote_history_enabled", Boolean.toString(historyEnabled));
        props.setProperty("vote_history_max_entries", Integer.toString(historyMaxEntries));
        props.setProperty("vote_history_retention_days", Integer.toString(historyRetentionDays));
    }

    // basic vote getters
    public int getVoteDurationSeconds() { return voteDurationSeconds; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public double getVotePassPercentage() { return votePassPercentage; }
    public int getMinimumPlayers() { return minimumPlayers; }
    public boolean isAllowSelfVoting() { return allowSelfVoting; }
    public boolean isNotifyPlayerOnVoteStart() { return notifyPlayerOnVoteStart; }
    public boolean isRequireKickReason() { return requireKickReason; }
    public int getMaxReasonLength() { return maxReasonLength; }
    public int getTargetCooldownSeconds() { return targetCooldownSeconds; }

    // protection system getters
    public boolean isNewPlayerProtectionEnabled() { return newPlayerProtectionEnabled; }
    public int getNewPlayerProtectionDuration() { return newPlayerProtectionDuration; }
    public boolean isPostKickProtectionEnabled() { return postKickProtectionEnabled; }
    public int getPostKickProtectionDuration() { return postKickProtectionDuration; }
    public int getExtendedKickProtectionDuration() { return extendedKickProtectionDuration; }
    public boolean isHarassmentDetectionEnabled() { return harassmentDetectionEnabled; }
    public int getHarassmentKickThreshold() { return harassmentKickThreshold; }
    public int getHarassmentTimeWindow() { return harassmentTimeWindow; }
    public boolean isVoteThresholdModifiersEnabled() { return voteThresholdModifiersEnabled; }
    public double getLightVoteModifier() { return lightVoteModifier; }
    public double getHeavyVoteModifier() { return heavyVoteModifier; }
    public int getLightModifierThreshold() { return lightModifierThreshold; }
    public int getHeavyModifierThreshold() { return heavyModifierThreshold; }
    public int getDataCleanupDays() { return dataCleanupDays; }

    public boolean isPermissionsEnabled() { return permissionsEnabled; }
    public int getPermissionStartDefaultLevel() { return permissionStartDefaultLevel; }
    public int getPermissionVoteDefaultLevel() { return permissionVoteDefaultLevel; }
    public int getPermissionAdminDefaultLevel() { return permissionAdminDefaultLevel; }
    public int getPermissionExemptDefaultLevel() { return permissionExemptDefaultLevel; }
    public boolean isHistoryEnabled() { return historyEnabled; }
    public int getHistoryMaxEntries() { return historyMaxEntries; }
    public int getHistoryRetentionDays() { return historyRetentionDays; }
}
