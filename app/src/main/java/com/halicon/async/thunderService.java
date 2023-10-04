package com.halicon.async;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.FileUtils;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Random;

import linc.com.library.AudioTool;
import linc.com.library.callback.OnFileComplete;

public class thunderService extends Service {
    Random rand = new Random();
    String sound;
    MediaPlayer mp;
    File moddedTempFile, uneditTempFile;
    String resprefix = "android.resource://com.halicon.async/raw/";
    boolean ready, checkLoopRunning;
    Thread thread, soundEffectLooper, stopMain;
    int duration = 0, waitInt = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!checkLoopRunning){
            checkLoop();
        }
        sound = intent.getExtras().getString("sound");
        try {
            startAudio();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        ready = false;
        super.onDestroy();
    }

    void startAudio() throws IOException {
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    mp = new MediaPlayer();
                    mp.setLooping(false);
                    mp.setVolume(1.0f,1.0f);
                    Uri path = getFile();
                    mp.setDataSource(thunderService.this, path);
                    MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                    metaRetriever.setDataSource(thunderService.this, path);
                    duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    mp.prepare();
                    mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mp.start();
                            Log.d("yeah", "sound effect started!");
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
        soundEffectLooper = new Thread(){
            @Override
            public void run(){
                try {
                    while(duration == 0){
                        sleep(100);
                    }
                    sleep(duration + 100);
                    if(!ready){
                        soundEffectLooper.interrupt();
                    }
                    mp.stop();
                    mp.release();
                    if (MainVariables.window) {
                        if (moddedTempFile != null && moddedTempFile.exists()) {
                            moddedTempFile.delete();
                        }
                        if (uneditTempFile != null && uneditTempFile.exists()) {
                            uneditTempFile.delete();
                        }
                    }
                    int random = rand.nextInt(600 - 100) + 100;
                    waitInt = 0;
                    while (waitInt < random && ready) {
                        sleep(100);
                        if(!ready){
                            soundEffectLooper.interrupt();
                        }
                        waitInt++;
                        Log.d("yeah", String.valueOf(waitInt));
                    }
                    if(ready){
                        thread.run();
                        soundEffectLooper.run();
                    }
                }catch(InterruptedException e){
                    throw new RuntimeException(e);
                }
            }
        };
        soundEffectLooper.start();
    }
    Uri getFile() throws IOException{
        int random = rand.nextInt(7 - 1) + 1;
        if(MainVariables.window){
            File outputDir = getApplicationContext().getCacheDir();
            InputStream ins = getResources().openRawResource(
                    getResources().getIdentifier(sound+"_"+random,
                            "raw", getPackageName()));
            uneditTempFile = File.createTempFile("temp", ".mp3", outputDir);
            moddedTempFile = File.createTempFile("filtered", ".mp3", outputDir);
            copyInputStreamToFile(ins, uneditTempFile);
            try {
                AudioTool.getInstance(this)
                        .withAudio(uneditTempFile)
                        .filterAudio(1, 1000, new OnFileComplete() {
                            @Override
                            public void onComplete(File output) {

                            }
                        })
                        .saveCurrentTo(moddedTempFile.getPath())
                        .release();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return Uri.parse(moddedTempFile.getPath());
        }else{
            return Uri.parse(resprefix + sound+"_"+random);
        }
    }
    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[8192];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }
    void checkLoop(){

        checkLoopRunning = true;
        Thread loop = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        sleep(1000);
                        if(!MainVariables.sfxBooleans.get(sound) || MainVariables.disableAllSounds){
                            ready = false;
                            stopSelf();
                        }else{
                            ready = true;
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        loop.start();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    }