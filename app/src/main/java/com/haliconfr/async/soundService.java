package com.haliconfr.async;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.io.IOException;
import java.util.Random;

public class soundService extends Service {

    private static final String TAG = "SoundService";
    private static final String CHANNEL_ID = "SoundServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private SimpleExoPlayer player;
    private SimpleExoPlayer newPlayer;
    private String currentTrack;
    private String requestedTrack;
    private boolean isWindowEffect;
    private boolean requestedWindowEffect;
    private boolean isFading = false;
    private Handler handler;
    private Runnable checkRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        player = new SimpleExoPlayer.Builder(this).build();
        handler = new Handler();

        checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (requestedTrack != null && (!requestedTrack.equals(currentTrack) || requestedWindowEffect != isWindowEffect)) {
                    changeSound(requestedTrack, requestedWindowEffect);
                }
                handler.postDelayed(this, 1000); // Check every second
            }
        };

        handler.post(checkRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "CHANGE_SOUND":
                        String newTrack = intent.getStringExtra("TRACK");
                        boolean windowEffect = intent.getBooleanExtra("WINDOW_EFFECT", false);
                        requestedTrack = newTrack;
                        requestedWindowEffect = windowEffect;
                        break;
                    case "SET_WINDOW_EFFECT":
                        boolean enableWindowEffect = intent.getBooleanExtra("ENABLE", false);
                        requestedWindowEffect = enableWindowEffect;
                        if (currentTrack != null) {
                            requestedTrack = currentTrack; // Ensure changeSound() is called to apply the effect
                        }
                        break;
                    case "SHUTDOWN":
                        stopForeground(true);
                        System.exit(0);
                        break;
                }
            }
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopButtonIntent = new Intent(this, soundService.class);
        stopButtonIntent.setAction("SHUTDOWN");
        PendingIntent stopPendingIntent = PendingIntent.getService(this,
                0, stopButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Async")
                .setContentText("Playing in background")
                .addAction(R.drawable.cross, "Stop", stopPendingIntent)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void changeSound(final String newTrack, boolean windowEffect) {
        if (isFading) return; // Prevent re-entry during fade
        isFading = true;

        if (currentTrack == null) {
            playTrack(newTrack, windowEffect);
            currentTrack = newTrack;
            isWindowEffect = windowEffect;
            isFading = false;
            return;
        }

        Uri newUri = Uri.parse(getTrackUri(newTrack, windowEffect));
        newPlayer = new SimpleExoPlayer.Builder(this).build();
        newPlayer.setRepeatMode(SimpleExoPlayer.REPEAT_MODE_ONE);
        MediaItem mediaItem = MediaItem.fromUri(newUri);
        newPlayer.setMediaItem(mediaItem);
        newPlayer.prepare();

        newPlayer.setVolume(0f);
        newPlayer.play();

        ValueAnimator fadeOut = ValueAnimator.ofFloat(1f, 0f);
        ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);

        fadeOut.setDuration(2000);
        fadeIn.setDuration(2000);

        fadeOut.addUpdateListener(animation -> player.setVolume((float) animation.getAnimatedValue()));
        fadeIn.addUpdateListener(animation -> newPlayer.setVolume((float) animation.getAnimatedValue()));

        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                player.stop();
                player.release();
                player = newPlayer;
                currentTrack = newTrack;
                isWindowEffect = windowEffect;
                isFading = false;
            }
        });

        fadeOut.start();
        fadeIn.start();
    }

    private void playTrack(String newTrack, boolean windowEffect) {
        Uri uri = Uri.parse(getTrackUri(newTrack, windowEffect));
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.setRepeatMode(SimpleExoPlayer.REPEAT_MODE_ONE);
        player.prepare();
        player.setVolume(0f);
        player.play();

        ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
        fadeIn.setDuration(2000);
        fadeIn.addUpdateListener(animation -> player.setVolume((float) animation.getAnimatedValue()));
        fadeIn.start();
    }

    private String getTrackUri(String track, boolean windowEffect) {
        if (windowEffect) {
            switch (track) {
                case "soft":
                    return "android.resource://" + getPackageName() + "/" + R.raw.soft_window;
                case "heavy":
                    return "android.resource://" + getPackageName() + "/" + R.raw.heavy_window;
                case "silence":
                    return "android.resource://" + getPackageName() + "/" + R.raw.silence;
            }
        } else {
            switch (track) {
                case "soft":
                    return "android.resource://" + getPackageName() + "/" + R.raw.soft_base;
                case "heavy":
                    return "android.resource://" + getPackageName() + "/" + R.raw.heavy_base;
                case "silence":
                    return "android.resource://" + getPackageName() + "/" + R.raw.silence;
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkRunnable);
        if (player != null) {
            player.release();
        }
        if (newPlayer != null) {
            newPlayer.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sound Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setLightColor(Color.BLUE);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task removed, stopping service");
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }
}