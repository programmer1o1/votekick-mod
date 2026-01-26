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
 * Simple packet to tell the client to close the vote UI.
 * Sent when votes finish or are cancelled.
 */
public record HideVotePanelPayload()
        //? if >=1.20.2 {
        /*implements CustomPacketPayload
        *///?}
{
    //? if >=1.21.11 {
    /*public static final Identifier ID = Identifier.fromNamespaceAndPath(VoteKickMod.MOD_ID, "hide_vote_panel");
    *///?} else if >=1.21 {
    /*public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VoteKickMod.MOD_ID, "hide_vote_panel");
    *///?} else {
    public static final ResourceLocation ID = new ResourceLocation(VoteKickMod.MOD_ID, "hide_vote_panel");
    //?}

    //? if >=1.20.6 {
    /*public static final CustomPacketPayload.Type<HideVotePanelPayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HideVotePanelPayload> STREAM_CODEC =
            StreamCodec.of(PayloadIo::writeHideVotePanel, PayloadIo::readHideVotePanel);

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
        PayloadIo.writeHideVotePanel(buf, this);
    }
    *///?}
}
