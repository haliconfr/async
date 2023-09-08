package com.halicon.async;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class thunderService extends Service {
    Random rand = new Random();
    MediaPlayer mp;
    public boolean ready, first;
    String resprefix = "android.resource://com.halicon.async/raw/";
    boolean enabled;
    Thread thread;

    @Override
    public void onCreate() {
        super.onCreate();
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        enabled = intent.getExtras().getBoolean("enabled");
        if(enabled){
            ready = true;
        }
        try {
            startAudio();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Service.START_STICKY;
    }
    void startAudio() throws IOException {
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(ready) {
                        if(enabled){
                            mp = new MediaPlayer();
                            mp.setLooping(false);
                            mp.setVolume(1.0f,1.0f);
                            mp.setAudioAttributes(new AudioAttributes.Builder()
                                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build());
                            int random = rand.nextInt(7 - 1) + 1;
                            switch(random){
                                case 1:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + "thunder_1"));
                                    break;
                                case 2:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + "thunder_2"));
                                    break;
                                case 3:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + "thunder_3"));
                                    break;
                                case 4:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + "thunder_4"));
                                    break;
                                case 5:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + "thunder_5"));
                                    break;
                                case 6:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + "thunder_6"));
                                    break;
                            }
                            Log.d("balls", "thunder sound effect " + random + " started");
                            mp.prepare();
                            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mediaPlayer) {
                                    mp.start();
                                }
                            });
                            sleep(rand.nextInt(70000 - 10000) + 10000);
                            mp.stop();
                        }else{
                            mp.stop();
                            ready = false;
                            Log.d("balls", "thread ended!");
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    }