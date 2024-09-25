package com.haliconfr.async;

import android.animation.Animator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class Splash extends AppCompatActivity {
    TextView transitionView;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        transitionView = findViewById(R.id.setTransition);
        transitionView.setAlpha(0);
        SharedPreferences sp = getSharedPreferences("settings",0);
        MainVariables.enabled = sp.getString("sounds", "thunder ");
        MainVariables.timer = sp.getInt("timer", 0);
        if(sp.getBoolean("init", true)){
            SharedPreferences.Editor editor = getSharedPreferences("settings",0).edit();
            editor.putBoolean("init", false);
            editor.apply();
            ImageView walkthrough3 = findViewById(R.id.walkthrough3);
            ImageView walkthrough2 = findViewById(R.id.walkthrough2);
            ImageView walkthrough1 = findViewById(R.id.walkthrough1);
            walkthrough(walkthrough1);
            walkthrough1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    walkthrough(walkthrough2);
                }
            });
            walkthrough2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    walkthrough(walkthrough3);
                }
            });
            walkthrough3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    transition(MainActivity.class);
                }
            });
        }else{
            transition(MainActivity.class);
        }
    }
    void transition(Class destination){
        transitionView.animate().alpha(1.0f).setDuration(500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                Intent intent = new Intent(Splash.this, destination);
                intent.putExtra("init", true);
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
    void walkthrough(ImageView image){
        transitionView.animate().alpha(1.0f).setDuration(500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                transitionView.animate().alpha(0.0f).setDuration(500);
                image.setVisibility(View.VISIBLE);
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