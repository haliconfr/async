package com.halicon.async;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Objects;

public class loopingPremSfx extends Service {
    private static final String TAG = "loopingPremSfx";
    private MediaPlayer mediaPlayer1;
    private MediaPlayer mediaPlayer2;
    private boolean isMediaPlayer1Playing = true;
    private Handler handler = new Handler();
    private String currentAudioPath;
    private String currentSoundEffectName;
    private boolean isPlaying = false;
    private static final int FADE_DURATION = 1000; // 1 second fade duration
    private static final int FADE_INTERVAL = 50;   // Fade interval in milliseconds
    private Runnable volumeUpdater;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        mediaPlayer1 = new MediaPlayer();
        mediaPlayer2 = new MediaPlayer();
        mediaPlayer1.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
        volumeUpdater = new Runnable() {
            @Override
            public void run() {
                updateVolume();
                handler.postDelayed(this, 1000); // Update every second
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String effectName = intent.getStringExtra("EFFECT_NAME");
        String audioPath = sfxPath(effectName);
        Log.d(TAG, "onStartCommand: audioPath=" + audioPath);

        if (audioPath != null) {
            if (audioPath.equals(currentAudioPath) && isPlaying) {
                stopAudio();
            } else {
                currentSoundEffectName = effectName; // Store the current sound effect name
                playAudio(audioPath);
                currentAudioPath = audioPath;
            }
        }

        handler.post(volumeUpdater); // Start volume updater
        return START_STICKY;
    }

    private void playAudio(String audioPath) {
        resetPlayers();

        Uri uri = Uri.parse(audioPath);
        try {
            mediaPlayer1.setDataSource(this, uri);
            mediaPlayer2.setDataSource(this, uri);

            mediaPlayer1.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer1 prepared");
                startPlayback(mediaPlayer1);
            });
            mediaPlayer2.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer2 prepared");
                setupNextPlayer(mediaPlayer2);
            });

            mediaPlayer1.prepareAsync();
            mediaPlayer2.prepareAsync();

            isPlaying = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error setting data source: " + e.getMessage());
        }
    }

    private void stopAudio() {
        Log.d(TAG, "Stopping audio");
        handler.removeCallbacksAndMessages(null);
        if (isMediaPlayer1Playing) {
            fadeOut(mediaPlayer1);
        } else {
            fadeOut(mediaPlayer2);
        }
        isPlaying = false;
    }

    private void resetPlayers() {
        Log.d(TAG, "Resetting players");
        if (mediaPlayer1.isPlaying()) mediaPlayer1.stop();
        if (mediaPlayer2.isPlaying()) mediaPlayer2.stop();
        mediaPlayer1.reset();
        mediaPlayer2.reset();
        handler.removeCallbacksAndMessages(null);
    }

    private void startPlayback(MediaPlayer mediaPlayer) {
        Log.d(TAG, "Starting playback");
        mediaPlayer.start();
        fadeIn(mediaPlayer);
        scheduleFade(mediaPlayer);
    }

    private void setupNextPlayer(MediaPlayer mediaPlayer) {
        mediaPlayer.setOnCompletionListener(mp -> startNextPlayback());
    }

    private void startNextPlayback() {
        Log.d(TAG, "Starting next playback");
        if (isPlaying) {
            if (isMediaPlayer1Playing) {
                mediaPlayer2.seekTo(0);
                startPlayback(mediaPlayer2);
            } else {
                mediaPlayer1.seekTo(0);
                startPlayback(mediaPlayer1);
            }
            isMediaPlayer1Playing = !isMediaPlayer1Playing;
        }
    }

    private void scheduleFade(MediaPlayer mediaPlayer) {
        int duration = mediaPlayer.getDuration();
        Log.d(TAG, "Scheduling fade with duration: " + duration);
        handler.postDelayed(this::startNextPlayback, duration - FADE_DURATION - 500); // Start fading out 500ms before FADE_DURATION
    }

    private void fadeOut(final MediaPlayer mediaPlayer) {
        final int fadeOutStep = FADE_INTERVAL;
        final float initialVolume = getCurrentVolume(); // Get the current volume
        final float stepVolume = initialVolume / (FADE_DURATION / fadeOutStep);
        final Handler fadeHandler = new Handler();

        fadeHandler.post(new Runnable() {
            float volume = initialVolume; // Start from the current volume

            @Override
            public void run() {
                if (volume > 0.0f) {
                    volume -= stepVolume;
                    mediaPlayer.setVolume(volume, volume);
                    fadeHandler.postDelayed(this, fadeOutStep);
                } else {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    Log.d(TAG, "MediaPlayer faded out and stopped");
                }
            }
        });
    }

    private void fadeIn(final MediaPlayer mediaPlayer) {
        final int fadeInStep = FADE_INTERVAL;
        final float targetVolume = MainVariables.volumeLevels.getOrDefault(currentSoundEffectName, 1.0f);
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

    String sfxPath(String effectName) {
        int resId = getResources().getIdentifier(effectName, "raw", getPackageName());
        if (resId == 0) {
            Log.e(TAG, "Resource not found: " + effectName);
            return "";
        }
        return "android.resource://" + getPackageName() + "/" + resId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (mediaPlayer1 != null) {
            mediaPlayer1.release();
        }
        if (mediaPlayer2 != null) {
            mediaPlayer2.release();
        }
        handler.removeCallbacksAndMessages(null);
    }

    private void updateVolume() {
        if (currentSoundEffectName != null) {
            Float volume = MainVariables.volumeLevels.getOrDefault(currentSoundEffectName, 1.0f);
            Log.d(TAG, "Updating volume: " + volume);
            if (isMediaPlayer1Playing) {
                mediaPlayer1.setVolume(volume, volume);
            } else {
                mediaPlayer2.setVolume(volume, volume);
            }
        }
    }

    private float getCurrentVolume() {
        Float volume = MainVariables.volumeLevels.getOrDefault(currentSoundEffectName, 1.0f);
        return volume != null ? volume : 1.0f;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}