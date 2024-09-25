package com.haliconfr.async;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class randFreeSfx extends Service {

    private static final String TAG = "SoundEffectService";
    private Map<String, MediaPlayer> playerMap = new HashMap<>();
    private Map<String, ScheduledFuture<?>> soundEffectFutures = new HashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private Random random = new Random();
    private Handler handler = new Handler();
    private Runnable volumeUpdater;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        volumeUpdater = new Runnable() {
            @Override
            public void run() {
                updateVolumes();
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(volumeUpdater);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            String effectName = intent.getStringExtra("EFFECT_NAME");
            if (effectName != null) {
                switch (action) {
                    case "PLAY_SOUND_EFFECT":
                        playSoundEffect(effectName);
                        break;
                    case "STOP_SOUND_EFFECT":
                        stopSoundEffect(effectName);
                        break;
                }
            }
        }
        return START_STICKY;
    }

    private void playSoundEffect(String effectName) {
        Log.d(TAG, "Playing sound effect: " + effectName);
        if (soundEffectFutures.containsKey(effectName)) {
            Log.d(TAG, "Sound effect already playing: " + effectName);
            return;
        }

        // Play the sound effect immediately
        playNextVariant(effectName);
    }

    private void playNextVariant(String effectName) {
        int variantIndex = random.nextInt(6) + 1; // Assuming 6 variants
        String uriString = getEffectUri(effectName, variantIndex);
        Uri uri = Uri.parse(uriString);

        if (uriString == null || uriString.isEmpty()) {
            Log.e(TAG, "URI is invalid for effect: " + effectName + " variant: " + variantIndex);
            return;
        }

        Log.d(TAG, "URI for " + effectName + " variant " + variantIndex + ": " + uriString);

        MediaPlayer player = new MediaPlayer();
        playerMap.put(effectName, player);

        try {
            player.setDataSource(getApplicationContext(), uri);
            player.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared for " + effectName + " variant " + variantIndex);
                updatePlayerVolume(player, effectName); // Set initial volume
                mp.start();
            });
            player.setOnCompletionListener(mp -> {
                Log.d(TAG, "Playback ended for effect: " + effectName + " variant: " + variantIndex);
                mp.release();
                playerMap.remove(effectName);

                // Schedule the next play with a random delay
                int delay = random.nextInt(11) + 5; // 5 to 15 seconds
                Runnable nextPlayTask = new Runnable() {
                    @Override
                    public void run() {
                        playNextVariant(effectName);
                    }
                };
                ScheduledFuture<?> future = scheduler.schedule(nextPlayTask, delay, TimeUnit.SECONDS);
                soundEffectFutures.put(effectName, future);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what + " - " + extra);
                mp.release();
                playerMap.remove(effectName);

                // Schedule the next play even on error
                int delay = random.nextInt(11) + 5; // 5 to 15 seconds
                Runnable nextPlayTask = new Runnable() {
                    @Override
                    public void run() {
                        playNextVariant(effectName);
                    }
                };
                ScheduledFuture<?> future = scheduler.schedule(nextPlayTask, delay, TimeUnit.SECONDS);
                soundEffectFutures.put(effectName, future);
                return true;
            });
            player.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "IOException preparing MediaPlayer: " + e.getMessage());
            player.release();
            playerMap.remove(effectName);
        }
    }

    private void updateVolumes() {
        for (Map.Entry<String, MediaPlayer> entry : playerMap.entrySet()) {
            String effectName = entry.getKey();
            MediaPlayer player = entry.getValue();
            updatePlayerVolume(player, effectName);
        }
    }

    private void updatePlayerVolume(MediaPlayer player, String effectName) {
        float volume = MainVariables.volumeLevels.getOrDefault(effectName, 1.0f);
        player.setVolume(volume, volume);
    }

    private void stopSoundEffect(String effectName) {
        Log.d(TAG, "Stopping sound effect: " + effectName);
        ScheduledFuture<?> future = soundEffectFutures.remove(effectName);
        if (future != null) {
            future.cancel(true);
        }

        MediaPlayer player = playerMap.remove(effectName);
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    private String getEffectUri(String effectName, int variantIndex) {
        String resourceName = effectName + "_" + variantIndex;
        int resId = getResources().getIdentifier(resourceName, "raw", getPackageName());
        if (resId == 0) {
            Log.e(TAG, "Resource not found: " + resourceName);
            return "";
        }
        return "android.resource://" + getPackageName() + "/" + resId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (MediaPlayer player : playerMap.values()) {
            player.stop();
            player.release();
        }
        scheduler.shutdown();
        handler.removeCallbacks(volumeUpdater);
        Log.d(TAG, "Service destroyed");
    }
}