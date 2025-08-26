package sierra.thing.votekick.vote;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.network.VoteKickNetworking;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main vote handling logic. This runs when someone starts a votekick.
 * Handles pretty much everything from UI to vote counting to the actual kicking.
 */
public class VoteSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    // UI colors - might want to make these configurable eventually
    private static final TextColor COLOR_YES = TextColor.fromRgb(0x55FF55);   // Green
    private static final TextColor COLOR_NO = TextColor.fromRgb(0xFF5555);    // Red
    private static final TextColor COLOR_WARNING = TextColor.fromRgb(0xFFAA00); // Orange
    private static final TextColor COLOR_INFO = TextColor.fromRgb(0xFFFF55);  // Yellow

    // Constants
    private static final int TICKS_PER_SECOND = 20;
    private static final int UI_UPDATE_INTERVAL = 10; // Update UI every half second (10 ticks)
    private static final int KICK_DELAY_MS = 2000;    // 2 second delay before kicking - gives them time to read the message

    // Vote data
    private final UUID initiatorUUID;
    private final UUID targetUUID;
    private final String initiatorName;
    private final String targetName;
    private final Map<UUID, Boolean> playerVotes = new HashMap<>();
    private final int totalVotesNeeded;
    private final long startTime;
    private final int voteDuration;
    private final int totalEligibleVoters;

    // State tracking
    private int ticksRemaining;
    private int uiUpdateTick = 0;
    private boolean hasPlayedStartSound = false;
    private boolean hasPlayedEndSound = false;
    private boolean kickScheduled = false;
    private final String kickReason;

    // Cooldown tracking - shared across all VoteSessions
    // Had to make this thread-safe after that weird bug with simultaneous votes
    private static final Map<UUID, Long> playersOnCooldown = new ConcurrentHashMap<>();

    // Times to announce remaining time (seconds)
    private final Set<Integer> announcementTimes = new HashSet<>(Arrays.asList(30, 15, 5));

    /**
     * Creates a new vote session to kick someone.
     *
     * @param initiatorUUID Player who started the vote
     * @param targetUUID Player who might get kicked
     * @param initiatorName Display name of starter
     * @param targetName Display name of target
     * @param kickReason Why they're being kicked
     * @param playerCount Total players on server
     * @param voteDurationSeconds How long vote lasts
     */
    public VoteSession(UUID initiatorUUID, UUID targetUUID, String initiatorName,
                       String targetName, String kickReason, int playerCount,
                       int voteDurationSeconds) {
        this.initiatorUUID = initiatorUUID;
        this.targetUUID = targetUUID;
        this.initiatorName = initiatorName;
        this.targetName = targetName;
        this.kickReason = kickReason != null && !kickReason.isEmpty() ?
                kickReason : "No reason provided";
        this.startTime = System.currentTimeMillis();
        this.voteDuration = voteDurationSeconds;
        this.ticksRemaining = voteDurationSeconds * TICKS_PER_SECOND;

        // Target player can't vote, so they don't count
        this.totalEligibleVoters = playerCount - 1;

        // Calculate required votes based on config percentage
        double requiredPercentage = VoteKickMod.getConfig().getVotePassPercentage();
        this.totalVotesNeeded = Math.max(1, (int) Math.ceil(totalEligibleVoters * requiredPercentage));

        // Initiator automatically votes yes
        playerVotes.put(initiatorUUID, true);

        LOGGER.debug("Vote started: eligible voters={}, votes needed={}",
                totalEligibleVoters, totalVotesNeeded);

        // Clean up expired cooldowns - might as well do it here
        cleanupExpiredCooldowns();
    }

    /**
     * Called every tick while vote is active.
     * Counts down timer and handles UI update schedule.
     */
    public void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }

        if (uiUpdateTick >= UI_UPDATE_INTERVAL) {
            uiUpdateTick = 0;
        } else {
            uiUpdateTick++;
        }
    }

    /**
     * Has the vote finished yet?
     */
    public boolean hasEnded() {
        // Give a second for everyone to see the vote started before
        // allowing it to fail (prevents flash votes that nobody sees)
        long startDelayTime = 1000;
        if (System.currentTimeMillis() - startTime < startDelayTime) {
            // Don't check for failure in the first second
            return ticksRemaining <= 0 || hasEnoughVotes();
        }

        // After the delay, check all end conditions
        return ticksRemaining <= 0 || hasEnoughVotes() || hasFailedVote();
    }

    /**
     * Updates vote UI for all players.
     * Called by the server tick handler.
     */
    public void updateVoteUI(MinecraftServer server) {
        if (server == null) return;

        // First update - play sound and show UI
        if (!hasPlayedStartSound) {
            playSoundToAll(server, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            hasPlayedStartSound = true;
            sendInitialVoteUI(server);
        }

        // Let people know when vote is almost over
        processTimeAnnouncements(server);

        // Only update UI every half second to reduce packet spam
        if (uiUpdateTick == 0) {
            updateVotePanels(server);
        }
    }

    public String getKickReason() {
        return kickReason;
    }

    /**
     * Shows the vote UI to everyone when vote starts
     */
    private void sendInitialVoteUI(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String title = "Kick player: " + targetName + "?";
            String subtitle = "Reason: " + kickReason;
            boolean isTarget = player.getUUID().equals(targetUUID);

            VoteKickNetworking.sendShowVotePanel(
                    player,
                    title,
                    subtitle,
                    getSecondsRemaining(),
                    getYesVotes(),
                    getNoVotes(),
                    totalVotesNeeded,
                    isTarget
            );
        }
    }

    /**
     * Sends chat messages when vote is running out of time
     */
    private void processTimeAnnouncements(MinecraftServer server) {
        int secondsRemaining = getSecondsRemaining();

        if (announcementTimes.contains(secondsRemaining)) {
            // Remove so we don't announce the same time twice
            announcementTimes.remove(secondsRemaining);

            // Let everyone know time is running out
            Component announcement = Component.literal("Vote: " + secondsRemaining + " seconds remaining")
                    .setStyle(Style.EMPTY.withColor(COLOR_WARNING));
            server.getPlayerList().broadcastSystemMessage(announcement, false);

            // Play a sound so people notice even if they're not reading chat
            playSoundToAll(server, SoundEvents.NOTE_BLOCK_HAT.value(), 0.7F, 1.0F);
        }
    }

    /**
     * Updates the counters on everyone's vote UI
     */
    private void updateVotePanels(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            VoteKickNetworking.sendUpdateVotePanel(
                    player,
                    getSecondsRemaining(),
                    getYesVotes(),
                    getNoVotes()
            );
        }
    }

    /**
     * Records a player's vote and updates UI.
     *
     * @param player Player voting
     * @param inFavor true=yes, false=no
     * @return success - false if already voted or ineligible
     */
    public boolean castVote(ServerPlayer player, boolean inFavor) {
        if (player == null || hasEnded()) {
            return false;
        }

        UUID playerUUID = player.getUUID();

        // Target can't vote on their own kick + no double voting
        if (playerVotes.containsKey(playerUUID) || playerUUID.equals(targetUUID)) {
            return false;
        }

        // Record the vote
        playerVotes.put(playerUUID, inFavor);

        // Play sounds and show chat message
        MinecraftServer server = player.getServer();
        if (server != null) {
            // Different sounds for yes/no votes
            SoundEvent sound = inFavor ? SoundEvents.UI_BUTTON_CLICK.value() : SoundEvents.VILLAGER_NO;
            playSoundToAll(server, sound, 0.5F, inFavor ? 1.2F : 0.8F);

            // Tell everyone how they voted
            broadcastVote(server, player.getScoreboardName(), inFavor);

            // Update numbers on UI immediately
            updateVotePanels(server);
        }

        return true;
    }

    /**
     * Tells everyone who voted what
     */
    private void broadcastVote(MinecraftServer server, String playerName, boolean inFavor) {
        String voteText = inFavor ? "YES" : "NO";
        Component message = Component.literal(playerName + " voted " + voteText)
                .setStyle(Style.EMPTY.withColor(inFavor ? COLOR_YES : COLOR_NO));

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /**
     * Handles what happens when vote ends.
     * Called automatically when hasEnded() returns true.
     */
    public void processResults(MinecraftServer server) {
        if (server == null || hasPlayedEndSound) {
            return;
        }

        // Mark as processed
        hasPlayedEndSound = true;
        boolean voteSucceeded = hasEnoughVotes();

        // Play result sound
        SoundEvent sound = voteSucceeded ?
                SoundEvents.PLAYER_LEVELUP :
                SoundEvents.VILLAGER_NO;
        playSoundToAll(server, sound, 1.0F, voteSucceeded ? 1.0F : 0.7F);

        // Hide vote UI from everyone
        VoteKickNetworking.broadcastHideVotePanel(server.getPlayerList().getPlayers());

        // Show result message in chat
        broadcastResult(server, voteSucceeded);

        // Kick player after short delay if vote passed
        if (voteSucceeded && !kickScheduled) {
            scheduleKick(server);
        }

        // Put initiator on cooldown to prevent spam
        startCooldown(initiatorUUID);
    }

    /**
     * Tells everyone if vote passed or failed
     */
    private void broadcastResult(MinecraftServer server, boolean voteSucceeded) {
        String resultText = voteSucceeded ?
                "Vote passed! Player " + targetName + " will be kicked. Reason: " + kickReason :
                "Vote failed! Not enough votes to kick " + targetName + ". Reason was: " + kickReason;

        Component resultMessage = Component.literal(resultText)
                .setStyle(Style.EMPTY.withColor(voteSucceeded ? COLOR_YES : COLOR_NO));

        server.getPlayerList().broadcastSystemMessage(resultMessage, false);
    }

    /**
     * Schedules player kick with small delay.
     * Delay gives them a chance to see why they were kicked.
     */
    private void scheduleKick(MinecraftServer server) {
        kickScheduled = true;

        // Use Timer for delay - there's probably a better way to do this
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                kickPlayer(server);
            }
        }, KICK_DELAY_MS);
    }

    /**
     * Actually kicks the player and tells everyone
     */
    private void kickPlayer(MinecraftServer server) {
        try {
            ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                // Message shown on kick screen
                Component kickMessage = Component.literal("You were voted off\nReason: " + kickReason)
                        .setStyle(Style.EMPTY.withColor(COLOR_NO));

                target.connection.disconnect(kickMessage);

                // Tell everyone
                Component broadcastMessage = Component.literal(targetName +
                                " has been removed from the game (Reason: " + kickReason + ")")
                        .setStyle(Style.EMPTY.withColor(COLOR_WARNING));
                server.getPlayerList().broadcastSystemMessage(broadcastMessage, false);
            }
        } catch (Exception e) {
            LOGGER.error("Error kicking player: {}", e.getMessage());
        }
    }

    /**
     * Plays a sound for all players.
     * Used for vote notifications/feedback.
     */
    private void playSoundToAll(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to play sound effect: {}", e.getMessage());
        }
    }

    /**
     * Checks if enough yes votes to pass
     */
    private boolean hasEnoughVotes() {
        return getYesVotes() >= totalVotesNeeded;
    }

    /**
     * Checks if vote has already failed.
     * This happens when there aren't enough eligible voters left
     * to reach the required yes votes.
     */
    private boolean hasFailedVote() {
        // Get current counts
        int yesVotes = getYesVotes();
        int noVotes = getNoVotes();

        // People who haven't voted yet
        int remainingVoters = totalEligibleVoters - yesVotes - noVotes;

        // Check if it's mathematically impossible to reach needed votes
        boolean failed = yesVotes + remainingVoters < totalVotesNeeded;

        if (failed) {
            LOGGER.debug("Vote marked as failed: yes={}, no={}, remaining={}, needed={}",
                    yesVotes, noVotes, remainingVoters, totalVotesNeeded);
        }

        return failed;
    }

    /**
     * How many players can vote in this session
     */
    private int getPlayerCount() {
        return totalEligibleVoters;
    }

    /**
     * Puts a player on cooldown so they can't spam votes.
     * Duration from config.
     */
    public static void startCooldown(UUID playerUUID) {
        if (playerUUID == null) return;

        int cooldownSeconds = VoteKickMod.getConfig().getCooldownSeconds();
        if (cooldownSeconds <= 0) {
            return; // Disabled in config
        }

        // Calculate when cooldown ends
        long cooldownEndTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);

        // Store it
        playersOnCooldown.put(playerUUID, cooldownEndTime);

        LOGGER.debug("Player {} put on cooldown for {} seconds", playerUUID, cooldownSeconds);
    }

    /**
     * Checks if player is on cooldown and can't start votes
     */
    public static boolean isOnCooldown(UUID playerUUID) {
        if (playerUUID == null) return false;

        // Randomly clean up expired cooldowns to avoid memory leak
        // Not super efficient but it works
        if (Math.random() < 0.1) { // 10% chance per check
            cleanupExpiredCooldowns();
        }

        // Check if player has cooldown and it's still valid
        Long cooldownEnd = playersOnCooldown.get(playerUUID);
        if (cooldownEnd != null) {
            boolean onCooldown = System.currentTimeMillis() < cooldownEnd;

            // Clean up expired entry
            if (!onCooldown) {
                playersOnCooldown.remove(playerUUID);
            }

            return onCooldown;
        }

        return false;
    }

    /**
     * Gets seconds left on cooldown for a player
     */
    public static int getRemainingCooldown(UUID playerUUID) {
        if (playerUUID == null) return 0;

        Long cooldownEnd = playersOnCooldown.get(playerUUID);
        if (cooldownEnd != null) {
            long remainingMillis = cooldownEnd - System.currentTimeMillis();
            if (remainingMillis > 0) {
                return (int)(remainingMillis / 1000);
            } else {
                // Auto-cleanup
                playersOnCooldown.remove(playerUUID);
            }
        }

        return 0;
    }

    /**
     * Cleans up expired cooldowns to avoid memory bloat
     */
    private static void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        playersOnCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    // Getters for various values

    public UUID getInitiatorUUID() {
        return initiatorUUID;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public String getTargetName() {
        return targetName;
    }

    public int getYesVotes() {
        return (int) playerVotes.values().stream().filter(Boolean::booleanValue).count();
    }

    public int getNoVotes() {
        return (int) playerVotes.values().stream().filter(vote -> !vote).count();
    }

    public int getTotalVotes() {
        return playerVotes.size();
    }

    public int getTotalVotesNeeded() {
        return totalVotesNeeded;
    }

    public int getSecondsRemaining() {
        return Math.max(0, ticksRemaining / TICKS_PER_SECOND);
    }

    public long getVoteAgeMillis() {
        return System.currentTimeMillis() - startTime;
    }

    public Map<UUID, Boolean> getPlayerVotes() {
        return Collections.unmodifiableMap(playerVotes);
    }

    public boolean hasPlayerVoted(UUID playerUUID) {
        return playerVotes.containsKey(playerUUID);
    }

    public long getStartTime() {
        return startTime;
    }

    public int getVoteDuration() {
        return voteDuration;
    }
}