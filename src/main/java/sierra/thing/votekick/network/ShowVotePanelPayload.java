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
)
        //? if >=1.20.2 {
        /*implements CustomPacketPayload
        *///?}
{
    //? if >=1.21.11 {
    /*public static final Identifier ID = Identifier.fromNamespaceAndPath(VoteKickMod.MOD_ID, "show_vote_panel");
    *///?} else if >=1.21 {
    /*public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VoteKickMod.MOD_ID, "show_vote_panel");
    *///?} else {
    public static final ResourceLocation ID = new ResourceLocation(VoteKickMod.MOD_ID, "show_vote_panel");
    //?}

    //? if >=1.20.6 {
    /*public static final CustomPacketPayload.Type<ShowVotePanelPayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ShowVotePanelPayload> STREAM_CODEC =
            StreamCodec.of(PayloadIo::writeShowVotePanel, PayloadIo::readShowVotePanel);

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
        PayloadIo.writeShowVotePanel(buf, this);
    }
    *///?}
}
