// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import com.irisvision.flexrcoremodule.enums.In;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;

public abstract class FlexContext implements FlexContextInterface{
    //private Context context;
    private FlexFragment flexFragment;

    private int appH = 0;
    private boolean isAppBar = false;

    public final void initFlexStarter(FlexFragment.FlexStarter flexStarter) {
        flexStarter.start(this);
        isAppBar = flexStarter.hasAppBar();
        appH = flexStarter.appBarHeight();
    }

    public abstract void initializer_();

    @Override
    public final Context getFlexContext() {
        /*Get each time the context by the getFlexFragment() method because its the only element that is attached to FlexActivity
        and its the only element that gives the really and current context, if the context change the FlexFragment will know it
        and if recreate is needed, so the Old FlexFragment will be erased and the new will be recreated by the system, by the same way
        all the Flex Others elements because all are attached to the FlexFragment*/

        /* requireContext() Or getContext() can be used those two are methods of Fragment class;
         * ** getContext() can return null if the Fragment isn't attach yet to his activity
         * ** when called before attach method of Activity/Fragment lifCycle
         * ** and requiredContext() will run "IllegalStateException" Exception if context is null
         * */

        return getFlexFragment().requireContext();
    }

    public final void contextualize(FlexFragment flexFragment) {
        if (this.flexFragment == null){
            this.flexFragment = flexFragment;
        }
    }

    @Override
    public final FlexActivity getFlexActivity() {
        return (FlexActivity) getFlexContext();
    }

    @Override
    public final Resources getFlexResources() {
        return getFlexContext().getResources();
    }

    /**
     * Resolves a resource <b>by its name</b> instead of by an {@code R.…} identifier.
     *
     * <p>The resolution is dynamic, which is what makes screens describable as data: the name is the
     * contract, and no generated identifier has to exist at compile time.
     *
     * <p>Because it is dynamic, a name cannot be seen by the resource shrinker. A name that does not
     * resolve is therefore an <b>error</b>, never a silent {@code 0}: an icon that disappears or a
     * separator that vanishes is far harder to diagnose than an exception.
     *
     * @param in          the resource type
     * @param resourceName the resource name, as written in the code
     * @return the resolved identifier, never {@code 0}
     * @throws IllegalArgumentException if no resource of that name and type exists, or if resource
     *                                  shrinking removed it
     */
    @Override
    public final int flexAsset(In in, String resourceName){
        int identifier = getFlexResources().getIdentifier(
                resourceName, in.name(), getFlexContext().getPackageName());

        if (identifier == 0) {
            throw new IllegalArgumentException(
                    "Resource not found: \"" + resourceName + "\" of type \"" + in.name()
                            + "\" in package \"" + getFlexContext().getPackageName() + "\"."
                            + " Possible causes: the name is misspelled; no resource of that type carries it;"
                            + " or the application enables shrinkResources, which removes resources referenced"
                            + " only by name — keep them with a res/raw/keep.xml file (tools:keep attribute).");
        }
        return identifier;
    }

    @Override
    public final Drawable flexDrawable(String drawableName) {
        return ContextCompat.getDrawable(getFlexContext(), flexAsset(In.drawable, drawableName));
    }

    @Override
    public final DisplayMetrics getFlexDisplayMetrics() {
        DisplayMetrics metrics = getDisplayMetrics();

        if(!getFlexActivity().isFullScreen() && hasAppBar())
            metrics.heightPixels = metrics.heightPixels - appBarHeight();

        else if(getFlexActivity().isFullScreen()){
            metrics = getDisplayMetrics_fullScreen();

            if(hasAppBar())
                metrics.heightPixels = metrics.heightPixels - appBarHeight();
        }

        return metrics;
    }

    @Override
    public final DisplayMetrics getDisplayMetrics() {
        return freshMetrics(false);
    }

    @Override
    public final DisplayMetrics getDisplayMetrics_fullScreen() {
        return freshMetrics(true);
    }

    /**
     * Returns a <b>brand-new</b> metrics object on every call.
     *
     * <p>That is a requirement, not a detail: {@link #getFlexDisplayMetrics()} adjusts
     * {@code heightPixels} in place. If both methods shared one instance, the result would depend on the
     * order and number of previous calls, and a caller that kept the reference would see its numbers
     * change under its feet. Here, every caller owns the object it receives.
     *
     * @param fullScreen {@code true} for the real screen size (system bars included), {@code false} for
     *                   the area available to the application
     */
    private DisplayMetrics freshMetrics(boolean fullScreen){
        DisplayMetrics result = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getFlexContext().getSystemService(Context.WINDOW_SERVICE);

        if (fullScreen) {
            windowManager.getDefaultDisplay().getRealMetrics(result);
        } else {
            windowManager.getDefaultDisplay().getMetrics(result);
        }

        return result;
    }

    public final String signature() {
        return getFlexFragment()+":"+getFlexContext();
    }

    @Override
    public final Configuration getFlexConfig() {
        return getFlexResources().getConfiguration();
    }

    @Override
    public final LocaleListCompat getFlexLocals() {
        return ConfigurationCompat.getLocales(getFlexConfig());
    }

    public final boolean hasAppBar(){
        return this.isAppBar;
    }
    public final int appBarHeight(){
        return this.appH;
    }

    @Override
    public final FlexFragment getFlexFragment() {
        return this.flexFragment;
    }

    @Override
    public final void launchAndClose(Class<? extends FlexActivity> flexActivityClass, Bundle bundle) {
        Intent intent = new Intent(getFlexActivity(), flexActivityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if(bundle != null){
            intent.putExtras(bundle);
        }
        getFlexActivity().startActivity(intent);
    }

    @Override
    public final void launchAndKeep(Class<? extends FlexActivity> flexActivityClass, Bundle bundle) {
        Intent intent = new Intent(getFlexActivity(), flexActivityClass);
        //Have to define Flags for the keep currentFlexActivity
        if(bundle != null){
            intent.putExtras(bundle);
        }
        getFlexActivity().startActivity(intent);
    }
}
