// VoteKickConfig.java
package sierra.thing.votekick.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

import java.util.Properties;

public class VoteKickConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    // defaults
    private static final int DEFAULT_VOTE_DURATION = 30;
    private static final int DEFAULT_COOLDOWN = 120;
    private static final double DEFAULT_PASS_PERCENTAGE = 0.6;
    private static final int DEFAULT_MIN_PLAYERS = 2;
    private static final boolean DEFAULT_REQUIRE_REASON = true;
    private static final int DEFAULT_MAX_REASON_LENGTH = 100;
    private static final boolean DEFAULT_PROTECTION_ENABLED = true;
    private static final int DEFAULT_PROTECTION_DURATION = 600; // 10 minutes

    // limits
    private static final int MIN_VOTE_DURATION = 5;
    private static final int MAX_VOTE_DURATION = 300;
    private static final int MIN_COOLDOWN = 0;
    private static final int MAX_COOLDOWN = 3600;
    private static final int MIN_REASON_LENGTH = 10;
    private static final int MAX_REASON_LENGTH = 500;

    // config values
    private final int voteDurationSeconds;
    private final int cooldownSeconds;
    private final double votePassPercentage;
    private final int minimumPlayers;
    private final boolean allowSelfVoting;
    private final boolean notifyPlayerOnVoteStart;
    private final boolean requireKickReason;
    private final int maxReasonLength;
    private final boolean protectionEnabled;
    private final int protectionDurationSeconds;

    public VoteKickConfig() {
        this.voteDurationSeconds = DEFAULT_VOTE_DURATION;
        this.cooldownSeconds = DEFAULT_COOLDOWN;
        this.votePassPercentage = DEFAULT_PASS_PERCENTAGE;
        this.minimumPlayers = DEFAULT_MIN_PLAYERS;
        this.allowSelfVoting = false;
        this.notifyPlayerOnVoteStart = true;
        this.requireKickReason = DEFAULT_REQUIRE_REASON;
        this.maxReasonLength = DEFAULT_MAX_REASON_LENGTH;
        this.protectionEnabled = DEFAULT_PROTECTION_ENABLED;
        this.protectionDurationSeconds = DEFAULT_PROTECTION_DURATION;
    }

    public VoteKickConfig(Properties props) {
        // vote duration
        int duration = DEFAULT_VOTE_DURATION;
        try {
            duration = Integer.parseInt(props.getProperty("vote_duration_seconds",
                    String.valueOf(DEFAULT_VOTE_DURATION)));
            if (duration < MIN_VOTE_DURATION || duration > MAX_VOTE_DURATION) {
                LOGGER.warn("Invalid vote_duration_seconds ({}), must be between {} and {}. Using default: {}",
                        duration, MIN_VOTE_DURATION, MAX_VOTE_DURATION, DEFAULT_VOTE_DURATION);
                duration = DEFAULT_VOTE_DURATION;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid vote_duration_seconds format, using default: {}", DEFAULT_VOTE_DURATION);
        }
        this.voteDurationSeconds = duration;

        // cooldown
        int cooldown = DEFAULT_COOLDOWN;
        try {
            cooldown = Integer.parseInt(props.getProperty("cooldown_seconds",
                    String.valueOf(DEFAULT_COOLDOWN)));
            if (cooldown < MIN_COOLDOWN || cooldown > MAX_COOLDOWN) {
                LOGGER.warn("Invalid cooldown_seconds ({}), must be between {} and {}. Using default: {}",
                        cooldown, MIN_COOLDOWN, MAX_COOLDOWN, DEFAULT_COOLDOWN);
                cooldown = DEFAULT_COOLDOWN;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid cooldown_seconds format, using default: {}", DEFAULT_COOLDOWN);
        }
        this.cooldownSeconds = cooldown;

        // pass percentage
        double percentage = DEFAULT_PASS_PERCENTAGE;
        try {
            percentage = Double.parseDouble(props.getProperty("vote_pass_percentage",
                    String.valueOf(DEFAULT_PASS_PERCENTAGE)));
            if (percentage <= 0.0 || percentage > 1.0) {
                LOGGER.warn("Invalid vote_pass_percentage ({}), must be between 0.0 and 1.0. Using default: {}",
                        percentage, DEFAULT_PASS_PERCENTAGE);
                percentage = DEFAULT_PASS_PERCENTAGE;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid vote_pass_percentage format, using default: {}", DEFAULT_PASS_PERCENTAGE);
        }
        this.votePassPercentage = percentage;

        // minimum players
        int minPlayers = DEFAULT_MIN_PLAYERS;
        try {
            minPlayers = Integer.parseInt(props.getProperty("minimum_players",
                    String.valueOf(DEFAULT_MIN_PLAYERS)));
            if (minPlayers < 2) {
                LOGGER.warn("Invalid minimum_players ({}), must be at least 2. Using default: {}",
                        minPlayers, DEFAULT_MIN_PLAYERS);
                minPlayers = DEFAULT_MIN_PLAYERS;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid minimum_players format, using default: {}", DEFAULT_MIN_PLAYERS);
        }
        this.minimumPlayers = minPlayers;

        // booleans
        this.allowSelfVoting = Boolean.parseBoolean(
                props.getProperty("allow_self_voting", "false"));
        this.notifyPlayerOnVoteStart = Boolean.parseBoolean(
                props.getProperty("notify_target_on_vote_start", "true"));
        this.requireKickReason = Boolean.parseBoolean(
                props.getProperty("require_kick_reason", "true"));
        this.protectionEnabled = Boolean.parseBoolean(
                props.getProperty("protection_enabled", "true"));

        // max reason length
        int maxLength = DEFAULT_MAX_REASON_LENGTH;
        try {
            maxLength = Integer.parseInt(props.getProperty("max_reason_length",
                    String.valueOf(DEFAULT_MAX_REASON_LENGTH)));
            if (maxLength < MIN_REASON_LENGTH || maxLength > MAX_REASON_LENGTH) {
                LOGGER.warn("Invalid max_reason_length ({}), must be between {} and {}. Using default: {}",
                        maxLength, MIN_REASON_LENGTH, MAX_REASON_LENGTH, DEFAULT_MAX_REASON_LENGTH);
                maxLength = DEFAULT_MAX_REASON_LENGTH;
            }} catch (NumberFormatException e) {
            LOGGER.warn("Invalid max_reason_length format, using default: {}", DEFAULT_MAX_REASON_LENGTH);
        }
        this.maxReasonLength = maxLength;

        // protection duration
        int protectionDuration = DEFAULT_PROTECTION_DURATION;
        try {
            protectionDuration = Integer.parseInt(props.getProperty("protection_duration_seconds",
                    String.valueOf(DEFAULT_PROTECTION_DURATION)));
            if (protectionDuration < 0) {
                LOGGER.warn("Invalid protection_duration_seconds ({}), must be positive. Using default: {}",
                        protectionDuration, DEFAULT_PROTECTION_DURATION);
                protectionDuration = DEFAULT_PROTECTION_DURATION;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid protection_duration_seconds format, using default: {}", DEFAULT_PROTECTION_DURATION);
        }
        this.protectionDurationSeconds = protectionDuration;
    }

    public int getVoteDurationSeconds() {
        return voteDurationSeconds;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getVotePassPercentage() {
        return votePassPercentage;
    }

    public int getMinimumPlayers() {
        return minimumPlayers;
    }

    public boolean isAllowSelfVoting() {
        return allowSelfVoting;
    }

    public boolean isNotifyPlayerOnVoteStart() {
        return notifyPlayerOnVoteStart;
    }

    public boolean isRequireKickReason() {
        return requireKickReason;
    }

    public int getMaxReasonLength() {
        return maxReasonLength;
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    public int getProtectionDurationSeconds() {
        return protectionDurationSeconds;
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
        props.setProperty("protection_enabled", Boolean.toString(protectionEnabled));
        props.setProperty("protection_duration_seconds", Integer.toString(protectionDurationSeconds));
    }
}