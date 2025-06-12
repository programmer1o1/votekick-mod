package sierra.thing.votekick.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import sierra.thing.votekick.VoteKickMod;

/**
 * Payload sent from server to client to show the vote panel with initial data.
 */
public record ShowVotePanelPayload(
        String title,
        String subtitle,
        int time,
        int yesVotes,
        int noVotes,
        int votesNeeded,
        boolean isTarget
) implements CustomPacketPayload {
    public static final Type<ShowVotePanelPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VoteKickMod.MOD_ID, "show_vote_panel")
    );

    @Override
    public Type<?> type() {
        return TYPE;
    }
}