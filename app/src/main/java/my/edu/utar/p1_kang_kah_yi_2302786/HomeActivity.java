package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Rect;
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
import android.widget.FrameLayout;
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

    private ModeScoreDatabaseHelper scoreDatabase;
    private SharedPreferences preferences;
    private GameAudioManager audioManager;
    private AlertDialog activeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        scoreDatabase = new ModeScoreDatabaseHelper(this);
        ScoreDatabaseHelper legacyScores = new ScoreDatabaseHelper(this);
        try { scoreDatabase.importLegacyScores(legacyScores); }
        finally { legacyScores.close(); }
        // Seed only once per mode and preserve previously saved records.
        scoreDatabase.seedInitialSampleScores();
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        audioManager = new GameAudioManager(this);
        audioManager.setMusicMode(GameAudioManager.MusicMode.MENU);
        audioManager.setMusicActive(true);

        ImageView logo = findViewById(R.id.homeLogo);
        // Use the transparent four-tile artwork directly.
        logo.setBackground(null);
        logo.clearColorFilter();
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        View play = findViewById(R.id.playButton);
        View howToPlay = findViewById(R.id.howToPlayButton);
        View leaderboard = findViewById(R.id.leaderboardButton);
        View settings = findViewById(R.id.homeSettingsButton);

        styleButton(play, true);
        styleButton(howToPlay, false);
        styleButton(leaderboard, false);
        styleButton(settings, false);

        play.setOnClickListener(v -> {
            audioManager.playButtonClick();
            showModeSelectionDialog();
        });
        howToPlay.setOnClickListener(v -> {
            audioManager.playButtonClick();
            showHowToPlayDialog();
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

    /** Graphic multi-page instructions; available without starting or interrupting a game. */
    private void showHowToPlayDialog() {
        LinearLayout card = dialogCard();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow, fullWidth());

        TextView leftSpacer = label("", 18, "#30264D", true);
        topRow.addView(leftSpacer, new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView heading = label("HOW TO PLAY", 21, "#30264D", true);
        heading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        topRow.addView(heading, headingParams);

        TextView close = label("✕", 18, "#7858C8", true);
        close.setGravity(Gravity.CENTER);
        close.setClickable(true);
        close.setFocusable(true);
        close.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#227858C8")),
                roundedBackground(Color.parseColor("#F2EEFF"), 0, 15), null));
        topRow.addView(close, new LinearLayout.LayoutParams(dp(30), dp(30)));

        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(16), dp(14), dp(14));
        content.setBackground(roundedBackground(Color.parseColor("#FBFCFF"),
                Color.parseColor("#E4EAF4"), 18));
        LinearLayout.LayoutParams contentParams = fullWidth();
        contentParams.topMargin = dp(14);
        card.addView(content, contentParams);

        final TextView pageIndicator = label("1 / 4", 13, "#7858C8", true);
        pageIndicator.setGravity(Gravity.CENTER);

        final TextView previous = label("Previous", 14, "#5D5477", true);
        previous.setGravity(Gravity.CENTER);
        previous.setPadding(dp(14), dp(12), dp(14), dp(12));
        previous.setClickable(true);
        previous.setFocusable(true);

        final TextView next = dialogButton("Next");
        next.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams navParams = fullWidth();
        navParams.topMargin = dp(14);
        card.addView(navRow, navParams);

        LinearLayout.LayoutParams navButtonParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        navRow.addView(previous, navButtonParams);

        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        indicatorParams.leftMargin = dp(10);
        indicatorParams.rightMargin = dp(10);
        navRow.addView(pageIndicator, indicatorParams);

        navRow.addView(next, navButtonParams);

        final int[] page = {0};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card).setCancelable(true).create();

        final Runnable[] refreshPage = new Runnable[1];
        refreshPage[0] = () -> {
            content.removeAllViews();
            renderGuidePage(content, page[0]);
            pageIndicator.setText((page[0] + 1) + " / 4");

            boolean first = page[0] == 0;
            boolean last = page[0] == 3;

            previous.setTextColor(Color.parseColor(first ? "#B9B2CB" : "#5D5477"));
            previous.setBackground(roundedBackground(
                    Color.parseColor(first ? "#F4F5F8" : "#F6F9FD"),
                    Color.parseColor("#D9E6F4"), 14));
            previous.setEnabled(!first);
            previous.setAlpha(first ? 0.6f : 1f);

            next.setText(last ? "Done" : "Next");
        };

        previous.setOnClickListener(v -> {
            if (page[0] > 0) {
                audioManager.playButtonClick();
                page[0]--;
                refreshPage[0].run();
            }
        });

        next.setOnClickListener(v -> {
            audioManager.playButtonClick();
            if (page[0] < 3) {
                page[0]++;
                refreshPage[0].run();
            } else {
                dialog.dismiss();
            }
        });

        close.setOnClickListener(v -> {
            audioManager.playButtonClick();
            dialog.dismiss();
        });

        refreshPage[0].run();
        displayDialog(dialog);
    }

    private void renderGuidePage(LinearLayout content, int pageIndex) {
        if (pageIndex == 0) {
            TextView title = label("Find the Different Tile", 18, "#30264D", true);
            title.setGravity(Gravity.CENTER);
            content.addView(title, fullWidth());

            TextView shortText = label("Tap the tile with the different shade", 13, "#6A6280", false);
            shortText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams shortParams = fullWidth();
            shortParams.topMargin = dp(6);
            content.addView(shortText, shortParams);

            LinearLayout gridCard = new LinearLayout(this);
            gridCard.setOrientation(LinearLayout.VERTICAL);
            gridCard.setGravity(Gravity.CENTER);
            gridCard.setPadding(dp(12), dp(12), dp(12), dp(12));
            gridCard.setBackground(roundedBackground(Color.parseColor("#F6FAFF"), 0, 16));
            LinearLayout.LayoutParams gridParams = fullWidth();
            gridParams.topMargin = dp(14);
            content.addView(gridCard, gridParams);

            FrameLayout demoArea = new FrameLayout(this);
            LinearLayout.LayoutParams demoAreaParams = fullWidth();
            demoAreaParams.height = dp(132);
            gridCard.addView(demoArea, demoAreaParams);

            LinearLayout demoGrid = new LinearLayout(this);
            demoGrid.setOrientation(LinearLayout.VERTICAL);
            demoGrid.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams demoGridParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            demoGridParams.gravity = Gravity.CENTER;
            demoArea.addView(demoGrid, demoGridParams);

            View targetTile = null;
            for (int r = 0; r < 2; r++) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowParams = fullWidth();
                rowParams.topMargin = r == 0 ? 0 : dp(6);
                demoGrid.addView(row, rowParams);
                for (int c = 0; c < 2; c++) {
                    boolean target = (r == 0 && c == 1);
                    TextView tile = label(target ? "TAP" : "", 15,
                            "#FFFFFF", true);
                    tile.setGravity(Gravity.CENTER);
                    tile.setContentDescription(target ? "Tap the different tile" : "Normal tile");
                    tile.setBackground(roundedBackground(
                            Color.parseColor(target ? "#A77AE6" : "#D8C4F5"), 0, 10));
                    LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(54), dp(54));
                    tileParams.setMargins(dp(4), 0, dp(4), 0);
                    row.addView(tile, tileParams);
                    if (target) targetTile = tile;
                }
            }

            TextView plusOne = label("+1", 13, "#FFFFFF", true);
            plusOne.setGravity(Gravity.CENTER);
            plusOne.setPadding(dp(8), dp(4), dp(8), dp(4));
            plusOne.setBackground(gradientBackground(new int[]{
                    Color.parseColor("#A88CEB"), Color.parseColor("#7858C8")
            }, 20, 0));
            plusOne.setAlpha(0f);
            FrameLayout.LayoutParams plusParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plusParams.gravity = Gravity.TOP | Gravity.LEFT;
            demoArea.addView(plusOne, plusParams);

            View finalTargetTile = targetTile;
            if (finalTargetTile != null) {
                // Anchor the graphic to the actual tile position rather than
                // guessing pixel offsets. This works across screen sizes.
                demoArea.post(() -> {
                    if (!demoArea.isAttachedToWindow()) return;
                    Rect targetBounds = new Rect(0, 0,
                            finalTargetTile.getWidth(), finalTargetTile.getHeight());
                    demoArea.offsetDescendantRectToMyCoords(finalTargetTile, targetBounds);

                    FrameLayout.LayoutParams scoreLayout =
                            (FrameLayout.LayoutParams) plusOne.getLayoutParams();
                    scoreLayout.leftMargin = targetBounds.left + dp(13);
                    scoreLayout.topMargin = targetBounds.top + dp(9);
                    plusOne.setLayoutParams(scoreLayout);
                });

                finalTargetTile.setClickable(true);
                finalTargetTile.setOnClickListener(v -> {
                    audioManager.playCorrectTap();
                    finalTargetTile.setScaleX(0.92f);
                    finalTargetTile.setScaleY(0.92f);
                    finalTargetTile.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
                    plusOne.animate().cancel();
                    plusOne.setAlpha(0f);
                    plusOne.setTranslationY(dp(12));
                    plusOne.animate().alpha(1f).translationY(0).setDuration(180)
                            .withEndAction(() -> plusOne.animate().alpha(0f).translationY(dp(-8)).setStartDelay(380).setDuration(180).start())
                            .start();
                });
            }
        } else if (pageIndex == 1) {
            TextView title = label("Score and Time", 18, "#30264D", true);
            title.setGravity(Gravity.CENTER);
            content.addView(title, fullWidth());

            LinearLayout badgeRow = new LinearLayout(this);
            badgeRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams badgeParams = fullWidth();
            badgeParams.topMargin = dp(14);
            content.addView(badgeRow, badgeParams);

            LinearLayout scoreCard = miniGuideCard("+1", "Correct Tap", "#F1EAFE", "#7858C8");
            LinearLayout wrongCard = miniGuideCard("-1", "Wrong Tap (Hard Mode)", "#FFF1F1", "#D46060");
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            badgeRow.addView(scoreCard, cardParams);
            LinearLayout.LayoutParams cardParams2 = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cardParams2.leftMargin = dp(8);
            badgeRow.addView(wrongCard, cardParams2);

            LinearLayout timerCard = new LinearLayout(this);
            timerCard.setOrientation(LinearLayout.VERTICAL);
            timerCard.setPadding(dp(12), dp(12), dp(12), dp(12));
            timerCard.setBackground(roundedBackground(Color.parseColor("#EEF7FF"), 0, 16));
            LinearLayout.LayoutParams timerParams = fullWidth();
            timerParams.topMargin = dp(12);
            content.addView(timerCard, timerParams);

            timerCard.addView(label("5 seconds each level", 15, "#30264D", true), fullWidth());
            View barBg = new View(this);
            barBg.setBackground(roundedBackground(Color.parseColor("#DCE8F4"), 0, 6));
            LinearLayout.LayoutParams barBgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(10));
            barBgParams.topMargin = dp(10);
            timerCard.addView(barBg, barBgParams);
            View barFill = new View(this);
            barFill.setBackground(gradientBackground(new int[]{
                    Color.parseColor("#F4BE63"), Color.parseColor("#E39A35")
            }, 6, 0));
            LinearLayout.LayoutParams barFillParams = new LinearLayout.LayoutParams(dp(120), dp(10));
            barFillParams.topMargin = dp(-10);
            timerCard.addView(barFill, barFillParams);
        } else if (pageIndex == 2) {
            TextView pill = label("Easy Mode", 16, "#365B82", true);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(dp(14), dp(9), dp(14), dp(9));
            pill.setBackground(roundedBackground(Color.parseColor("#E8F6FF"), 0, 16));
            content.addView(pill, fullWidth());

            LinearLayout.LayoutParams rowParams = fullWidth();
            rowParams.topMargin = dp(14);
            content.addView(featureRow("◉", "Visible color difference", "#7858C8"), rowParams);
            LinearLayout.LayoutParams rowParams2 = fullWidth();
            rowParams2.topMargin = dp(10);
            content.addView(featureRow("8", "Eight levels", "#5A9BD4"), rowParams2);
            LinearLayout.LayoutParams rowParams3 = fullWidth();
            rowParams3.topMargin = dp(10);
            content.addView(featureRow("Ⅱ", "Pause after each level", "#7B6BD6"), rowParams3);
            LinearLayout.LayoutParams rowParams4 = fullWidth();
            rowParams4.topMargin = dp(10);
            content.addView(featureRow("0", "Wrong tap: no penalty", "#47A17C"), rowParams4);
        } else {
            TextView pill = label("Hard Mode", 16, "#7858C8", true);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(dp(14), dp(9), dp(14), dp(9));
            pill.setBackground(roundedBackground(Color.parseColor("#F0E8FF"), 0, 16));
            content.addView(pill, fullWidth());

            LinearLayout.LayoutParams rowParams = fullWidth();
            rowParams.topMargin = dp(14);
            content.addView(featureRow("◐", "Colors get harder", "#7858C8"), rowParams);
            LinearLayout.LayoutParams rowParams2 = fullWidth();
            rowParams2.topMargin = dp(10);
            content.addView(featureRow("➜", "Levels auto advance", "#5A9BD4"), rowParams2);
            LinearLayout.LayoutParams rowParams3 = fullWidth();
            rowParams3.topMargin = dp(10);
            content.addView(featureRow("-1", "Wrong tap deducts 1 point", "#D46060"), rowParams3);
            LinearLayout.LayoutParams rowParams4 = fullWidth();
            rowParams4.topMargin = dp(10);
            content.addView(featureRow("🏆", "Separate top 25 scores", "#C98B1B"), rowParams4);
        }
    }

    private LinearLayout miniGuideCard(String bigText, String smallText,
                                       String background, String textColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(14), dp(10), dp(14));
        card.setBackground(roundedBackground(Color.parseColor(background), 0, 14));
        TextView big = label(bigText, 26, textColor, true);
        big.setGravity(Gravity.CENTER);
        card.addView(big, fullWidth());
        TextView small = label(smallText, 11, "#5E5574", true);
        small.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams smallParams = fullWidth();
        smallParams.topMargin = dp(4);
        card.addView(small, smallParams);
        return card;
    }

    private LinearLayout featureRow(String iconText, String bodyText, String iconColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(roundedBackground(Color.parseColor("#FBFCFF"),
                Color.parseColor("#E2E7F0"), 14));

        TextView icon = label(iconText, 18, iconColor, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundedBackground(Color.parseColor("#F4F7FB"), 0, 12));
        row.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView body = label(bodyText, 14, "#30264D", true);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bodyParams.leftMargin = dp(12);
        row.addView(body, bodyParams);
        return row;
    }

    /** Choose a mode before gameplay; the mode stays selected for this entire run. */
    private void showModeSelectionDialog() {
        LinearLayout card = dialogCard();
        TextView title = label("SELECT GAME MODE", 22, "#30264D", true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, fullWidth());

        TextView easy = modeChoice("EASY MODE", "#E8F6FF", "#365B82");
        LinearLayout.LayoutParams easyParams = fullWidth();
        easyParams.topMargin = dp(22);
        card.addView(easy, easyParams);

        TextView hard = modeChoice("HARD MODE", "#F0E8FF", "#7858C8");
        LinearLayout.LayoutParams hardParams = fullWidth();
        hardParams.topMargin = dp(12);
        card.addView(hard, hardParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card).setCancelable(true).create();
        easy.setOnClickListener(v -> startChosenMode(dialog, GameMode.EASY));
        hard.setOnClickListener(v -> startChosenMode(dialog, GameMode.HARD));
        displayDialog(dialog);
    }

    private TextView modeChoice(String title,
                                String background, String foreground) {
        TextView choice = label(title, 16, foreground, true);
        choice.setGravity(Gravity.CENTER);
        choice.setPadding(dp(14), dp(18), dp(14), dp(18));
        choice.setClickable(true);
        choice.setFocusable(true);
        choice.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#225A45A8")),
                roundedBackground(Color.parseColor(background),
                        Color.parseColor("#D8D2EB"), 15), null));
        return choice;
    }

    private void startChosenMode(AlertDialog dialog, GameMode mode) {
        audioManager.playButtonClick();
        dialog.dismiss();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(GameMode.EXTRA_MODE, mode.key());
        startActivity(intent);
    }

    /** Builds a scrollable Top 25 table for the selected mode. */
    private void showLeaderboardDialog() { showLeaderboardDialog(GameMode.EASY); }

    private void showLeaderboardDialog(GameMode selectedMode) {
        final List<ModeScoreDatabaseHelper.ScoreEntry> scores;
        try {
            scores = scoreDatabase.getTop25(selectedMode);
        } catch (SQLiteException error) {
            Toast.makeText(this, "Couldn't load saved scores.", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout card = dialogCard();
        TextView title = label("🏆  TOP 25 SCORES", 22, "#30264D", true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, fullWidth());

        addLeaderboardTabs(card, selectedMode);

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
            TextView empty = label("No Scores Yet. Play Your First Game!",
                    14, "#716A8E", false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(48), dp(8), dp(48));
            rows.addView(empty, fullWidth());
        } else {
            for (int i = 0; i < scores.size(); i++) {
                ModeScoreDatabaseHelper.ScoreEntry entry = scores.get(i);
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
        TextView close = dialogButton("Back to Menu");
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

    private void addLeaderboardTabs(LinearLayout card, GameMode selectedMode) {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tabsParams = fullWidth();
        tabsParams.topMargin = dp(14);
        card.addView(tabs, tabsParams);
        for (GameMode tabMode : GameMode.values()) {
            boolean selected = tabMode == selectedMode;
            TextView tab = label(tabMode.label() + " Mode", 14,
                    selected ? "#FFFFFF" : "#50456B", true);
            tab.setGravity(Gravity.CENTER);
            tab.setClickable(true);
            tab.setFocusable(true);
            tab.setBackground(roundedBackground(Color.parseColor(
                    selected ? "#7858C8" : "#EAF5FC"), 0, 13));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(43), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            tabs.addView(tab, lp);
            tab.setOnClickListener(v -> {
                if (selected) return;
                audioManager.playButtonClick();
                showLeaderboardDialog(tabMode);
            });
        }
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
        addSettingsSwitch(card, "Sound Effects", PREF_SFX);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card).setCancelable(true).create();
        TextView done = dialogButton("Save & Close");
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
