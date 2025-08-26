package sierra.thing.votekick.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

import java.util.concurrent.CompletableFuture;

/**
 * Client-side mod entry point.
 * Handles the UI, keybinds, and networking with server.
 */
public class VoteKickClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID + "-client");

    // Made public so the mixin can see them
    // TODO: Maybe there's a cleaner way to do this?
    public static KeyMapping voteYesKey;
    public static KeyMapping voteNoKey;

    // Set when player votes so they can't spam votes
    private static boolean hasVoted = false;

    // Channel identifiers
    private static final ResourceLocation SHOW_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "show_vote_panel");
    private static final ResourceLocation UPDATE_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "update_vote_panel");
    private static final ResourceLocation HIDE_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "hide_vote_panel");
    private static final ResourceLocation CAST_VOTE = new ResourceLocation(VoteKickMod.MOD_ID, "cast_vote");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing VoteKick client v{}", VoteKickMod.VERSION);

        // Set up the UI
        VoteKickHud.init();

        // Setup keybinds
        registerKeyBindings();

        // Tell server we have the mod when connecting
        registerModPresenceHandler();

        // Set up packet handling
        registerNetworkHandlers();

        // Handle disconnects etc.
        registerEventHandlers();

        LOGGER.info("VoteKick client initialized - vote keys can be rebound in Controls menu");
    }

    /**
     * Sets up F1/F2 as default vote keys.
     * Players can rebind these in options.
     */
    private void registerKeyBindings() {
        // Originally used J/K but people complained
        voteYesKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.votekick.vote_yes",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F1,
                "category.votekick"
        ));

        voteNoKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.votekick.vote_no",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F2,
                "category.votekick"
        ));
    }

    /**
     * Sends vote to server.
     * Called by the keyboard mixin when F1/F2 is pressed.
     */
    public static void castVote(boolean voteYes) {
        if (!VoteKickHud.isVotePanelShowing()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Prevent double votes
        if (VoteKickHud.hasPlayerVoted()) {
            client.player.displayClientMessage(
                    Component.literal("You have already voted!"),
                    true // Overlay message
            );
            return;
        }

        // Update UI to show they voted
        VoteKickHud.markPlayerVoted();

        // Show a message in chat
        client.player.displayClientMessage(
                Component.literal("You voted " + (voteYes ? "YES" : "NO")),
                false
        );

        // Tell the server
        try {
            LOGGER.debug("Sending vote: {}", voteYes ? "YES" : "NO");
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(voteYes);
            ClientPlayNetworking.send(CAST_VOTE, buf);
        } catch (Exception e) {
            LOGGER.error("Error sending vote to server", e);
            client.player.displayClientMessage(
                    Component.literal("Error sending vote to server"),
                    false
            );
        }
    }

    /**
     * Resets vote flags when a vote ends
     */
    public static void resetVoteState() {
        hasVoted = false;
    }

    /**
     * Server checks if we have the mod during login.
     * This responds so we don't get kicked.
     */
    private void registerModPresenceHandler() {
        ClientLoginNetworking.registerGlobalReceiver(
                VoteKickMod.MOD_PRESENCE_CHANNEL,
                (client, handler, buf, responseSender) -> {
                    LOGGER.debug("Responding to server mod presence check");
                    return CompletableFuture.completedFuture(PacketByteBufs.create());
                }
        );
    }

    /**
     * Handle client state changes - mostly cleanup
     */
    private void registerEventHandlers() {
        // When player leaves server, clean up the UI
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.debug("Client disconnected, cleaning up vote UI");
            VoteKickHud.onClientDisconnect();
            VoteKickHud.resetVoteState();
        });

        // When vote UI disappears, reset vote state
        VoteKickHud.setOnHideListener(() -> {
            LOGGER.debug("Vote panel hidden, resetting vote state");
            VoteKickHud.resetVoteState();
        });
    }

    /**
     * Set up packet handlers for vote messages from server
     */
    private void registerNetworkHandlers() {
        // When server starts a vote
        ClientPlayNetworking.registerGlobalReceiver(
                SHOW_VOTE_PANEL,
                (client, handler, buf, responseSender) -> {
                    String title = buf.readUtf();
                    String subtitle = buf.readUtf();
                    int time = buf.readInt();
                    int yesVotes = buf.readInt();
                    int noVotes = buf.readInt();
                    int votesNeeded = buf.readInt();
                    boolean isTarget = buf.readBoolean();

                    client.execute(() -> {
                        try {
                            LOGGER.debug("Showing vote panel: target={}", isTarget);
                            VoteKickHud.resetVoteState();
                            VoteKickHud.showVotePanel(
                                    title,
                                    subtitle,
                                    time,
                                    yesVotes,
                                    noVotes,
                                    votesNeeded,
                                    isTarget
                            );
                        } catch (Exception e) {
                            LOGGER.error("Error showing vote panel", e);
                        }
                    });
                }
        );

        // When vote count changes or time ticks down
        ClientPlayNetworking.registerGlobalReceiver(
                UPDATE_VOTE_PANEL,
                (client, handler, buf, responseSender) -> {
                    int time = buf.readInt();
                    int yesVotes = buf.readInt();
                    int noVotes = buf.readInt();

                    client.execute(() -> {
                        try {
                            VoteKickHud.updateVotePanel(
                                    time,
                                    yesVotes,
                                    noVotes
                            );
                        } catch (Exception e) {
                            LOGGER.error("Error updating vote panel", e);
                        }
                    });
                }
        );

        // When vote ends
        ClientPlayNetworking.registerGlobalReceiver(
                HIDE_VOTE_PANEL,
                (client, handler, buf, responseSender) -> {
                    client.execute(() -> {
                        try {
                            LOGGER.debug("Hiding vote panel");
                            VoteKickHud.hideVotePanel();
                        } catch (Exception e) {
                            LOGGER.error("Error hiding vote panel", e);
                        }
                    });
                }
        );
    }
}