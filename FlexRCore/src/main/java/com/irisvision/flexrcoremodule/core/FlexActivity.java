// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.irisvision.flexrcoremodule.enums.FlexScope;
import com.irisvision.flexrcoremodule.observable.FlexReactive;
import com.irisvision.flexrcoremodule.rooting.FlexFragmentNavigator_interface;
import com.irisvision.flexrcoremodule.rooting.FragmentNavigate;
import com.irisvision.flexrcoremodule.rooting.NavigatorFlexFragment;
import com.irisvision.flexrcoremodule.structure.FlexBaseContainer;
import com.irisvision.flexrcoremodule.ui.component.FlexBottomSheet;
import com.irisvision.flexrfxmodule.AnimateActivityScope;
import com.irisvision.flexrfxmodule.FlexAnimateScopeManager;
import com.irisvision.flexrfxmodule.FlexChoreo;
import com.irisvision.flexrfxmodule.FlexChoreoScope;
import com.irisvision.flexrthememodule.FlexTheme;

public class FlexActivity extends AppCompatActivity implements ScopeIdentity,FragmentNavigate, Init_, AnimateActivityScope {

    private boolean fullScreen = false;
    private FlexBaseContainer baseContainer;
    private FlexActivityContainerWrapper wrapper = new FlexActivityContainerWrapper();


    @Override
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Supprimer la barre de titre
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        init_();
        flexCreate(savedInstanceState);
    }

    @Override
    public void init(){

    }

    @Override
    protected void onResume() {
        super.onResume();
        //The dev can chose to call stop method resumeAllFlexAnimations of their FlexAnimate class witch scope is
        //set to FlexActivityScope
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //The dev can chose to call stop method pauseAllFlexAnimations of their FlexAnimate class witch scope is
        //set to FlexActivityScope
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        FlexReactive.clean(FlexScope.FlexActivity, ""+this);
        FlexTheme.clearObsActivityScope(""+this);
        FlexAnimateScopeManager.clearFlexActivityScope(""+this);
        FlexChoreoScope.clearFlexActivityScope(""+this);
    }

    @Override
    protected void onNightModeChanged(int mode) {
        super.onNightModeChanged(mode);
    }

    @Override
    protected void onLocalesChanged(@NonNull LocaleListCompat locales) {
        super.onLocalesChanged(locales);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    protected void flexCreate(Bundle savedInstanceState){

    }

    private void init_() {
        /*Some initial code */
        initContainer();
        initFlexReactive();
        initFlexTheme();
    }

    private void initContainer(){
        baseContainer = new FlexBaseContainer(this);
        baseContainer.content(wrapper,"c");
        /*
        * ** It's in this initContainer() that i will directly attach the main Fragment of the activity
        * ** By calling the initialRoot value and call the Fragment that named with this initialRoot value
        * ** corresponding to the navigationKey
        *
        * */
    }

    public final int containerID(){
        return this.wrapper.getContainerID();
    }

    protected final void initialFragment(FlexFragment flexFragment, boolean ToBackstack){
        if (ToBackstack){
            getSupportFragmentManager().beginTransaction()
                    .replace(containerID(), flexFragment)
                    .addToBackStack(flexFragment.getNavigationKey())
                    .commit();
        }else {
            getSupportFragmentManager().beginTransaction()
                    .replace(containerID(), flexFragment)
                    .commit();
        }
    }

    //This method must be Override in All FlexActivity and call Class, and this super.mainFragment() method
    // in order to provide Initial Fragment for the Activity
//    protected void mainFragment(FlexFragment flexFragment, boolean ToBackstack){
//        initialFragment(flexFragment, ToBackstack);
//    };

    protected void initFlexReactive(){
        /* This the protected method that user must to redefine like this way only one time in the main activity that
         * ** That will launch the application
         */
    }

    protected void initFlexTheme(){
        //FlexTheme.currentTheme = "orange";
        //FlexTheme.pushTheme("light", List.of("#FF005544", "#22554444", "#775544", "#554444"));
        //FlexTheme.pushTheme("dark", List.of("#FF5544", "#554444", "#ff775544", "#554444"));
        //FlexTheme.pushTheme("sun", List.of("#ff005544", "#ff554444", "#77FF44", "#554444"));
        //FlexTheme.pushTheme("summer", List.of("#FF005544", "#22554444", "#775544", "#554444"));
        //FlexTheme.pushTheme("spring", List.of("#FF005544", "#22554444", "#775544", "#554444"));
    }

    @Override
    public boolean onNextFlexFragment() {
        return true;
    }

    @Override
    public boolean onCloseFlexFragment() {
        return true;
    }

    public final void launchSheet(FlexBottomSheet sheet){
        sheet.show(getSupportFragmentManager(), sheet.getTags());
    }

    public final boolean isFullScreen(){
        return this.fullScreen;
    }

    protected final void fullScreen(){

        // Opaque : pas de translucence
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        // Pour API >= 21 : couleurs transparentes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Status bar transparente
            getWindow().setStatusBarColor(Color.TRANSPARENT);

            // Navigation bar transparente
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        // Sticky immersive mode: system icons stay visible, the content draws behind them.
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        getWindow().getDecorView().setSystemUiVisibility(flags);


        this.fullScreen = true;
    }

    /*protected final void fullScreeByInsets(FlexActivity flexActivity){
        ViewCompat.setOnApplyWindowInsetsListener(flexActivity.getWindow().getDecorView().getRootView(), (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets1.left,insets1.top,insets1.right,insets1.bottom);
            return insets;
        });

    }*/

    protected final void fullscreenModern() {
        WindowInsetsController controller;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller = getWindow().getInsetsController();

            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
                this.fullScreen = true;
            }
        }
    }

    @Override
    public final void identify() {

    }

    @Override
    public final String ref(String scope) {
        String scopeId = "";

        if(scope.equals(FlexScope.FlexActivity.name()))
            scopeId = ""+this;

        return scopeId;
    }

    public final class FlexActivityContainerWrapper implements BaseContainerBridge{
        private int containerID;
        @Override
        public void bridge(FrameLayout frameLayout, String s) {
            FlexActivity.this.setContentView(frameLayout);
            this.containerID = frameLayout.getId();
        }

        int getContainerID(){
            return this.containerID;
        }
    }
}
