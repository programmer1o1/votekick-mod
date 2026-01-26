package sierra.thing.votekick.network;

import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import sierra.thing.votekick.VoteKickMod;
//? if >=1.20.2 {
/*import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.20.6 {
/^import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
^///?}
*///?}

/**
 * Payload sent from client to server when a player casts a vote.
 */
public record CastVotePayload(boolean voteYes)
        //? if >=1.20.2 {
        /*implements CustomPacketPayload
        *///?}
{
    //? if >=1.21.11 {
    /*public static final Identifier ID = Identifier.fromNamespaceAndPath(VoteKickMod.MOD_ID, "cast_vote");
    *///?} else if >=1.21 {
    /*public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VoteKickMod.MOD_ID, "cast_vote");
    *///?} else {
    public static final ResourceLocation ID = new ResourceLocation(VoteKickMod.MOD_ID, "cast_vote");
    //?}

    //? if >=1.20.6 {
    /*public static final CustomPacketPayload.Type<CastVotePayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CastVotePayload> STREAM_CODEC =
            StreamCodec.of(PayloadIo::writeCastVote, PayloadIo::readCastVote);

    @Override
    public CustomPacketPayload.Type<?> type() {
        return TYPE;
    }
    *///?} else if >=1.20.2 {
    /*@Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        PayloadIo.writeCastVote(buf, this);
    }
    *///?}
}
