// PlayerProtectionManager.java
package sierra.thing.votekick.protection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * manages player protection to prevent abuse
 * like when someone gets kicked 6 times in a row... that's just harassment
 */
public class PlayerProtectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    // protection data
    private final Map<UUID, PlayerProtectionData> protectionData = new ConcurrentHashMap<>();
    private final Set<UUID> knownPlayers = ConcurrentHashMap.newKeySet();

    // constants for protection periods
    private static final long NEW_PLAYER_PROTECTION_MS = 300_000; // 5 minutes for new players
    private static final long KICK_PROTECTION_MS = 600_000; // 10 minutes after being kicked
    private static final long REPEATED_KICK_PROTECTION_MS = 1800_000; // 30 minutes if kicked multiple times
    private static final int KICKS_FOR_EXTENDED_PROTECTION = 3; // after 3 kicks, longer protection

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    public PlayerProtectionManager() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        this.dataFile = new File(configDir, "votekick_protection.json");
    }

    /**
     * check if player is currently protected from votes
     */
    public boolean isProtected(UUID playerUUID) {
        PlayerProtectionData data = protectionData.get(playerUUID);
        if (data == null) return false;

        // check all protection types
        long now = System.currentTimeMillis();
        return data.protectionUntil > now;
    }

    /**
     * get remaining protection time in seconds
     */
    public int getRemainingProtectionTime(UUID playerUUID) {
        PlayerProtectionData data = protectionData.get(playerUUID);
        if (data == null) return 0;

        long remaining = data.protectionUntil - System.currentTimeMillis();
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }

    /**
     * grant protection to a new player
     */
    public void grantNewPlayerProtection(UUID playerUUID) {
        PlayerProtectionData data = protectionData.computeIfAbsent(playerUUID, k -> new PlayerProtectionData());
        data.protectionUntil = System.currentTimeMillis() + NEW_PLAYER_PROTECTION_MS;
        data.isNewPlayer = true;
        knownPlayers.add(playerUUID);
        save();
    }

    /**
     * record a kick and grant appropriate protection
     */
    public void recordKick(UUID playerUUID, String reason) {
        PlayerProtectionData data = protectionData.computeIfAbsent(playerUUID, k -> new PlayerProtectionData());

        // increment kick count
        data.totalKicks++;
        data.lastKickTime = System.currentTimeMillis();
        data.kickHistory.add(new KickRecord(System.currentTimeMillis(), reason));

        // keep only last 10 kicks in history
        if (data.kickHistory.size() > 10) {
            data.kickHistory.remove(0);
        }

        // calculate protection duration based on recent kicks
        long protectionDuration = calculateProtectionDuration(data);
        data.protectionUntil = System.currentTimeMillis() + protectionDuration;

        // no longer a new player after first kick
        data.isNewPlayer = false;

        LOGGER.info("Player {} kicked (total: {}), protected for {} seconds",
                playerUUID, data.totalKicks, protectionDuration / 1000);

        save();
    }

    /**
     * calculate how long to protect based on kick history
     */
    private long calculateProtectionDuration(PlayerProtectionData data) {
        // count recent kicks (last hour)
        long oneHourAgo = System.currentTimeMillis() - 3600_000;
        long recentKicks = data.kickHistory.stream()
                .filter(k -> k.timestamp > oneHourAgo)
                .count();

        // escalating protection based on harassment level
        if (recentKicks >= 5) {
            return 3600_000; // 1 hour if kicked 5+ times in an hour
        } else if (recentKicks >= 3) {
            return REPEATED_KICK_PROTECTION_MS; // 30 min
        } else if (data.totalKicks >= KICKS_FOR_EXTENDED_PROTECTION) {
            return REPEATED_KICK_PROTECTION_MS; // 30 min for repeat offenders
        } else {
            return KICK_PROTECTION_MS; // 10 min standard
        }
    }

    /**
     * get how many times a player has been kicked recently
     */
    public int getRecentKickCount(UUID playerUUID, long withinMs) {
        PlayerProtectionData data = protectionData.get(playerUUID);
        if (data == null) return 0;

        long since = System.currentTimeMillis() - withinMs;
        return (int) data.kickHistory.stream()
                .filter(k -> k.timestamp > since)
                .count();
    }

    /**
     * check if player has joined before
     */
    public boolean hasJoinedBefore(UUID playerUUID) {
        return knownPlayers.contains(playerUUID);
    }

    /**
     * get vote threshold modifier based on player's kick history
     * returns a multiplier for required vote percentage
     */
    public double getVoteThresholdModifier(UUID playerUUID) {
        PlayerProtectionData data = protectionData.get(playerUUID);
        if (data == null) return 1.0;

        // make it harder to kick someone who's been kicked a lot
        if (data.totalKicks >= 5) return 1.5; // need 50% more votes
        if (data.totalKicks >= 3) return 1.25; // need 25% more votes

        // check recent kicks
        int recentKicks = getRecentKickCount(playerUUID, 3600_000); // last hour
        if (recentKicks >= 2) return 1.35; // need 35% more votes if kicked twice recently

        return 1.0;
    }

    /**
     * cleanup old data
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        long thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000);

        protectionData.entrySet().removeIf(entry -> {
            PlayerProtectionData data = entry.getValue();
            // remove if no protection and hasn't been kicked in 30 days
            return data.protectionUntil < now &&
                    (data.kickHistory.isEmpty() ||
                            data.kickHistory.get(data.kickHistory.size() - 1).timestamp < thirtyDaysAgo);
        });
    }

    /**
     * save protection data to disk
     */
    public void save() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            Map<String, Object> saveData = new HashMap<>();

            // convert UUIDs to strings for json
            Map<String, PlayerProtectionData> stringKeyedData = new HashMap<>();
            protectionData.forEach((uuid, data) -> stringKeyedData.put(uuid.toString(), data));

            saveData.put("protectionData", stringKeyedData);
            saveData.put("knownPlayers", knownPlayers.stream().map(UUID::toString).toList());

            gson.toJson(saveData, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save protection data", e);
        }
    }

    /**
     * load protection data from disk
     */
    public void load() {
        if (!dataFile.exists()) return;

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> loadedData = gson.fromJson(reader, type);

            if (loadedData != null) {
                // load protection data
                Map<String, PlayerProtectionData> stringKeyedData = gson.fromJson(
                        gson.toJson(loadedData.get("protectionData")),
                        new TypeToken<Map<String, PlayerProtectionData>>(){}.getType()
                );

                if (stringKeyedData != null) {
                    stringKeyedData.forEach((uuidStr, data) -> {
                        try {
                            protectionData.put(UUID.fromString(uuidStr), data);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid UUID in protection data: {}", uuidStr);
                        }
                    });
                }

                // load known players
                List<String> knownPlayerStrings = gson.fromJson(
                        gson.toJson(loadedData.get("knownPlayers")),
                        new TypeToken<List<String>>(){}.getType()
                );

                if (knownPlayerStrings != null) {
                    knownPlayerStrings.forEach(uuidStr -> {
                        try {
                            knownPlayers.add(UUID.fromString(uuidStr));
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid UUID in known players: {}", uuidStr);
                        }
                    });
                }
            }

            LOGGER.info("Loaded protection data for {} players", protectionData.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load protection data", e);
        }
    }

    /**
     * data class for player protection info
     */
    private static class PlayerProtectionData {
        long protectionUntil = 0;
        int totalKicks = 0;
        long lastKickTime = 0;
        boolean isNewPlayer = false;
        List<KickRecord> kickHistory = new ArrayList<>();
    }

    /**
     * record of a single kick event
     */
    private static class KickRecord {
        final long timestamp;
        final String reason;

        KickRecord(long timestamp, String reason) {
            this.timestamp = timestamp;
            this.reason = reason;
        }
    }
}