package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Independent Top-25 boards for each mode, stored persistently on the device. */
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

    /**
     * Copy existing pre-mode scores into Easy exactly once.
     * The original database is not modified or deleted. An empty legacy board
     * is also marked migrated, avoiding unwanted future duplicate imports.
     */
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
