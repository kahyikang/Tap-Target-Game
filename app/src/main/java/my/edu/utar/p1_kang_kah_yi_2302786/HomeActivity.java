package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.List;
import java.util.Locale;

/**
 * App launcher. Gameplay is only started after the player presses Play Game.
 * Uses the same persistent SQLite scores and audio preference keys as MainActivity.
 */
public class HomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "tap_target_settings";
    private static final String PREF_MUSIC = "music_enabled";
    private static final String PREF_SFX = "sfx_enabled";

    private ScoreDatabaseHelper scoreDatabase;
    private SharedPreferences preferences;
    private GameAudioManager audioManager;
    private AlertDialog activeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        scoreDatabase = new ScoreDatabaseHelper(this);
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        audioManager = new GameAudioManager(this);
        audioManager.setMusicMode(GameAudioManager.MusicMode.MENU);
        audioManager.setMusicActive(true);

        ImageView logo = findViewById(R.id.homeLogo);
        // A soft ice-blue/lilac accent behind the logo.
        logo.setBackground(gradientBackground(new int[]{
                Color.parseColor("#DDF3FF"), Color.parseColor("#E9DFFF")
        }, 24, 0));
        logo.setColorFilter(Color.parseColor("#7858C8"));
        View play = findViewById(R.id.playButton);
        View leaderboard = findViewById(R.id.leaderboardButton);
        View settings = findViewById(R.id.homeSettingsButton);

        styleButton(play, true);
        styleButton(leaderboard, false);
        styleButton(settings, false);

        play.setOnClickListener(v -> {
            audioManager.playButtonClick();
            startActivity(new Intent(this, MainActivity.class));
        });
        leaderboard.setOnClickListener(v -> {
            audioManager.playButtonClick();
            showLeaderboardDialog();
        });
        settings.setOnClickListener(v -> {
            audioManager.playButtonClick();
            showSettingsDialog();
        });
    }

    /** Builds a scrollable list using the SAME database as the game-over flow. */
    private void showLeaderboardDialog() {
        final List<ScoreDatabaseHelper.ScoreEntry> scores;
        try {
            scores = scoreDatabase.getTop25();
        } catch (SQLiteException error) {
            Toast.makeText(this, "Couldn't load saved scores.", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout card = dialogCard();
        TextView title = label("🏆  TOP 25 SCORES", 22, "#30264D", true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, fullWidth());


        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        int maxListHeight = Math.min(dp(390),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.47f));
        int contentHeight = scores.isEmpty() ? dp(110) : dp(54 * scores.size());
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.min(maxListHeight, contentHeight));
        scrollParams.topMargin = dp(18);
        card.addView(scroll, scrollParams);

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (scores.isEmpty()) {
            TextView empty = label("No scores yet. Play your first game!",
                    14, "#716A8E", false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(48), dp(8), dp(48));
            rows.addView(empty, fullWidth());
        } else {
            for (int i = 0; i < scores.size(); i++) {
                ScoreDatabaseHelper.ScoreEntry entry = scores.get(i);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(13), dp(12), dp(13));
                row.setBackground(roundedBackground(
                        i % 2 == 0 ? Color.parseColor("#F1EDFC") : Color.WHITE,
                        0, 10));

                TextView rank = label(String.format(Locale.US, "%02d", i + 1),
                        14, "#7858C8", true);
                row.addView(rank, new LinearLayout.LayoutParams(dp(37),
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView player = label(entry.name, 14, "#30264D", false);
                player.setSingleLine(true);
                player.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(player, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView value = label(String.valueOf(entry.score), 16, "#30264D", true);
                value.setGravity(Gravity.END);
                row.addView(value, new LinearLayout.LayoutParams(
                        dp(58), ViewGroup.LayoutParams.WRAP_CONTENT));

                LinearLayout.LayoutParams rowParams = fullWidth();
                rowParams.bottomMargin = dp(4);
                rows.addView(row, rowParams);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card).setCancelable(true).create();
        TextView close = dialogButton("Back to menu");
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        closeParams.topMargin = dp(18);
        card.addView(close, closeParams);
        close.setOnClickListener(v -> {
            audioManager.playButtonClick();
            dialog.dismiss();
        });
        displayDialog(dialog);
    }

    /** Preferences directly control live audio playback. */
    private void showSettingsDialog() {
        LinearLayout card = dialogCard();
        // Compact padding for Settings only (does not affect the Leaderboard).
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        TextView title = label("GAME SETTINGS", 21, "#30264D", true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, fullWidth());

        addSettingsSwitch(card, "Music", PREF_MUSIC);
        addSettingsSwitch(card, "Sound effects", PREF_SFX);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card).setCancelable(true).create();
        TextView done = dialogButton("Save & close");
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        doneParams.topMargin = dp(12);
        card.addView(done, doneParams);
        done.setOnClickListener(v -> {
            audioManager.playButtonClick();
            dialog.dismiss();
        });
        displayDialog(dialog);
    }

    private void addSettingsSwitch(LinearLayout card, String title,
                                   String preferenceKey) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = fullWidth();
        rowParams.topMargin = dp(10);
        card.addView(row, rowParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        labels.addView(label(title, 16, "#30264D", true), fullWidth());
        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setContentDescription(title);
        toggle.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#7858C8"), Color.parseColor("#C9C4D5")}));
        toggle.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#DACDF5"), Color.parseColor("#E4E1EA")}));
        toggle.setChecked(preferences.getBoolean(preferenceKey, true));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(preferenceKey, checked).apply();
            audioManager.refreshPreferences();
            audioManager.playButtonClick();
        });
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleParams.leftMargin = dp(10);
        row.addView(toggle, toggleParams);
    }

    private void styleButton(View button, boolean primary) {
        // Richer violet on Play; almost-white ice blue on secondary buttons.
        GradientDrawable fill = primary
                ? gradientBackground(new int[]{
                        Color.parseColor("#A18AEC"), Color.parseColor("#7858C8")}, 16, 0)
                : gradientBackground(new int[]{
                        Color.parseColor("#FFFFFF"), Color.parseColor("#EAF6FF")},
                        16, Color.parseColor("#D9E6F4"));
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#237858C8")),
                fill, null));
        button.setClickable(true);
        button.setFocusable(true);
        // The Play icon is white on purple; the secondary icons are purple on white.
        if (button instanceof ViewGroup) {
            ViewGroup row = (ViewGroup) button;
            for (int index = 0; index < row.getChildCount(); index++) {
                View child = row.getChildAt(index);
                if (child instanceof ImageView) {
                    ((ImageView) child).setColorFilter(
                            primary ? Color.WHITE : Color.parseColor("#7858C8"));
                }
            }
        }
    }

    private LinearLayout dialogCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(24), dp(20), dp(20));
        card.setBackground(roundedBackground(Color.WHITE, 0, 24));
        return card;
    }

    private TextView dialogButton(String text) {
        TextView button = label(text, 15, "#FFFFFF", true);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#22FFFFFF")),
                gradientBackground(new int[]{
                        Color.parseColor("#A18AEC"), Color.parseColor("#7858C8")
                }, 14, 0), null));
        return button;
    }

    private TextView label(String text, int sizeSp, String hexColor, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.parseColor(hexColor));
        view.setTextSize(sizeSp);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable roundedBackground(int fill, int stroke, int cornerDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(cornerDp));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }


    // Reusable gradient for Home buttons and the menu logo.
    private GradientDrawable gradientBackground(int[] colors, int cornerDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(cornerDp));
        if (strokeColor != 0) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void displayDialog(AlertDialog dialog) {
        if (isFinishing() || isDestroyed()) return;
        if (activeDialog != null && activeDialog.isShowing()) activeDialog.dismiss();
        activeDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (activeDialog == dialog) activeDialog = null;
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int availableWidth = getResources().getDisplayMetrics().widthPixels - dp(36);
            window.setLayout(Math.min(availableWidth, dp(420)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        audioManager.onResume();
        audioManager.refreshPreferences();
    }

    @Override
    protected void onPause() {
        audioManager.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (activeDialog != null) activeDialog.dismiss();
        if (scoreDatabase != null) scoreDatabase.close();
        if (audioManager != null) audioManager.release();
        super.onDestroy();
    }
}
