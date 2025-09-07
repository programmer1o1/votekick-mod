// VoteKickClient.java
package sierra.thing.votekick.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.client.config.ClientConfig;
import sierra.thing.votekick.network.CastVotePayload;
import sierra.thing.votekick.network.HideVotePanelPayload;
import sierra.thing.votekick.network.ShowVotePanelPayload;
import sierra.thing.votekick.network.UpdateVotePanelPayload;

import java.util.concurrent.CompletableFuture;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public class VoteKickClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID + "-client");

    public static KeyMapping voteYesKey;
    public static KeyMapping voteNoKey;

    private static boolean hasVoted = false;
    private static ClientConfig clientConfig;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing VoteKick client v{}", VoteKickMod.VERSION);

        clientConfig = new ClientConfig();
        clientConfig.load();

        VoteKickHud.init();
        registerKeyBindings();
        registerModPresenceHandler();
        registerNetworkHandlers();
        registerEventHandlers();

        LOGGER.info("VoteKick client initialized");
    }

    private void registerKeyBindings() {
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

    public static void castVote(boolean voteYes) {
        if (!VoteKickHud.isVotePanelShowing()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (VoteKickHud.hasPlayerVoted()) {
            client.player.displayClientMessage(
                    Component.literal("You have already voted!"),
                    true
            );
            return;
        }

        VoteKickHud.markPlayerVoted();

        client.player.displayClientMessage(
                Component.literal("You voted " + (voteYes ? "YES" : "NO")),
                false
        );

        // play local sound if enabled
        if (clientConfig.isSoundEnabled()) {
            playLocalSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    0.5f, voteYes ? 1.2f : 0.8f);
        }

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
     * play sound locally if sounds are enabled
     */
    public static void playLocalSound(SoundEvent sound, float volume, float pitch) {
        if (clientConfig != null && clientConfig.isSoundEnabled()) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
            }
        }
    }

    public static void resetVoteState() {
        hasVoted = false;
    }

    private void registerModPresenceHandler() {
        ClientLoginNetworking.registerGlobalReceiver(
                VoteKickMod.MOD_PRESENCE_CHANNEL,
                (client, handler, buf, responseSender) -> {
                    LOGGER.debug("Responding to server mod presence check");
                    return CompletableFuture.completedFuture(new FriendlyByteBuf(Unpooled.buffer()));
                }
        );
    }

    private void registerEventHandlers() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.debug("Client disconnected, cleaning up vote UI");
            VoteKickHud.onClientDisconnect();
            VoteKickHud.resetVoteState();
        });

        VoteKickHud.setOnHideListener(() -> {
            LOGGER.debug("Vote panel hidden, resetting vote state");
            VoteKickHud.resetVoteState();
        });
    }

    private void registerNetworkHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
                ShowVotePanelPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
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

                            // play notification sound only if enabled
                            playLocalSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
                        } catch (Exception e) {
                            LOGGER.error("Error showing vote panel", e);
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UpdateVotePanelPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
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

        ClientPlayNetworking.registerGlobalReceiver(
                HideVotePanelPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
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

    public static ClientConfig getClientConfig() {
        return clientConfig;
    }
}