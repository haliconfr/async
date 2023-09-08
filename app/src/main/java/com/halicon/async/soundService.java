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

import java.io.IOException;
import java.util.Random;

public class soundService extends Service {
    int currentMP;
    float mpVolume, mp2Volume;
    MediaPlayer mp, mp2;
    String path;
    public boolean ready;
    boolean first;

    @Override
    public void onCreate() {
        super.onCreate();
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            path = intent.getExtras().getString("path");
        }
        if (!first) {
            try {
                startAudio(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            first = true;
        } else {
            if(ready) {
                switch (currentMP) {
                    case 1:
                        try {
                            startAudio(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case 2:
                        try {
                            startNewAudio(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                }
            }
        }
        return Service.START_STICKY;
    }
    void startAudio(String path) throws IOException {
        enableButtons(false);
        ready = false;
        mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        mp.setVolume(0.0f,0.0f);
        mp.setLooping(true);
        mpVolume = 0.0f;
        try {
            mp.setDataSource(soundService.this, Uri.parse(path));
            mp.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(!ready) {
                        sleep(50);
                        if(mpVolume < 1){
                            mpVolume+=0.01f;
                            Log.d("balls", "MP2 volume = " + String.valueOf(mp2Volume));
                            if(mp2Volume > 0){
                                mp2Volume-=0.01f;
                            }
                            mp.setVolume(mpVolume, mpVolume);
                            if(mp2 != null){
                                mp2.setVolume(mp2Volume, mp2Volume);
                            }
                        }else{
                            Log.d("balls", "stop second");
                            if(mp2 != null){
                                mp2.stop();
                                mp2.release();
                            }
                            currentMP = 2;
                            ready = true;
                            enableButtons(true);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
    void startNewAudio(String path) throws IOException {
        enableButtons(false);
        ready = false;
        mp2 = new MediaPlayer();
        mp2.setAudioAttributes(new AudioAttributes.Builder()
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        mp2.setVolume(0.0f,0.0f);
        mp2.setLooping(true);
        mp2Volume = 0.0f;
        try {
            mp2.setDataSource(soundService.this, Uri.parse(path));
            mp2.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(!ready) {
                        sleep(50);
                        if(mp2Volume < 1){
                            mpVolume-=0.01f;
                            mp2Volume+=0.01f;
                            Log.d("balls", "MP1 volume = " + String.valueOf(mpVolume));
                            Log.d("balls", "MP2 volume = " + String.valueOf(mp2Volume));
                            mp.setVolume(mpVolume, mpVolume);
                            mp2.setVolume(mp2Volume, mp2Volume);
                        }else{
                            Log.d("balls", "stop main");
                            mp.stop();
                            mp.release();
                            currentMP = 1;
                            ready = true;
                            enableButtons(true);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
    void enableButtons (boolean enabled){
        MainActivity.buttonsEnabled = enabled;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    }