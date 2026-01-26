package sierra.thing.votekick.vote;

public enum VoteOutcome {
    PASSED(true, "Passed"),
    FAILED(false, "Failed"),
    CANCELED(false, "Canceled"),
    FORCED_PASS(true, "Forced");

    private final boolean shouldKick;
    private final String label;

    VoteOutcome(boolean shouldKick, String label) {
        this.shouldKick = shouldKick;
        this.label = label;
    }

    public boolean shouldKick() {
        return shouldKick;
    }

    public String getLabel() {
        return label;
    }
}
