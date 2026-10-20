package com.irisvision.flexrcoremodule.layoutParams;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.core.os.LocaleListCompat;

import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexFragment;
import com.irisvision.flexrcoremodule.enums.In;

/**
 * Contexte FlexR minimal pour les tests unitaires.
 *
 * <p>Only the value conversions are really exercised: the Android context methods
 * (ressources, assets, lancement d'Activity) ne participent pas aux calculs de marges, de dimensions
 * or percentages, and therefore throw an explicit exception if a test reaches them — which makes
 * any unintended dependency visible.
 *
 * <p>Density is fixed at {@code 1.0}, which makes dp values and pixels identical: the
 * attentes des tests se lisent directement en dp.
 */
final class TestFlexContext implements FlexContextInterface {

    private final DisplayMetrics metrics;

    TestFlexContext(int widthPixels, int heightPixels, float density) {
        this.metrics = new DisplayMetrics();
        this.metrics.widthPixels = widthPixels;
        this.metrics.heightPixels = heightPixels;
        this.metrics.density = density;
        this.metrics.densityDpi = (int) (density * 160);
    }

    /** A 1000 × 2000 px context, density 1.0. */
    static TestFlexContext standard() {
        return new TestFlexContext(1000, 2000, 1.0f);
    }

    @Override
    public DisplayMetrics getFlexDisplayMetrics() {
        return metrics;
    }

    @Override
    public DisplayMetrics getDisplayMetrics() {
        return metrics;
    }

    @Override
    public DisplayMetrics getDisplayMetrics_fullScreen() {
        return metrics;
    }

    private static UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException(
                "The test context does not implement " + method
                        + ": the code path under test should not call it.");
    }

    @Override
    public Context getFlexContext() {
        // Aucun environnement Android n'est disponible dans ces tests : les chemins exigeant un
        // a real Context (instantiating a View) is out of scope here.
        throw unsupported("getFlexContext()");
    }

    @Override
    public FlexActivity getFlexActivity() {
        throw unsupported("getFlexActivity()");
    }

    @Override
    public Resources getFlexResources() {
        throw unsupported("getFlexResources()");
    }

    @Override
    public Configuration getFlexConfig() {
        throw unsupported("getFlexConfig()");
    }

    @Override
    public LocaleListCompat getFlexLocals() {
        throw unsupported("getFlexLocals()");
    }

    @Override
    public int flexAsset(In in, String assetName) {
        throw unsupported("flexAsset()");
    }

    @Override
    public Drawable flexDrawable(String drawableName) {
        throw unsupported("flexDrawable()");
    }

    @Override
    public FlexFragment getFlexFragment() {
        throw unsupported("getFlexFragment()");
    }

    @Override
    public String signature() {
        throw unsupported("signature()");
    }

    @Override
    public void launchAndClose(Class<? extends FlexActivity> flexActivityClass, Bundle bundle) {
        throw unsupported("launchAndClose()");
    }

    @Override
    public void launchAndKeep(Class<? extends FlexActivity> flexActivityClass, Bundle bundle) {
        throw unsupported("launchAndKeep()");
    }
}
