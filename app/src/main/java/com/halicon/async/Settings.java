package com.halicon.async;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class Settings extends AppCompatActivity {
    Spinner spinner;
    View[] icons = new View[2];
    boolean first;
    TextView transitionView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        ImageView start = findViewById(R.id.startSet);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                transition();
            }
        });
        icons[0] = findViewById(R.id.thunderSet);
        icons[1] = findViewById(R.id.trafficSet);
        for(View v : icons){
            setIcon(v);
        }
        transitionView = findViewById(R.id.setTransition);
        transitionView.setAlpha(1);
        transitionView.animate().alpha(0.0f).setDuration(1000);
        spinner = findViewById(R.id.timer);
        String[] array_spinner = new String[4];
        array_spinner[0] = "8 hours";
        array_spinner[1] = "2 hours";
        array_spinner[2] = "30 mins";
        array_spinner[3] = "off";
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, R.layout.list_item, R.id.itemText, array_spinner);
        spinner.setAdapter(adapter);
        spinner.setSelection(3);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                double timer = 0;
                switch (i){
                    case(0):
                        timer = 480;
                        break;
                    case(1):
                        timer = 120;
                        break;
                    case(2):
                        timer = 30;
                        break;
                    case(3):
                        timer = 0;
                        break;
                }
                if(!first){
                    MainVariables.timer = timer;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
    void addSound(View view){
        if(!MainVariables.enabled.contains(view.getTag().toString())){
            MainVariables.enabled = MainVariables.enabled + view.getTag().toString() + " ";
            view.setForeground(getResources().getDrawable(getResources()
                    .getIdentifier(view.getTag().toString() +  "_pressed", "drawable", getPackageName())));
        }else{
            MainVariables.enabled = MainVariables.enabled.replace(view.getTag().toString() + " ", "");
            view.setForeground(getResources().getDrawable(getResources()
                    .getIdentifier(view.getTag().toString(), "drawable", getPackageName())));
        }
    }
    void setIcon(View view){
        if(MainVariables.enabled.contains(view.getTag().toString())) {
            view.setForeground(getResources().getDrawable(getResources()
                    .getIdentifier(view.getTag().toString() +  "_pressed", "drawable", getPackageName())));
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSound(v);
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
                Intent intent = new Intent(Settings.this, MainActivity.class);
                intent.putExtra("intent", false);
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