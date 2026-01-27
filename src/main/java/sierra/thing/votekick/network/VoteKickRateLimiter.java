package sierra.thing.votekick.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class VoteKickRateLimiter {
    private static final long CAST_VOTE_MIN_INTERVAL_MS = 250;
    private static final long STALE_ENTRY_MS = 5 * 60 * 1000L;
    private static final Map<UUID, Long> lastCastVote = new ConcurrentHashMap<>();

    private VoteKickRateLimiter() {
    }

    public static boolean allowCastVote(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();
        Long last = lastCastVote.get(playerId);
        if (last != null && now - last < CAST_VOTE_MIN_INTERVAL_MS) {
            return false;
        }

        lastCastVote.put(playerId, now);
        maybeCleanup(now);
        return true;
    }

    private static void maybeCleanup(long now) {
        if (ThreadLocalRandom.current().nextInt(256) != 0) {
            return;
        }
        lastCastVote.entrySet().removeIf(entry -> now - entry.getValue() > STALE_ENTRY_MS);
    }
}
