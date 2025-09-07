// VoteKickMod.java
package sierra.thing.votekick;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.commands.VoteKickCommand;
import sierra.thing.votekick.config.VoteKickConfig;
import sierra.thing.votekick.network.CastVotePayload;
import sierra.thing.votekick.network.PayloadRegistry;
import sierra.thing.votekick.network.VoteKickNetworking;
import sierra.thing.votekick.protection.PlayerProtectionManager;
import sierra.thing.votekick.vote.VoteSession;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class VoteKickMod implements ModInitializer {
    public static final String MOD_ID = "votekick";
    public static final String VERSION = "2.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceLocation MOD_PRESENCE_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "presence");

    private static VoteKickConfig config;
    private static final Map<UUID, VoteSession> activeVotes = new HashMap<>();
    private static PlayerProtectionManager protectionManager;

    private static VoteKickMod instance;
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        LOGGER.info("VoteKick v{} loading", VERSION);

        instance = this;
        loadConfig();

        // init the protection system for anti-abuse
        protectionManager = new PlayerProtectionManager();

        PayloadRegistry.register();
        registerCommands();
        registerEventHandlers();
        registerNetworkHandlers();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                VoteKickCommand.register(dispatcher)
        );
    }

    private void registerEventHandlers() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.setServer(server);
            // load protection data when server starts
            protectionManager.load();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // save protection data when server stops
            protectionManager.save();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // cleanup old protection entries
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
        });

        // handle player joins - check if they're protected
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();

            // new player protection - they get a grace period
            if (!protectionManager.hasJoinedBefore(player.getUUID())) {
                protectionManager.grantNewPlayerProtection(player.getUUID());
                LOGGER.info("New player {} granted protection period", player.getGameProfile().getName());
            }

            // check if they're in protection period from recent kick
            if (protectionManager.isProtected(player.getUUID())) {
                int remaining = protectionManager.getRemainingProtectionTime(player.getUUID());
                player.sendSystemMessage(Component.literal(
                        "You have kick immunity for " + remaining + " seconds"
                ));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            handlePlayerDisconnect(handler.getPlayer(), server);
        });
    }

    private void handlePlayerDisconnect(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        UUID playerUUID = player.getUUID();

        VoteSession targetSession = activeVotes.get(playerUUID);
        if (targetSession != null) {
            VoteKickNetworking.broadcastHideVotePanel(server.getPlayerList().getPlayers());

            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("Vote canceled: " + targetSession.getTargetName() + " left the game"),
                    false
            );

            activeVotes.remove(playerUUID);
            return;
        }

        for (VoteSession session : activeVotes.values()) {
            if (session.hasPlayerVoted(playerUUID)) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    VoteKickNetworking.sendUpdateVotePanel(
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

    private void registerNetworkHandlers() {
        ResourceLocation CAST_VOTE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "cast_vote");

        ServerPlayNetworking.registerGlobalReceiver(CastVotePayload.TYPE, (payload, context) -> {
            context.player().getServer().execute(() -> {
                ServerPlayer player = context.player();

                server.execute(() -> {
                    if (activeVotes.isEmpty()) {
                        player.sendSystemMessage(Component.literal("No vote in progress"));
                        return;
                    }

                    VoteSession session = activeVotes.values().iterator().next();
                    boolean success = session.castVote(player, payload.voteYes());

                    if (!success) {
                                player.sendSystemMessage(Component.literal("You've already voted or aren't eligible to vote"));
                    }
                });
            });
        });

        setupModPresenceCheck();
    }

    private void setupModPresenceCheck() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sync) ->
                sender.sendPacket(MOD_PRESENCE_CHANNEL, PacketByteBufs.create())
        );

        ServerLoginNetworking.registerGlobalReceiver(MOD_PRESENCE_CHANNEL, (server, handler, understood, buf, sync, sender) -> {
            if (!understood) {
                handler.disconnect(Component.literal(
                        "This server requires the VoteKick mod.\n" +
                                "Please install it to connect."
                ));
                LOGGER.info("Rejected {} - missing VoteKick mod", handler.getUserName());
            }
        });
    }

    private void loadConfig() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
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

    public static VoteKickConfig getConfig() {
        return config;
    }

    public static PlayerProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public static Map<UUID, VoteSession> getActiveVotes() {
        return activeVotes;
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

    public static VoteKickMod getInstance() {
        return instance;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}