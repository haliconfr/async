package com.halicon.async;

import android.animation.Animator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class Splash extends AppCompatActivity {
    TextView transitionView;
    MediaPlayer woosh;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        woosh = new MediaPlayer();
        TextView begin = findViewById(R.id.startButton);
        transitionView = findViewById(R.id.transition);
        transitionView.setAlpha(0);
        begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    woosh.setDataSource(getApplicationContext(), Uri.parse( "android.resource://com.halicon.async/" + R.raw.woosh));
                    woosh.prepare();
                    woosh.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                transition();
            }
        });
    }
    void transition(){
        transitionView.animate().alpha(1.0f).setDuration(1500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                Intent intent = new Intent(Splash.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animator) {

            }
        });
    }
}