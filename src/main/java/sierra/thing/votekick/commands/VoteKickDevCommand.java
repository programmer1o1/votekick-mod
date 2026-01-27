package sierra.thing.votekick.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.commands.VoteKickCommand;
import sierra.thing.votekick.permissions.VoteKickPermissions;
import sierra.thing.votekick.protection.PlayerProtectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class VoteKickDevCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    private static final TextColor ERROR_COLOR = TextColor.fromRgb(0xFF5555);
    private static final TextColor INFO_COLOR = TextColor.fromRgb(0xAAAAAA);
    private static final TextColor SUCCESS_COLOR = TextColor.fromRgb(0x55FF55);

    private static final String CARPET_MOD_ID = "carpet";
    private static final String NAME_PREFIX = "vk_test_";
    private static final int NAME_ATTEMPTS = 6;
    private static final String TEST_REASON = "devtest";
    private static final int SPAWN_CHECK_INTERVAL_MS = 250;
    private static final int SPAWN_CHECK_ATTEMPTS = 20;
    private static final int SECOND_SPAWN_DELAY_MS = 500;
    private static final int VOTE_START_DELAY_MS = 1500;
    private static final int AUTO_VOTE_DELAY_MS = 2500;
    private static final int CLEANUP_DELAY_MS = 8000;
    private static final Timer CLEANUP_TIMER = new Timer("votekick-devtest-cleanup", true);
    private static final Set<String> ACTIVE_TEST_PLAYERS = ConcurrentHashMap.newKeySet();

    private VoteKickDevCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("votekick-test")
                        .requires(source -> VoteKickMod.platform().isDevelopmentEnvironment())
                        .executes(context -> runTest(context.getSource()))
        );

        dispatcher.register(
                Commands.literal("vk-test")
                        .requires(source -> VoteKickMod.platform().isDevelopmentEnvironment())
                        .executes(context -> runTest(context.getSource()))
        );
    }

    private static int runTest(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        if (!VoteKickMod.platform().isDevelopmentEnvironment()) {
            sendError(source, "This command is only available in development environments.");
            return 0;
        }

        if (!VoteKickMod.platform().isModLoaded(CARPET_MOD_ID)) {
            sendError(source, "Carpet mod is not loaded. Install it to run the dev test.");
            return 0;
        }

        if (!VoteKickMod.getActiveVotes().isEmpty()) {
            sendError(source, "A vote is already in progress. Finish it before running the dev test.");
            return 0;
        }

        String firstName = generateTestName(server);
        String secondName = generateTestName(server, firstName);

        List<String> spawnedPlayers = new ArrayList<>();
        if (!spawnFakePlayer(server, source, firstName)) {
            sendError(source, "Failed to spawn Carpet test player " + firstName + ".");
            return 0;
        }
        spawnedPlayers.add(firstName);
        registerTestPlayer(firstName);

        sendInfo(source, "Dev test scheduled: vote starts in " + (VOTE_START_DELAY_MS / 1000.0) + "s, " +
                "auto-votes in " + (AUTO_VOTE_DELAY_MS / 1000.0) + "s, cleanup in " +
                (CLEANUP_DELAY_MS / 1000.0) + "s.");

        scheduleFirstJoinCheck(source, server, spawnedPlayers, firstName, secondName, SPAWN_CHECK_ATTEMPTS);
        return 1;
    }

    private static ServerPlayer pickInitiator(CommandSourceStack source, MinecraftServer server,
                                              ServerPlayer firstPlayer, ServerPlayer secondPlayer) {
        if (source.getEntity() instanceof ServerPlayer player && VoteKickPermissions.canStartVote(player)) {
            return player;
        }

        if (firstPlayer != null && VoteKickPermissions.canStartVote(firstPlayer)) {
            return firstPlayer;
        }

        if (secondPlayer != null && VoteKickPermissions.canStartVote(secondPlayer)) {
            return secondPlayer;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (VoteKickPermissions.canStartVote(player)) {
                return player;
            }
        }

        return null;
    }

    private static String generateTestName(MinecraftServer server) {
        return generateTestName(server, null);
    }

    private static String generateTestName(MinecraftServer server, String reservedName) {
        for (int attempt = 0; attempt < NAME_ATTEMPTS; attempt++) {
            String suffix = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            String name = NAME_PREFIX + suffix;
            if (reservedName != null && reservedName.equalsIgnoreCase(name)) {
                continue;
            }
            if (server.getPlayerList().getPlayerByName(name) == null) {
                return name;
            }
        }
        return NAME_PREFIX + "0000";
    }

    private static String sanitizePlayerName(String name) {
        if (name == null) return "";
        // Only allow valid Minecraft username characters
        return name.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private static boolean spawnFakePlayer(MinecraftServer server, CommandSourceStack source, String name) {
        if (findPlayerByName(server, name) != null) {
            return true;
        }

        CommandSourceStack spawnSource = source.getEntity() instanceof ServerPlayer player
                ? player.createCommandSourceStack()
                : server.createCommandSourceStack();

        return executeCommand(spawnSource, "player " + sanitizePlayerName(name) + " spawn");
    }

    private static boolean executeCommand(CommandSourceStack source, String command) {
        try {
            return source.getServer().getCommands().getDispatcher().execute(command, source) > 0;
        } catch (CommandSyntaxException e) {
            LOGGER.warn("Dev test command failed: {}", command, e);
            return false;
        }
    }

    private static void scheduleCleanup(MinecraftServer server, List<String> names) {
        if (names.isEmpty()) {
            return;
        }

        CLEANUP_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    if (shouldAbort(server)) {
                        return;
                    }
                    cleanupNow(server, names);
                });
            }
        }, CLEANUP_DELAY_MS);
    }

    private static void scheduleVoteStart(CommandSourceStack source, MinecraftServer server, ServerPlayer initiator,
                                          ServerPlayer target, List<String> spawnedPlayers, String targetName) {
        CLEANUP_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    if (shouldAbort(server)) {
                        return;
                    }
                    try {
                        // Use direct API call instead of command string to prevent command injection
                        int result = VoteKickCommand.startVoteKick(
                            initiator.createCommandSourceStack(),
                            target,
                            TEST_REASON
                        );
                        if (result == 0) {
                            sendError(source, "Failed to start vote kick from " + initiator.getScoreboardName() + ".");
                            cleanupNow(server, spawnedPlayers);
                            return;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Error starting vote kick: {}", e.getMessage());
                        sendError(source, "Failed to start vote kick from " + initiator.getScoreboardName() + ".");
                        cleanupNow(server, spawnedPlayers);
                        return;
                    }

                    if (VoteKickMod.getActiveVotes().isEmpty()) {
                        sendError(source, "Vote kick did not start. Check permissions or protection settings.");
                        cleanupNow(server, spawnedPlayers);
                        return;
                    }

                    scheduleAutoVotes(source, server, initiator, target, spawnedPlayers, targetName);
                });
            }
        }, VOTE_START_DELAY_MS);
    }

    private static void scheduleAutoVotes(CommandSourceStack source, MinecraftServer server, ServerPlayer initiator,
                                          ServerPlayer target, List<String> spawnedPlayers, String targetName) {
        CLEANUP_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    if (shouldAbort(server)) {
                        return;
                    }
                    if (VoteKickMod.getActiveVotes().isEmpty()) {
                        sendInfo(source, "No active vote found when auto-voting. Cleaning up test players.");
                        cleanupNow(server, spawnedPlayers);
                        return;
                    }

                    int votesCast = 0;
                    for (ServerPlayer voter : server.getPlayerList().getPlayers()) {
                        if (voter.getUUID().equals(target.getUUID()) || voter.getUUID().equals(initiator.getUUID())) {
                            continue;
                        }
                        if (executeCommand(voter.createCommandSourceStack(), "vote yes")) {
                            votesCast++;
                        }
                    }

                    sendInfo(source, "Dev test started: " + initiator.getScoreboardName() + " -> " + targetName +
                            ". Extra yes votes cast: " + votesCast + ".");
                    sendSuccess(source, "Watch chat/UI for the vote result. Cleanup scheduled in " +
                            (CLEANUP_DELAY_MS / 1000) + "s.");

                    scheduleCleanup(server, spawnedPlayers);
                });
            }
        }, AUTO_VOTE_DELAY_MS);
    }

    private static void scheduleFirstJoinCheck(CommandSourceStack source, MinecraftServer server,
                                               List<String> spawnedPlayers, String firstName,
                                               String secondName, int remainingChecks) {
        CLEANUP_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    if (shouldAbort(server)) {
                        return;
                    }
                    ServerPlayer firstPlayer = findPlayerByName(server, firstName);
                    if (firstPlayer != null) {
                        scheduleSecondSpawn(source, server, spawnedPlayers, firstName, secondName);
                        return;
                    }

                    if (remainingChecks <= 1) {
                        sendError(source, "First Carpet test player did not spawn in time. Check server logs for details.");
                        cleanupNow(server, spawnedPlayers);
                        return;
                    }

                    scheduleFirstJoinCheck(source, server, spawnedPlayers, firstName, secondName, remainingChecks - 1);
                });
            }
        }, SPAWN_CHECK_INTERVAL_MS);
    }

    private static void scheduleSecondSpawn(CommandSourceStack source, MinecraftServer server,
                                            List<String> spawnedPlayers, String firstName,
                                            String secondName) {
        CLEANUP_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    if (shouldAbort(server)) {
                        return;
                    }

                    if (!spawnFakePlayer(server, source, secondName)) {
                        sendError(source, "Failed to spawn Carpet test player " + secondName + ".");
                        cleanupNow(server, spawnedPlayers);
                        return;
                    }

                    if (!spawnedPlayers.contains(secondName)) {
                        spawnedPlayers.add(secondName);
                        registerTestPlayer(secondName);
                    }

                    scheduleSecondJoinCheck(source, server, spawnedPlayers, firstName, secondName, SPAWN_CHECK_ATTEMPTS);
                });
            }
        }, SECOND_SPAWN_DELAY_MS);
    }

    private static void scheduleSecondJoinCheck(CommandSourceStack source, MinecraftServer server,
                                                List<String> spawnedPlayers, String firstName,
                                                String secondName, int remainingChecks) {
        CLEANUP_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    if (shouldAbort(server)) {
                        return;
                    }

                    ServerPlayer firstPlayer = findPlayerByName(server, firstName);
                    ServerPlayer secondPlayer = findPlayerByName(server, secondName);
                    if (firstPlayer != null && secondPlayer != null) {
                        PlayerProtectionManager protectionManager = VoteKickMod.getProtectionManager();
                        protectionManager.clearProtection(firstPlayer.getUUID());
                        protectionManager.clearProtection(secondPlayer.getUUID());

                        ServerPlayer initiator = pickInitiator(source, server, firstPlayer, secondPlayer);
                        if (initiator == null) {
                            sendError(source, "No player with permission to start a vote was found.");
                            cleanupNow(server, spawnedPlayers);
                            return;
                        }

                        ServerPlayer target = initiator.getUUID().equals(firstPlayer.getUUID()) ? secondPlayer : firstPlayer;

                        int playerCount = server.getPlayerList().getPlayerCount();
                        int minPlayers = VoteKickMod.getConfig().getMinimumPlayers();
                        if (playerCount < minPlayers) {
                            sendError(source, "Need at least " + minPlayers + " players online. Currently " + playerCount + ".");
                            cleanupNow(server, spawnedPlayers);
                            return;
                        }

                        scheduleVoteStart(source, server, initiator, target, spawnedPlayers, target.getScoreboardName());
                        return;
                    }

                    if (remainingChecks <= 1) {
                        sendError(source, "Second Carpet test player did not spawn in time. Check server logs for details.");
                        cleanupNow(server, spawnedPlayers);
                        return;
                    }

                    scheduleSecondJoinCheck(source, server, spawnedPlayers, firstName, secondName, remainingChecks - 1);
                });
            }
        }, SPAWN_CHECK_INTERVAL_MS);
    }

    private static ServerPlayer findPlayerByName(MinecraftServer server, String name) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getScoreboardName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    private static void cleanupNow(MinecraftServer server, List<String> names) {
        if (names.isEmpty()) {
            return;
        }
        if (shouldAbort(server)) {
            return;
        }
        CommandSourceStack cleanupSource = server.createCommandSourceStack();
        for (String name : names) {
            executeCommand(cleanupSource, "player " + sanitizePlayerName(name) + " kill");
            unregisterTestPlayer(name);
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player, MinecraftServer server) {
        if (server == null || ACTIVE_TEST_PLAYERS.isEmpty() || VoteKickMod.isServerStopping()) {
            return;
        }

        if (player != null) {
            String leavingName = normalizeName(player.getScoreboardName());
            if (ACTIVE_TEST_PLAYERS.remove(leavingName)) {
                return;
            }
        }

        int realPlayers = 0;
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            if (!isTestPlayer(serverPlayer)) {
                realPlayers++;
            }
        }

        if (player != null && !isTestPlayerName(player.getScoreboardName())) {
            realPlayers = Math.max(0, realPlayers - 1);
        }

        if (realPlayers == 0) {
            List<String> names = new ArrayList<>(ACTIVE_TEST_PLAYERS);
            server.execute(() -> cleanupNow(server, names));
        }
    }

    private static boolean shouldAbort(MinecraftServer server) {
        return server == null || VoteKickMod.isServerStopping() || ACTIVE_TEST_PLAYERS.isEmpty();
    }

    private static boolean isTestPlayer(ServerPlayer player) {
        return player != null && isTestPlayerName(player.getScoreboardName());
    }

    private static boolean isTestPlayerName(String name) {
        if (name == null) {
            return false;
        }
        String normalized = normalizeName(name);
        return normalized.startsWith(NAME_PREFIX) || ACTIVE_TEST_PLAYERS.contains(normalized);
    }

    private static void registerTestPlayer(String name) {
        if (name == null) {
            return;
        }
        ACTIVE_TEST_PLAYERS.add(normalizeName(name));
    }

    private static void unregisterTestPlayer(String name) {
        if (name == null) {
            return;
        }
        ACTIVE_TEST_PLAYERS.remove(normalizeName(name));
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private static void sendError(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).setStyle(Style.EMPTY.withColor(ERROR_COLOR)));
    }

    private static void sendInfo(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).setStyle(Style.EMPTY.withColor(INFO_COLOR)), false);
    }

    private static void sendSuccess(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).setStyle(Style.EMPTY.withColor(SUCCESS_COLOR)), false);
    }
}
