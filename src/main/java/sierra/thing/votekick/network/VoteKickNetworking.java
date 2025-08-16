package sierra.thing.votekick.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Handles all the networking between server and clients.
 * Should probably refactor this later to use a more elegant design.
 */
public class VoteKickNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    // Channel identifiers
    private static final ResourceLocation SHOW_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "show_vote_panel");
    private static final ResourceLocation UPDATE_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "update_vote_panel");
    private static final ResourceLocation HIDE_VOTE_PANEL = new ResourceLocation(VoteKickMod.MOD_ID, "hide_vote_panel");

    /**
     * Shows the vote UI on a client.
     *
     * @param player     Target player
     * @param title      Main vote question
     * @param subtitle   Details about the vote
     * @param time       Time left in seconds
     * @param yes        Current yes votes
     * @param no         Current no votes
     * @param needed     Votes needed to pass
     * @param isTarget   If this player is being voted on - changes their UI
     */
    public static void sendShowVotePanel(ServerPlayer player, String title, String subtitle,
                                         int time, int yes, int no, int needed, boolean isTarget) {
        if (player == null) return;

        try {
            FriendlyByteBuf buf = PacketByteBufs.create();
            PayloadRegistry.writeString(buf, title);
            PayloadRegistry.writeString(buf, subtitle);
            buf.writeInt(time);
            buf.writeInt(yes);
            buf.writeInt(no);
            buf.writeInt(needed);
            buf.writeBoolean(isTarget);

            ServerPlayNetworking.send(player, SHOW_VOTE_PANEL, buf);
            LOGGER.debug("Sent ShowVotePanel to {}, isTarget={}", player.getScoreboardName(), isTarget);
        } catch (Exception e) {
            LOGGER.error("Error sending ShowVotePanel to {}", player.getScoreboardName(), e);
        }
    }

    /**
     * Updates an existing vote UI with new counts/time.
     * Much smaller packet than sending the full UI again.
     */
    public static void sendUpdateVotePanel(ServerPlayer player, int time, int yes, int no) {
        if (player == null) return;

        try {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeInt(time);
            buf.writeInt(yes);
            buf.writeInt(no);

            ServerPlayNetworking.send(player, UPDATE_VOTE_PANEL, buf);
            LOGGER.trace("Sent UpdateVotePanel to {}: time={}, yes={}, no={}",
                    player.getScoreboardName(), time, yes, no);
        } catch (Exception e) {
            LOGGER.error("Error sending UpdateVotePanel to {}", player.getScoreboardName(), e);
        }
    }

    /**
     * Tells client to close their vote UI.
     * TODO: might make separate class for this idk
     */
    public static void sendHideVotePanel(ServerPlayer player) {
        if (player == null) return;

        try {
            FriendlyByteBuf buf = PacketByteBufs.create();
            ServerPlayNetworking.send(player, HIDE_VOTE_PANEL, buf);
            LOGGER.debug("Sent HideVotePanel to {}", player.getScoreboardName());
        } catch (Exception e) {
            LOGGER.error("Error sending HideVotePanel to {}", player.getScoreboardName(), e);
        }
    }

    /**
     * Shows vote UI to all players. Target player gets special UI.
     *
     * TODO: this doesn't get called rn, but i will make it work later
     */
    public static void broadcastShowVotePanel(Iterable<ServerPlayer> players, String title,
                                              String subtitle, int time, int yes, int no, int needed,
                                              UUID targetUUID) {
        if (players == null || targetUUID == null) return;

        try {
            int count = 0;
            for (ServerPlayer player : players) {
                if (player != null) {
                    boolean isTarget = player.getUUID().equals(targetUUID);
                    sendShowVotePanel(player, title, subtitle, time, yes, no, needed, isTarget);
                    count++;
                }
            }
            LOGGER.debug("Broadcast ShowVotePanel to {} players", count);
        } catch (Exception e) {
            LOGGER.error("Error broadcasting ShowVotePanel", e);
        }
    }

    /**
     * Updates vote counters/timer for all players.
     * Happens every tick during active votes - keeps UI current.
     * TODO: doesn't get called rn lol
     */
    public static void broadcastUpdateVotePanel(Iterable<ServerPlayer> players, int time, int yes, int no) {
        if (players == null) return;

        try {
            // Track valid players for better logging
            Collection<ServerPlayer> validPlayers = new ArrayList<>();
            for (ServerPlayer player : players) {
                if (player != null) {
                    validPlayers.add(player);
                }
            }

            // Send to everyone - create new buffer for each player
            for (ServerPlayer player : validPlayers) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeInt(time);
                buf.writeInt(yes);
                buf.writeInt(no);
                ServerPlayNetworking.send(player, UPDATE_VOTE_PANEL, buf);
            }

            LOGGER.trace("Broadcast UpdateVotePanel to {} players: time={}, yes={}, no={}",
                    validPlayers.size(), time, yes, no);
        } catch (Exception e) {
            LOGGER.error("Error broadcasting UpdateVotePanel", e);
        }
    }

    /**
     * Closes vote UI for all players.
     * Called when votes end or are cancelled.
     */
    public static void broadcastHideVotePanel(Iterable<ServerPlayer> players) {
        if (players == null) return;

        try {
            int count = 0;
            for (ServerPlayer player : players) {
                if (player != null) {
                    // Create a new buffer for each player
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    ServerPlayNetworking.send(player, HIDE_VOTE_PANEL, buf);
                    count++;
                }
            }

            LOGGER.debug("Broadcast HideVotePanel to {} players", count);
        } catch (Exception e) {
            LOGGER.error("Error broadcasting HideVotePanel", e);
        }
    }
}