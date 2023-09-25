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
    String sound;
    MediaPlayer mp;
    public boolean ready;
    String resprefix = "android.resource://com.halicon.async/raw/";
    boolean enabled;
    Thread thread;

    @Override
    public void onCreate() {
        super.onCreate();
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        enabled = intent.getExtras().getBoolean("enabled");
        sound = intent.getExtras().getString("sound");
        Log.d("yeah", sound + " service started");
        if(enabled){
            ready = true;
        }else{
            ready = false;
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
                            int random = rand.nextInt(7 - 1) + 1;
                            switch(random){
                                case 1:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + sound+"_1"));
                                    break;
                                case 2:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + sound+"_2"));
                                    break;
                                case 3:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + sound+"_3"));
                                    break;
                                case 4:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + sound+"_4"));
                                    break;
                                case 5:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + sound+"_5"));
                                    break;
                                case 6:
                                    mp.setDataSource(thunderService.this, Uri.parse(resprefix + sound+"_6"));
                                    break;
                            }
                            Log.d("yeah", "sound effect " + random + " started");
                            mp.prepare();
                            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mediaPlayer) {
                                    mp.start();
                                }
                            });
                            sleep(rand.nextInt(80000 - 20000) + 20000);
                            mp.stop();
                        }else{
                            mp.stop();
                            ready = false;
                            thread.interrupt();
                            Log.d("yeah", "thread ended!");
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
