package sierra.thing.votekick.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import sierra.thing.votekick.VoteKickMod;

/**
 * Payload sent from client to server when a player casts a vote.
 */
public record CastVotePayload(boolean voteYes) implements FabricPacket {
    public static final PacketType<CastVotePayload> TYPE = PacketType.create(
            new ResourceLocation(VoteKickMod.MOD_ID, "cast_vote"),
            CastVotePayload::new
    );

    public CastVotePayload(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(voteYes);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}