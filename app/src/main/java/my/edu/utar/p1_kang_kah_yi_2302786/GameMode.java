package my.edu.utar.p1_kang_kah_yi_2302786;

// The selected game rules and the persistent leaderboard partition.
public enum GameMode {
    EASY("easy", "Easy"),
    HARD("hard", "Hard");

    public static final String EXTRA_MODE = "tap_target_mode";

    private final String storageKey;
    private final String displayName;

    GameMode(String storageKey, String displayName) {
        this.storageKey = storageKey;
        this.displayName = displayName;
    }

    public String key() {
        return storageKey;
    }
    public String label() {
        return displayName;
    }

    // Reads the Intent value safely, falling back to Easy for missing or unknown values.
    public static GameMode fromExtra(String value) {
        return HARD.storageKey.equalsIgnoreCase(value) ? HARD : EASY;
    }
}
