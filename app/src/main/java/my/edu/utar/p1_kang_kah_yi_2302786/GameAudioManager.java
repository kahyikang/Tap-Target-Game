package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

/**
 * Two separate looping soundtracks: one for menus/results and one for gameplay.
 * Sound effects (including all button clicks) follow the existing SFX switch.
 * Audio is bundled locally in res/raw, with no internet or storage permission.
 */
public final class GameAudioManager {

    public enum MusicMode { MENU, GAME }

    private static final String TAG = "GameAudioManager";
    private static final String PREFS_NAME = "tap_target_settings";
    private static final String PREF_MUSIC = "music_enabled";
    private static final String PREF_SFX = "sfx_enabled";

    private final SharedPreferences preferences;
    private MediaPlayer menuMusic;
    private MediaPlayer gameMusic;
    private SoundPool soundPool;
    private int correctSoundId;
    private int wrongSoundId;
    private int levelSoundId;
    private int countdownBeepId;
    private int countdownGoId;
    private int congratulationsId;
    private int buttonClickId;

    private MusicMode musicMode = MusicMode.MENU;
    private boolean foreground = false;
    private boolean musicActive = false;

    public GameAudioManager(Context context) {
        Context appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        try {
            menuMusic = MediaPlayer.create(appContext, R.raw.bg_loop);
            if (menuMusic != null) {
                menuMusic.setLooping(true);
                menuMusic.setVolume(0.5f, 0.50f);
            }
            gameMusic = MediaPlayer.create(appContext, R.raw.game_loop);
            if (gameMusic != null) {
                gameMusic.setLooping(true);
                // Keep the competitive track below the tap sound effects.
                gameMusic.setVolume(0.5f, 0.5f);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not initialize music", e);
        }

        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attributes)
                    .setMaxStreams(8)
                    .build();
            correctSoundId = soundPool.load(appContext, R.raw.tap_correct, 1);
            wrongSoundId = soundPool.load(appContext, R.raw.tap_wrong, 1);
            levelSoundId = soundPool.load(appContext, R.raw.level_complete, 1);
            countdownBeepId = soundPool.load(appContext, R.raw.countdown_beep, 1);
            countdownGoId = soundPool.load(appContext, R.raw.countdown_go, 1);
            congratulationsId = soundPool.load(appContext, R.raw.congratulations, 1);
            buttonClickId = soundPool.load(appContext, R.raw.button_click, 1);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not initialize sound effects", e);
            if (soundPool != null) soundPool.release();
            soundPool = null;
        }
    }

    public void onResume() {
        foreground = true;
        updateMusic();
    }

    public void onPause() {
        foreground = false;
        updateMusic();
    }

    /** Set the desired soundtrack; never allows both tracks to play together. */
    public void setMusicMode(MusicMode mode) {
        musicMode = mode;
        updateMusic();
    }

    /** Pauses music during the 3-2-1 intro, pauses, and between levels. */
    public void setMusicActive(boolean active) {
        musicActive = active;
        updateMusic();
    }

    /** Respond immediately when the user changes Music in either Settings screen. */
    public void refreshPreferences() {
        updateMusic();
    }

    private void updateMusic() {
        boolean enabled = foreground && musicActive &&
                preferences.getBoolean(PREF_MUSIC, true);
        changePlayback(menuMusic, enabled && musicMode == MusicMode.MENU);
        changePlayback(gameMusic, enabled && musicMode == MusicMode.GAME);
    }

    private void changePlayback(MediaPlayer player, boolean shouldPlay) {
        if (player == null) return;
        try {
            if (shouldPlay && !player.isPlaying()) {
                player.start();
            } else if (!shouldPlay && player.isPlaying()) {
                player.pause();
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "Could not change music playback", e);
        }
    }

    // Independent sound effects: available even when background music is muted.
    public void playCorrectTap()      { playSound(correctSoundId, 0.5f); }
    public void playWrongTap()        { playSound(wrongSoundId, 0.5f); }
    public void playLevelComplete()   { playSound(levelSoundId, 0.5f); }
    public void playCountdownBeep()   { playSound(countdownBeepId, 0.5f); }
    public void playCountdownGo()     { playSound(countdownGoId, 0.5f); }
    public void playCongratulations() { playSound(congratulationsId, 0.50f); }
    public void playButtonClick()     { playSound(buttonClickId, 0.50f); }

    private void playSound(int id, float volume) {
        if (!foreground || soundPool == null || id == 0 ||
                !preferences.getBoolean(PREF_SFX, true)) return;
        soundPool.play(id, volume, volume, 1, 0, 1f);
    }

    public void release() {
        foreground = false;
        musicActive = false;
        if (menuMusic != null) {
            menuMusic.release();
            menuMusic = null;
        }
        if (gameMusic != null) {
            gameMusic.release();
            gameMusic = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
