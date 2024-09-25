package com.haliconfr.async;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class loopingPremSfx extends Service {
    private static final String TAG = "loopingPremSfx";
    private Map<String, MediaPlayer> mediaPlayerMap = new HashMap<>();
    private Handler handler = new Handler();
    private static final int FADE_DURATION = 1000; // 1 second fade duration
    private static final int FADE_INTERVAL = 50;   // Fade interval in milliseconds

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null && action.equals("UPDATE_VOLUME")) {
            String effectName = intent.getStringExtra("EFFECT_NAME");
            float newVolume = intent.getFloatExtra("VOLUME", 1.0f);
            updateVolume(effectName, newVolume);
            return START_STICKY;
        }
        String effectName = intent.getStringExtra("EFFECT_NAME");
        String audioPath = sfxPath(effectName);
        Log.d(TAG, "onStartCommand: audioPath=" + audioPath);

        if (audioPath != null) {
            MediaPlayer existingPlayer = mediaPlayerMap.get(effectName);
            if (existingPlayer != null && existingPlayer.isPlaying()) {
                fadeOut(existingPlayer, effectName);
            } else {
                playAudio(effectName, audioPath);
            }
        }

        return START_STICKY;
    }

    private void playAudio(String effectName, String audioPath) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        Uri uri = Uri.parse(audioPath);
        try {
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared");
                startPlayback(mediaPlayer, effectName);
            });
            mediaPlayer.prepareAsync();
            mediaPlayerMap.put(effectName, mediaPlayer);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error setting data source: " + e.getMessage());
        }
    }

    private void startPlayback(MediaPlayer mediaPlayer, String effectName) {
        Log.d(TAG, "Starting playback");
        mediaPlayer.start();
        fadeIn(mediaPlayer);
        mediaPlayer.setLooping(true);
    }

    private void fadeOut(final MediaPlayer mediaPlayer, final String effectName) {
        final int fadeOutStep = FADE_INTERVAL;
        final float initialVolume = getCurrentVolume(effectName);
        final float stepVolume = initialVolume / (FADE_DURATION / fadeOutStep);
        final Handler fadeHandler = new Handler();

        fadeHandler.post(new Runnable() {
            float volume = initialVolume;

            @Override
            public void run() {
                if (volume > 0.0f) {
                    volume -= stepVolume;
                    mediaPlayer.setVolume(volume, volume);
                    fadeHandler.postDelayed(this, fadeOutStep);
                } else {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayerMap.remove(effectName);
                    Log.d(TAG, "MediaPlayer faded out and stopped");
                }
            }
        });
    }

    private void fadeIn(final MediaPlayer mediaPlayer) {
        final int fadeInStep = FADE_INTERVAL;
        final float targetVolume = 1.0f;
        final float stepVolume = targetVolume / (FADE_DURATION / fadeInStep);
        final Handler fadeHandler = new Handler();

        fadeHandler.post(new Runnable() {
            float volume = 0.0f;

            @Override
            public void run() {
                if (volume < targetVolume) {
                    volume += stepVolume;
                    mediaPlayer.setVolume(volume, volume);
                    fadeHandler.postDelayed(this, fadeInStep);
                } else {
                    mediaPlayer.setVolume(targetVolume, targetVolume);
                    Log.d(TAG, "MediaPlayer faded in to target volume: " + targetVolume);
                }
            }
        });
    }

    private String sfxPath(String effectName) {
        int resId = getResources().getIdentifier(effectName, "raw", getPackageName());
        if (resId == 0) {
            Log.e(TAG, "Resource not found: " + effectName);
            return "";
        }
        return "android.resource://" + getPackageName() + "/" + resId;
    }

    private float getCurrentVolume(String effectName) {
        // Implement logic to retrieve the current volume for the given effectName
        // Assuming a default volume of 1.0f
        return 1.0f;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        for (MediaPlayer mediaPlayer : mediaPlayerMap.values()) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
        }
        mediaPlayerMap.clear();
    }
    private void updateVolume(String effectName, float volume) {
        MediaPlayer mediaPlayer = mediaPlayerMap.get(effectName);
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.setVolume(volume, volume);
            Log.d(TAG, "Volume updated for: " + effectName + " to: " + volume);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}