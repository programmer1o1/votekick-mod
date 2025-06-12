package sierra.thing.votekick.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import sierra.thing.votekick.VoteKickMod;

/**
 * Simple packet to tell the client to close the vote UI.
 * Sent when votes finish or are cancelled.
 */
public record HideVotePanelPayload() implements CustomPacketPayload {
    // Unique identifier for this packet type - server uses this to know what we're sending
    public static final Type<HideVotePanelPayload> TYPE = new Type<>(
            ResourceLocation.parse(VoteKickMod.MOD_ID + ":hide_vote_panel")
    );

    @Override
    public Type<?> type() {
        return TYPE;
    }

    // No actual data in this packet - it's just a signal
    // Could probably add a "reason" field later if we want to show different messages
}