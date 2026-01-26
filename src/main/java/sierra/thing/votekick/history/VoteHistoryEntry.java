package sierra.thing.votekick.history;

import sierra.thing.votekick.vote.VoteOutcome;

public class VoteHistoryEntry {
    public long timestamp;
    public String initiatorName;
    public String initiatorUuid;
    public String targetName;
    public String targetUuid;
    public String reason;
    public int yesVotes;
    public int noVotes;
    public int votesNeeded;
    public int totalEligible;
    public VoteOutcome outcome;
    public String endedBy;

    public VoteHistoryEntry() {
    }

    public VoteHistoryEntry(long timestamp,
                            String initiatorName,
                            String initiatorUuid,
                            String targetName,
                            String targetUuid,
                            String reason,
                            int yesVotes,
                            int noVotes,
                            int votesNeeded,
                            int totalEligible,
                            VoteOutcome outcome,
                            String endedBy) {
        this.timestamp = timestamp;
        this.initiatorName = initiatorName;
        this.initiatorUuid = initiatorUuid;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
        this.reason = reason;
        this.yesVotes = yesVotes;
        this.noVotes = noVotes;
        this.votesNeeded = votesNeeded;
        this.totalEligible = totalEligible;
        this.outcome = outcome;
        this.endedBy = endedBy;
    }
}
