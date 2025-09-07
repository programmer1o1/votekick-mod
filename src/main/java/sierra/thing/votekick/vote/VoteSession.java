// VoteSession.java
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
import sierra.thing.votekick.protection.PlayerProtectionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoteSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    private static final TextColor COLOR_YES = TextColor.fromRgb(0x55FF55);
    private static final TextColor COLOR_NO = TextColor.fromRgb(0xFF5555);
    private static final TextColor COLOR_WARNING = TextColor.fromRgb(0xFFAA00);
    private static final TextColor COLOR_INFO = TextColor.fromRgb(0xFFFF55);

    private static final int TICKS_PER_SECOND = 20;
    private static final int UI_UPDATE_INTERVAL = 10;
    private static final int KICK_DELAY_MS = 2000;

    private final UUID initiatorUUID;
    private final UUID targetUUID;
    private final String initiatorName;
    private final String targetName;
    private final Map<UUID, Boolean> playerVotes = new HashMap<>();
    private final int totalVotesNeeded;
    private final long startTime;
    private final int voteDuration;
    private final int totalEligibleVoters;

    private int ticksRemaining;
    private int uiUpdateTick = 0;
    private boolean hasInitializedUI = false;
    private boolean hasPlayedEndSound = false;
    private boolean kickScheduled = false;
    private final String kickReason;

    // cooldown tracking with vote fatigue for same targets
    private static final Map<UUID, Long> playersOnCooldown = new ConcurrentHashMap<>();
    private static final Map<String, Long> targetVoteCooldowns = new ConcurrentHashMap<>();

    private final Set<Integer> announcementTimes = new HashSet<>(Arrays.asList(30, 15, 5));

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

        this.totalEligibleVoters = playerCount - 1;

        // calculate required votes with protection modifier
        PlayerProtectionManager protectionManager = VoteKickMod.getProtectionManager();
        double requiredPercentage = VoteKickMod.getConfig().getVotePassPercentage();
        double modifier = protectionManager.getVoteThresholdModifier(targetUUID);

        // apply modifier but cap at 90% to keep it reasonable
        double adjustedPercentage = Math.min(requiredPercentage * modifier, 0.9);

        this.totalVotesNeeded = Math.max(1, (int) Math.ceil(totalEligibleVoters * adjustedPercentage));

        // auto yes vote from initiator
        playerVotes.put(initiatorUUID, true);

        LOGGER.debug("Vote started: eligible voters={}, votes needed={} (modifier={})",
                totalEligibleVoters, totalVotesNeeded, modifier);

        cleanupExpiredCooldowns();
    }

    /**
     * check if initiator is trying to spam votes against same target
     */
    public static boolean isTargetOnCooldown(UUID initiatorUUID, UUID targetUUID) {
        String key = initiatorUUID.toString() + ":" + targetUUID.toString();
        Long cooldownEnd = targetVoteCooldowns.get(key);

        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            return true;
        }

        // cleanup if expired
        if (cooldownEnd != null) {
            targetVoteCooldowns.remove(key);
        }

        return false;
    }

    /**
     * get remaining cooldown for voting against specific target
     */
    public static int getRemainingTargetCooldown(UUID initiatorUUID, UUID targetUUID) {
        String key = initiatorUUID.toString() + ":" + targetUUID.toString();
        Long cooldownEnd = targetVoteCooldowns.get(key);

        if (cooldownEnd != null) {
            long remaining = cooldownEnd - System.currentTimeMillis();
            return remaining > 0 ? (int)(remaining / 1000) : 0;
        }

        return 0;
    }

    /**
     * start cooldown for voting against same target
     */
    private static void startTargetCooldown(UUID initiatorUUID, UUID targetUUID) {
        String key = initiatorUUID.toString() + ":" + targetUUID.toString();
        long cooldownMs = VoteKickMod.getConfig().getTargetCooldownSeconds() * 1000L;
        targetVoteCooldowns.put(key, System.currentTimeMillis() + cooldownMs);
    }

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

    public boolean hasEnded() {
        long startDelayTime = 1000;
        if (System.currentTimeMillis() - startTime < startDelayTime) {
            return ticksRemaining <= 0 || hasEnoughVotes();
        }

        return ticksRemaining <= 0 || hasEnoughVotes() || hasFailedVote();
    }

    public void updateVoteUI(MinecraftServer server) {
        if (server == null) return;

        // send initial UI only once
        if (!hasInitializedUI) {
            hasInitializedUI = true;
            sendInitialVoteUI(server);
        }

        processTimeAnnouncements(server);

        if (uiUpdateTick == 0) {
            updateVotePanels(server);
        }
    }

    public String getKickReason() {
        return kickReason;
    }

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

    private void processTimeAnnouncements(MinecraftServer server) {
        int secondsRemaining = getSecondsRemaining();

        if (announcementTimes.contains(secondsRemaining)) {
            announcementTimes.remove(secondsRemaining);

            Component announcement = Component.literal("Vote: " + secondsRemaining + " seconds remaining")
                    .setStyle(Style.EMPTY.withColor(COLOR_WARNING));
            server.getPlayerList().broadcastSystemMessage(announcement, false);

            // time warning sounds are now handled client-side
            // clients will play warning sounds based on their settings
        }
    }

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

    public boolean castVote(ServerPlayer player, boolean inFavor) {
        if (player == null || hasEnded()) {
            return false;
        }

        UUID playerUUID = player.getUUID();

        if (playerVotes.containsKey(playerUUID) || playerUUID.equals(targetUUID)) {
            return false;
        }

        playerVotes.put(playerUUID, inFavor);

        MinecraftServer server = player.getServer();
        if (server != null) {
            // vote sounds are now handled client-side when the vote is cast
            // no server-side sounds needed here

            broadcastVote(server, player.getScoreboardName(), inFavor);
            updateVotePanels(server);
        }

        return true;
    }

    private void broadcastVote(MinecraftServer server, String playerName, boolean inFavor) {
        String voteText = inFavor ? "YES" : "NO";
        Component message = Component.literal(playerName + " voted " + voteText)
                .setStyle(Style.EMPTY.withColor(inFavor ? COLOR_YES : COLOR_NO));

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    public void processResults(MinecraftServer server) {
        if (server == null || hasPlayedEndSound) {
            return;
        }

        hasPlayedEndSound = true;
        boolean voteSucceeded = hasEnoughVotes();

        // result sounds are handled client-side when the panel is hidden
        // no need for server-side sounds

        VoteKickNetworking.broadcastHideVotePanel(server.getPlayerList().getPlayers());

        broadcastResult(server, voteSucceeded);

        if (voteSucceeded && !kickScheduled) {
            scheduleKick(server);
        }

        // cooldowns for anti-spam
        startCooldown(initiatorUUID);
        startTargetCooldown(initiatorUUID, targetUUID);
    }

    private void broadcastResult(MinecraftServer server, boolean voteSucceeded) {
        String resultText = voteSucceeded ?
                "Vote passed! Player " + targetName + " will be kicked. Reason: " + kickReason :
                "Vote failed! Not enough votes to kick " + targetName + ". Reason was: " + kickReason;

        Component resultMessage = Component.literal(resultText)
                .setStyle(Style.EMPTY.withColor(voteSucceeded ? COLOR_YES : COLOR_NO));

        server.getPlayerList().broadcastSystemMessage(resultMessage, false);
    }

    private void scheduleKick(MinecraftServer server) {
        kickScheduled = true;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                kickPlayer(server);
            }
        }, KICK_DELAY_MS);
    }

    private void kickPlayer(MinecraftServer server) {
        try {
            ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                // record the kick for protection system
                VoteKickMod.getProtectionManager().recordKick(targetUUID, kickReason);

                Component kickMessage = Component.literal("You were voted off\nReason: " + kickReason)
                        .setStyle(Style.EMPTY.withColor(COLOR_NO));

                target.connection.disconnect(kickMessage);

                Component broadcastMessage = Component.literal(targetName +
                                " has been removed from the game (Reason: " + kickReason + ")")
                        .setStyle(Style.EMPTY.withColor(COLOR_WARNING));
                server.getPlayerList().broadcastSystemMessage(broadcastMessage, false);
            }
        } catch (Exception e) {
            LOGGER.error("Error kicking player: {}", e.getMessage());
        }
    }

    private boolean hasEnoughVotes() {
        return getYesVotes() >= totalVotesNeeded;
    }

    private boolean hasFailedVote() {
        int yesVotes = getYesVotes();
        int noVotes = getNoVotes();
        int remainingVoters = totalEligibleVoters - yesVotes - noVotes;

        boolean failed = yesVotes + remainingVoters < totalVotesNeeded;

        if (failed) {
            LOGGER.debug("Vote marked as failed: yes={}, no={}, remaining={}, needed={}",
                    yesVotes, noVotes, remainingVoters, totalVotesNeeded);
        }

        return failed;
    }

    private int getPlayerCount() {
        return totalEligibleVoters;
    }

    public static void startCooldown(UUID playerUUID) {
        if (playerUUID == null) return;

        int cooldownSeconds = VoteKickMod.getConfig().getCooldownSeconds();
        if (cooldownSeconds <= 0) {
            return;
        }

        long cooldownEndTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        playersOnCooldown.put(playerUUID, cooldownEndTime);

        LOGGER.debug("Player {} put on cooldown for {} seconds", playerUUID, cooldownSeconds);
    }

    public static boolean isOnCooldown(UUID playerUUID) {
        if (playerUUID == null) return false;

        if (Math.random() < 0.1) {
            cleanupExpiredCooldowns();
        }

        Long cooldownEnd = playersOnCooldown.get(playerUUID);
        if (cooldownEnd != null) {
            boolean onCooldown = System.currentTimeMillis() < cooldownEnd;

            if (!onCooldown) {
                playersOnCooldown.remove(playerUUID);
            }

            return onCooldown;
        }

        return false;
    }

    public static int getRemainingCooldown(UUID playerUUID) {
        if (playerUUID == null) return 0;

        Long cooldownEnd = playersOnCooldown.get(playerUUID);
        if (cooldownEnd != null) {
            long remainingMillis = cooldownEnd - System.currentTimeMillis();
            if (remainingMillis > 0) {
                return (int)(remainingMillis / 1000);
            } else {
                playersOnCooldown.remove(playerUUID);
            }
        }

        return 0;
    }

    private static void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        playersOnCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
        targetVoteCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    // getters
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