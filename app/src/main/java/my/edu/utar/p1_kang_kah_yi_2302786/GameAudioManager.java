package my.edu.utar.p1_kang_kah_yi_2302786;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

/**
 * Manages original, bundled music and short gameplay sounds.
 *
 * Each Activity creates its own instance and calls onResume/onPause/release.
 * SharedPreferences keys match the existing Home and Game settings switches.
 * No storage or network permissions are needed for res/raw audio.
 */
public final class GameAudioManager {

    private static final String TAG = "GameAudioManager";
    private static final String PREFS_NAME = "tap_target_settings";
    private static final String PREF_MUSIC = "music_enabled";
    private static final String PREF_SFX = "sfx_enabled";

    private final SharedPreferences preferences;
    private MediaPlayer backgroundMusic;
    private SoundPool soundPool;
    private int correctSoundId;
    private int wrongSoundId;
    private int levelSoundId;
    private boolean foreground;
    private boolean musicActive;

    public GameAudioManager(Context context) {
        Context appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        try {
            backgroundMusic = MediaPlayer.create(appContext, R.raw.bg_loop);
            if (backgroundMusic != null) {
                backgroundMusic.setLooping(true);
                backgroundMusic.setVolume(0.24f, 0.24f);
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not initialize background music", error);
            backgroundMusic = null;
        }

        try {
            AudioAttributes soundAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(soundAttributes)
                    .setMaxStreams(4)
                    .build();
            correctSoundId = soundPool.load(appContext, R.raw.tap_correct, 1);
            wrongSoundId = soundPool.load(appContext, R.raw.tap_wrong, 1);
            levelSoundId = soundPool.load(appContext, R.raw.level_complete, 1);
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not initialize sound effects", error);
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

    /** True for the menu and active gameplay; false for pause and results. */
    public void setMusicActive(boolean active) {
        musicActive = active;
        updateMusic();
    }

    /** Apply the Music switch immediately without recreating the activity. */
    public void refreshPreferences() {
        updateMusic();
    }

    private void updateMusic() {
        if (backgroundMusic == null) return;
        boolean play = foreground && musicActive &&
                preferences.getBoolean(PREF_MUSIC, true);
        try {
            if (play && !backgroundMusic.isPlaying()) {
                backgroundMusic.start();
            } else if (!play && backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
            }
        } catch (IllegalStateException error) {
            Log.w(TAG, "Music could not change playback state", error);
        }
    }

    public void playCorrectTap() {
        playSound(correctSoundId, 0.72f);
    }

    public void playWrongTap() {
        playSound(wrongSoundId, 0.35f);
    }

    public void playLevelComplete() {
        playSound(levelSoundId, 0.55f);
    }

    private void playSound(int soundId, float volume) {
        if (!foreground || soundPool == null || soundId == 0 ||
                !preferences.getBoolean(PREF_SFX, true)) return;
        soundPool.play(soundId, volume, volume, 1, 0, 1f);
    }

    public void release() {
        foreground = false;
        musicActive = false;
        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
