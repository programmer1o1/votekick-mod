package sierra.thing.votekick.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import sierra.thing.votekick.VoteKickMod;

/**
 * Simple packet to tell the client to close the vote UI.
 * Sent when votes finish or are cancelled.
 */
public record HideVotePanelPayload() implements FabricPacket {
    // Unique identifier for this packet type - server uses this to know what we're sending
    public static final PacketType<HideVotePanelPayload> TYPE = PacketType.create(
            new ResourceLocation(VoteKickMod.MOD_ID, "hide_vote_panel"),
            buf -> new HideVotePanelPayload()
    );

    public HideVotePanelPayload(FriendlyByteBuf buf) {
        this();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        // No actual data in this packet - it's just a signal
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    // No actual data in this packet - it's just a signal
    // Could probably add a "reason" field later if we want to show different messages
}