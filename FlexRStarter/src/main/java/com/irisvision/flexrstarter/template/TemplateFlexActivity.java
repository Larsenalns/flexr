// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrstarter.template;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrcoremodule.core.Init_;
import com.irisvision.flexrcoremodule.core.ScopeIdentity;
import com.irisvision.flexrcoremodule.observable.FlexConsumer;
import com.irisvision.flexrcoremodule.observable.FlexProvider;
import com.irisvision.flexrcoremodule.observable.FlexReactive;
import com.irisvision.flexrthememodule.FlexTheme;

import java.util.List;

public class TemplateFlexActivity extends FlexActivity implements Init_, FlexConsumer {

    @Override
    protected void flexCreate(Bundle savedInstanceState) {
        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        init();
    }

    @Override
    public void init() {
        initialFragment(new TemplateFlexFragmentHome(), true);
        fullScreen();
        FlexReactive.get("home").subscribeActivityScope(this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // pushConfig() notifies synchronously. The navigation it triggers is
        // automatically brought back to the main thread by
        // NavigatorFlexFragment.navigate(), which makes this call safe even here.
        ((TemplateFlexConfigNotify) FlexReactive.get("config")).pushConfig();
    }

    @Override
    protected void initFlexReactive() {
        // Single declaration of the application's providers, in the starting Activity.
        FlexReactive.declare(List.of(
                new TemplateFlexConfigNotify(), new TemplateFlexProviderHome()
        ));
    }

    @Override
    protected void initFlexTheme() {
        // Convention de couleur : #AARRGGBB.
        FlexTheme.pushTheme("light", List.of("#FFE0E0E0", "#FFFFFFFF", "#FF775544", "#FF554444"));
        FlexTheme.pushTheme("dark", List.of("#FFF94144", "#FFDC2F02", "#FF775544", "#FF554444"));
        FlexTheme.pushTheme("orange", List.of("#FF005544", "#FFFFDF36", "#FF00FF00", "#FFE0E0E0"));

        // Applies the initial theme and notifies observers: appChangeTheme(...) must be called
        // AFTER the pushTheme(...) calls, otherwise the theme is not declared yet and the call does nothing.
        FlexTheme.appChangeTheme("orange");
    }

    @Override
    public ScopeIdentity identity() {
        return this;
    }

    @Override
    public void notifyDataChanged(FlexProvider flexProvider) {
        if (flexProvider instanceof TemplateFlexProviderHome) {
            TemplateFlexProviderHome home = (TemplateFlexProviderHome) flexProvider;
            Log.d("FlexR", "counter-> " + home.counter + "    Title->" + home.appBarText());
        }
    }
}
