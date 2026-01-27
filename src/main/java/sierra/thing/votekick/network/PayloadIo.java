package sierra.thing.votekick.network;

import net.minecraft.network.FriendlyByteBuf;

public final class PayloadIo {
    private static final int MAX_STRING_LENGTH = 256;

    private PayloadIo() {
    }

    public static void writeCastVote(FriendlyByteBuf buf, CastVotePayload payload) {
        buf.writeBoolean(payload.voteYes());
    }

    public static CastVotePayload readCastVote(FriendlyByteBuf buf) {
        return new CastVotePayload(buf.readBoolean());
    }

    public static void writeShowVotePanel(FriendlyByteBuf buf, ShowVotePanelPayload payload) {
        writeString(buf, payload.title());
        writeString(buf, payload.subtitle());
        buf.writeInt(payload.time());
        buf.writeInt(payload.yesVotes());
        buf.writeInt(payload.noVotes());
        buf.writeInt(payload.votesNeeded());
        buf.writeBoolean(payload.isTarget());
    }

    public static ShowVotePanelPayload readShowVotePanel(FriendlyByteBuf buf) {
        return new ShowVotePanelPayload(
                readString(buf),
                readString(buf),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void writeUpdateVotePanel(FriendlyByteBuf buf, UpdateVotePanelPayload payload) {
        buf.writeInt(payload.time());
        buf.writeInt(payload.yesVotes());
        buf.writeInt(payload.noVotes());
    }

    public static UpdateVotePanelPayload readUpdateVotePanel(FriendlyByteBuf buf) {
        return new UpdateVotePanelPayload(
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void writeHideVotePanel(FriendlyByteBuf buf, HideVotePanelPayload payload) {
    }

    public static HideVotePanelPayload readHideVotePanel(FriendlyByteBuf buf) {
        return new HideVotePanelPayload();
    }

    private static void writeString(FriendlyByteBuf buf, String str) {
        if (str == null) {
            buf.writeUtf("");
        } else if (str.length() > MAX_STRING_LENGTH) {
            buf.writeUtf(str.substring(0, MAX_STRING_LENGTH));
        } else {
            buf.writeUtf(str);
        }
    }

    private static String readString(FriendlyByteBuf buf) {
        return buf.readUtf(MAX_STRING_LENGTH);
    }
}
