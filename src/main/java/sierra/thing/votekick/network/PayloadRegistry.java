package sierra.thing.votekick.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

/**
 * Network payload stuff. Handles all the communication between
 * client and server for votes.
 */
public class PayloadRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    // Cap string length to prevent buffer overflow exploits
    // 256 should be plenty for our needs
    private static final int MAX_STRING_LENGTH = 256;

    /**
     * Sets up all the packet types we need for the mod.
     * Call this once at startup.
     */
    public static void register() {
        LOGGER.debug("Registering network payload types");

        try {
            // Grab registry refs - one for each direction
            PayloadTypeRegistry<RegistryFriendlyByteBuf> clientToServer = PayloadTypeRegistry.playC2S();
            PayloadTypeRegistry<RegistryFriendlyByteBuf> serverToClient = PayloadTypeRegistry.playS2C();

            // C2S - things clients send to server
            registerCastVotePayload(clientToServer);

            // S2C - things server broadcasts to clients
            registerShowVotePanelPayload(serverToClient);
            registerUpdateVotePanelPayload(serverToClient);
            registerHideVotePanelPayload(serverToClient);

            LOGGER.debug("Successfully registered all network payloads");
        } catch (Exception e) {
            // If this happens, we're screwed - mod won't work right
            LOGGER.error("Failed to register network payloads", e);
        }
    }

    private static void registerCastVotePayload(PayloadTypeRegistry<RegistryFriendlyByteBuf> registry) {
        registry.register(
                CastVotePayload.TYPE,
                StreamCodec.of(
                        (buf, payload) -> buf.writeBoolean(payload.voteYes()),
                        buf -> new CastVotePayload(buf.readBoolean())
                )
        );
        LOGGER.trace("Registered CastVotePayload");
    }

    private static void registerShowVotePanelPayload(PayloadTypeRegistry<RegistryFriendlyByteBuf> registry) {
        registry.register(
                ShowVotePanelPayload.TYPE,
                StreamCodec.of(
                        (buf, payload) -> {
                            // Write UI text with sanity checks
                            writeString(buf, payload.title());
                            writeString(buf, payload.subtitle());
                            buf.writeInt(payload.time());
                            buf.writeInt(payload.yesVotes());
                            buf.writeInt(payload.noVotes());
                            buf.writeInt(payload.votesNeeded());
                            buf.writeBoolean(payload.isTarget());
                        },
                        buf -> new ShowVotePanelPayload(
                                readString(buf),
                                readString(buf),
                                buf.readInt(),
                                buf.readInt(),
                                buf.readInt(),
                                buf.readInt(),
                                buf.readBoolean()
                        )
                )
        );
        LOGGER.trace("Registered ShowVotePanelPayload");
    }

    private static void registerUpdateVotePanelPayload(PayloadTypeRegistry<RegistryFriendlyByteBuf> registry) {
        registry.register(
                UpdateVotePanelPayload.TYPE,
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeInt(payload.time());
                            buf.writeInt(payload.yesVotes());
                            buf.writeInt(payload.noVotes());
                        },
                        buf -> new UpdateVotePanelPayload(
                                buf.readInt(),
                                buf.readInt(),
                                buf.readInt()
                        )
                )
        );
        LOGGER.trace("Registered UpdateVotePanelPayload");
    }

    private static void registerHideVotePanelPayload(PayloadTypeRegistry<RegistryFriendlyByteBuf> registry) {
        registry.register(
                HideVotePanelPayload.TYPE,
                StreamCodec.of(
                        (buf, payload) -> {}, // Empty payload, just a signal
                        buf -> new HideVotePanelPayload()
                )
        );
        LOGGER.trace("Registered HideVotePanelPayload");
    }

    /**
     * Safely write string to buffer - had a nasty crash once with unchecked strings
     */
    private static void writeString(RegistryFriendlyByteBuf buf, String str) {
        if (str == null) {
            buf.writeUtf("");
        } else if (str.length() > MAX_STRING_LENGTH) {
            // Truncate long strings - better than crashing
            buf.writeUtf(str.substring(0, MAX_STRING_LENGTH));
        } else {
            buf.writeUtf(str);
        }
    }

    /**
     * Read string from buffer with safety checks
     * Not sure this is necessary since server should be sending valid data
     * but better safe than sorry
     */
    private static String readString(RegistryFriendlyByteBuf buf) {
        String str = buf.readUtf();
        if (str.length() > MAX_STRING_LENGTH) {
            // Should never happen unless someone's messing with the packets
            return str.substring(0, MAX_STRING_LENGTH);
        }
        return str;
    }
}