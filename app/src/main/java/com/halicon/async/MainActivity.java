package com.halicon.async;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    Spinner spinner;
    Button window, thunder;
    boolean enabled;
    ImageView windowSheet;
    public static Boolean buttonsEnabled = true;
    String mode, name, selected, previousItem;
    ImageView rain1;
    boolean windowSelected;
    int previousItemIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buttonsEnabled = true;
        setContentView(R.layout.activity_main);
        selected = " ";
        TextView transitionView = findViewById(R.id.transition2);
        transitionView.setAlpha(1);
        transitionView.animate().alpha(0.0f).setDuration(2000);
        rain1 = findViewById(R.id.rain1);
        spinner = findViewById(R.id.spinner);
        window = findViewById(R.id.window);
        windowSheet = findViewById(R.id.windowSheet);
        thunder = findViewById(R.id.thunder);
        window.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonsEnabled) {
                    enableWindow();
                }
            }
        });
        thunder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startThunder();
            }
        });
        String[] array_spinner = new String[2];
        array_spinner[0] = "Heavy";
        array_spinner[1] = "Soft";
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, R.layout.list_item, R.id.itemText, array_spinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!buttonsEnabled) {
                    spinner.setSelection(previousItemIndex);
                    return;
                }
                previousItemIndex = i;
                previousItem = selected;
                buttonsEnabled = false;
                selected = spinner.getSelectedItem().toString();
                animateRain();
                startAudio();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    void startAudio() {
        Intent intent = new Intent(this, soundService.class);
        intent.putExtra("path", getPath());
        startService(intent);
    }

    void startThunder() {
        Intent thunderIntent = new Intent(this, thunderService.class);
        if (enabled) {
            thunder.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.thunder, null));
            thunderIntent.putExtra("enabled", false);
            enabled = false;
        } else {
            thunder.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.thunder_pressed, null));
            thunderIntent.putExtra("enabled", true);
            enabled = true;
        }
        startService(thunderIntent);
    }

    String getPath() {
        name = selected.toLowerCase();
        if (windowSelected) {
            mode = "window";
        } else {
            mode = "base";
        }
        String path;
        if (!Objects.equals(previousItem, name + mode)) {
            path = "android.resource://com.halicon.async/raw/" + name + "_" + mode;
        } else {
            path = "android.resource://com.halicon.async/raw/silence";
        }
        return path;
    }

    void animateRain() {
        if (selected.toLowerCase().equals("heavy")) {
            Glide.with(MainActivity.this)
                    .load(R.drawable.heavy_rain)
                    .transition(DrawableTransitionOptions.withCrossFade(4000))
                    .apply(new RequestOptions().override(1080, 1920)
                            .error(R.drawable.icon).centerCrop()
                    )
                    .into(rain1);
        } else {
            Glide.with(MainActivity.this)
                    .load(R.drawable.light_rain)
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
            startAudio();
            Glide.with(MainActivity.this)
                    .load(R.drawable.window)
                    .transition(DrawableTransitionOptions.withCrossFade(1000))
                    .apply(new RequestOptions().override(1080, 1920)
                            .error(R.drawable.icon).centerCrop()
                    )
                    .into(windowSheet);
        } else {
            window.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.windowbutton, null));
            windowSelected = false;
            startAudio();
            Glide.with(MainActivity.this)
                    .load(R.drawable.empty)
                    .transition(DrawableTransitionOptions.withCrossFade(1000))
                    .apply(new RequestOptions().override(1080, 1920)
                            .error(R.drawable.icon).centerCrop()
                    )
                    .into(windowSheet);
        }
    }

    public void timer() {
        if (MainVariables.timer != 0) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(MainVariables.timer);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    finishAffinity();
                    System.exit(0);
                }

                ;
            };
            thread.start();
        }
    }
}