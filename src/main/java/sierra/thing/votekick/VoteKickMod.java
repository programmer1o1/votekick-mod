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
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Channel ID for checking if clients have the mod - kinda hacky but works
    public static final ResourceLocation MOD_PRESENCE_CHANNEL = new ResourceLocation(MOD_ID, "presence");

    private static VoteKickConfig config;
    // Stores active votes - should really only be one at a time but using a map for flexibility
    private static final Map<UUID, VoteSession> activeVotes = new HashMap<>();

    private static VoteKickMod instance;
    private MinecraftServer server;
    @Override
    public void onInitialize() {
        LOGGER.info("VoteKick v{} loading", VERSION);

        instance = this;
        loadConfig();
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
        });

        // Update vote timers and UI - maybe I should move this to only run when votes are active?
        ServerTickEvents.END_SERVER_TICK.register(server -> {
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

        // Handle player disconnects during a vote
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            handlePlayerDisconnect(handler.getPlayer(), server);
        });
    }

    private void handlePlayerDisconnect(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        UUID playerUUID = player.getUUID();

        // Is this the player being voted on?
        VoteSession targetSession = activeVotes.get(playerUUID);
        if (targetSession != null) {
            // Target left - rage quit? lol
            VoteKickNetworking.broadcastHideVotePanel(server.getPlayerList().getPlayers());

            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("Vote canceled: " + targetSession.getTargetName() + " left the game"),
                    false
            );

            activeVotes.remove(playerUUID);
            return;
        }

        // Maybe they were a voter - need to update UI if so
        for (VoteSession session : activeVotes.values()) {
            if (session.hasPlayerVoted(playerUUID)) {
                // TODO: should we remove their vote if they disconnect? For now it stays
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    VoteKickNetworking.sendUpdateVotePanel(
                            p,
                            session.getSecondsRemaining(),
                            session.getYesVotes(),
                            session.getNoVotes()
                    );
                }
                break; // Player can only vote in one session
            }
        }
    }

    private void registerNetworkHandlers() {
        // Define the ResourceLocation for the packet
        ResourceLocation CAST_VOTE = new ResourceLocation(MOD_ID, "cast_vote");

        // This gets called when a player clicks yes/no in the vote UI
        ServerPlayNetworking.registerGlobalReceiver(CAST_VOTE, (server, player, handler, buf, responseSender) -> {
            boolean voteYes = buf.readBoolean();

            server.execute(() -> {
                if (activeVotes.isEmpty()) {
                    player.sendSystemMessage(Component.literal("No vote in progress"));
                    return;
                }

                VoteSession session = activeVotes.values().iterator().next();
                boolean success = session.castVote(player, voteYes);

                if (!success) {
                    player.sendSystemMessage(Component.literal("You've already voted or aren't eligible to vote"));
                }
            });
        });

        // Make sure clients have the mod installed
        setupModPresenceCheck();
    }

    private void setupModPresenceCheck() {
        // Ask client if they have our mod during login
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sync) ->
                sender.sendPacket(MOD_PRESENCE_CHANNEL, PacketByteBufs.create())
        );

        // Boot them if they don't have it
        ServerLoginNetworking.registerGlobalReceiver(MOD_PRESENCE_CHANNEL, (server, handler, understood, buf, sync, sender) -> {
            if (!understood) {
                handler.disconnect(Component.literal(
                        "This server requires the VoteKick mod.\n" +
                                "Please install it to connect."
                ));
                LOGGER.info("Rejected {} - missing VoteKick mod", handler.getUserName());
            } else {
                LOGGER.debug("{} has VoteKick mod installed", handler.getUserName());
            }
        });
    }

    private void loadConfig() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File configFile = new File(configDir, MOD_ID + ".properties");
        Properties props = new Properties();

        // Try to load existing config file
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                props.load(reader);
                LOGGER.info("Loaded VoteKick config");
            } catch (IOException e) {
                LOGGER.error("Failed to read config", e);
            }
        }

        // Create config and save with default values if needed
        config = new VoteKickConfig(props);

        try (FileWriter writer = new FileWriter(configFile)) {
            // Write back to file with any missing defaults filled in
            config.updateProperties(props);
            props.store(writer, "VoteKick Mod Configuration");
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public static VoteKickConfig getConfig() {
        return config;
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