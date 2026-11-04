package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.text.InputFilter;
import android.text.InputType;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // One color pair is picked at random whenever a level starts.
    // All tiles use the normal color except one target tile.
    private static class ColorPair {
        final int normalColor;
        final int targetColor;

        ColorPair(String normalHex, String targetHex) {
            normalColor = Color.parseColor(normalHex);
            targetColor = Color.parseColor(targetHex);
        }
    }

    private static ColorPair pair(String normalHex, String targetHex) {
        return new ColorPair(normalHex, targetHex);
    }

    private static class LevelConfig {
        final int gridSize;
        final long durationMs;
        final ColorPair[] colorPairs;

        LevelConfig(int gridSize, ColorPair... colorPairs) {
            this(gridSize, 5_000L, colorPairs);
        }

        LevelConfig(int gridSize, long durationMs, ColorPair... colorPairs) {
            this.gridSize = gridSize;
            this.durationMs = durationMs;
            this.colorPairs = colorPairs;
        }
    }

    // To change the look of a level, edit its pairs below.
    // Format: pair("#NORMAL_TILE_COLOR", "#TARGET_TILE_COLOR").
    // Later levels use shades that are progressively more alike.
    private final List<LevelConfig> levels = Arrays.asList(
            new LevelConfig(2, // Level 1: 2 x 2
                    pair("#B9DCF4", "#70B1E0"), // blue
                    pair("#B9E4D5", "#69C5A7"), // mint
                    pair("#D8C8EB", "#A98DDA"), // lavender
                    pair("#F1D1B8", "#E39E72")  // peach
            ),
            new LevelConfig(3, // Level 2: 3 x 3
                    pair("#D0E5F3", "#9EC4E0"), // blue
                    pair("#C7E8C7", "#A2D3A2"), // green
                    pair("#F1D7D7", "#DDB1B1"), // pink
                    pair("#E2D8F3", "#C3B8E7")  // lilac
            ),
            new LevelConfig(4, // Level 3: 4 x 4
                    pair("#C7DFED", "#A6CCD9"), // blue
                    pair("#D0E7DB", "#B5D7C3"), // mint
                    pair("#E7D9F0", "#D3C3E2"), // purple
                    pair("#F0DBCC", "#DFC5B0")  // sand
            ),
            new LevelConfig(5, // Level 4: 5 x 5
                    pair("#BCD9EA", "#A6CADB"), // blue
                    pair("#BFE5DD", "#A9D8CB"), // teal
                    pair("#E8D8EA", "#D7C8DE"), // pink/lilac
                    pair("#EADFC8", "#D8D2B6")  // beige
            ),
            new LevelConfig(6, // Level 5: 6 x 6
                    pair("#BDD9DA", "#ADD0CF"), // teal
                    pair("#DFD5EE", "#D0CBE4"), // lavender
                    pair("#E8D6C9", "#DACDBE"), // peach
                    pair("#D6E3BD", "#C8DAB1")  // light green
            ),
            new LevelConfig(7, // Level 6: 7 x 7
                    pair("#C7DEEB", "#BDD7E4"), // blue
                    pair("#CDE6D9", "#C2DFD1"), // mint
                    pair("#E7D6E8", "#DCCFE1"), // pink
                    pair("#E8DEC9", "#DDD7C2")  // cream
            ),
            new LevelConfig(8, // Level 7: 8 x 8
                    pair("#D0E4ED", "#C6DDE5"), // blue
                    pair("#D7E6CF", "#CDDFC7"), // sage
                    pair("#E8D9DE", "#DED2D6"), // rose
                    pair("#DDD8EB", "#D3D1E3")  // lilac
            ),
            new LevelConfig(9, // Level 8: 9 x 9
                    pair("#D9E7EE", "#D1E1E8"), // blue
                    pair("#D8E8DB", "#D0E2D5"), // green
                    pair("#ECDCE3", "#E4D6DD"), // pink
                    pair("#E7E0D8", "#DFDAD2")  // warm grey
            )
    );

    private enum GameState {
        INTRO, PLAYING, PAUSED, LEVEL_COMPLETE, GAME_OVER
    }

    private TextView levelText;
    private TextView scoreLabel;
    private TextView scoreText;
    private TextView timerText;
    private GridLayout gameGrid;
    private FrameLayout boardContainer;
    private ImageButton pauseButton;
    private ImageButton settingsButton;
    private FrameLayout timerTrack;
    private View timerFill;
    private GradientDrawable timerFillDrawable;

    private FrameLayout introOverlay;
    private TextView introTitle;
    private TextView introNumber;
    private TextView introHint;
    private final Handler introHandler = new Handler(Looper.getMainLooper());
    private Runnable introRunnable;
    private boolean introRestartOnResume = false;

    private SharedPreferences preferences;
    private ScoreDatabaseHelper scoreDatabase;
    private GameAudioManager audioManager;
    private long lastWrongTapSoundMs = 0L;
    private static final String PREFS_NAME = "tap_target_settings";
    private static final String PREF_MUSIC = "music_enabled";
    private static final String PREF_SFX = "sfx_enabled";

    private final List<TextView> tiles = new ArrayList<>();
    private final Random random = new Random();
    private ColorPair currentColorPair; // Chosen once per level, not after every tap.

    private int levelIndex = 0;
    private int totalScore = 0;
    private int highlightedIndex = -1;
    private GameState state = GameState.GAME_OVER;

    private long timeRemainingMs = 5_000L;
    private long deadlineMs = 0L;
    private CountDownTimer countdown;
    private boolean pausedByBackground = false;
    private boolean countdownStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        levelText = findViewById(R.id.levelText);
        scoreLabel = findViewById(R.id.scoreLabel);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);
        gameGrid = findViewById(R.id.gameGrid);
        boardContainer = findViewById(R.id.boardContainer);
        pauseButton = findViewById(R.id.pauseButton);
        settingsButton = findViewById(R.id.settingsButton);
        timerTrack = findViewById(R.id.timerTrack);
        timerFill = findViewById(R.id.timerFill);
        introOverlay = findViewById(R.id.introOverlay);
        introTitle = findViewById(R.id.introTitle);
        introNumber = findViewById(R.id.introNumber);
        introHint = findViewById(R.id.introHint);
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        scoreDatabase = new ScoreDatabaseHelper(this);
        audioManager = new GameAudioManager(this);

        // Centered level + score badges; no dividers or surrounding board panel.
        levelText.setBackground(roundedBackground(Color.parseColor("#E8EEF8"), 0, 50));
        scoreLabel.setBackground(roundedBackground(Color.parseColor("#E8EEF8"), 0, 50));
        // Keep the board transparent so the tiles sit directly on the page.
        boardContainer.setBackgroundColor(Color.TRANSPARENT);
        timerTrack.setBackground(roundedBackground(Color.parseColor("#E2E8F0"), 0, 30));
        timerFillDrawable = roundedBackground(Color.parseColor("#2563EB"), 0, 30);
        timerFill.setBackground(timerFillDrawable);
        styleHeaderButton(pauseButton);
        styleHeaderButton(settingsButton);

        pauseButton.setOnClickListener(v -> pauseLevel());
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        timerTrack.post(this::updateTimerProgress);
        startNewGame();
    }

    private void startNewGame() {
        totalScore = 0;
        startLevel(0);
    }

    private void startLevel(int newIndex) {
        stopIntroCountdown();
        stopCountdown();
        levelIndex = newIndex;
        LevelConfig config = levels.get(levelIndex);
        timeRemainingMs = config.durationMs;
        currentColorPair = config.colorPairs[random.nextInt(config.colorPairs.length)];
        highlightedIndex = -1;
        // Only Level 1 needs the special "Are You Ready?" introduction.
        boolean firstLevel = newIndex == 0;
        state = firstLevel ? GameState.INTRO : GameState.PLAYING;
        audioManager.setMusicActive(true);
        pauseButton.setEnabled(!firstLevel);
        settingsButton.setEnabled(!firstLevel);
        introOverlay.setVisibility(firstLevel ? View.VISIBLE : View.GONE);
        gameGrid.removeAllViews();
        tiles.clear();
        updateHeader();

        boardContainer.post(() -> {
            if (levelIndex != newIndex ||
                    (state != GameState.INTRO && state != GameState.PLAYING)) return;
            createGrid(config.gridSize);
            highlightRandomTile();
            if (firstLevel) {
                startIntroCountdown();
            } else {
                startCountdown();
            }
        });
    }

    // A short countdown before Level 1 only. The five-second gameplay
    // timer does not begin until the "START!" message disappears.
    private void startIntroCountdown() {
        stopIntroCountdown();
        if (state != GameState.INTRO) return;
        introOverlay.setVisibility(View.VISIBLE);
        showIntroStep(0);
    }

    private void showIntroStep(int step) {
        if (state != GameState.INTRO || isFinishing() || isDestroyed()) return;
        if (step >= 5) {
            introOverlay.setVisibility(View.GONE);
            introRunnable = null;
            state = GameState.PLAYING;
            startCountdown();
            return;
        }

        final String[] messages = {"READY", "3", "2", "1", "START!"};
        introTitle.setText(step == 0 ? "ARE YOU READY!" :
                (step == 4 ? "LET'S GO!" : "GET READY"));
        introNumber.setText(messages[step]);
        introNumber.setTextColor(Color.parseColor(step == 4 ? "#16A34A" : "#2563EB"));
        introHint.setText(step == 0
                ? "Find the tile with a different shade."
                : step == 4 ? "Tap as fast as you can!" : "Focus on the colors");

        // Subtle pop animation to distinguish each countdown number.
        introNumber.animate().cancel();
        introNumber.setAlpha(0.25f);
        introNumber.setScaleX(1.25f);
        introNumber.setScaleY(1.25f);
        introNumber.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240).start();

        long displayMs = (step == 0 || step == 4) ? 650L : 1000L;
        introRunnable = () -> showIntroStep(step + 1);
        introHandler.postDelayed(introRunnable, displayMs);
    }

    private void stopIntroCountdown() {
        if (introRunnable != null) {
            introHandler.removeCallbacks(introRunnable);
            introRunnable = null;
        }
        if (introNumber != null) introNumber.animate().cancel();
    }

    private void createGrid(int size) {
        tiles.clear();
        gameGrid.removeAllViews();
        gameGrid.setRowCount(size);
        gameGrid.setColumnCount(size);

        // Use smaller gaps for larger boards so 6x6 through 9x9 fit the screen.
        int margin = size >= 6 ? dp(1) : dp(2);
        // Fit the square board into BOTH available width and height.
        // This leaves room for the new top toolbar and countdown bar.
        int boardWidth = boardContainer.getWidth()
                - boardContainer.getPaddingLeft() - boardContainer.getPaddingRight();
        int boardHeight = boardContainer.getHeight()
                - boardContainer.getPaddingTop() - boardContainer.getPaddingBottom();
        int available = Math.max(1, Math.min(boardWidth, boardHeight));
        int tileSize = Math.max(1, (available - size * margin * 2) / size);

        for (int index = 0; index < size * size; index++) {
            final int tileIndex = index;
            TextView tile = new TextView(this);
            tile.setGravity(Gravity.CENTER);
            // No star or label: players spot the target by its color alone.
            tile.setContentDescription("Tile " + (index + 1));
            tile.setBackground(tileBackground(false));
            tile.setOnClickListener(v -> handleTileTap(tileIndex));

            int row = index / size;
            int column = index % size;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(row), GridLayout.spec(column)
            );
            params.width = tileSize;
            params.height = tileSize;
            params.setMargins(margin, margin, margin, margin);

            gameGrid.addView(tile, params);
            tiles.add(tile);
        }
    }

    private void handleTileTap(int index) {
        if (state != GameState.PLAYING || !countdownStarted) return;
        if (SystemClock.elapsedRealtime() >= deadlineMs) {
            completeLevel();
            return;
        }
        if (index != highlightedIndex) {
            // Rate-limit incorrect-tap audio to avoid a noisy burst of sounds.
            long now = SystemClock.elapsedRealtime();
            if (now - lastWrongTapSoundMs >= 120L) {
                audioManager.playWrongTap();
                lastWrongTapSoundMs = now;
            }
            return;
        }

        totalScore++;
        audioManager.playCorrectTap();
        animateCorrectTap(tiles.get(index));
        updateHeader();
        animateScore();
        highlightRandomTile(); // Repeating the same target is allowed.
    }

    // Tiny press effect without changing the tile color or showing a symbol.
    private void animateCorrectTap(TextView tile) {
        tile.animate().cancel();
        tile.setScaleX(1f);
        tile.setScaleY(1f);
        tile.animate().scaleX(0.93f).scaleY(0.93f)
                .setDuration(75L)
                .withEndAction(() -> tile.animate().scaleX(1f).scaleY(1f)
                        .setDuration(115L).start())
                .start();
    }

    // Subtle feedback on the large score number after each correct tap.
    private void animateScore() {
        scoreText.animate().cancel();
        scoreText.setScaleX(1.12f);
        scoreText.setScaleY(1.12f);
        scoreText.animate().scaleX(1f).scaleY(1f)
                .setDuration(170L).start();
    }

    private void highlightRandomTile() {
        if (tiles.isEmpty()) return;
        highlightedIndex = random.nextInt(tiles.size());

        for (int index = 0; index < tiles.size(); index++) {
            boolean active = index == highlightedIndex;
            TextView tile = tiles.get(index);
            tile.setBackground(tileBackground(active));
            tile.setContentDescription(active
                    ? "Highlighted target, tile " + (index + 1)
                    : "Tile " + (index + 1));
        }
    }

    private GradientDrawable tileBackground(boolean active) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(12));
        background.setColor(active ? currentColorPair.targetColor : currentColorPair.normalColor);
        // Keep identical borders on all tiles; the color is the only clue.
        background.setStroke(dp(1), Color.parseColor("#FFFFFF"));
        return background;
    }

    private void startCountdown() {
        if (timeRemainingMs <= 0L) {
            completeLevel();
            return;
        }
        countdownStarted = true;
        pauseButton.setEnabled(true);
        settingsButton.setEnabled(true);
        deadlineMs = SystemClock.elapsedRealtime() + timeRemainingMs;
        updateHeader();
        countdown = new CountDownTimer(timeRemainingMs, 50L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (state != GameState.PLAYING) return;
                timeRemainingMs = Math.max(0L, deadlineMs - SystemClock.elapsedRealtime());
                updateHeader();
            }

            @Override
            public void onFinish() {
                if (state == GameState.PLAYING) completeLevel();
            }
        }.start();
    }

    private void stopCountdown() {
        if (countdown != null) {
            countdown.cancel();
            countdown = null;
        }
        countdownStarted = false;
    }

    private void pauseLevel() {
        if (state != GameState.PLAYING || !countdownStarted) return;
        timeRemainingMs = Math.max(0L, deadlineMs - SystemClock.elapsedRealtime());
        stopCountdown();
        if (timeRemainingMs == 0L) {
            completeLevel();
            return;
        }
        state = GameState.PAUSED;
        audioManager.setMusicActive(false);
        pauseButton.setEnabled(false);
        settingsButton.setEnabled(false);
        updateHeader();
        showPauseDialog();
    }

    private void resumeLevel() {
        if (state != GameState.PAUSED) return;
        state = GameState.PLAYING;
        audioManager.setMusicActive(true);
        startCountdown(); // Continue with remaining time, not a fresh 5 seconds.
    }

    // The four dialogs below share one custom, light-themed layout.
    private void showPauseDialog() {
        showStyledDialog(
                "Ⅱ", "GAME PAUSED", "Take a break",
                "Your remaining time is saved until you resume.",
                Color.parseColor("#2563EB"), Color.parseColor("#DBEAFE"),
                new String[]{"LEVEL", "SCORE", "TIME LEFT"},
                new String[]{(levelIndex + 1) + "/" + levels.size(),
                        String.valueOf(totalScore), formatSeconds(timeRemainingMs)},
                "Resume game", this::resumeLevel,
                "End game", () -> confirmEndGame(this::showPauseDialog), true
        );
    }

    private void completeLevel() {
        if (state != GameState.PLAYING) return;
        stopCountdown();
        timeRemainingMs = 0L;
        state = GameState.LEVEL_COMPLETE;
        audioManager.setMusicActive(false);
        audioManager.playLevelComplete();
        pauseButton.setEnabled(false);
        settingsButton.setEnabled(false);
        updateHeader();
        showLevelCompleteDialog();
    }

    private void showLevelCompleteDialog() {
        boolean lastLevel = levelIndex == levels.size() - 1;
        int nextSize = lastLevel ? 0 : levels.get(levelIndex + 1).gridSize;

        showStyledDialog(
                "✓", "LEVEL COMPLETE", "Level " + (levelIndex + 1) + " cleared!",
                lastLevel ? "You finished all " + levels.size() + " levels!"
                        : "Great job! Ready for the next color challenge?",
                Color.parseColor("#15803D"), Color.parseColor("#DCFCE7"),
                new String[]{"LEVEL", "TOTAL SCORE", lastLevel ? "STATUS" : "NEXT GRID"},
                new String[]{(levelIndex + 1) + "/" + levels.size(),
                        String.valueOf(totalScore), lastLevel ? "Done" : nextSize + " x " + nextSize},
                lastLevel ? "Finish game" : "Next level",
                () -> {
                    if (lastLevel) {
                        // Level 8 was completed naturally: no exit confirmation needed.
                        endGame();
                    } else {
                        startLevel(levelIndex + 1);
                    }
                },
                lastLevel ? null : "End game",
                lastLevel ? null : () -> confirmEndGame(this::showLevelCompleteDialog),
                true
        );
    }

    private void confirmEndGame(Runnable returnToPreviousDialog) {
        showStyledDialog(
                "!", "CONFIRM ACTION", "End your game?",
                "Your current score will be finalized. This cannot be undone.",
                Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"),
                new String[]{"CURRENT LEVEL", "TOTAL SCORE"},
                new String[]{String.valueOf(levelIndex + 1), String.valueOf(totalScore)},
                "Keep playing", returnToPreviousDialog,
                "Yes, end game", this::endGame, true
        );
    }

    private void endGame() {
        stopCountdown();
        state = GameState.GAME_OVER;
        audioManager.setMusicActive(false);
        pauseButton.setEnabled(false);
        settingsButton.setEnabled(false);

        showStyledDialog(
                "★", "RUN COMPLETE", "Game over",
                "Nice effort! You reached Level " + (levelIndex + 1) + ".",
                Color.parseColor("#7C3AED"), Color.parseColor("#EDE9FE"),
                new String[]{"LEVEL REACHED", "FINAL SCORE"},
                new String[]{(levelIndex + 1) + "/" + levels.size(),
                        String.valueOf(totalScore)},
                "View results", this::showPostGameResults,
                "Play again", this::startNewGame, false,
                "Main menu", this::returnToHome
        );
    }

    // Leaderboard flow: qualified -> name entry -> saved -> Top 25 table.
    // Unqualified scores still lead to the Top 25 table, without a name prompt.
    private void showPostGameResults() {
        try {
            if (scoreDatabase.qualifies(totalScore)) {
                showNameEntryDialog();
            } else {
                showLeaderboardDialog();
            }
        } catch (SQLiteException error) {
            Toast.makeText(this, "Unable to read scores. Please try again.",
                    Toast.LENGTH_LONG).show();
            showLeaderboardErrorDialog();
        }
    }

    private void showNameEntryDialog() {
        if (isFinishing() || isDestroyed()) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(24), dp(22), dp(22));
        card.setBackground(roundedBackground(Color.WHITE, 0, 24));

        TextView title = makeText("🏆  TOP 25 SCORE!", 21,
                Color.parseColor("#0F172A"), true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, fullWidth());

        TextView subtitle = makeText("Your score of " + totalScore
                        + " qualifies! Enter a name to save it on this device.",
                14, Color.parseColor("#64748B"), false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = fullWidth();
        subtitleParams.topMargin = dp(10);
        card.addView(subtitle, subtitleParams);

        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint("Your name");
        nameInput.setTextSize(16);
        nameInput.setTextColor(Color.parseColor("#0F172A"));
        nameInput.setHintTextColor(Color.parseColor("#94A3B8"));
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        nameInput.setBackground(roundedBackground(Color.parseColor("#F8FAFC"),
                Color.parseColor("#CBD5E1"), 12));
        nameInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams inputParams = fullWidth();
        inputParams.topMargin = dp(22);
        card.addView(nameInput, inputParams);

        TextView helper = makeText("Up to 20 characters · Names cannot be blank",
                12, Color.parseColor("#64748B"), false);
        LinearLayout.LayoutParams helperParams = fullWidth();
        helperParams.topMargin = dp(8);
        card.addView(helper, helperParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card).setCancelable(false).create();

        TextView save = makeDialogButton("Save score", true, false);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        saveParams.topMargin = dp(20);
        card.addView(save, saveParams);
        save.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Please enter your name");
                nameInput.requestFocus();
                return;
            }
            try {
                if (!scoreDatabase.insertIfQualified(name, totalScore)) {
                    Toast.makeText(this, "Score did not qualify for Top 25.",
                            Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
                showLeaderboardDialog();
            } catch (SQLiteException error) {
                nameInput.setError("Could not save score. Please try again.");
            }
        });

        TextView skip = makeDialogButton("Skip saving", false, false);
        LinearLayout.LayoutParams skipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        skipParams.topMargin = dp(9);
        card.addView(skip, skipParams);
        skip.setOnClickListener(v -> {
            dialog.dismiss();
            showLeaderboardDialog();
        });
        displayDialog(dialog);
    }

    private void showLeaderboardDialog() {
        if (isFinishing() || isDestroyed()) return;
        final List<ScoreDatabaseHelper.ScoreEntry> entries;
        try {
            entries = scoreDatabase.getTop25();
        } catch (SQLiteException error) {
            showLeaderboardErrorDialog();
            return;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(20), dp(18), dp(20));
        card.setBackground(roundedBackground(Color.WHITE, 0, 24));

        TextView title = makeText("🏆  TOP 25 SCORES", 21,
                Color.parseColor("#0F172A"), true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, fullWidth());

        TextView subtitle = makeText("Highest scores saved on this phone",
                12, Color.parseColor("#64748B"), false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = fullWidth();
        subParams.topMargin = dp(6);
        card.addView(subtitle, subParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.min(dp(390), (int) (getResources().getDisplayMetrics().heightPixels * 0.46f)));
        scrollParams.topMargin = dp(15);
        card.addView(scroll, scrollParams);

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (entries.isEmpty()) {
            TextView empty = makeText("No scores yet. Play a round to set a record!",
                    14, Color.parseColor("#64748B"), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(30), dp(18), dp(30));
            rows.addView(empty, fullWidth());
        } else {
            for (int i = 0; i < entries.size(); i++) {
                ScoreDatabaseHelper.ScoreEntry score = entries.get(i);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(11), dp(12), dp(11), dp(12));
                row.setBackground(roundedBackground(
                        i % 2 == 0 ? Color.parseColor("#F1F5F9") : Color.WHITE,
                        0, 10));

                TextView rank = makeText(String.format(Locale.US, "%02d", i + 1),
                        13, Color.parseColor("#2563EB"), true);
                row.addView(rank, new LinearLayout.LayoutParams(dp(35),
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView name = makeText(score.name, 14,
                        Color.parseColor("#0F172A"), false);
                name.setSingleLine(true);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                row.addView(name, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView points = makeText(String.valueOf(score.score), 16,
                        Color.parseColor("#0F172A"), true);
                points.setGravity(Gravity.END);
                row.addView(points, new LinearLayout.LayoutParams(dp(65),
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                LinearLayout.LayoutParams rowParams = fullWidth();
                rowParams.bottomMargin = dp(4);
                rows.addView(row, rowParams);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card).setCancelable(false).create();

        TextView playAgain = makeDialogButton("Play again", true, false);
        LinearLayout.LayoutParams playAgainParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        playAgainParams.topMargin = dp(18);
        card.addView(playAgain, playAgainParams);
        playAgain.setOnClickListener(v -> {
            dialog.dismiss();
            startNewGame();
        });

        TextView close = makeDialogButton("Main menu", false, false);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        closeParams.topMargin = dp(8);
        card.addView(close, closeParams);
        close.setOnClickListener(v -> {
            dialog.dismiss();
            returnToHome();
        });
        displayDialog(dialog);
    }

    // Return to the existing HomeActivity instead of closing the app.
    private void returnToHome() {
        stopIntroCountdown();
        stopCountdown();
        audioManager.setMusicActive(false);
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showLeaderboardErrorDialog() {
        showStyledDialog("!", "SCORE ERROR", "Couldn't load scores",
                "Your game score hasn't been saved. You can retry opening the leaderboard.",
                Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"),
                new String[]{"CURRENT SCORE"}, new String[]{String.valueOf(totalScore)},
                "Try again", this::showPostGameResults,
                "Play again", this::startNewGame, false);
    }

    // SETTINGS: choices are saved now, but music/sound playback will be
    // connected to these booleans in a future enhancement.
    private void showSettingsDialog() {
        if (state != GameState.PLAYING || !countdownStarted) return;

        timeRemainingMs = Math.max(0L, deadlineMs - SystemClock.elapsedRealtime());
        stopCountdown();
        if (timeRemainingMs == 0L) {
            completeLevel();
            return;
        }
        state = GameState.PAUSED;
        audioManager.setMusicActive(false);
        pauseButton.setEnabled(false);
        settingsButton.setEnabled(false);
        updateHeader();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(24), dp(22), dp(22));
        card.setBackground(roundedBackground(Color.WHITE, 0, 24));

        TextView heading = makeText("GAME SETTINGS", 23, Color.parseColor("#0F172A"), true);
        card.addView(heading, fullWidth());

        TextView description = makeText(
                "Adjust the music and sound effects while your game is paused.",
                13, Color.parseColor("#64748B"), false);
        description.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams descriptionParams = fullWidth();
        descriptionParams.topMargin = dp(10);
        card.addView(description, descriptionParams);

        addSettingsSwitch(card, "Music", "Play looping background music", PREF_MUSIC);
        addSettingsSwitch(card, "Sound effects", "Play tap and level-complete sounds", PREF_SFX);

        TextView note = makeText("Your game is paused while settings are open.",
                12, Color.parseColor("#64748B"), false);
        LinearLayout.LayoutParams noteParams = fullWidth();
        noteParams.topMargin = dp(20);
        card.addView(note, noteParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card)
                .setCancelable(false)
                .create();
        dialog.setCanceledOnTouchOutside(false);

        TextView close = makeDialogButton("Save & resume", true, false);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        closeParams.topMargin = dp(18);
        card.addView(close, closeParams);
        close.setOnClickListener(v -> {
            dialog.dismiss();
            resumeLevel();
        });

        displayDialog(dialog);
    }

    private void addSettingsSwitch(LinearLayout card,
                                   String title, String subtitle, String preferenceKey) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lineParams = fullWidth();
        lineParams.topMargin = dp(22);
        card.addView(line, lineParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        line.addView(labels, labelsParams);
        labels.addView(makeText(title, 16, Color.parseColor("#0F172A"), true));
        TextView description = makeText(subtitle, 11, Color.parseColor("#64748B"), false);
        LinearLayout.LayoutParams subtitleParams = fullWidth();
        subtitleParams.topMargin = dp(3);
        labels.addView(description, subtitleParams);

        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setContentDescription(title);
        toggle.setChecked(preferences.getBoolean(preferenceKey, true));
        toggle.setOnCheckedChangeListener((button, value) -> {
            preferences.edit().putBoolean(preferenceKey, value).apply();
            audioManager.refreshPreferences();
        });
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        switchParams.leftMargin = dp(10);
        line.addView(toggle, switchParams);
    }

    private void styleHeaderButton(ImageButton button) {
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#1E2563EB")),
                roundedBackground(Color.WHITE, Color.parseColor("#E2E8F0"), 15), null));
        button.setImageTintList(ColorStateList.valueOf(Color.parseColor("#334155")));
        button.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(11), dp(11), dp(11), dp(11));
    }

    private void updateTimerProgress() {
        if (timerTrack == null || timerFill == null) return;
        int width = timerTrack.getWidth();
        if (width <= 0) return;
        long duration = levels.get(levelIndex).durationMs;
        float fraction = duration == 0 ? 0f
                : Math.max(0f, Math.min(1f, (float) timeRemainingMs / duration));
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) timerFill.getLayoutParams();
        int newWidth = Math.round(width * fraction);
        if (params.width != newWidth) {
            params.width = newWidth;
            timerFill.setLayoutParams(params);
        }
        int progressColor = fraction > 0.50f ? Color.parseColor("#2563EB")
                : fraction > 0.20f ? Color.parseColor("#F59E0B")
                : Color.parseColor("#EF4444");
        timerFillDrawable.setColor(progressColor);
    }

    // ----------------------------------------------------------------------
    // Reusable dialog UI. All views are built in Java; no extra XML is needed.
    // ----------------------------------------------------------------------

    private AlertDialog activeDialog;

    private void showStyledDialog(
            String symbol, String eyebrow, String title, String description,
            int accentColor, int iconBackgroundColor,
            String[] metricLabels, String[] metricValues,
            String primaryLabel, Runnable primaryAction,
            String secondaryLabel, Runnable secondaryAction,
            boolean secondaryDanger
    ) {
        showStyledDialog(symbol, eyebrow, title, description,
                accentColor, iconBackgroundColor, metricLabels, metricValues,
                primaryLabel, primaryAction, secondaryLabel, secondaryAction,
                secondaryDanger, null, null);
    }

    // Optional third action is used for Main menu on the Game Over dialog.
    private void showStyledDialog(
            String symbol, String eyebrow, String title, String description,
            int accentColor, int iconBackgroundColor,
            String[] metricLabels, String[] metricValues,
            String primaryLabel, Runnable primaryAction,
            String secondaryLabel, Runnable secondaryAction,
            boolean secondaryDanger,
            String tertiaryLabel, Runnable tertiaryAction
    ) {
        if (isFinishing() || isDestroyed()) return;

        // Close any previous popup to avoid stacking dialogs.
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(22), dp(24), dp(22), dp(22));
        card.setBackground(roundedBackground(Color.WHITE, 0, 24));

        TextView symbolView = makeText(symbol, 27, accentColor, true);
        symbolView.setGravity(Gravity.CENTER);
        symbolView.setBackground(roundedBackground(iconBackgroundColor, 0, 50));
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        symbolParams.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(symbolView, symbolParams);

        TextView eyebrowView = makeText(eyebrow, 11, accentColor, true);
        eyebrowView.setLetterSpacing(0.12f);
        eyebrowView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams eyebrowParams = fullWidth();
        eyebrowParams.topMargin = dp(16);
        card.addView(eyebrowView, eyebrowParams);

        TextView titleView = makeText(title, 23, Color.parseColor("#0F172A"), true);
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = fullWidth();
        titleParams.topMargin = dp(5);
        card.addView(titleView, titleParams);

        TextView subtitleView = makeText(description, 14, Color.parseColor("#64748B"), false);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams subtitleParams = fullWidth();
        subtitleParams.topMargin = dp(9);
        card.addView(subtitleView, subtitleParams);

        if (metricLabels.length != metricValues.length) {
            throw new IllegalArgumentException("Metric labels and values must match");
        }
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsRowParams = fullWidth();
        statsRowParams.topMargin = dp(20);
        card.addView(statsRow, statsRowParams);

        for (int i = 0; i < metricLabels.length; i++) {
            LinearLayout statCard = new LinearLayout(this);
            statCard.setOrientation(LinearLayout.VERTICAL);
            statCard.setGravity(Gravity.CENTER);
            statCard.setPadding(dp(4), dp(13), dp(4), dp(13));
            statCard.setBackground(roundedBackground(Color.parseColor("#F1F5F9"), 0, 13));

            TextView statValue = makeText(metricValues[i], 18, Color.parseColor("#0F172A"), true);
            statValue.setGravity(Gravity.CENTER);
            statCard.addView(statValue, fullWidth());

            TextView statLabel = makeText(metricLabels[i], 9, Color.parseColor("#64748B"), true);
            statLabel.setGravity(Gravity.CENTER);
            statLabel.setLetterSpacing(0.04f);
            LinearLayout.LayoutParams labelParams = fullWidth();
            labelParams.topMargin = dp(4);
            statCard.addView(statLabel, labelParams);

            LinearLayout.LayoutParams statParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            statParams.setMargins(dp(3), 0, dp(3), 0);
            statsRow.addView(statCard, statParams);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(card)
                .setCancelable(false)
                .create();
        dialog.setCanceledOnTouchOutside(false);

        TextView primaryButton = makeDialogButton(primaryLabel, true, false);
        LinearLayout.LayoutParams primaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        primaryParams.topMargin = dp(22);
        card.addView(primaryButton, primaryParams);
        primaryButton.setOnClickListener(view -> {
            dialog.dismiss();
            primaryAction.run();
        });

        if (secondaryLabel != null && secondaryAction != null) {
            TextView secondaryButton = makeDialogButton(secondaryLabel, false, secondaryDanger);
            LinearLayout.LayoutParams secondaryParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            secondaryParams.topMargin = dp(9);
            card.addView(secondaryButton, secondaryParams);
            secondaryButton.setOnClickListener(view -> {
                dialog.dismiss();
                secondaryAction.run();
            });
        }

        if (tertiaryLabel != null && tertiaryAction != null) {
            TextView tertiaryButton = makeDialogButton(tertiaryLabel, false, false);
            LinearLayout.LayoutParams tertiaryParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            tertiaryParams.topMargin = dp(9);
            card.addView(tertiaryButton, tertiaryParams);
            tertiaryButton.setOnClickListener(view -> {
                dialog.dismiss();
                tertiaryAction.run();
            });
        }

        displayDialog(dialog);
    }

    private void displayDialog(AlertDialog dialog) {
        if (activeDialog != null && activeDialog.isShowing()) activeDialog.dismiss();
        activeDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (activeDialog == dialog) activeDialog = null;
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = getResources().getDisplayMetrics().widthPixels - dp(36);
            window.setLayout(Math.min(width, dp(420)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private TextView makeText(String text, float sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView makeDialogButton(String label, boolean primary, boolean danger) {
        TextView button = makeText(label, 15,
                primary ? Color.WHITE : danger ? Color.parseColor("#DC2626")
                        : Color.parseColor("#334155"), true);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setContentDescription(label);

        int fill = primary ? Color.parseColor("#2563EB") : Color.WHITE;
        int stroke = primary ? 0 : danger ? Color.parseColor("#FECACA")
                : Color.parseColor("#CBD5E1");
        GradientDrawable background = roundedBackground(fill, stroke, 14);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#220F172A")), background, null));
        return button;
    }

    private GradientDrawable roundedBackground(int fillColor, int strokeColor, int cornerDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(cornerDp));
        if (strokeColor != 0) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void updateHeader() {
        levelText.setText("Level " + (levelIndex + 1) + "/" + levels.size());
        scoreText.setText(String.valueOf(totalScore));
        timerText.setText(formatSeconds(timeRemainingMs));
        updateTimerProgress();
    }

    private String formatSeconds(long ms) {
        return String.format(Locale.US, "%.1fs", ms / 1000.0);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onPause() {
        audioManager.onPause();
        if (state == GameState.INTRO) {
            stopIntroCountdown();
            introRestartOnResume = true;
        }
        if (state == GameState.PLAYING && countdownStarted) {
            timeRemainingMs = Math.max(0L, deadlineMs - SystemClock.elapsedRealtime());
            stopCountdown();
            state = GameState.PAUSED;
            audioManager.setMusicActive(false);
            pauseButton.setEnabled(false);
            settingsButton.setEnabled(false);
            pausedByBackground = true;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        audioManager.onResume();
        if (introRestartOnResume && state == GameState.INTRO) {
            introRestartOnResume = false;
            boardContainer.post(this::startIntroCountdown);
        }
        if (pausedByBackground) {
            pausedByBackground = false;
            gameGrid.post(() -> {
                if (state != GameState.PAUSED) return;
                if (timeRemainingMs == 0L) {
                    state = GameState.PLAYING;
                    completeLevel();
                } else {
                    updateHeader();
                    showPauseDialog();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        stopIntroCountdown();
        stopCountdown();
        if (activeDialog != null) activeDialog.dismiss();
        if (scoreDatabase != null) scoreDatabase.close();
        if (audioManager != null) audioManager.release();
        super.onDestroy();
    }
}
