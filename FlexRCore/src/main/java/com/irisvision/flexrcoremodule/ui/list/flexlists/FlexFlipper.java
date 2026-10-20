// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import android.view.animation.Animation;
import android.widget.ViewFlipper;

import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.container.BoxImpl;

/**
 * Switches between several child views, backed by an Android {@link ViewFlipper}.
 *
 * <p>Children are added through the FlexR API and exactly one of them is visible at a time. The
 * flipper advances on its own once {@link #interval(int)} and {@link #start()} have been called,
 * or on demand through {@link #goToNext()} and {@link #goToPrevious()}. Animations are given
 * either as an animation resource id taken from the constants below, or as a ready-made
 * {@link Animation}.
 *
 * <pre>{@code
 * new FlexFlipper(flexContext)
 *         .inAnimation(FlexFlipper.FADE_IN)
 *         .interval(3000)
 *         .start();
 * }</pre>
 *
 * <p>Threading: the class is not thread-safe. Every method below must run on the UI thread, and
 * automatic flipping is scheduled on that thread's message queue.
 */
public class FlexFlipper extends BoxImpl {

    /** Animation resource: the incoming view slides in from the left. */
    public static final int SLIDE_IN_LEFT = android.R.anim.slide_in_left;
    /** Animation resource: the outgoing view slides out to the right. */
    public static final int SLIDE_OUT_RIGHT = android.R.anim.slide_out_right;
    /** Animation resource: the incoming view fades in. */
    public static final int FADE_IN = android.R.anim.fade_in;
    /** Animation resource: the outgoing view fades out. */
    public static final int FADE_OUT = android.R.anim.fade_out;

    private final ViewFlipper viewFlipper;

    /**
     * Creates the flipper and its backing {@code ViewFlipper}.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     */
    public FlexFlipper(FlexContextInterface flexContext) {
        super(flexContext);
        this.viewFlipper = new ViewFlipper(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Attaches the flipper to the FlexR content slot.
     *
     * @param flexViewBridge the bridge provided by the framework
     * @return {@code true} when the bridge accepted the content
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.viewFlipper);
    }

    /**
     * Attaches the flipper as the child container of the box.
     *
     * @param boxBridge the box bridge provided by the framework
     * @return {@code true} when the bridge accepted the container
     */
    @Override
    public final boolean boxWrapper(BoxBridge boxBridge) {
        return content(boxBridge, this.viewFlipper);
    }

    /**
     * Starts the automatic flipping.
     *
     * <p>Flips then happen every interval set with {@link #interval(int)}, or every platform
     * default interval when none was set.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper start(){
        this.viewFlipper.startFlipping();
        return this;
    }

    /**
     * Stops the automatic flipping; the current child stays visible.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper stop(){
        this.viewFlipper.stopFlipping();
        return this;
    }

    /**
     * Sets the delay between two automatic flips.
     *
     * @param flippingInterval delay in milliseconds
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper interval(int flippingInterval){
        this.viewFlipper.setFlipInterval(flippingInterval);
        return this;
    }

    /**
     * Shows the next child, wrapping to the first one after the last.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper goToNext(){
        this.viewFlipper.showNext();
        return this;
    }

    /**
     * Shows the previous child, wrapping to the last one before the first.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper goToPrevious(){
        this.viewFlipper.showPrevious();
        return this;
    }

    /**
     * Sets the animation played when a new child becomes visible.
     *
     * @param animation the animation to play; {@code null} removes it
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper inAnimation(Animation animation){
        this.viewFlipper.setInAnimation(animation);
        return this;
    }

    /**
     * Sets the animation played when the current child goes out of view.
     *
     * @param animation the animation to play; {@code null} removes it
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper outAnimation(Animation animation){
        this.viewFlipper.setOutAnimation(animation);
        return this;
    }

    /**
     * Sets the entry animation from an animation resource.
     *
     * @param animateType resource id, usually one of {@link #SLIDE_IN_LEFT} or {@link #FADE_IN}
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper inAnimation(int animateType){
        this.viewFlipper.setInAnimation(f.getFlexContext(), animateType);
        return this;
    }

    /**
     * Sets the exit animation from an animation resource.
     *
     * @param animateType resource id, usually one of {@link #SLIDE_OUT_RIGHT} or {@link #FADE_OUT}
     * @return {@code this}, to allow chaining
     */
    public final FlexFlipper outAnimation(int animateType){
        this.viewFlipper.setOutAnimation(f.getFlexContext(), animateType);
        return this;
    }
}
