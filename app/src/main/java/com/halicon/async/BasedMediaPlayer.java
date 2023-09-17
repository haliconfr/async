package com.halicon.async;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public class BasedMediaPlayer {

    private static final String TAG = BasedMediaPlayer.class.getName();
    private Context mContext = null;
    private Uri mUri = null;

    private MediaPlayer mCurrentPlayer = null;
    private MediaPlayer mNextPlayer = null;

    public static BasedMediaPlayer create(Context context, Uri uri) {
        return new BasedMediaPlayer(context, uri);
    }
    private void createNextMediaPlayerRaw() {
        mNextPlayer = new MediaPlayer();
        setAudioAttributes(mNextPlayer);
        mNextPlayer.setVolume(1.0f, 1.0f);
        try {
            mNextPlayer.setDataSource(mContext, mUri);
            mNextPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mNextPlayer.seekTo(0);
                    mCurrentPlayer.setNextMediaPlayer(mNextPlayer);
                    mCurrentPlayer.setOnCompletionListener(onCompletionListener);
                }
            });
            mNextPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    BasedMediaPlayer(Context context, Uri uri) {
        mContext = context;
        if(mUri == null){
            mUri = uri;
        }
        mCurrentPlayer = new MediaPlayer();
        setAudioAttributes(mCurrentPlayer);
        mCurrentPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mCurrentPlayer.start();
                createNextMediaPlayerRaw();
            }
        });
    }


    private final MediaPlayer.OnCompletionListener onCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mCurrentPlayer = mNextPlayer;
                    createNextMediaPlayerRaw();
                    mediaPlayer.release();
                    Log.d("yeah", "looped");
                }
            };


    public boolean isPlaying() throws IllegalStateException {
        if (mCurrentPlayer != null) {
            return mCurrentPlayer.isPlaying();
        } else {
            return false;
        }
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if (mCurrentPlayer != null) {
            mCurrentPlayer.setVolume(leftVolume, rightVolume);
        } else {
            Log.d(TAG, "setVolume()");
        }

    }

    public void stop() throws IllegalStateException {
        if (mCurrentPlayer != null && mCurrentPlayer.isPlaying()) {
            Log.d(TAG, "stop()");
            mCurrentPlayer.stop();
        }

    }

    public void release() {
        Log.d(TAG, "release()");
        if (mCurrentPlayer != null)
            mCurrentPlayer.release();
        if (mNextPlayer != null)
            mNextPlayer.release();
    }

    public void reset() {
        if (mCurrentPlayer != null) {
            Log.d(TAG, "reset()");
            mCurrentPlayer.reset();
        } else {
            Log.d(TAG, "reset() | " +
                    "mCurrentPlayer is NULL");
        }

    }
    public void prepare() throws IOException {
        mCurrentPlayer.prepare();
    }
    public void setDataSource(Uri source) throws IOException {
        mUri = source;
        mCurrentPlayer.setDataSource(mContext, source);
    }
    public void setAudioAttributes(MediaPlayer mp){
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
    }
    public void seekTo(int time){
        mCurrentPlayer.seekTo(time);
    }
}