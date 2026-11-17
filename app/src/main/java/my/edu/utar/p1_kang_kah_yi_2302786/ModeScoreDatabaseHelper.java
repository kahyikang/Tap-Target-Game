package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// Independent Top-25 boards for each mode, stored persistently on the device.
public class ModeScoreDatabaseHelper extends SQLiteOpenHelper {
    // A NEW database, so the original ScoreDatabaseHelper database is never altered.
    private static final String DB_NAME = "tap_target_mode_scores.db";
    private static final int DB_VERSION = 1;
    private static final int LIMIT = 25;

    public static class ScoreEntry {
        public final String name;
        public final int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    public ModeScoreDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // Creates mode-tagged scores, a ranking index, and one-time migration markers.
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS mode_scores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, score INTEGER NOT NULL, " +
                "mode TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_mode_rank ON " +
                "mode_scores(mode, score DESC, id ASC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS migration_state (" +
                "name TEXT PRIMARY KEY)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Reserved for future non-destructive schema upgrades.
    }

    // Populate up to 25 example entries per mode, without overwriting or deleting
    // previously saved player scores. A durable marker prevents reseeding even when
    // sample entries later fall out of the Top 25.
    // Call this after importLegacyScores(), so legacy user scores take priority.
    public void seedInitialSampleScores() {
        final String[] easyNames = {
                "Alex", "Bella", "Casey", "Dylan", "Eden",
                "Finn", "Grace", "Hugo", "Iris", "Jamie",
                "Kelly", "Luca", "Maya", "Noah", "Olive",
                "Parker", "Quinn", "Riley", "Sage", "Taylor",
                "Uma", "Victor", "Wren", "Xander", "Zoe"
        };
        final int[] easyScores = {
                40, 38, 36, 34, 32, 30, 28, 26, 24, 22,
                20, 19, 18, 18, 17, 16, 16, 16, 12, 10,
                9, 9, 8, 8, 8
        };
        final String[] hardNames = {
                "Nova", "Jett", "Luna", "Ash", "Milo",
                "Cora", "Reese", "Dex", "Robin", "Lila",
                "Jasper", "Mia", "Arlo", "Cleo", "Beau",
                "Kira", "Rory", "Blair", "June", "Piper",
                "Remy", "Ellis", "Zane", "Indie", "Pax"
        };
        final int[] hardScores = {
                24, 23, 22, 21, 20, 19, 18, 17, 16, 15,
                14, 13, 12, 11, 11, 10, 10, 10, 9, 9,
                9, 8, 7, 6, 5
        };

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            seedModeOnce(db, GameMode.EASY, easyNames, easyScores);
            seedModeOnce(db, GameMode.HARD, hardNames, hardScores);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void seedModeOnce(SQLiteDatabase db, GameMode mode,
                              String[] names, int[] scores) {
        if (names.length != LIMIT || scores.length != LIMIT) {
            throw new IllegalArgumentException("Expected 25 sample names and scores");
        }
        String markerName = "demo_scores_v1_" + mode.key();
        try (Cursor flag = db.rawQuery(
                "SELECT name FROM migration_state WHERE name=?",
                new String[]{markerName})) {
            if (flag.moveToFirst()) return;
        }

        int currentCount = 0;
        try (Cursor countCursor = db.rawQuery(
                "SELECT COUNT(*) FROM mode_scores WHERE mode=?",
                new String[]{mode.key()})) {
            if (countCursor.moveToFirst()) currentCount = countCursor.getInt(0);
        }
        // Do not trim existing records to make room for sample entries.
        int spaces = Math.max(0, LIMIT - currentCount);
        for (int i = 0; i < spaces; i++) {
            insertRow(db, names[i], scores[i], mode);
        }

        ContentValues marker = new ContentValues();
        marker.put("name", markerName);
        db.insertOrThrow("migration_state", null, marker);
    }


    // Copy existing pre-mode scores into Easy exactly once.
    // The original database is not modified or deleted. An empty legacy board
    // is also marked migrated, avoiding unwanted future duplicate imports.
    public void importLegacyScores(ScoreDatabaseHelper legacy) {
        SQLiteDatabase db = getWritableDatabase();
        List<ScoreDatabaseHelper.ScoreEntry> oldScores;
        // Do not lock the new database while reading the old database.
        try {
            oldScores = legacy.getTop25();
        } catch (RuntimeException error) {
            // Leave the marker unset so the import can be retried later.
            return;
        }
        db.beginTransaction();
        try {
            try (Cursor flag = db.rawQuery(
                    "SELECT name FROM migration_state WHERE name='legacy_to_easy'", null)) {
                if (flag.moveToFirst()) {
                    db.setTransactionSuccessful();
                    return;
                }
            }
            for (ScoreDatabaseHelper.ScoreEntry old : oldScores) {
                insertRow(db, old.name, old.score, GameMode.EASY);
            }
            trimToTop25(db, GameMode.EASY);
            ContentValues marker = new ContentValues();
            marker.put("name", "legacy_to_easy");
            db.insertOrThrow("migration_state", null, marker);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Filters by mode and ranks equal scores by their earlier insertion ID.
    public List<ScoreEntry> getTop25(GameMode mode) {
        List<ScoreEntry> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT name, score FROM mode_scores WHERE mode=? " +
                        "ORDER BY score DESC, id ASC LIMIT 25",
                new String[]{mode.key()})) {
            while (c.moveToNext()) result.add(new ScoreEntry(c.getString(0), c.getInt(1)));
        }
        return result;
    }

    public boolean qualifies(int score, GameMode mode) {
        return qualifies(getReadableDatabase(), score, mode);
    }

    private boolean qualifies(SQLiteDatabase db, int score, GameMode mode) {
        try (Cursor c = db.rawQuery(
                "SELECT score FROM mode_scores WHERE mode=? " +
                        "ORDER BY score DESC, id ASC LIMIT 1 OFFSET 24",
                new String[]{mode.key()})) {
            // If fewer than 25 scores exist, any nonnegative score can enter.
            if (!c.moveToFirst()) return score >= 0;
            // Earlier scores win ties for the 25th position.
            return score > c.getInt(0);
        }
    }

    // Validates input, rechecks eligibility, and trims this mode in one transaction.
    public boolean insertIfQualified(String name, int score, GameMode mode) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > 20 || score < 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        boolean inserted = false;
        db.beginTransaction();
        try {
            if (qualifies(db, score, mode)) {
                insertRow(db, cleanName, score, mode);
                trimToTop25(db, mode);
                inserted = true;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return inserted;
    }

    private void insertRow(SQLiteDatabase db, String name, int score, GameMode mode) {
        ContentValues row = new ContentValues();
        row.put("name", name);
        row.put("score", score);
        row.put("mode", mode.key());
        row.put("created_at", System.currentTimeMillis());
        db.insertOrThrow("mode_scores", null, row);
    }

    private void trimToTop25(SQLiteDatabase db, GameMode mode) {
        // SQL parameter bindings protect the mode value; each board is trimmed separately.
        db.execSQL("DELETE FROM mode_scores WHERE mode=? AND id NOT IN (" +
                "SELECT id FROM mode_scores WHERE mode=? " +
                "ORDER BY score DESC, id ASC LIMIT 25)",
                new Object[]{mode.key(), mode.key()});
    }
}
