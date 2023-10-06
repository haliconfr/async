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
import java.util.Map;
import java.util.Objects;

import linc.com.library.AudioTool;
import linc.com.library.callback.OnFileComplete;

public class sfxService extends Service {
    int currentMP, duration;
    float maximumVolume = 0.5f;
    float mpVolume, mp2Volume;
    MediaPlayer mp, mp2;
    Boolean soundAlreadyWindowed = false;
    String soundName;
    public boolean ready, loop, first = true, first2 = false, stoplooping;
    String resprefix = "android.resource://com.halicon.async/raw/";
    boolean checkLoopRunning;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        checkLoop();
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
        ready = false;
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(sfxService.this, Uri.parse(resprefix + soundName));
        duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        mp = new MediaPlayer();
        mp.setVolume(0.0f,0.0f);
        mpVolume = 0.0f;
        mp.setDataSource(sfxService.this, Uri.parse(resprefix + soundName));
        mp.prepare();
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                loop();
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
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(sfxService.this, Uri.parse(resprefix + soundName));
        duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        mp2 = new MediaPlayer();
        mp2.setVolume(0.0f,0.0f);
        mp2Volume = 0.0f;
        mp2.setDataSource(sfxService.this, Uri.parse(resprefix + soundName));
        mp2.prepare();
        mp2.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                loop();
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
        stoplooping = false;
        loop = true;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (loop) {
                        int sleepfor = 0;
                        int target = duration - 5000;
                        while(sleepfor < target){
                            sleep(100);
                            sleepfor = sleepfor + 100;
                            if(stoplooping && ready){
                                sleepfor = target;
                            }
                            Log.d("yeah", String.valueOf(sleepfor));
                            Log.d("yeah", String.valueOf(target));
                        }
                        switchMPs();
                        loop = false;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
    }
    void checkLoop(){
        checkLoopRunning = true;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (checkLoopRunning) {
                        String path = getKeyForTrueValue(MainVariables.sfxBooleans);
                        if(soundName == null || !soundName.equals(path)){
                            soundName = path;
                            if(first2){
                                startAudio();
                                first2 = false;
                            }
                            if(first){
                                first = false;
                                first2 = true;
                            }else{
                                stoplooping = true;
                            }
                        }
                        if(MainVariables.window){
                            if(!soundAlreadyWindowed){
                                maximumVolume = maximumVolume - 0.2f;
                                soundAlreadyWindowed = true;
                                stoplooping = true;
                            }
                        }else{
                            if(soundAlreadyWindowed){
                                maximumVolume = 0.5f;
                                soundAlreadyWindowed = false;
                                stoplooping = true;
                            }
                        }
                        sleep(1500);
                        if(!loop){
                            loop();
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
    }
    public static String getKeyForTrueValue(Map<String, Boolean> map) {
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (entry.getValue()) {
                return entry.getKey();
            }
        }
        return "silence";
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}