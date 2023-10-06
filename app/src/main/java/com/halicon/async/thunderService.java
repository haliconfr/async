package com.halicon.async;

import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import linc.com.library.AudioTool;
import linc.com.library.callback.OnFileComplete;

public class thunderService extends Service {
    Random rand = new Random();
    Boolean first = true;
    String sound;
    MediaPlayer mp;
    File moddedTempFile, uneditTempFile;
    String resprefix = "android.resource://com.halicon.async/raw/";
    Thread thread;
    int duration = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkLoop();
        return Service.START_STICKY;
    }

    void startAudio() throws IOException {
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true){
                        if(sound != null){
                            duration = 0;
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
                                }
                            });
                            sleep(duration + 500);
                            try{
                                if(mp!=null&&!mp.isPlaying()){
                                    mp.stop();
                                    mp.release();
                                }
                            }catch(IllegalStateException ignored){
                            }
                            if (MainVariables.window) {
                                if (moddedTempFile != null && moddedTempFile.exists()) {
                                    moddedTempFile.delete();
                                }
                                if (uneditTempFile != null && uneditTempFile.exists()) {
                                    uneditTempFile.delete();
                                }
                            }
                            int random = rand.nextInt(80000 - 1000) + 1000;
                            sleep(random);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
    }
    Uri getFile() throws IOException{
        int random = rand.nextInt(7 - 1) + 1;
        if(MainVariables.window && !Objects.equals(sound, "silence")){
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
            if(!Objects.equals(sound, "silence")){
                return Uri.parse(resprefix + sound+"_"+random);
            }else{
                return Uri.parse(resprefix + sound);
            }
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
        Thread loop = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        sleep(1000);
                        sound = getKeyForTrueValue(MainVariables.thundBooleans);
                        if(!Objects.equals(sound, "silence") && first){
                            startAudio();
                            first = false;
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        loop.start();
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
