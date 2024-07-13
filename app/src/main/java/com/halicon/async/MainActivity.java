package com.halicon.async;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.slider.Slider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    Spinner spinner;
    Button window;
    Button[] sounds = new Button[6];
    String[] names;
    ImageView windowSheet;
    String selected, previousItem;
    ImageView rain1, settings;
    TextView transitionView;
    private Map<Integer, String> soundEffectIds = new HashMap<>();
    boolean windowSelected, premium;
    int previousItemIndex = 0;
    int spinnerSelection;
    LinearLayout sliderMenu;
    SeekBar volSlider;
    boolean menuVisible = false;
    ViewGroup rootLayout;
    View currentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        soundEffectIds.put(1, "thunder");
        soundEffectIds.put(2, "traffic");
        setContentView(R.layout.activity_main);
        volSlider = findViewById(R.id.volSlider);
        volSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (currentButton != null) {
                    String soundEffect = (String) currentButton.getTag();
                    float volume = progress / 100.0f;
                    MainVariables.volumeLevels.put(soundEffect, volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        rootLayout = findViewById(R.id.root_layout);
        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (menuVisible) {
                    hideMenu();
                }
                return false;
            }
        });
        sliderMenu = findViewById(R.id.slider_menu);
        settings = findViewById(R.id.settings);
        if(MainVariables.timer > 0){
            timer();
        }
        if(getIntent().getBooleanExtra("init", false)){
            settings.setAlpha(0.0f);
            settings.animate().alpha(1.0f).setDuration(500);
        }
        selected = " ";
        transitionView = findViewById(R.id.transition2);
        transitionView.setVisibility(View.VISIBLE);
        transitionView.setAlpha(1);
        rain1 = findViewById(R.id.rain1);
        spinner = findViewById(R.id.spinner);
        window = findViewById(R.id.window);
        windowSheet = findViewById(R.id.windowSheet);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.setClickable(false);
                transition(Settings.class);
            }
        });
        sounds[0] = findViewById(R.id.sound1);
        sounds[1] = findViewById(R.id.sound2);
        sounds[2] = findViewById(R.id.sound3);
        sounds[3] = findViewById(R.id.sound4);
        sounds[4] = findViewById(R.id.sound5);
        sounds[5] = findViewById(R.id.sound6);
        initMenu();
        window.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableWindow();
            }
        });
        SharedPreferences sp = getSharedPreferences("settings",0);
        premium = sp.getBoolean("premium", false);
        String[] array_spinner;
        if(premium){
            array_spinner = new String[3];
            array_spinner[2] = "Off";
        }else{
            array_spinner = new String[2];
        }
        array_spinner[0] = "Heavy";
        array_spinner[1] = "Soft";
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, R.layout.list_item, R.id.itemText, array_spinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                previousItemIndex = i;
                previousItem = selected;
                selected = spinner.getSelectedItem().toString();
                SharedPreferences.Editor editor = getSharedPreferences("settings",0).edit();
                editor.putInt("spinnerSelection", i);
                spinnerSelection = i;
                editor.apply();
                animateRain();
                Intent sndIntent = new Intent(MainActivity.this, soundService.class);
                sndIntent.setAction("CHANGE_SOUND");
                sndIntent.putExtra("TRACK", trackName(spinnerSelection));
                sndIntent.putExtra("WINDOW_EFFECT", windowSelected);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(sndIntent);
                }else{
                    startService(sndIntent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        int selection = sp.getInt("spinnerSelection", 1);
        spinner.setSelection(selection);
        transitionView.animate().alpha(0.0f).setDuration(500);
    }

    void startSFX(Button button, String sound) {
        boolean enableSound;
        if(menuVisible){
            hideMenu();
        }
        Drawable pressed = getResources().getDrawable(getResources().getIdentifier(sound + "_pressed", "drawable", getPackageName()));
        Drawable notPressed = getResources().getDrawable(getResources().getIdentifier(sound, "drawable", getPackageName()));
        if(!sound.contains("prem")){
            //relates to randomly played sounds
            if (MainVariables.thundBooleans.get(sound)) {
                //if the sound is active, turn it off
                enableSound = false;
                button.setForeground(notPressed);
                MainVariables.thundBooleans.put(sound, false);
            } else {
                //if the sound is inactive, turn it on
                enableSound = true;
                button.setForeground(pressed);
                MainVariables.thundBooleans.put(sound, true);
            }
            Intent sndIntent = new Intent(MainActivity.this, randFreeSfx.class);
            if (enableSound) {
                sndIntent.setAction("PLAY_SOUND_EFFECT");
            } else {
                sndIntent.setAction("STOP_SOUND_EFFECT");
            }
            sndIntent.putExtra("EFFECT_NAME", sound);
            startService(sndIntent);
        } else {
            //relates to premium sounds
            if (MainVariables.sfxBooleans.get(sound)) {
                //if the sound is active, turn it off
                enableSound = false;
                button.setForeground(notPressed);
                MainVariables.sfxBooleans.put(sound, false);
            } else {
                //if the sound is inactive, turn it on
                enableSound = true;
                button.setForeground(pressed);
                MainVariables.sfxBooleans.put(sound, true);
            }
            Intent sndIntent = new Intent(MainActivity.this, loopingPremSfx.class);
            sndIntent.putExtra("EFFECT_NAME", sound);
            startService(sndIntent);
        }
    }
    void animateRain() {
        if (selected.equalsIgnoreCase("heavy")) {
            Glide.with(MainActivity.this)
                    .load(R.drawable.heavy_rain)
                    .transition(DrawableTransitionOptions.withCrossFade(4000))
                    .apply(new RequestOptions().override(1080, 1920)
                            .error(R.drawable.icon).centerCrop()
                    )
                    .into(rain1);
        } else if(selected.equalsIgnoreCase("soft")){
            Glide.with(MainActivity.this)
                    .load(R.drawable.light_rain)
                    .transition(DrawableTransitionOptions.withCrossFade(4000))
                    .apply(new RequestOptions().override(1080, 1920)
                            .error(R.drawable.icon).centerCrop()
                    )
                    .into(rain1);
        } else if(selected.equalsIgnoreCase("off")){
            Glide.with(MainActivity.this)
                    .load(R.drawable.empty)
                    .transition(DrawableTransitionOptions.withCrossFade(4000))
                    .apply(new RequestOptions().override(1080, 1920)
                            .error(R.drawable.icon).centerCrop()
                    )
                    .into(rain1);
        }
    }

    void enableWindow() {
        if (!windowSelected) {
            window.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.window_pressed, null));
            windowSelected = true;
            Glide.with(MainActivity.this)
                    .load(R.drawable.window)
                    .transition(withCrossFade(500))
                    .apply(new RequestOptions().override(1080, 1920)
                            .error(R.drawable.icon).centerCrop()
                    )
                    .into(windowSheet);
        } else {
            window.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.windowbutton, null));
            windowSelected = false;
            Glide.with(MainActivity.this)
                    .load(R.drawable.empty)
                    .transition(withCrossFade(500))
                    .apply(new RequestOptions().override(1080, 1920)
                            .error(R.drawable.icon).centerCrop()
                    )
                    .into(windowSheet);
        }
        Intent sndIntent = new Intent(MainActivity.this, soundService.class);
        sndIntent.setAction("SET_WINDOW_EFFECT");
        sndIntent.putExtra("ENABLE", windowSelected);
        startService(sndIntent);
    }
    String trackName(int spinSel){
        switch(spinSel){
            case 0:
                return "heavy";
            case 1:
                return "soft";
            case 2:
                return "silence";
        }
        return null;
    }
    public void timer() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(MainVariables.timer * 60000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                finishAffinity();
                System.exit(0);
            }
        };
        thread.start();
    }
    void initMenu() {
        names = MainVariables.enabled.trim().split(" ");
        if (!names[0].isEmpty()) {
            for (String name : names) {
                int i = Arrays.asList(names).indexOf(name);
                sounds[i].setVisibility(View.VISIBLE);
                sounds[i].setForeground(getResources().getDrawable(getResources().getIdentifier(name, "drawable", getPackageName())));
                sounds[i].setTag(name); // Store sound effect name as a tag

                sounds[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startSFX((Button) view, name);
                    }
                });

                sounds[i].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        currentButton = (Button) v;
                        showMenu(v);
                        return true; // Return true to indicate the event is consumed
                    }
                });

                if (name.contains("prem")) {
                    MainVariables.sfxBooleans.put(name, false);
                } else {
                    MainVariables.thundBooleans.put(name, false);
                }
            }
        }
    }
    void transition(Class destination){
        transitionView.animate().alpha(1.0f).setDuration(500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                settings.setClickable(true);
                MainVariables.window = false;
                Intent intent = new Intent(MainActivity.this, destination);
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
    private void showMenu(View anchorView) {
        if(menuVisible){
            hideMenu();
        }
        currentButton = (Button) anchorView;
        String soundEffect = (String) currentButton.getTag(); // Retrieve the sound effect name from the button's tag
        if(Boolean.TRUE.equals(MainVariables.thundBooleans.get(soundEffect)) || Boolean.TRUE.equals(MainVariables.sfxBooleans.get(soundEffect))){
            volSlider.setBackgroundResource(R.drawable.slider_pressed);
        }else{volSlider.setBackgroundResource(R.drawable.slider);}

        // Set the slider's progress to the current volume for this sound effect
        if (soundEffect != null) {
            float currentVolume = MainVariables.volumeLevels.getOrDefault(soundEffect, 1.0f);
            volSlider.setProgress((int) (currentVolume * 100));
        }

        menuVisible = true;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(anchorView, "scaleX", 1.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(anchorView, "scaleY", 1.3f);
        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());

        scaleX.start();
        scaleY.start();

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        sliderMenu.setVisibility(View.VISIBLE);
        sliderMenu.setAlpha(0f);
        sliderMenu.animate().alpha(1f).setDuration(300).start();

        int buttonHeight = anchorView.getHeight();
        int buttonWidth = anchorView.getWidth();

        sliderMenu.setTranslationX(location[0] + buttonWidth);
        sliderMenu.setTranslationY(location[1] - buttonHeight*0.2f);

        sliderMenu.setTranslationX(-sliderMenu.getWidth()*0.1f);
        sliderMenu.animate().translationX(location[0] + buttonWidth*1.2f).setDuration(300).start();
    }

    private void hideMenu() {
        menuVisible = false;
        for (Button bt : sounds) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(bt, "scaleX", 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(bt, "scaleY", 1f);
            scaleX.setDuration(300);
            scaleY.setDuration(300);
            scaleX.setInterpolator(new DecelerateInterpolator());
            scaleY.setInterpolator(new DecelerateInterpolator());

            scaleX.start();
            scaleY.start();
        }

        sliderMenu.animate().alpha(0f).translationX(-sliderMenu.getWidth()*0.1f).setDuration(300).withEndAction(new Runnable() {
            @Override
            public void run() {
                sliderMenu.setVisibility(View.GONE);
            }
        }).start();
    }
}