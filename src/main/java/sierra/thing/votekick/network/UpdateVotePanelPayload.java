package sierra.thing.votekick.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import sierra.thing.votekick.VoteKickMod;

/**
 * Payload sent from server to client to update vote panel with current vote counts.
 */
public record UpdateVotePanelPayload(
        int time,
        int yesVotes,
        int noVotes
) implements FabricPacket {
    public static final PacketType<UpdateVotePanelPayload> TYPE = PacketType.create(
            new ResourceLocation(VoteKickMod.MOD_ID, "update_vote_panel"),
            UpdateVotePanelPayload::new
    );

    public UpdateVotePanelPayload(FriendlyByteBuf buf) {
        this(
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(time);
        buf.writeInt(yesVotes);
        buf.writeInt(noVotes);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}