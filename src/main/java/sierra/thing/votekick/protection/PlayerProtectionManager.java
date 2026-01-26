// PlayerProtectionManager.java
package sierra.thing.votekick.protection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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

public class PlayerProtectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    private final Map<UUID, PlayerProtectionData> protectionData = new ConcurrentHashMap<>();
    private final Set<UUID> knownPlayers = ConcurrentHashMap.newKeySet();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    public PlayerProtectionManager() {
        File configDir = VoteKickMod.platform().getConfigDir().toFile();
        this.dataFile = new File(configDir, "votekick_protection.json");
    }

    public boolean isProtected(UUID playerUUID) {
        if (!anyProtectionEnabled()) {
            return false;
        }

        PlayerProtectionData data = protectionData.get(playerUUID);
        if (data == null) return false;

        long now = System.currentTimeMillis();
        return data.protectionUntil > now;
    }

    public int getRemainingProtectionTime(UUID playerUUID) {
        if (!anyProtectionEnabled()) {
            return 0;
        }

        PlayerProtectionData data = protectionData.get(playerUUID);
        if (data == null) return 0;

        long remaining = data.protectionUntil - System.currentTimeMillis();
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }

    public void grantNewPlayerProtection(UUID playerUUID) {
        if (!VoteKickMod.getConfig().isNewPlayerProtectionEnabled()) {
            knownPlayers.add(playerUUID);
            return;
        }

        PlayerProtectionData data = protectionData.computeIfAbsent(playerUUID, k -> new PlayerProtectionData());
        long duration = VoteKickMod.getConfig().getNewPlayerProtectionDuration() * 1000L;
        data.protectionUntil = System.currentTimeMillis() + duration;
        data.isNewPlayer = true;
        knownPlayers.add(playerUUID);
        save();
    }

    public void recordKick(UUID playerUUID, String reason) {
        PlayerProtectionData data = protectionData.computeIfAbsent(playerUUID, k -> new PlayerProtectionData());

        data.totalKicks++;
        data.lastKickTime = System.currentTimeMillis();
        data.kickHistory.add(new KickRecord(System.currentTimeMillis(), reason));

        if (data.kickHistory.size() > 10) {
            data.kickHistory.remove(0);
        }

        if (VoteKickMod.getConfig().isPostKickProtectionEnabled()) {
            long protectionDuration = calculateProtectionDuration(data);
            data.protectionUntil = System.currentTimeMillis() + protectionDuration;
        }

        data.isNewPlayer = false;

        LOGGER.info("Player {} kicked (total: {}), protected for {} seconds",
                playerUUID, data.totalKicks,
                VoteKickMod.getConfig().isPostKickProtectionEnabled() ?
                        calculateProtectionDuration(data) / 1000 : 0);

        save();
    }

    private long calculateProtectionDuration(PlayerProtectionData data) {
        if (!VoteKickMod.getConfig().isPostKickProtectionEnabled()) {
            return 0;
        }

        if (!VoteKickMod.getConfig().isHarassmentDetectionEnabled()) {
            return VoteKickMod.getConfig().getPostKickProtectionDuration() * 1000L;
        }

        long timeWindow = VoteKickMod.getConfig().getHarassmentTimeWindow() * 1000L;
        long windowStart = System.currentTimeMillis() - timeWindow;

        long recentKicks = data.kickHistory.stream()
                .filter(k -> k.timestamp > windowStart)
                .count();

        int harassmentThreshold = VoteKickMod.getConfig().getHarassmentKickThreshold();

        if (recentKicks >= harassmentThreshold + 2) {
            return 3600_000L; // 1 hour for severe harassment
        } else if (recentKicks >= harassmentThreshold) {
            return VoteKickMod.getConfig().getExtendedKickProtectionDuration() * 1000L;
        } else {
            return VoteKickMod.getConfig().getPostKickProtectionDuration() * 1000L;
        }
    }

    public int getRecentKickCount(UUID playerUUID, long withinMs) {
        if (!VoteKickMod.getConfig().isHarassmentDetectionEnabled()) {
            return 0;
        }

        PlayerProtectionData data = protectionData.get(playerUUID);
        if (data == null) return 0;

        long since = System.currentTimeMillis() - withinMs;
        return (int) data.kickHistory.stream()
                .filter(k -> k.timestamp > since)
                .count();
    }

    public boolean hasJoinedBefore(UUID playerUUID) {
        return knownPlayers.contains(playerUUID);
    }

    public double getVoteThresholdModifier(UUID playerUUID) {
        if (!VoteKickMod.getConfig().isVoteThresholdModifiersEnabled()) {
            return 1.0;
        }

        PlayerProtectionData data = protectionData.get(playerUUID);
        if (data == null) return 1.0;

        int lightThreshold = VoteKickMod.getConfig().getLightModifierThreshold();
        int heavyThreshold = VoteKickMod.getConfig().getHeavyModifierThreshold();

        if (data.totalKicks >= heavyThreshold) {
            return VoteKickMod.getConfig().getHeavyVoteModifier();
        }

        if (data.totalKicks >= lightThreshold) {
            return VoteKickMod.getConfig().getLightVoteModifier();
        }

        if (VoteKickMod.getConfig().isHarassmentDetectionEnabled()) {
            long timeWindow = VoteKickMod.getConfig().getHarassmentTimeWindow() * 1000L;
            int recentKicks = getRecentKickCount(playerUUID, timeWindow);
            int harassmentThreshold = VoteKickMod.getConfig().getHarassmentKickThreshold();

            if (recentKicks >= harassmentThreshold - 1) {
                return VoteKickMod.getConfig().getLightVoteModifier();
            }
        }

        return 1.0;
    }

    public void cleanup() {
        if (!anyProtectionEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long cleanupTime = VoteKickMod.getConfig().getDataCleanupDays() * 24L * 60 * 60 * 1000;
        long expireTime = now - cleanupTime;

        protectionData.entrySet().removeIf(entry -> {
            PlayerProtectionData data = entry.getValue();
            return data.protectionUntil < now &&
                    (data.kickHistory.isEmpty() ||
                            data.kickHistory.get(data.kickHistory.size() - 1).timestamp < expireTime);
        });
    }

    private boolean anyProtectionEnabled() {
        return VoteKickMod.getConfig().isNewPlayerProtectionEnabled() ||
                VoteKickMod.getConfig().isPostKickProtectionEnabled() ||
                VoteKickMod.getConfig().isHarassmentDetectionEnabled() ||
                VoteKickMod.getConfig().isVoteThresholdModifiersEnabled();
    }

    public void save() {
        if (!anyProtectionEnabled()) {
            return;
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            Map<String, Object> saveData = new HashMap<>();

            Map<String, PlayerProtectionData> stringKeyedData = new HashMap<>();
            protectionData.forEach((uuid, data) -> stringKeyedData.put(uuid.toString(), data));

            saveData.put("protectionData", stringKeyedData);
            saveData.put("knownPlayers", knownPlayers.stream().map(UUID::toString).toList());

            gson.toJson(saveData, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save protection data", e);
        }
    }

    public void load() {
        if (!anyProtectionEnabled()) {
            return;
        }

        if (!dataFile.exists()) return;

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> loadedData = gson.fromJson(reader, type);

            if (loadedData != null) {
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

    private static class PlayerProtectionData {
        long protectionUntil = 0;
        int totalKicks = 0;
        long lastKickTime = 0;
        boolean isNewPlayer = false;
        List<KickRecord> kickHistory = new ArrayList<>();
    }

    private static class KickRecord {
        final long timestamp;
        final String reason;

        KickRecord(long timestamp, String reason) {
            this.timestamp = timestamp;
            this.reason = reason;
        }
    }
}
