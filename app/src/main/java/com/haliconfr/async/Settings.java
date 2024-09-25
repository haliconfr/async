package com.haliconfr.async;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class Settings extends AppCompatActivity {
    Spinner spinner;
    View[] icons = new View[3];
    View[] moreIcons = new View[4];
    ImageView start, indicator;
    TextView transitionView, premiumLock;
    BillingClient billingClient;
    ProductDetails productDetails;
    Boolean premium;
    View firstSelectedIcon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        setContentView(R.layout.settings);

        indicator = findViewById(R.id.tick);
        indicator.setAlpha(0.0f);
        start = findViewById(R.id.startSet);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start.setClickable(false);
                SharedPreferences.Editor editor = getSharedPreferences("settings",0).edit();
                editor.putString("sounds", MainVariables.enabled);
                editor.putInt("timer", MainVariables.timer);
                editor.apply();
                transition();
            }
        });

        icons[0] = findViewById(R.id.thunderSet);
        icons[1] = findViewById(R.id.trafficSet);
        icons[2] = findViewById(R.id.birdsSet);
        for(View v : icons){
            setIcon(v);
        }

        SharedPreferences sp = getSharedPreferences("settings",0);
        premium = sp.getBoolean("premium", true);
        transitionView = findViewById(R.id.setTransition);
        transitionView.setAlpha(1);
        transitionView.animate().alpha(0.0f).setDuration(500);
        spinner = findViewById(R.id.timer);
        premiumLock = findViewById(R.id.premiumLock);
        if(premium){
            premiumLock.setVisibility(View.GONE);
            moreIcons[0] = findViewById(R.id.streamSet);
            moreIcons[1] = findViewById(R.id.cafeSet);
            moreIcons[2] = findViewById(R.id.cicadasSet);
            moreIcons[3] = findViewById(R.id.clockSet);
            for(View v : moreIcons){
                setIcon(v);
            }
        } else {
            moreIcons[0] = findViewById(R.id.clockSet);
            moreIcons[1] = findViewById(R.id.clockSet);
            moreIcons[2] = findViewById(R.id.clockSet);
            moreIcons[3] = findViewById(R.id.clockSet);
            for(View v : moreIcons){
                setIcon(v);
            }
        }

        premiumLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buyPremium();
            }
        });

        String[] array_spinner = new String[4];
        array_spinner[0] = "8 hours";
        array_spinner[1] = "2 hours";
        array_spinner[2] = "30 mins";
        array_spinner[3] = "off";
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item, R.id.itemText, array_spinner);
        spinner.setAdapter(adapter);
        switch(MainVariables.timer){
            case(0):
                spinner.setSelection(3);
                break;
            case(30):
                spinner.setSelection(2);
                break;
            case(120):
                spinner.setSelection(1);
                break;
            case(480):
                spinner.setSelection(0);
                break;
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int timer = 0;
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
                MainVariables.timer = timer;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    void addSound(View view, boolean fixing) {
        // Toggle the icon selection (select/deselect)
        if (!MainVariables.enabled.contains(view.getTag().toString())) {
            // Select the icon
            MainVariables.enabled = MainVariables.enabled + view.getTag().toString() + " ";
            view.setForeground(getResources().getDrawable(getResources()
                    .getIdentifier(view.getTag().toString() + "_pressed", "drawable", getPackageName())));
            animateTick((Button) view, true);
        } else {
            // Deselect the icon
            MainVariables.enabled = MainVariables.enabled.replace(view.getTag().toString() + " ", "");
            view.setForeground(getResources().getDrawable(getResources()
                    .getIdentifier(view.getTag().toString(), "drawable", getPackageName())));
            animateTick((Button) view, false);
            Log.d("DEBUG", MainVariables.sfxBooleans + " might include " + view.getTag().toString());
            Log.d("DEBUG", MainVariables.thundBooleans + " might include " + view.getTag().toString());
            if(Boolean.TRUE.equals(MainVariables.sfxBooleans.get(view.getTag().toString()))){
                Intent sndIntent = new Intent(Settings.this, loopingPremSfx.class);
                sndIntent.putExtra("EFFECT_NAME", view.getTag().toString());
                sndIntent.setAction("STOP_SOUND_EFFECT");
                startService(sndIntent);
                MainVariables.sfxBooleans.put(view.getTag().toString(), false);
            }
            if(Boolean.TRUE.equals(MainVariables.thundBooleans.get(view.getTag().toString()))){
                Intent sndIntent = new Intent(Settings.this, randFreeSfx.class);
                sndIntent.putExtra("EFFECT_NAME", view.getTag().toString());
                sndIntent.setAction("STOP_SOUND_EFFECT");
                startService(sndIntent);
                MainVariables.thundBooleans.put(view.getTag().toString(), false);
            }

            // If the user deselects the auto-deselected icon, reset the tracker
            if (view == firstSelectedIcon) {
                firstSelectedIcon = null;
            }
        }
        // Split the enabled icons into an array
        String selectedIcons[] = MainVariables.enabled.split(" ");
        // If more than 6 icons are selected, auto-deselect the first one selected, but only once
        if (selectedIcons.length > 6 && !fixing && firstSelectedIcon == null) {
            // Track the first selected icon and deselect it
            firstSelectedIcon = getViewFromTag(selectedIcons[0]);
            addSound(firstSelectedIcon, true);
        }
        Log.d("DEBUG", MainVariables.enabled);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    void setIcon(View view){
        if(MainVariables.enabled.contains(view.getTag().toString())){
            view.setForeground(getResources().getDrawable(getResources()
                    .getIdentifier(view.getTag().toString() +  "_pressed", "drawable", getPackageName())));
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSound(v, false);
            }
        });
    }

    void transition(){
        transitionView.animate().alpha(1.0f).setDuration(500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                start.setClickable(true);
                Intent intent = new Intent(Settings.this, MainActivity.class);
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
    void buyPremium() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Billing client setup is OK
                    Log.d("Billing", "Billing setup finished. Querying product details...");

                    QueryProductDetailsParams queryProductDetailsParams =
                            QueryProductDetailsParams.newBuilder()
                                    .setProductList(
                                            ImmutableList.of(
                                                    QueryProductDetailsParams.Product.newBuilder()
                                                            .setProductId("premium")
                                                            .setProductType(BillingClient.ProductType.INAPP)
                                                            .build()))
                                    .build();

                    billingClient.queryProductDetailsAsync(
                            queryProductDetailsParams,
                            new ProductDetailsResponseListener() {
                                @Override
                                public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList) {
                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                            && productDetailsList != null
                                            && !productDetailsList.isEmpty()) {

                                        // Successfully retrieved product details
                                        ProductDetails productDetails = productDetailsList.get(0);
                                        Log.d("Billing", "Product details retrieved: " + productDetails.getName());

                                        // Set up billing flow params
                                        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                                                ImmutableList.of(
                                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                                .setProductDetails(productDetails)
                                                                .build()
                                                );

                                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                .setProductDetailsParamsList(productDetailsParamsList)
                                                .build();

                                        // Launch billing flow
                                        billingResult = billingClient.launchBillingFlow(Settings.this, billingFlowParams);

                                        // Check if billing flow launched successfully
                                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                            Log.d("Billing", "Billing flow launched successfully.");
                                        } else {
                                            Log.e("Billing", "Failed to launch billing flow: " + billingResult.getDebugMessage());
                                            Toast.makeText(Settings.this, "Failed to launch billing flow: " + billingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        // Handle the error if product details retrieval failed
                                        Log.e("Billing", "Product details retrieval failed: " + billingResult.getDebugMessage());
                                        Toast.makeText(Settings.this, "Product details could not be retrieved: " + billingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                    );
                } else {
                    // Billing setup failed
                    Log.e("Billing", "Billing setup failed: " + billingResult.getDebugMessage());
                    Toast.makeText(Settings.this, "Billing setup failed: " + billingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Toast.makeText(Settings.this, "mf worried bout the wrong liquid grelp", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                restartClass();
                Toast.makeText(Settings.this, "Refreshing past purchase...", Toast.LENGTH_SHORT).show();
            }
        }
    };

    void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
            restartClass();
        }
    }

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {

        }
    };

    void restartClass(){
        SharedPreferences.Editor editor = getSharedPreferences("settings",0).edit();
        editor.putBoolean("premium", true);
        editor.apply();
        transitionView.setAlpha(0);
        transitionView.animate().alpha(1.0f).setDuration(500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                Intent intent = new Intent(Settings.this, Settings.class);
                startActivity(intent);
                overridePendingTransition(0,0);
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animator) {

            }
        });
    }

    void animateTick(Button button, boolean tick){
        indicator.setAlpha(1.0f);
        if(tick){
            indicator.setForeground(AppCompatResources.getDrawable(Settings.this, R.drawable.tick));
        }else{
            indicator.setForeground(AppCompatResources.getDrawable(Settings.this, R.drawable.cross));
        }
        indicator.setX(button.getX()+65);
        indicator.setY(button.getY()-90);
        indicator.animate().alpha(0.0f).setDuration(500);
        indicator.animate().x(button.getX()+65).y(button.getY()-115).setDuration(300);
    }

    View getViewFromTag(String tag){
        int id = getResources().getIdentifier(tag.replace("prem", "") + "Set", "id", getPackageName());
        return findViewById(id);
    }
}