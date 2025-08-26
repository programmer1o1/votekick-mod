package sierra.thing.votekick.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
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
            // No explicit registration needed for Fabric API packets in 1.20.1
            // The TYPE fields in each payload class take care of registration

            LOGGER.debug("Successfully registered all network payloads");
        } catch (Exception e) {
            // If this happens, we're screwed - mod won't work right
            LOGGER.error("Failed to register network payloads", e);
        }
    }

    /**
     * Safely write string to buffer - had a nasty crash once with unchecked strings
     */
    public static void writeString(FriendlyByteBuf buf, String str) {
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
    public static String readString(FriendlyByteBuf buf) {
        String str = buf.readUtf();
        if (str.length() > MAX_STRING_LENGTH) {
            // Should never happen unless someone's messing with the packets
            return str.substring(0, MAX_STRING_LENGTH);
        }
        return str;
    }
}