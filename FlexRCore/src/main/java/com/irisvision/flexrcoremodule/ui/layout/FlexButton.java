// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.content.res.ColorStateList;

import com.google.android.material.button.MaterialButton;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.enums.In;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;
import com.irisvision.flexrthememodule.FlexTheme;

import java.util.Locale;

/**
 * Material button.
 *
 * <p><b>Units.</b> Corner radius, elevation and stroke width are expressed in <b>dp</b> and
 * converted by {@link FlexValue#fixed}, as in {@code FlexCard}: one parameter name means one unit
 * across the whole library.
 *
 * <p><b>Theme.</b> Colors set by palette index (ripple, stroke) are remembered and re-applied on
 * every theme switch; text and hint colors are handled by {@link TextBase_}.
 *
 * <p>Every method returns the instance, so calls chain in the order they apply:
 *
 * <pre>{@code
 * new FlexButton(flexContext)
 *         .text("Submit")
 *         .radius(8)
 *         .btnElevation(2)
 *         .icon("submit", "text_end");
 * }</pre>
 *
 * <p><b>Threading:</b> every setter mutates a live {@code MaterialButton} and must run on the UI
 * thread; click listeners attached outside this class are invoked on that same thread.
 */
public class FlexButton extends TextBase_ {

    /** Sentinel meaning "no palette index is remembered". */
    private static final int NO_THEME = -1;

    private final MaterialButton button;

    private int rippleTheme = NO_THEME;
    private int strokeTheme = NO_THEME;
    private int strokeWidthPx;

    /**
     * Creates the widget, its backing {@code MaterialButton} and the bridge between the two.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public FlexButton(FlexContextInterface flexContext) {
        super(flexContext);
        this.button = new MaterialButton(f.getFlexContext());
        initInnerBridge();
        textViewBridge();
    }

    /**
     * Attaches the given bridge to the backing button.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.button);
    }

    //Every method returns the current instance so calls can be chained:
    /* radius(5)
    * .btnElevation(5)
    * .textColor(3).icon("submit", "text_end");
    *
    * */

    /**
     * Sets the corner radius.
     *
     * @param radius corner radius in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexButton radius(int radius){
        this.button.setCornerRadius(FlexValue.fixed(f, radius));
        return this;
    }

    /**
     * Sets the elevation.
     *
     * @param elevation elevation in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexButton btnElevation(int elevation){
        this.button.setElevation(FlexValue.fixed(f, elevation));
        return this;
    }

    /**
     * Sets the ripple color from the current palette and remembers the index for theme switches.
     *
     * @param themeColorIndex index in the current palette
     * @return this instance, for chaining
     */
    public final FlexButton ripple(int themeColorIndex){
        this.rippleTheme = themeColorIndex;
        this.button.setRippleColor(ColorStateList.valueOf(FlexTheme.load(themeColorIndex)));
        return this;
    }

    /**
     * Sets the button outline: width and color.
     *
     * <p>The width is converted to pixels once and stored, so a theme switch can re-apply the
     * stroke without reading it again.
     *
     * @param strokeWidth outline width in <b>dp</b>
     * @param strokeColor palette index of the outline color, remembered for theme switches
     * @return this instance, for chaining
     */
    public final FlexButton stroke(int strokeWidth, int strokeColor){
        this.strokeTheme = strokeColor;
        this.strokeWidthPx = FlexValue.fixed(f, strokeWidth);
        this.button.setStrokeWidth(this.strokeWidthPx);
        this.button.setStrokeColor(ColorStateList.valueOf(FlexTheme.load(strokeColor)));
        return this;
    }

    /**
     * Sets the icon and its position relative to the text.
     *
     * @param iconName name of the drawable asset to use
     * @param gravity  {@code "start"}, {@code "text_start"} (default), {@code "end"},
     *                 {@code "text_end"}, {@code "top"} or {@code "text_top"}, case-insensitive
     * @return this instance, for chaining
     * @throws IllegalArgumentException if {@code gravity} is neither {@code null} nor one of the
     *                                  accepted keywords
     */
    public final FlexButton icon(String iconName, String gravity){
        this.button.setIconResource(f.flexAsset(In.drawable, iconName));
        this.button.setIconGravity(iconGravity(gravity));
        return this;
    }

    private static int iconGravity(String gravity){
        if (gravity == null) return MaterialButton.ICON_GRAVITY_TEXT_START;

        switch (gravity.trim().toUpperCase(Locale.ROOT)){
            case "START":      return MaterialButton.ICON_GRAVITY_START;
            case "TEXT_START": return MaterialButton.ICON_GRAVITY_TEXT_START;
            case "END":        return MaterialButton.ICON_GRAVITY_END;
            case "TEXT_END":   return MaterialButton.ICON_GRAVITY_TEXT_END;
            case "TOP":        return MaterialButton.ICON_GRAVITY_TOP;
            case "TEXT_TOP":   return MaterialButton.ICON_GRAVITY_TEXT_TOP;
            default:
                throw new IllegalArgumentException(
                        "icon(..., \"" + gravity + "\") : unknown position. Use \"start\", \"text_start\", "
                                + "\"end\", \"text_end\", \"top\" or \"text_top\".");
        }
    }

    /**
     * Re-applies the remembered theme colors (ripple and stroke), then lets {@link TextBase_} handle
     * the text colors and {@code FlexView} the rest.
     */
    @Override
    public void ifThemeChange() {
        super.ifThemeChange();

        if (rippleTheme != NO_THEME) {
            this.button.setRippleColor(ColorStateList.valueOf(FlexTheme.load(rippleTheme)));
        }
        if (strokeTheme != NO_THEME) {
            this.button.setStrokeWidth(this.strokeWidthPx);
            this.button.setStrokeColor(ColorStateList.valueOf(FlexTheme.load(strokeTheme)));
        }
    }
}
