package sierra.thing.votekick;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.commands.VoteKickCommand;
import sierra.thing.votekick.commands.VoteKickDevCommand;
import sierra.thing.votekick.config.VoteKickConfig;
import sierra.thing.votekick.history.VoteHistoryManager;
import sierra.thing.votekick.platform.Platform;
//? if fabric {
import sierra.thing.votekick.platform.fabric.FabricPlatform;
//?} else if neoforge {
/*import sierra.thing.votekick.platform.neoforge.NeoforgePlatform;
*///?}
import sierra.thing.votekick.protection.PlayerProtectionManager;
import sierra.thing.votekick.vote.VoteOutcome;
import sierra.thing.votekick.vote.VoteSession;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class VoteKickMod {
    public static final String MOD_ID = /*$ mod_id*/ "votekick";
    public static final String VERSION = /*$ mod_version*/ "3.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    //? if >=1.21.11 {
    /*public static final Identifier MOD_PRESENCE_CHANNEL = Identifier.fromNamespaceAndPath(MOD_ID, "presence");
    *///?} else if >=1.21 {
    /*public static final ResourceLocation MOD_PRESENCE_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "presence");
    *///?} else {
    public static final ResourceLocation MOD_PRESENCE_CHANNEL = new ResourceLocation(MOD_ID, "presence");
    //?}

    private static final Platform PLATFORM = createPlatformInstance();

    private static VoteKickConfig config;
    private static final Map<UUID, VoteSession> activeVotes = new HashMap<>();
    private static PlayerProtectionManager protectionManager;
    private static VoteHistoryManager historyManager;
    private static volatile boolean serverStopping = false;

    public static void init() {
        LOGGER.info("VoteKick v{} loading on {}", VERSION, PLATFORM.loader());

        loadConfig();
        protectionManager = new PlayerProtectionManager();
        historyManager = new VoteHistoryManager();
    }

    public static Platform platform() {
        return PLATFORM;
    }

    public static String profileName(GameProfile profile) {
        //? if >=1.21.9 {
        /*return profile.name();
        *///?} else {
        return profile.getName();
        //?}
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        VoteKickCommand.register(dispatcher);
        if (platform().isDevelopmentEnvironment()) {
            VoteKickDevCommand.register(dispatcher);
        }
    }

    public static void onServerStarting(MinecraftServer server) {
        serverStopping = false;
        protectionManager.load();
        historyManager.load();
    }

    public static void onServerStopping(MinecraftServer server) {
        serverStopping = true;
        protectionManager.save();
        historyManager.save();
    }

    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 1200 == 0) { // every minute
            protectionManager.cleanup();
        }

        activeVotes.entrySet().removeIf(entry -> {
            VoteSession session = entry.getValue();
            session.tick();

            if (session.hasEnded()) {
                session.processResults(server);
                return true;
            }

            session.updateVoteUI(server);
            return false;
        });
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!protectionManager.hasJoinedBefore(player.getUUID())) {
            protectionManager.grantNewPlayerProtection(player.getUUID());
            LOGGER.info("New player {} granted protection period", profileName(player.getGameProfile()));
        }

        if (protectionManager.isProtected(player.getUUID())) {
            int remaining = protectionManager.getRemainingProtectionTime(player.getUUID());
            player.sendSystemMessage(Component.literal(
                    "You have kick immunity for " + remaining + " seconds"
            ));
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player, MinecraftServer server) {
        if (platform().isDevelopmentEnvironment()) {
            VoteKickDevCommand.onPlayerDisconnect(player, server);
        }

        UUID playerUUID = player.getUUID();

        VoteSession targetSession = activeVotes.get(playerUUID);
        if (targetSession != null) {
            targetSession.endVote(server, VoteOutcome.CANCELED, null,
                    "Vote canceled: " + targetSession.getTargetName() + " left the game", false);
            activeVotes.remove(playerUUID);
            return;
        }

        for (VoteSession session : activeVotes.values()) {
            if (session.hasPlayerVoted(playerUUID)) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    sierra.thing.votekick.network.VoteKickNetworking.sendUpdateVotePanel(
                            p,
                            session.getSecondsRemaining(),
                            session.getYesVotes(),
                            session.getNoVotes()
                    );
                }
                break;
            }
        }
    }

    private static void loadConfig() {
        File configDir = platform().getConfigDir().toFile();
        File configFile = new File(configDir, MOD_ID + ".properties");
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                props.load(reader);
                LOGGER.info("Loaded VoteKick config");
            } catch (IOException e) {
                LOGGER.error("Failed to read config", e);
            }
        }

        config = new VoteKickConfig(props);

        try (FileWriter writer = new FileWriter(configFile)) {
            config.updateProperties(props);
            props.store(writer, "VoteKick Mod Configuration");
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public static void reloadConfig() {
        loadConfig();
        if (historyManager != null) {
            historyManager.load();
        }
    }

    private static Platform createPlatformInstance() {
        //? if fabric {
        return new FabricPlatform();
        //?} else if neoforge {
        /*return new NeoforgePlatform();
        *///?}
    }

    public static VoteKickConfig getConfig() {
        return config;
    }

    public static PlayerProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public static VoteHistoryManager getHistoryManager() {
        return historyManager;
    }

    public static Map<UUID, VoteSession> getActiveVotes() {
        return activeVotes;
    }

    public static boolean isServerStopping() {
        return serverStopping;
    }

    public static void addVote(UUID targetUUID, VoteSession session) {
        activeVotes.put(targetUUID, session);
    }

    public static void removeVote(UUID targetUUID) {
        activeVotes.remove(targetUUID);
    }

    public static boolean isVoteInProgress() {
        return !activeVotes.isEmpty();
    }
}
