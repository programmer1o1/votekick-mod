// VoteKickClient.java
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.client.config.ClientConfig;

import java.util.concurrent.CompletableFuture;

public class VoteKickClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID + "-client");

    public static KeyMapping voteYesKey;
    public static KeyMapping voteNoKey;

    private static boolean hasVoted = false;
    private static ClientConfig clientConfig;

    private static final ResourceLocation SHOW_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "show_vote_panel");
    private static final ResourceLocation UPDATE_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "update_vote_panel");
    private static final ResourceLocation HIDE_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "hide_vote_panel");
    private static final ResourceLocation CAST_VOTE = new ResourceLocation(VoteKickMod.MOD_ID, "cast_vote");

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
            playLocalSound(voteYes ? net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value() :
                            net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                    0.5f, voteYes ? 1.2f : 0.8f);
        }

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
                    return CompletableFuture.completedFuture(PacketByteBufs.create());
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

                            // play notification sound only if enabled
                            playLocalSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
                        } catch (Exception e) {
                            LOGGER.error("Error showing vote panel", e);
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UPDATE_VOTE_PANEL,
                (client, handler, buf, responseSender) -> {
                    int time = buf.readInt();
                    int yesVotes = buf.readInt();
                    int noVotes = buf.readInt();

                    client.execute(() -> {
                        try {
                            VoteKickHud.updateVotePanel(time, yesVotes, noVotes);
                        } catch (Exception e) {
                            LOGGER.error("Error updating vote panel", e);
                        }
                    });
                }
        );

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

    public static ClientConfig getClientConfig() {
        return clientConfig;
    }
}