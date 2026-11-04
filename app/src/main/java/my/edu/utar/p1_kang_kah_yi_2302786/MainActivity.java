package my.edu.utar.p1_kang_kah_yi_2302786;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Each level has one normal tile color and one target tile color.
    // The colors become more similar as levels get harder.
    private static class LevelConfig {
        final int gridSize;
        final long durationMs;
        final int normalColor;
        final int targetColor;

        LevelConfig(int gridSize, String normalHex, String targetHex) {
            this(gridSize, normalHex, targetHex, 5_000L);
        }

        LevelConfig(int gridSize, String normalHex, String targetHex, long durationMs) {
            this.gridSize = gridSize;
            this.durationMs = durationMs;
            this.normalColor = Color.parseColor(normalHex);
            this.targetColor = Color.parseColor(targetHex);
        }
    }

    // Format: LevelConfig(gridSize, normalTileColor, targetTileColor).
    // All levels last 5 seconds by default.
    private final List<LevelConfig> levels = Arrays.asList(
            new LevelConfig(2, "#B9DCF4", "#5299D3"), // Level 1: blue, 2 x 2
            new LevelConfig(3, "#B9E4D5", "#78C5AA"), // Level 2: mint, 3 x 3
            new LevelConfig(4, "#D8C8EB", "#BCA7D6"), // Level 3: lavender, 4 x 4
            new LevelConfig(5, "#E9D4C6", "#DCC1AF"), // Level 4: peach, 5 x 5
            new LevelConfig(6, "#B8DADA", "#A5C8C7"), // Level 5: teal, 6 x 6
            new LevelConfig(7, "#D9D6EA", "#CCC9DF"), // Level 6: lilac, 7 x 7
            new LevelConfig(8, "#DEDFCE", "#D1D3C5"), // Level 7: sage, 8 x 8
            new LevelConfig(9, "#E4DBDF", "#D9D1D6")  // Level 8: rose-gray, 9 x 9
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
        timeRemainingMs = levels.get(levelIndex).durationMs;
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
        int margin = size >= 6 ? dp(2) : dp(4);
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
        LevelConfig current = levels.get(levelIndex);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(12));
        background.setColor(active ? current.targetColor : current.normalColor);
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

    private void showPauseDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Game paused")
                .setMessage("Level " + (levelIndex + 1)
                        + " · Score: " + totalScore
                        + "\nTime left: " + formatSeconds(timeRemainingMs))
                .setCancelable(false)
                .setPositiveButton("Resume", (dialog, which) -> resumeLevel())
                .setNegativeButton("End Game", (dialog, which) ->
                        confirmEndGame(this::showPauseDialog))
                .show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Level " + (levelIndex + 1) + " complete!")
                .setMessage("Total score: " + totalScore)
                .setCancelable(false)
                .setPositiveButton(lastLevel ? "Finish Game" : "Next Level", (dialog, which) -> {
                    if (lastLevel) {
                        confirmEndGame(this::showLevelCompleteDialog);
                    } else {
                        startLevel(levelIndex + 1);
                    }
                });

        if (!lastLevel) {
            builder.setNegativeButton("End Game", (dialog, which) ->
                    confirmEndGame(this::showLevelCompleteDialog));
        }
        builder.show();
    }

    private void confirmEndGame(Runnable onCancel) {
        new AlertDialog.Builder(this)
                .setTitle("End game?")
                .setMessage("Your score is " + totalScore
                        + ". Do you want to finish the game?")
                .setCancelable(false)
                .setPositiveButton("Yes, End Game", (dialog, which) -> endGame())
                .setNegativeButton("Cancel", (dialog, which) -> onCancel.run())
                .show();
    }

    private void endGame() {
        stopCountdown();
        state = GameState.GAME_OVER;
        pauseButton.setEnabled(false);

        // NEXT PHASE: check Top 25 qualification, enter name, save score,
        // and display the persistent leaderboard.
        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("You reached Level " + (levelIndex + 1)
                        + ".\nFinal score: " + totalScore
                        + "\n\nLeaderboard comes next!")
                .setCancelable(false)
                .setPositiveButton("Play Again", (dialog, which) -> startNewGame())
                .setNegativeButton("Close", (dialog, which) -> finish())
                .show();
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
        super.onDestroy();
    }
}
