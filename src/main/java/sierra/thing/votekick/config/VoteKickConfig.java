package sierra.thing.votekick.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

import java.util.Properties;

/**
 * Handles all the mod's settings.
 * Everything that can be tweaked by server admins lives here.
 */
public class VoteKickConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    // Default settings - tweak these if the defaults seem off
    private static final int DEFAULT_VOTE_DURATION = 30;      // 30 sec seems reasonable
    private static final int DEFAULT_COOLDOWN = 120;          // 2 min between votes to prevent spam
    private static final double DEFAULT_PASS_PERCENTAGE = 0.6; // 60% yes votes required to pass
    private static final int DEFAULT_MIN_PLAYERS = 2;         // Need at least 2 players (duh)
    private static final boolean DEFAULT_REQUIRE_REASON = true; // Force people to say why
    private static final int DEFAULT_MAX_REASON_LENGTH = 100;  // Keep reasons reasonably short

    // Sanity limits - prevent people from setting crazy values
    private static final int MIN_VOTE_DURATION = 5;     // Any faster is just trolling
    private static final int MAX_VOTE_DURATION = 300;   // 5 min is plenty
    private static final int MIN_COOLDOWN = 0;          // 0 = disable cooldown
    private static final int MAX_COOLDOWN = 3600;       // 1 hour max cooldown
    private static final int MIN_REASON_LENGTH = 10;    // Make it at least somewhat meaningful
    private static final int MAX_REASON_LENGTH = 500;   // No essays please

    // Actual config values
    private final int voteDurationSeconds;
    private final int cooldownSeconds;
    private final double votePassPercentage;
    private final int minimumPlayers;
    private final boolean allowSelfVoting;
    private final boolean notifyPlayerOnVoteStart;
    private final boolean requireKickReason;
    private final int maxReasonLength;

    /**
     * Default constructor - use when no config exists yet
     */
    public VoteKickConfig() {
        this.voteDurationSeconds = DEFAULT_VOTE_DURATION;
        this.cooldownSeconds = DEFAULT_COOLDOWN;
        this.votePassPercentage = DEFAULT_PASS_PERCENTAGE;
        this.minimumPlayers = DEFAULT_MIN_PLAYERS;
        this.allowSelfVoting = false;
        this.notifyPlayerOnVoteStart = true;
        this.requireKickReason = DEFAULT_REQUIRE_REASON;
        this.maxReasonLength = DEFAULT_MAX_REASON_LENGTH;
    }

    /**
     * Loads config from properties file.
     * Validates everything to make sure we don't get crazy values.
     */
    public VoteKickConfig(Properties props) {
        // Vote duration - how long voting lasts
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

        // Cooldown - stops people from spamming votes
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

        // How many yes votes needed (as percentage)
        // 0.5 = majority, 1.0 = everyone must vote yes
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

        // Minimum players - prevent votekick on tiny servers
        // Must be at least 2 - can't kick if there's only one player!
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

        // Can you vote to kick yourself? Weird but some people wanted this
        this.allowSelfVoting = Boolean.parseBoolean(
                props.getProperty("allow_self_voting", "false"));

        // Should target know they're being voted on?
        // Set false for stealth kicks
        this.notifyPlayerOnVoteStart = Boolean.parseBoolean(
                props.getProperty("notify_target_on_vote_start", "true"));

        // Force players to give a reason for kicking
        this.requireKickReason = Boolean.parseBoolean(
                props.getProperty("require_kick_reason", "true"));

        // Max length for kick reasons
        int maxLength = DEFAULT_MAX_REASON_LENGTH;
        try {
            maxLength = Integer.parseInt(props.getProperty("max_reason_length",
                    String.valueOf(DEFAULT_MAX_REASON_LENGTH)));
            if (maxLength < MIN_REASON_LENGTH || maxLength > MAX_REASON_LENGTH) {
                LOGGER.warn("Invalid max_reason_length ({}), must be between {} and {}. Using default: {}",
                        maxLength, MIN_REASON_LENGTH, MAX_REASON_LENGTH, DEFAULT_MAX_REASON_LENGTH);
                maxLength = DEFAULT_MAX_REASON_LENGTH;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid max_reason_length format, using default: {}", DEFAULT_MAX_REASON_LENGTH);
        }
        this.maxReasonLength = maxLength;
    }

    /**
     * How long votes last in seconds
     */
    public int getVoteDurationSeconds() {
        return voteDurationSeconds;
    }

    /**
     * Cooldown between starting votes
     * (prevents vote spam)
     */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * What percentage of yes votes needed to pass
     * (0.5 = simple majority, 0.66 = 2/3 majority, etc)
     */
    public double getVotePassPercentage() {
        return votePassPercentage;
    }

    /**
     * Minimum players needed before voting works
     */
    public int getMinimumPlayers() {
        return minimumPlayers;
    }

    /**
     * Can players vote to kick themselves?
     * Not sure why anyone would but hey
     */
    public boolean isAllowSelfVoting() {
        return allowSelfVoting;
    }

    /**
     * Should target know they're being voted on?
     */
    public boolean isNotifyPlayerOnVoteStart() {
        return notifyPlayerOnVoteStart;
    }

    /**
     * Must provide a reason when starting a vote
     */
    public boolean isRequireKickReason() {
        return requireKickReason;
    }

    /**
     * Max length for kick reasons
     */
    public int getMaxReasonLength() {
        return maxReasonLength;
    }

    /**
     * Saves all settings to the properties file
     */
    public void updateProperties(Properties props) {
        props.setProperty("vote_duration_seconds", Integer.toString(voteDurationSeconds));
        props.setProperty("cooldown_seconds", Integer.toString(cooldownSeconds));
        props.setProperty("vote_pass_percentage", Double.toString(votePassPercentage));
        props.setProperty("minimum_players", Integer.toString(minimumPlayers));
        props.setProperty("allow_self_voting", Boolean.toString(allowSelfVoting));
        props.setProperty("notify_target_on_vote_start", Boolean.toString(notifyPlayerOnVoteStart));
        props.setProperty("require_kick_reason", Boolean.toString(requireKickReason));
        props.setProperty("max_reason_length", Integer.toString(maxReasonLength));
    }
}