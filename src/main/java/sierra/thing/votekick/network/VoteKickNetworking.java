package sierra.thing.votekick.network;

//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
//?} else if neoforge {
/*//? if >=1.21.8 {
/^import net.neoforged.neoforge.client.network.ClientPacketDistributor;
^///?}
import net.neoforged.neoforge.network.PacketDistributor;
*///?}
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

import java.util.UUID;

/**
 * Handles all the networking between server and clients.
 * Should probably refactor this later to use a more elegant design.
 */
public class VoteKickNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    public static void sendCastVote(boolean voteYes) {
        try {
            CastVotePayload payload = new CastVotePayload(voteYes);
            //? if fabric {
            //? if >=1.20.6 {
            /*ClientPlayNetworking.send(payload);
            *///?} else {
            FriendlyByteBuf buf = PacketByteBufs.create();
            PayloadIo.writeCastVote(buf, payload);
            ClientPlayNetworking.send(CastVotePayload.ID, buf);
            //?}
            //?} else if neoforge {
            /*//? if >=1.21.8 {
            /^ClientPacketDistributor.sendToServer(payload);
            ^///?} else {
            //? if >=1.20.6 {
            /^PacketDistributor.sendToServer(payload);
            ^///?} else {
            PacketDistributor.SERVER.noArg().send(payload);
            //?}
            //?}
            *///?}
        } catch (Exception e) {
            LOGGER.error("Error sending CastVote payload", e);
        }
    }

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
            ShowVotePanelPayload payload = new ShowVotePanelPayload(
                    title, subtitle, time, yes, no, needed, isTarget
            );
            //? if fabric {
            //? if >=1.20.6 {
            /*ServerPlayNetworking.send(player, payload);
            *///?} else {
            FriendlyByteBuf buf = PacketByteBufs.create();
            PayloadIo.writeShowVotePanel(buf, payload);
            ServerPlayNetworking.send(player, ShowVotePanelPayload.ID, buf);
            //?}
            //?} else if neoforge {
            /*//? if >=1.20.6 {
            /^PacketDistributor.sendToPlayer(player, payload);
            ^///?} else {
            PacketDistributor.PLAYER.with(player).send(payload);
            //?}
            *///?}
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
            UpdateVotePanelPayload payload = new UpdateVotePanelPayload(time, yes, no);
            //? if fabric {
            //? if >=1.20.6 {
            /*ServerPlayNetworking.send(player, payload);
            *///?} else {
            FriendlyByteBuf buf = PacketByteBufs.create();
            PayloadIo.writeUpdateVotePanel(buf, payload);
            ServerPlayNetworking.send(player, UpdateVotePanelPayload.ID, buf);
            //?}
            //?} else if neoforge {
            /*//? if >=1.20.6 {
            /^PacketDistributor.sendToPlayer(player, payload);
            ^///?} else {
            PacketDistributor.PLAYER.with(player).send(payload);
            //?}
            *///?}
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
            HideVotePanelPayload payload = new HideVotePanelPayload();
            //? if fabric {
            //? if >=1.20.6 {
            /*ServerPlayNetworking.send(player, payload);
            *///?} else {
            FriendlyByteBuf buf = PacketByteBufs.create();
            PayloadIo.writeHideVotePanel(buf, payload);
            ServerPlayNetworking.send(player, HideVotePanelPayload.ID, buf);
            //?}
            //?} else if neoforge {
            /*//? if >=1.20.6 {
            /^PacketDistributor.sendToPlayer(player, payload);
            ^///?} else {
            PacketDistributor.PLAYER.with(player).send(payload);
            //?}
            *///?}
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
            int count = 0;
            for (ServerPlayer player : players) {
                if (player != null) {
                    sendUpdateVotePanel(player, time, yes, no);
                    count++;
                }
            }

            LOGGER.trace("Broadcast UpdateVotePanel to {} players: time={}, yes={}, no={}", count, time, yes, no);
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
                    sendHideVotePanel(player);
                    count++;
                }
            }

            LOGGER.debug("Broadcast HideVotePanel to {} players", count);
        } catch (Exception e) {
            LOGGER.error("Error broadcasting HideVotePanel", e);
        }
    }
}
