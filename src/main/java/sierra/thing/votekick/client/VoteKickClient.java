package sierra.thing.votekick.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.client.config.ClientConfig;
import sierra.thing.votekick.network.VoteKickNetworking;

public class VoteKickClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID + "-client");

    public static KeyMapping voteYesKey;
    public static KeyMapping voteNoKey;

    private static ClientConfig clientConfig;

    public static void initClient() {
        LOGGER.info("Initializing VoteKick client v{}", VoteKickMod.VERSION);

        clientConfig = new ClientConfig();
        clientConfig.load();

        VoteKickHud.setOnHideListener(() -> {
            LOGGER.debug("Vote panel hidden, resetting vote state");
            VoteKickHud.resetVoteState();
        });

        LOGGER.info("VoteKick client initialized");
    }

    public static KeyMapping createVoteYesKey() {
        //? if >=1.21.9 {
        /*return new KeyMapping(
                "key.votekick.vote_yes",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F1,
                KeyMapping.Category.MISC
        );
        *///?} else {
        return new KeyMapping(
                "key.votekick.vote_yes",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F1,
                "category.votekick"
        );
        //?}
    }

    public static KeyMapping createVoteNoKey() {
        //? if >=1.21.9 {
        /*return new KeyMapping(
                "key.votekick.vote_no",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F2,
                KeyMapping.Category.MISC
        );
        *///?} else {
        return new KeyMapping(
                "key.votekick.vote_no",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F2,
                "category.votekick"
        );
        //?}
    }

    public static void setKeyMappings(KeyMapping yesKey, KeyMapping noKey) {
        voteYesKey = yesKey;
        voteNoKey = noKey;
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

        if (clientConfig.isSoundEnabled()) {
            playLocalSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, voteYes ? 1.2f : 0.8f);
        }

        try {
            LOGGER.debug("Sending vote: {}", voteYes ? "YES" : "NO");
            VoteKickNetworking.sendCastVote(voteYes);
        } catch (Exception e) {
            LOGGER.error("Error sending vote to server", e);
            client.player.displayClientMessage(
                    Component.literal("Error sending vote to server"),
                    false
            );
        }
    }

    public static void playLocalSound(SoundEvent sound, float volume, float pitch) {
        if (clientConfig != null && clientConfig.isSoundEnabled()) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                //? if >=1.21.11 {
                /*client.player.playSound(sound, volume, pitch);
                *///?} else {
                client.player.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
                //?}
            }
        }
    }

    public static ClientConfig getClientConfig() {
        return clientConfig;
    }
}
