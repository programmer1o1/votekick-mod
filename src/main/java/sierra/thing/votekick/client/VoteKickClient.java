package sierra.thing.votekick.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.network.CastVotePayload;
import sierra.thing.votekick.network.HideVotePanelPayload;
import sierra.thing.votekick.network.ShowVotePanelPayload;
import sierra.thing.votekick.network.UpdateVotePanelPayload;

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
            ClientPlayNetworking.send(new CastVotePayload(voteYes));
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
                ShowVotePanelPayload.TYPE,
                (payload, context) -> {
                    Minecraft client = context.client();
                    client.execute(() -> {
                        try {
                            LOGGER.debug("Showing vote panel: target={}", payload.isTarget());
                            VoteKickHud.resetVoteState();
                            VoteKickHud.showVotePanel(
                                    payload.title(),
                                    payload.subtitle(),
                                    payload.time(),
                                    payload.yesVotes(),
                                    payload.noVotes(),
                                    payload.votesNeeded(),
                                    payload.isTarget()
                            );
                        } catch (Exception e) {
                            LOGGER.error("Error showing vote panel", e);
                        }
                    });
                }
        );

        // When vote count changes or time ticks down
        ClientPlayNetworking.registerGlobalReceiver(
                UpdateVotePanelPayload.TYPE,
                (payload, context) -> {
                    Minecraft client = context.client();
                    client.execute(() -> {
                        try {
                            VoteKickHud.updateVotePanel(
                                    payload.time(),
                                    payload.yesVotes(),
                                    payload.noVotes()
                            );
                        } catch (Exception e) {
                            LOGGER.error("Error updating vote panel", e);
                        }
                    });
                }
        );

        // When vote ends
        ClientPlayNetworking.registerGlobalReceiver(
                HideVotePanelPayload.TYPE,
                (payload, context) -> {
                    Minecraft client = context.client();
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