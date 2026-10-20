// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.component;

import android.annotation.SuppressLint;
import android.widget.FrameLayout;

import com.google.android.material.progressindicator.BaseProgressIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;

/**
 * Material progress indicator, either linear or circular.
 *
 * <h2>Stable facade</h2>
 *
 * <p>The view bridged to FlexR is a constant <b>host</b> {@link FrameLayout}: switching between
 * the linear and the circular mode replaces the indicator <i>inside</i> that host, never the host
 * itself. The host stays attached to the parent container, so a mode change is visible
 * immediately.
 *
 * <h2>Units</h2>
 *
 * <p>Thicknesses, radii, inner insets and sizes are expressed in <b>dp</b> and converted by
 * {@link FlexValue#fixed}, as everywhere else in FlexR.
 *
 * <h2>Call order</h2>
 *
 * <p>The setters of the <b>linear</b> mode ({@link #lineProgressSpec}, {@link #lineContiguous},
 * {@link #lineDisjoint}) configure the linear indicator; call
 * {@link #lineProgress(int, int, int, int)} to display it. None of them throws depending on the
 * order in which it is called: every indicator is created on demand.
 *
 * <p>Instances are not thread-safe. Every mutating method touches Android views, so it must be
 * called from the main thread.
 */
public class FlexProgress extends FlexView {

    /** Stable host: this is the view bridged to FlexR and added to the parent container. */
    private final FrameLayout host;

    private LinearProgressIndicator lineProgress;
    private CircularProgressIndicator circleProgress;
    private boolean isCircular = false;

    /**
     * Creates the progress view with a linear indicator selected by default.
     *
     * <p>A {@link FrameLayout} host is created and a {@link LinearProgressIndicator} is added to it
     * with match-parent bounds. That host is what {@link #contentWrapper} bridges to FlexR.
     *
     * @param flexContext FlexR context, used to obtain the Android context and convert dp values
     */
    public FlexProgress(FlexContextInterface flexContext) {
        super(flexContext);
        this.host = new FrameLayout(f.getFlexContext());
        this.lineProgress = new LinearProgressIndicator(f.getFlexContext());
        this.host.addView(this.lineProgress, matchParent());
        initInnerBridge();
    }

    /**
     * Bridges the internal host, and not the {@code FlexProgress} itself, to FlexR.
     *
     * @param flexViewBridge bridge supplied by FlexR for this view
     * @return the result of binding the host, as returned by {@code content(...)}
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.host);
    }

    //Material ProgressBar

    /**
     * Displays the circular mode.
     *
     * <p>A circular indicator is created on demand, configured with the given colors, thickness and
     * corner radius, then installed inside the host.
     *
     * @param indicatorColor    color of the moving progress arc
     * @param trackColor        color of the track behind the arc
     * @param trackThickness    track thickness in dp
     * @param trackCornerRadius corner radius of the track in dp
     * @return this instance, for chaining
     */
    public final FlexProgress circleProgress(int indicatorColor, int trackColor, int trackThickness, int trackCornerRadius){
        this.isCircular = true;
        this.circleProgress = circular();
        build(this.circleProgress, indicatorColor, trackColor, trackThickness, trackCornerRadius);
        show(this.circleProgress);
        return this;
    }

    /**
     * Displays the linear mode, which is the default one.
     *
     * <p>A linear indicator is created on demand, configured with the given colors, thickness and
     * corner radius, then installed inside the host.
     *
     * @param indicatorColor    color of the moving progress bar
     * @param trackColor        color of the track behind the bar
     * @param trackThickness    track thickness in dp
     * @param trackCornerRadius corner radius of the track in dp
     * @return this instance, for chaining
     */
    public final FlexProgress lineProgress(int indicatorColor, int trackColor, int trackThickness, int trackCornerRadius){
        this.isCircular = false;
        this.lineProgress = linear();
        build(this.lineProgress, indicatorColor, trackColor, trackThickness, trackCornerRadius);
        show(this.lineProgress);
        return this;
    }

    /**
     * Circular-only settings: inner inset, size and rotation direction.
     *
     * <p>The circular indicator is created on demand, so this method may be called before any call
     * to {@link #circleProgress(int, int, int, int)}.
     *
     * @param indicatorInset inner inset in dp
     * @param indicatorSize  diameter of the indicator in dp
     * @param reverse        {@code true} for a counter-clockwise rotation
     * @return this instance, for chaining
     */
    public final FlexProgress circularProgressSpec(int indicatorInset, int indicatorSize, boolean reverse){
        CircularProgressIndicator indicator = circular();
        indicator.setIndicatorInset(FlexValue.fixed(f, indicatorInset));
        indicator.setIndicatorSize(FlexValue.fixed(f, indicatorSize));
        if (reverse) indicator.setIndicatorDirection(CircularProgressIndicator.INDICATOR_DIRECTION_COUNTERCLOCKWISE);
        else indicator.setIndicatorDirection(CircularProgressIndicator.INDICATOR_DIRECTION_CLOCKWISE);
        return this;
    }

    /**
     * Linear-only settings: fill direction and size of the track stop indicator.
     *
     * @param reverse                {@code true} to fill from the end towards the start
     * @param trackStopIndicatorSize size of the stop indicator in dp
     * @return this instance, for chaining
     */
    public final FlexProgress lineProgressSpec(boolean reverse, int trackStopIndicatorSize){
        LinearProgressIndicator indicator = linear();
        if(reverse)indicator.setIndicatorDirection(LinearProgressIndicator.INDICATOR_DIRECTION_END_TO_START);
        else indicator.setIndicatorDirection(LinearProgressIndicator.INDICATOR_DIRECTION_START_TO_END);
        indicator.setTrackStopIndicatorSize(FlexValue.fixed(f, trackStopIndicatorSize));
        return this;
    }

    /**
     * Selects the contiguous indeterminate animation.
     *
     * <p>Shorthand for {@code lineAnimation} with
     * {@code INDETERMINATE_ANIMATION_TYPE_CONTIGUOUS}.
     *
     * @param anim   show and hide behavior to apply; must not be {@code null}
     * @param colors indicator colors, left untouched when empty
     * @return this instance, for chaining
     * @throws IllegalArgumentException when {@code anim} is {@code null}
     * @see #lineDisjoint(Anim, int...)
     */
    @SuppressLint("WrongConstant")
    public final FlexProgress lineContiguous(Anim anim, int... colors){
        return lineAnimation(LinearProgressIndicator.INDETERMINATE_ANIMATION_TYPE_CONTIGUOUS, anim, colors);
    }

    /**
     * Selects the disjoint indeterminate animation.
     *
     * <p>Shorthand for {@code lineAnimation} with
     * {@code INDETERMINATE_ANIMATION_TYPE_DISJOINT}.
     *
     * @param anim   show and hide behavior to apply; must not be {@code null}
     * @param colors indicator colors, left untouched when empty
     * @return this instance, for chaining
     * @throws IllegalArgumentException when {@code anim} is {@code null}
     * @see #lineContiguous(Anim, int...)
     */
    @SuppressLint("WrongConstant")
    public final FlexProgress lineDisjoint(Anim anim, int... colors){
        return lineAnimation(LinearProgressIndicator.INDETERMINATE_ANIMATION_TYPE_DISJOINT, anim, colors);
    }

    /**
     * Applies the indeterminate animation type, the show and hide behaviors and the indicator
     * colors to the linear indicator.
     *
     * @throws IllegalArgumentException when {@code anim} is {@code null}
     */
    @SuppressLint("WrongConstant")
    private FlexProgress lineAnimation(int animationType, Anim anim, int... colors){
        if (anim == null) {
            throw new IllegalArgumentException(
                    "anim == null : provide a FlexProgress.Anim value (for example Anim.fadeInFadeOut).");
        }

        LinearProgressIndicator indicator = linear();
        indicator.setIndeterminateAnimationType(animationType);
        if (colors != null && colors.length > 0) {
            indicator.setIndicatorColor(colors);
        }
        indicator.setShowAnimationBehavior(anim.getShowBehavior());
        indicator.setHideAnimationBehavior(anim.getHideBehavior());
        return this;
    }

    /**
     * Switches the displayed indicator between determinate and indeterminate.
     *
     * @param isIndeterminate {@code true} for an endless animation without a known progress value
     * @return this instance, for chaining
     */
    public final FlexProgress indeterminate(boolean isIndeterminate){
        current().setIndeterminate(isIndeterminate);
        return this;
    }

    /**
     * Replaces the drawable used to paint the progress.
     *
     * @param drawable name of the drawable, resolved through {@code f.flexDrawable(String)}
     * @return this instance, for chaining
     */
    public final FlexProgress progressDrawable(String drawable){
        current().setProgressDrawable(f.flexDrawable(drawable));
        return this;
    }

    /**
     * Sets the progress value of the displayed indicator.
     *
     * @param progressValue progress in the range of the current indicator, usually 0 to 100
     * @param animProgress  {@code true} to animate the transition
     * @return this instance, for chaining
     */
    public final FlexProgress progress(int progressValue, boolean animProgress){
        current().setProgressCompat(progressValue, animProgress);
        return this;
    }

    /** Indicator currently displayed, resolved from the selected mode. */
    private BaseProgressIndicator<?> current(){
        return this.isCircular ? circular() : linear();
    }

    private LinearProgressIndicator linear(){
        if (this.lineProgress == null) this.lineProgress = new LinearProgressIndicator(f.getFlexContext());
        return this.lineProgress;
    }

    private CircularProgressIndicator circular(){
        if (this.circleProgress == null) this.circleProgress = new CircularProgressIndicator(f.getFlexContext());
        return this.circleProgress;
    }

    /**
     * Replaces the indicator displayed inside the host. The bridged facade itself never changes.
     *
     * <p>All views are removed from the host first, so only one indicator is alive at a time. Must
     * be called on the main thread.
     */
    private void show(BaseProgressIndicator<?> indicator){
        this.host.removeAllViews();
        this.host.addView(indicator, matchParent());
    }

    private static FrameLayout.LayoutParams matchParent(){
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    /**
     * Applies the settings shared by both indicators.
     *
     * <p>Only the indicator passed in is touched: the host is already bridged, so no rebinding step
     * is needed.
     */
    private void build (BaseProgressIndicator<?> indicator, int indicatorColor, int trackColor, int trackThickness, int trackCornerRadius){
        indicator.setIndicatorColor(indicatorColor);
        indicator.setTrackColor(trackColor);
        indicator.setTrackThickness(FlexValue.fixed(f, trackThickness));
        indicator.setTrackCornerRadius(FlexValue.fixed(f, trackCornerRadius));
    }

    /**
     * Show and hide behaviors of a progress indicator, expressed with the Material constants.
     *
     * <p>Each constant pairs one {@code BaseProgressIndicator.SHOW_*} value with one
     * {@code HIDE_*} value. The numeric values are never hard-coded, so {@link #describe()} always
     * reports the behavior that is actually applied.
     *
     * <pre>{@code
     * progress.lineContiguous(FlexProgress.Anim.fadeInFadeOut, primaryColor);
     * }</pre>
     */
    public enum Anim {
        instantToggle(BaseProgressIndicator.SHOW_NONE, BaseProgressIndicator.HIDE_NONE),
        fadeOutInward(BaseProgressIndicator.SHOW_NONE, BaseProgressIndicator.HIDE_INWARD),
        fadeOutOutward(BaseProgressIndicator.SHOW_NONE, BaseProgressIndicator.HIDE_OUTWARD),
        fadeInInward(BaseProgressIndicator.SHOW_INWARD, BaseProgressIndicator.HIDE_NONE),
        fadeInOutward(BaseProgressIndicator.SHOW_OUTWARD, BaseProgressIndicator.HIDE_NONE),
        fadeInFadeOut(BaseProgressIndicator.SHOW_INWARD, BaseProgressIndicator.HIDE_INWARD),
        slideInSlideOut(BaseProgressIndicator.SHOW_OUTWARD, BaseProgressIndicator.HIDE_OUTWARD),
        fadeInSlideOut(BaseProgressIndicator.SHOW_INWARD, BaseProgressIndicator.HIDE_OUTWARD),
        slideInFadeOut(BaseProgressIndicator.SHOW_OUTWARD, BaseProgressIndicator.HIDE_INWARD);

        private final int show;
        private final int hide;

        Anim(int show, int hide) {
            this.show = show;
            this.hide = hide;
        }

        /**
         * @return the Material {@code SHOW_*} constant applied when the indicator appears
         */
        public int getShowBehavior() {
            return show;
        }

        /**
         * @return the Material {@code HIDE_*} constant applied when the indicator disappears
         */
        public int getHideBehavior() {
            return hide;
        }

        /** Human readable description, for example {@code "Show: INWARD, Hide: OUTWARD"}. */
        public String describe() {
            return "Show: " + behaviorLabel(show) + ", Hide: " + behaviorLabel(hide);
        }

        private static String behaviorLabel(int value) {
            switch (value) {
                case BaseProgressIndicator.SHOW_NONE:    return "NONE";
                case BaseProgressIndicator.SHOW_OUTWARD: return "OUTWARD";
                case BaseProgressIndicator.SHOW_INWARD:  return "INWARD";
                case BaseProgressIndicator.HIDE_ESCAPE:  return "ESCAPE";
                default: return "UNKNOWN";
            }
        }
    }

}
