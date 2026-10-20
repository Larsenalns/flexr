// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.content.res.ColorStateList;

import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;

import java.util.List;

/**
 * Two-thumb slider (low and high bounds), backed by
 * {@code com.google.android.material.slider.RangeSlider}.
 *
 * <p>Same conventions as {@link FlexSlider}: dimensions are in <b>dp</b>, and
 * {@link #valuesInit(float, float, float)} must be called before {@link #value(Float...)}.
 *
 * <p>The thumbs are ordered: index 0 is the low bound, index 1 the high bound. Once a value has
 * been set, the widget holds at least two values.
 *
 * <p><b>Threading:</b> every setter mutates a live {@code RangeSlider} and must run on the UI
 * thread; listeners registered here are invoked on that same thread.
 */
public class FlexSliderRange extends FlexView{
    private final RangeSlider rangeSlider;

    /**
     * Creates the widget, its backing {@code RangeSlider} and the bridge between the two.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public FlexSliderRange(FlexContextInterface flexContext) {
        super(flexContext);
        this.rangeSlider = new RangeSlider(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Attaches the given bridge to the backing range slider.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.rangeSlider);
    }

    /**
     * Sets the three track tints in one call.
     *
     * @param trackColor    general track color, used where the other two do not apply
     * @param activeColor   color of the track between the two thumbs
     * @param inactiveColor color of the track outside the two thumbs
     * @return this instance, for chaining
     */
    public final FlexSliderRange trackColor(int trackColor, int activeColor, int inactiveColor){
        this.rangeSlider.setTrackTintList(ColorStateList.valueOf(trackColor));
        this.rangeSlider.setTrackActiveTintList(ColorStateList.valueOf(activeColor));
        this.rangeSlider.setTrackInactiveTintList(ColorStateList.valueOf(inactiveColor));
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
    public final FlexSliderRange trackHeight(int trackHeight){
        this.rangeSlider.setTrackHeight(FlexValue.fixed(f, trackHeight));
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
    public final FlexSliderRange trackInsideCorner(int insideCornerSize){
        this.rangeSlider.setTrackInsideCornerSize(FlexValue.fixed(f, insideCornerSize));
        return this;
    }

    /**
     * Sets the thumb tint, radius and elevation in one call.
     *
     * @param thumbColor     resolved ARGB color of the thumbs
     * @param thumbRadius    thumb radius in <b>dp</b>
     * @param thumbElevation thumb elevation in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSliderRange thumbAppearance(int thumbColor, int thumbRadius, int thumbElevation){
        this.rangeSlider.setThumbRadius(FlexValue.fixed(f, thumbRadius));
        this.rangeSlider.setThumbElevation(FlexValue.fixed(f, thumbElevation));
        this.rangeSlider.setThumbTintList(ColorStateList.valueOf(thumbColor));
        return this;
    }
    /**
     * Sets the thumb outline and the gap between each thumb and the track.
     *
     * @param thumbStrokeColor resolved ARGB color of the outline
     * @param thumbStrokeWidth outline width in <b>dp</b>
     * @param gapSize          gap between a thumb and the track, in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSliderRange thumbStroke(int thumbStrokeColor, int thumbStrokeWidth, int gapSize){
        this.rangeSlider.setThumbStrokeWidth(FlexValue.fixed(f, thumbStrokeWidth));
        this.rangeSlider.setThumbTrackGapSize(FlexValue.fixed(f, gapSize));
        this.rangeSlider.setThumbStrokeColor(ColorStateList.valueOf(thumbStrokeColor));
        return this;
    }
    /**
     * Replaces the default thumbs with a custom drawable.
     *
     * @param drawable drawable name resolved through {@code flexDrawable(...)}
     * @return this instance, for chaining
     */
    public final FlexSliderRange thumbDrawable(String drawable){
        this.rangeSlider.setCustomThumbDrawable(f.flexDrawable(drawable));
        return this;
    }
    /**
     * Sets the halo drawn around the active thumb.
     *
     * @param haloColor  resolved ARGB color of the halo
     * @param haloRadius halo radius in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSliderRange halo(int haloColor, int haloRadius){
        this.rangeSlider.setHaloRadius(FlexValue.fixed(f, haloRadius));
        this.rangeSlider.setHaloTintList(ColorStateList.valueOf(haloColor));
        return this;
    }

    /**
     * Sets the three tick tints in one call.
     *
     * <p>Each tint is applied to its own target: general, active and inactive. No target receives
     * another target's color.
     *
     * @param tickColor     general tick color, used where the other two do not apply
     * @param activeColor   color of the ticks inside the selected range
     * @param inactiveColor color of the ticks outside the selected range
     * @return this instance, for chaining
     */
    public final FlexSliderRange tickColor(int tickColor, int activeColor, int inactiveColor){
        this.rangeSlider.setTickTintList(ColorStateList.valueOf(tickColor));
        this.rangeSlider.setTickActiveTintList(ColorStateList.valueOf(activeColor));
        this.rangeSlider.setTickInactiveTintList(ColorStateList.valueOf(inactiveColor));
        return this;
    }
    /**
     * Sets the radius of the active and inactive ticks.
     *
     * @param radiusActive   radius of the active ticks, in <b>dp</b>
     * @param radiusInactive radius of the inactive ticks, in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexSliderRange tickRadius(int radiusActive, int radiusInactive){
        this.rangeSlider.setTickActiveRadius(FlexValue.fixed(f, radiusActive));
        this.rangeSlider.setTickInactiveRadius(FlexValue.fixed(f, radiusInactive));
        return this;
    }
    /**
     * Sets the thumb values.
     *
     * @param values low bound first, high bound second; at least two values are required
     * @return this instance, for chaining
     */
    public final FlexSliderRange value(Float... values){
        this.rangeSlider.setValues(values);
        return this;
    }
    /**
     * Sets the thumb values from a list.
     *
     * @param values low bound first, high bound second; at least two values are required
     * @return this instance, for chaining
     */
    public final FlexSliderRange value(List<Float> values){
        this.rangeSlider.setValues(values);
        return this;
    }
    /**
     * Defines the value range and the step, before any value is set.
     *
     * <p>Call this first: a Material slider rejects a value outside its range, and the default
     * range is {@code 0..1}.
     *
     * @param from lowest selectable value
     * @param to   highest selectable value, greater than {@code from}
     * @param step increment between two selectable values, greater than zero
     * @return this instance, for chaining
     */
    public final FlexSliderRange valuesInit(float from, float to, float step){
        this.rangeSlider.setValueFrom(from);
        this.rangeSlider.setValueTo(to);
        this.rangeSlider.setStepSize(step);
        return this;
    }

    /**
     * Returns the low bound (first thumb value).
     *
     * @return value of the first thumb
     * @throws IndexOutOfBoundsException if fewer than two values have been set
     *         (see {@link #value(Float...)})
     */
    public final float min(){
        return this.rangeSlider.getValues().get(0);
    }

    /**
     * Returns the high bound (second thumb value).
     *
     * @return value of the second thumb
     * @throws IndexOutOfBoundsException if fewer than two values have been set
     */
    public final float max(){
        return this.rangeSlider.getValues().get(1);
    }

    /**
     * Returns the index of the thumb the user is currently interacting with.
     *
     * @return index of the active thumb, or {@code -1} when no thumb is being moved
     */
    public final int activeThumbIndex(){
        return this.rangeSlider.getActiveThumbIndex();
    }

    /**
     * Sets the minimum gap between the two thumbs, expressed <b>in value units</b> of the slider
     * and not in dp: it constrains the difference between the two values, not a screen dimension.
     *
     * @param separation minimum difference between the low and the high value
     * @return this instance, for chaining
     */
    public final FlexSliderRange thumbSeparation(int separation){
        this.rangeSlider.setMinSeparationValue(separation);
        return this;
    }

    //Events
    /**
     * Registers a listener notified while a thumb value changes.
     *
     * <p>The listener is additive: Material keeps the listeners already added, so repeated calls
     * stack up.
     *
     * @param changeListener listener to add
     * @return this instance, for chaining
     */
    public final FlexSliderRange onValueChange(RangeSlider.OnChangeListener changeListener){
        this.rangeSlider.addOnChangeListener(changeListener);
        return this;
    }
    /**
     * Registers a listener notified when the user starts and stops dragging a thumb.
     *
     * @param sliderTouchListener listener to add
     * @return this instance, for chaining
     */
    public final FlexSliderRange onSliderTouched(RangeSlider.OnSliderTouchListener sliderTouchListener){
        this.rangeSlider.addOnSliderTouchListener(sliderTouchListener);
        return this;
    }
}
