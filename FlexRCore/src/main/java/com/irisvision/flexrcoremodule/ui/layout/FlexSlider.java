// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.content.res.ColorStateList;

import com.google.android.material.slider.Slider;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;

/**
 * Single-value slider, backed by {@code com.google.android.material.slider.Slider}.
 *
 * <p><b>Units.</b> Every dimension accepted here is expressed in <b>dp</b> and converted to
 * pixels by {@link FlexValue#fixed}. This is the uniform FlexR convention, shared with
 * {@code FlexCard.radius(...)}: one parameter name never means dp in one class and px in
 * another.
 *
 * <p><b>Call order.</b> {@link #valuesInit(float, float, float)} must be called <b>before</b>
 * {@link #value(float)}. A Material {@code Slider} defaults to the range {@code 0..1}, and
 * {@code setValue} throws {@code IllegalStateException} for a value outside the configured
 * range; the step must also divide the value, otherwise Material rejects the setting.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * new FlexSlider(flexContext)
 *         .valuesInit(0f, 100f, 1f)
 *         .value(35f)
 *         .onValueChange((slider, value, fromUser) -> update(value));
 * }</pre>
 *
 * <p><b>Not exposed.</b> Material properties kept out of this facade:
 * {@code trackCornerSize}, {@code tickVisible}, {@code thumbWidth} / {@code thumbHeight},
 * {@code thumbElevationResource}, {@code labelBehavior}.
 *
 * <p><b>Threading:</b> every setter mutates a live {@code Slider} and must run on the UI thread;
 * listeners registered here are invoked on that same thread.
 */
public class FlexSlider extends FlexView {
    private final Slider slider;

    /**
     * Creates the widget, its backing {@code Slider} and the bridge between the two.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public FlexSlider(FlexContextInterface flexContext) {
        super(flexContext);
        this.slider = new Slider(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Attaches the given bridge to the backing slider.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.slider);
    }

    /**
     * Sets the three track tints in one call.
     *
     * @param trackColor    general track color, used where the other two do not apply
     * @param activeColor   color of the track between the start and the thumb
     * @param inactiveColor color of the track after the thumb
     * @return this instance, for chaining
     */
    public final FlexSlider trackColor(int trackColor, int activeColor, int inactiveColor){
        this.slider.setTrackTintList(ColorStateList.valueOf(trackColor));
        this.slider.setTrackActiveTintList(ColorStateList.valueOf(activeColor));
        this.slider.setTrackInactiveTintList(ColorStateList.valueOf(inactiveColor));
        return this;
    }

    /**
     * Sets the track thickness.
     *
     * <p>Material exposes no track width, so only the thickness is taken: every argument of this
     * method is applied.
     *
     * @param trackHeight thickness in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSlider trackHeight(int trackHeight){
        this.slider.setTrackHeight(FlexValue.fixed(f, trackHeight));
        return this;
    }

    /**
     * Sets the radius of the inner corners of the track.
     *
     * <p>Material 1.12 exposes only {@code setTrackInsideCornerSize} for track corners, hence the
     * method name.
     *
     * @param insideCornerSize corner radius in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSlider trackInsideCorner(int insideCornerSize){
        this.slider.setTrackInsideCornerSize(FlexValue.fixed(f, insideCornerSize));
        return this;
    }

    /**
     * Sets the thumb tint, radius and elevation in one call.
     *
     * @param thumbColor     resolved ARGB color of the thumb
     * @param thumbRadius    thumb radius in <b>dp</b>
     * @param thumbElevation thumb elevation in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSlider thumbAppearance(int thumbColor, int thumbRadius, int thumbElevation){
        this.slider.setThumbRadius(FlexValue.fixed(f, thumbRadius));
        this.slider.setThumbElevation(FlexValue.fixed(f, thumbElevation));
        this.slider.setThumbTintList(ColorStateList.valueOf(thumbColor));
        return this;
    }
    /**
     * Sets the thumb outline and the gap between the thumb and the track.
     *
     * @param thumbStrokeColor resolved ARGB color of the outline
     * @param thumbStrokeWidth outline width in <b>dp</b>
     * @param gapSize          gap between the thumb and the track, in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSlider thumbStroke(int thumbStrokeColor, int thumbStrokeWidth, int gapSize){
        this.slider.setThumbStrokeWidth(FlexValue.fixed(f, thumbStrokeWidth));
        this.slider.setThumbTrackGapSize(FlexValue.fixed(f, gapSize));
        this.slider.setThumbStrokeColor(ColorStateList.valueOf(thumbStrokeColor));
        return this;
    }
    /**
     * Replaces the default thumb with a custom drawable.
     *
     * @param drawable drawable name resolved through {@code flexDrawable(...)}
     * @return this instance, for chaining
     */
    public final FlexSlider thumbDrawable(String drawable){
        this.slider.setCustomThumbDrawable(f.flexDrawable(drawable));
        return this;
    }
    /**
     * Sets the halo drawn around the thumb.
     *
     * @param haloColor  resolved ARGB color of the halo
     * @param haloRadius halo radius in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSlider halo(int haloColor, int haloRadius){
        this.slider.setHaloRadius(FlexValue.fixed(f, haloRadius));
        this.slider.setHaloTintList(ColorStateList.valueOf(haloColor));
        return this;
    }

    /**
     * Sets the three tick tints in one call.
     *
     * <p>Each tint is applied to its own target: general, active and inactive. No target receives
     * another target's color.
     *
     * @param tickColor     general tick color, used where the other two do not apply
     * @param activeColor   color of the ticks inside the active range
     * @param inactiveColor color of the ticks outside the active range
     * @return this instance, for chaining
     */
    public final FlexSlider tickColor(int tickColor, int activeColor, int inactiveColor){
        this.slider.setTickTintList(ColorStateList.valueOf(tickColor));
        this.slider.setTickActiveTintList(ColorStateList.valueOf(activeColor));
        this.slider.setTickInactiveTintList(ColorStateList.valueOf(inactiveColor));
        return this;
    }
    /**
     * Sets the radius of the active and inactive ticks.
     *
     * @param radiusActive   radius of the active ticks, in <b>dp</b>
     * @param radiusInactive radius of the inactive ticks, in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSlider tickRadius(int radiusActive, int radiusInactive){
        this.slider.setTickActiveRadius(FlexValue.fixed(f, radiusActive));
        this.slider.setTickInactiveRadius(FlexValue.fixed(f, radiusInactive));
        return this;
    }
    /**
     * Sets the current value.
     *
     * @param value value to select; it must lie inside the range configured by
     *              {@link #valuesInit(float, float, float)} and be compatible with its step
     * @return this instance, for chaining
     * @throws IllegalStateException if the value is outside the configured range or off the step
     */
    public final FlexSlider value(float value){
        this.slider.setValue(value);
        return this;
    }
    /**
     * Defines the value range and the step, before any value is set.
     *
     * <p>Call this first: while the range and the step are still at their defaults,
     * {@link #value(float)} may be rejected.
     *
     * @param from lowest selectable value
     * @param to   highest selectable value, greater than {@code from}
     * @param step increment between two selectable values, greater than zero
     * @return this instance, for chaining
     */
    public final FlexSlider valuesInit(float from, float to, float step){
        this.slider.setValueFrom(from);
        this.slider.setValueTo(to);
        this.slider.setStepSize(step);
        return this;
    }

    //Events
    /**
     * Registers a listener notified while the value changes.
     *
     * <p>The listener is additive: Material keeps the listeners already added, so repeated calls
     * stack up.
     *
     * @param changeListener listener to add
     * @return this instance, for chaining
     */
    public final FlexSlider onValueChange(Slider.OnChangeListener changeListener){
        this.slider.addOnChangeListener(changeListener);
        return this;
    }
    /**
     * Registers a listener notified when the user starts and stops dragging.
     *
     * @param sliderTouchListener listener to add
     * @return this instance, for chaining
     */
    public final FlexSlider onSliderTouched(Slider.OnSliderTouchListener sliderTouchListener){
        this.slider.addOnSliderTouchListener(sliderTouchListener);
        return this;
    }
}
