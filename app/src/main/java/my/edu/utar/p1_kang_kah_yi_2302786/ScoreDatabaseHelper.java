package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores Top 25 player names and scores in a persistent on-device SQLite database.
 * Tie policy: once the table contains 25 scores, a new score must be strictly
 * greater than the current 25th score to qualify. Earlier entries win ties.
 */
public class ScoreDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "taptarget_scores.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "scores";
    private static final int MAX_ENTRIES = 25;

    public static class ScoreEntry {
        public final String name;
        public final int score;

        ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    public ScoreDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "player_name TEXT NOT NULL, "
                + "score INTEGER NOT NULL CHECK(score >= 0), "
                + "created_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 has no migrations yet. Add non-destructive migrations here
        // if a later version changes the database schema.
    }

    public boolean qualifies(int score) {
        return qualifies(getReadableDatabase(), score);
    }

    private boolean qualifies(SQLiteDatabase db, int score) {
        if (score < 0) return false;
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*), MIN(score) FROM " + TABLE, null)) {
            if (!cursor.moveToFirst()) return true;
            int count = cursor.getInt(0);
            if (count < MAX_ENTRIES) return true;
            return !cursor.isNull(1) && score > cursor.getInt(1);
        }
    }

    /** Insert only if eligible, then remove entries ranked below 25th. */
    public boolean insertIfQualified(String playerName, int score) {
        String cleaned = playerName == null ? "" : playerName.trim();
        if (cleaned.isEmpty() || cleaned.length() > 20 || score < 0) return false;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (!qualifies(db, score)) {
                db.setTransactionSuccessful();
                return false;
            }
            ContentValues values = new ContentValues();
            values.put("player_name", cleaned);
            values.put("score", score);
            values.put("created_at", System.currentTimeMillis());
            if (db.insertOrThrow(TABLE, null, values) < 0) return false;

            db.execSQL("DELETE FROM " + TABLE + " WHERE _id NOT IN ("
                    + "SELECT _id FROM " + TABLE
                    + " ORDER BY score DESC, _id ASC LIMIT " + MAX_ENTRIES + ")");
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public List<ScoreEntry> getTop25() {
        List<ScoreEntry> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE,
                new String[]{"player_name", "score"}, null, null,
                null, null, "score DESC, _id ASC", String.valueOf(MAX_ENTRIES))) {
            while (cursor.moveToNext()) {
                result.add(new ScoreEntry(cursor.getString(0), cursor.getInt(1)));
            }
        }
        return result;
    }
}
