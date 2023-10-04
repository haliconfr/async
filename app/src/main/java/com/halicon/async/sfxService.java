package com.halicon.async;

import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import linc.com.library.AudioTool;
import linc.com.library.callback.OnFileComplete;

public class sfxService extends Service {
    int currentMP, duration;
    float maximumVolume = 0.5f;
    float mpVolume, mp2Volume;
    MediaPlayer mp, mp2;
    Uri path;
    Boolean soundAlreadyWindowed;
    String soundName;
    public boolean ready;
    String resprefix = "android.resource://com.halicon.async/raw/";
    boolean first, checkLoopRunning;

    @Override
    public void onCreate() {
        super.onCreate();
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!checkLoopRunning){checkLoop();}
        soundAlreadyWindowed = false;
        soundName = intent.getExtras().getString("sound");
        try {
            path = getFile();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        Log.d("sfxService", resprefix + soundName);
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(sfxService.this, Uri.parse(resprefix + intent.getExtras().getString("sound")));
        duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        if (!first) {
            try {
                startAudio();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            first = true;
        }
        return Service.START_STICKY;
    }
    void switchMPs(){
        if(ready) {
            switch (currentMP) {
                case 1:
                    try {
                        startAudio();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case 2:
                    try {
                        startNewAudio();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
        }
    }
    void startAudio() throws IOException {
        loop();
        ready = false;
        mp = new MediaPlayer();
        mp.setVolume(0.0f,0.0f);
        mpVolume = 0.0f;
        try {
            mp.setDataSource(sfxService.this, path);
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
                        if(mpVolume < maximumVolume){
                            mpVolume+=0.01f;
                            if(mp2Volume > 0){
                                mp2Volume-=0.01f;
                            }
                            mp.setVolume(mpVolume, mpVolume);
                            if(mp2 != null){
                                mp2.setVolume(mp2Volume, mp2Volume);
                            }
                        }else{
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
    void startNewAudio() throws IOException {
        ready = false;
        mp2 = new MediaPlayer();
        mp2.setVolume(0.0f,0.0f);
        mp2Volume = 0.0f;
        try {
            mp2.setDataSource(sfxService.this, path);
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
                        if(mp2Volume < maximumVolume){
                            mpVolume-=0.01f;
                            mp2Volume+=0.01f;
                            mp.setVolume(mpVolume, mpVolume);
                            mp2.setVolume(mp2Volume, mp2Volume);
                        }else{
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
    void loop(){
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        sleep(duration - duration/20);
                        while(!ready){
                            sleep(500);
                        }
                        switchMPs();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
    }
    Uri getFile() throws IOException, InterruptedException {
        return Uri.parse(resprefix + soundName);
    }
    void checkLoop(){
        checkLoopRunning = true;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (checkLoopRunning) {
                        Log.d("yeah", "checking...");
                        sleep(1000);
                        if(!MainVariables.sfxBooleans.get(soundName) || MainVariables.disableAllSounds){
                            Log.d("yeah", "stop!!!");
                            path = Uri.parse("android.resource://com.halicon.async/raw/silence");
                            switchMPs();
                            stopService(new Intent(sfxService.this, sfxService.class));
                            if(!mp.isPlaying() && !mp2.isPlaying() && mp!=null && mp2!=null){
                                checkLoopRunning = false;
                            }
                        }
                        if(MainVariables.window){
                            if(!soundAlreadyWindowed){
                                maximumVolume = maximumVolume - 0.2f;
                                soundAlreadyWindowed = true;
                                switchMPs();
                            }
                        }else{
                            if(soundAlreadyWindowed){
                                maximumVolume = 0.5f;
                                soundAlreadyWindowed = false;
                                switchMPs();
                            }
                        }
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