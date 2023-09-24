package com.halicon.async;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;

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
        path = intent.getExtras().getString("path");
        if (!first) {
            try {
                startAudio(path);
                checkLoop();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            first = true;
        }
        return Service.START_STICKY;
    }
    void switchMPs(){
        if(ready) {
            loop();
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
    void startAudio(String path) throws IOException {
        ready = false;
        mp = new MediaPlayer();
        mp.setVolume(0.0f,0.0f);
        mpVolume = 0.0f;
        try {
            mp.setDataSource(soundService.this, Uri.parse(path));
            mp.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mp.start();
            }
        });
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(!ready) {
                        sleep(29);
                        if(mpVolume < 1){
                            mpVolume+=0.01f;
                            Log.d("yeah", "MP1 volume = " + String.valueOf(mpVolume));
                            Log.d("yeah", "MP2 volume = " + String.valueOf(mp2Volume));
                            if(mp2Volume > 0){
                                mp2Volume-=0.01f;
                            }
                            mp.setVolume(mpVolume, mpVolume);
                            if(mp2 != null){
                                mp2.setVolume(mp2Volume, mp2Volume);
                            }
                        }else{
                            Log.d("yeah", "stop second");
                            if(mp2 != null){
                                mp2.stop();
                                mp2.release();
                            }
                            currentMP = 2;
                            ready = true;
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
        ready = false;
        mp2 = new MediaPlayer();
        mp2.setVolume(0.0f,0.0f);
        mp2Volume = 0.0f;
        try {
            mp2.setDataSource(soundService.this, Uri.parse(path));
            mp2.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mp2.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mp2.start();
            }
        });
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(!ready) {
                        sleep(29);
                        if(mp2Volume < 1){
                            mpVolume-=0.01f;
                            mp2Volume+=0.01f;
                            Log.d("yeah", "MP1 volume = " + String.valueOf(mpVolume));
                            Log.d("yeah", "MP2 volume = " + String.valueOf(mp2Volume));
                            mp.setVolume(mpVolume, mpVolume);
                            mp2.setVolume(mp2Volume, mp2Volume);
                        }else{
                            Log.d("yeah", "stop main");
                            mp.stop();
                            mp.release();
                            currentMP = 1;
                            ready = true;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
    void checkLoop(){
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String oldPath = path;
                        sleep(5000);
                        if (path != oldPath) {
                            switchMPs();
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
    }
    void loop(){
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        sleep(150000);
                        switchMPs();
                    }
                } catch (InterruptedException e) {
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