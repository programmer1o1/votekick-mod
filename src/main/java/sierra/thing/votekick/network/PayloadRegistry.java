package sierra.thing.votekick.network;

//? if fabric {

//? if >=1.20.6 {
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
*///?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

public final class PayloadRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);
    private static boolean registered;

    private PayloadRegistry() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        //? if >=1.20.6 {
        /*PayloadTypeRegistry<RegistryFriendlyByteBuf> clientToServer = PayloadTypeRegistry.playC2S();
        PayloadTypeRegistry<RegistryFriendlyByteBuf> serverToClient = PayloadTypeRegistry.playS2C();

        clientToServer.register(CastVotePayload.TYPE, CastVotePayload.STREAM_CODEC);
        serverToClient.register(ShowVotePanelPayload.TYPE, ShowVotePanelPayload.STREAM_CODEC);
        serverToClient.register(UpdateVotePanelPayload.TYPE, UpdateVotePanelPayload.STREAM_CODEC);
        serverToClient.register(HideVotePanelPayload.TYPE, HideVotePanelPayload.STREAM_CODEC);

        LOGGER.debug("Registered network payload types");
        *///?} else {
        LOGGER.debug("Legacy networking in use; skipping payload type registry");
        //?}
    }
}
//?}
