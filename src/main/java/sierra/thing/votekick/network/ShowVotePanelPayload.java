package sierra.thing.votekick.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
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
) implements FabricPacket {
    public static final PacketType<ShowVotePanelPayload> TYPE = PacketType.create(
            new ResourceLocation(VoteKickMod.MOD_ID, "show_vote_panel"),
            ShowVotePanelPayload::new
    );

    public ShowVotePanelPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(title);
        buf.writeUtf(subtitle);
        buf.writeInt(time);
        buf.writeInt(yesVotes);
        buf.writeInt(noVotes);
        buf.writeInt(votesNeeded);
        buf.writeBoolean(isTarget);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}