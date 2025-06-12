package sierra.thing.votekick.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import sierra.thing.votekick.VoteKickMod;

/**
 * Payload sent from client to server when a player casts a vote.
 */
public record CastVotePayload(boolean voteYes) implements CustomPacketPayload {
    public static final Type<CastVotePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VoteKickMod.MOD_ID, "cast_vote")
    );

    @Override
    public Type<?> type() {
        return TYPE;
    }
}