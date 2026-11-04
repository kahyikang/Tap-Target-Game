package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        PLAYING, PAUSED, LEVEL_COMPLETE, GAME_OVER
    }

    private TextView levelText;
    private TextView scoreText;
    private TextView timerText;
    private GridLayout gameGrid;
    private Button pauseButton;

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
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);
        gameGrid = findViewById(R.id.gameGrid);
        pauseButton = findViewById(R.id.pauseButton);

        pauseButton.setOnClickListener(v -> pauseLevel());
        startNewGame();
    }

    private void startNewGame() {
        totalScore = 0;
        startLevel(0);
    }

    private void startLevel(int newIndex) {
        stopCountdown();
        levelIndex = newIndex;
        LevelConfig config = levels.get(levelIndex);
        timeRemainingMs = config.durationMs;
        currentColorPair = config.colorPairs[random.nextInt(config.colorPairs.length)];
        highlightedIndex = -1;
        state = GameState.PLAYING;
        pauseButton.setEnabled(true);
        gameGrid.removeAllViews();
        tiles.clear();
        updateHeader();

        // Wait for GridLayout to be measured before calculating square tile sizes.
        gameGrid.post(() -> {
            if (state != GameState.PLAYING || levelIndex != newIndex) return;
            createGrid(levels.get(levelIndex).gridSize);
            highlightRandomTile();
            startCountdown();
        });
    }

    private void createGrid(int size) {
        tiles.clear();
        gameGrid.removeAllViews();
        gameGrid.setRowCount(size);
        gameGrid.setColumnCount(size);

        // Use smaller gaps for larger boards so 6x6 through 9x9 fit the screen.
        int margin = size >= 6 ? dp(1) : dp(2);
        int boardWidth = gameGrid.getWidth();

        // Avoid a fixed minimum tile size, which could make Level 8 overflow.
        int tileSize = Math.max(1, (boardWidth - size * margin * 2) / size);

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
        if (index != highlightedIndex) return;

        totalScore++;
        updateHeader();
        highlightRandomTile(); // Repeating the same target is allowed.
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
        deadlineMs = SystemClock.elapsedRealtime() + timeRemainingMs;
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
        pauseButton.setEnabled(false);
        updateHeader();
        showPauseDialog();
    }

    private void resumeLevel() {
        if (state != GameState.PAUSED) return;
        state = GameState.PLAYING;
        pauseButton.setEnabled(true);
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
        pauseButton.setEnabled(false);
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
                        confirmEndGame(this::showLevelCompleteDialog);
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
        pauseButton.setEnabled(false);

        // NEXT PHASE: check Top 25 qualification, save the name and score,
        // and open the leaderboard from here.
        showStyledDialog(
                "★", "RUN COMPLETE", "Game over",
                "Nice effort! You reached Level " + (levelIndex + 1) + ".",
                Color.parseColor("#7C3AED"), Color.parseColor("#EDE9FE"),
                new String[]{"LEVEL REACHED", "FINAL SCORE"},
                new String[]{(levelIndex + 1) + "/" + levels.size(),
                        String.valueOf(totalScore)},
                "Play again", this::startNewGame,
                "Close game", this::finish, false
        );
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
        scoreText.setText("Score: " + totalScore);
        timerText.setText(formatSeconds(timeRemainingMs));
    }

    private String formatSeconds(long ms) {
        return String.format(Locale.US, "%.1fs", ms / 1000.0);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onPause() {
        if (state == GameState.PLAYING && countdownStarted) {
            timeRemainingMs = Math.max(0L, deadlineMs - SystemClock.elapsedRealtime());
            stopCountdown();
            state = GameState.PAUSED;
            pauseButton.setEnabled(false);
            pausedByBackground = true;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        stopCountdown();
        if (activeDialog != null) activeDialog.dismiss();
        super.onDestroy();
    }
}
