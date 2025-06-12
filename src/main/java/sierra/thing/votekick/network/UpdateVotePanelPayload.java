package sierra.thing.votekick.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import sierra.thing.votekick.VoteKickMod;

/**
 * Payload sent from server to client to update vote panel with current vote counts.
 */
public record UpdateVotePanelPayload(
        int time,
        int yesVotes,
        int noVotes
) implements CustomPacketPayload {
    public static final Type<UpdateVotePanelPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VoteKickMod.MOD_ID, "update_vote_panel")
    );

    @Override
    public Type<?> type() {
        return TYPE;
    }
}