package sierra.thing.votekick.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.vote.VoteOutcome;
import sierra.thing.votekick.vote.VoteSession;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VoteHistoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;
    private final List<VoteHistoryEntry> entries = new ArrayList<>();

    public VoteHistoryManager() {
        File configDir = VoteKickMod.platform().getConfigDir().toFile();
        this.dataFile = new File(configDir, "votekick_history.json");
    }

    public void recordSession(VoteSession session, VoteOutcome outcome, String endedBy) {
        if (session == null || outcome == null) {
            return;
        }

        VoteHistoryEntry entry = new VoteHistoryEntry(
                System.currentTimeMillis(),
                session.getInitiatorName(),
                session.getInitiatorUUID().toString(),
                session.getTargetName(),
                session.getTargetUUID().toString(),
                session.getKickReason(),
                session.getYesVotes(),
                session.getNoVotes(),
                session.getTotalVotesNeeded(),
                session.getTotalEligibleVoters(),
                outcome,
                endedBy
        );

        record(entry);
    }

    public void record(VoteHistoryEntry entry) {
        if (entry == null || !VoteKickMod.getConfig().isHistoryEnabled()) {
            return;
        }

        entries.add(entry);
        prune();
        save();
    }

    public void load() {
        entries.clear();

        if (!dataFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<List<VoteHistoryEntry>>() {}.getType();
            List<VoteHistoryEntry> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                entries.addAll(loaded);
                entries.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
            }

            if (VoteKickMod.getConfig().isHistoryEnabled()) {
                prune();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load vote history", e);
        }
    }

    public void save() {
        if (!VoteKickMod.getConfig().isHistoryEnabled()) {
            return;
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(entries, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save vote history", e);
        }
    }

    public void prune() {
        if (!VoteKickMod.getConfig().isHistoryEnabled()) {
            return;
        }

        long retentionMs = VoteKickMod.getConfig().getHistoryRetentionDays() * 24L * 60 * 60 * 1000;
        long cutoff = System.currentTimeMillis() - retentionMs;

        entries.removeIf(entry -> entry.timestamp < cutoff);

        int maxEntries = VoteKickMod.getConfig().getHistoryMaxEntries();
        if (entries.size() > maxEntries) {
            entries.subList(0, entries.size() - maxEntries).clear();
        }
    }

    public int size() {
        return entries.size();
    }

    public int getMaxPages(int pageSize) {
        if (pageSize <= 0) {
            return 1;
        }
        int total = entries.size();
        return Math.max(1, (int) Math.ceil(total / (double) pageSize));
    }

    public List<VoteHistoryEntry> getEntriesPage(int page, int pageSize) {
        if (pageSize <= 0 || entries.isEmpty()) {
            return Collections.emptyList();
        }

        int total = entries.size();
        int maxPage = getMaxPages(pageSize);
        int pageIndex = Math.min(Math.max(page, 1), maxPage) - 1;

        int endExclusive = total - (pageIndex * pageSize);
        int startInclusive = Math.max(0, endExclusive - pageSize);

        if (startInclusive >= endExclusive) {
            return Collections.emptyList();
        }

        List<VoteHistoryEntry> slice = entries.subList(startInclusive, endExclusive);
        List<VoteHistoryEntry> result = new ArrayList<>(slice);
        Collections.reverse(result);
        return result;
    }
}
