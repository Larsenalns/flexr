// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.CompoundButton;

import com.google.android.material.chip.Chip;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;
import com.irisvision.flexrthememodule.FlexTheme;

/**
 * Tag widget, backed by a Material {@code Chip}.
 *
 * <p><b>Units.</b> Icon size is in <b>dp</b>; text size is in <b>sp</b>, applied through
 * {@code FlexValue.textByDensity}, so it honors the user font-scale setting.
 *
 * <p><b>Theme.</b> A text color set by palette index is remembered and re-applied on every theme
 * switch; a color passed as a {@code ColorStateList} is applied as-is and is not tracked.
 *
 * <p>Two ready-made configurations are provided:
 * {@link #buildClosable(String, float, int, int, View.OnClickListener)} and
 * {@link #buildCheckable(String, float, int, String, CompoundButton.OnCheckedChangeListener)}.
 *
 * <p><b>Threading:</b> every call mutates a live {@code Chip} and must run on the UI thread.
 */
public class FlexTag extends FlexView {

    /** Sentinel meaning "no palette index is remembered". */
    private static final int NO_THEME = -1;

    private final Chip chip;

    private int textColorTheme = NO_THEME;

    /**
     * Creates the tag, its backing {@code Chip} and the bridge between the two.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public FlexTag(FlexContextInterface flexContext) {
        super(flexContext);
        this.chip = new Chip(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Attaches the given bridge to the backing {@code Chip}.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.chip);
    }

    /**
     * Configures the tag as a closable chip: label, text size, text color, close icon tint and the
     * listener fired when the close icon is clicked.
     *
     * @param tagText        text displayed in the chip
     * @param textSize       text size in sp
     * @param textColor      resolved ARGB text color
     * @param closeIconColor resolved ARGB color of the close icon
     * @param onClose        listener invoked when the close icon is clicked
     * @return this instance, for chaining
     */
    public final FlexTag buildClosable(String tagText, float textSize, int textColor, int closeIconColor, View.OnClickListener onClose){
        textProperties(tagText, textSize, ColorStateList.valueOf(textColor));
        this.chip.setCloseIconTint(ColorStateList.valueOf(closeIconColor));
        this.chip.setOnCloseIconClickListener(onClose);
        return this;
    }
    /**
     * Configures the tag as a checkable chip: label, text size, text color, checked icon and the
     * listener notified when the checked state changes.
     *
     * @param tagText     text displayed in the chip
     * @param textSize    text size in sp
     * @param textColor   resolved ARGB text color
     * @param checkedIcon drawable name of the icon shown when the chip is checked
     * @param onChecked   listener notified on checked-state changes
     * @return this instance, for chaining
     */
    public final FlexTag buildCheckable(String tagText, float textSize, int textColor, String checkedIcon, CompoundButton.OnCheckedChangeListener onChecked){
        textProperties(tagText,textSize, ColorStateList.valueOf(textColor));
        this.chip.setCheckedIcon(f.flexDrawable(checkedIcon));
        this.chip.setCheckable(true);
        this.chip.setOnCheckedChangeListener(onChecked);
        return this;
    }

    private void textProperties(String text, float textSize, ColorStateList texColor){
        this.chip.setText(text);
        FlexValue.textByDensity(f, this.chip, textSize);
        this.chip.setTextColor(texColor);
    }
    /**
     * Sets the text displayed in the chip.
     *
     * @param text text to display
     * @return this instance, for chaining
     */
    public final FlexTag text(String text){
        this.chip.setText(text);
        return this;
    }

    /**
     * Sets the text color from the palette of the current theme and remembers the index, so
     * {@link #ifThemeChange()} can re-apply it after a switch.
     *
     * @param themeColorIndex index in the current palette
     * @return this instance, for chaining
     */
    public final FlexTag textColor(int themeColorIndex){
        this.textColorTheme = themeColorIndex;
        this.chip.setTextColor(ColorStateList.valueOf(FlexTheme.load(themeColorIndex)));
        return this;
    }
    /**
     * Sets the text color from an explicit color state list. This value is not tracked across theme
     * switches.
     *
     * @param colorStateList colors to apply
     * @return this instance, for chaining
     */
    public final FlexTag textColor(ColorStateList colorStateList){
        this.chip.setTextColor(colorStateList);
        return this;
    }
    /**
     * Sets the text size in sp, honoring the user font-scale setting.
     *
     * @param textSize size in scaled pixels
     * @return this instance, for chaining
     */
    public final FlexTag textSize(float textSize){
        FlexValue.textByDensity(f, this.chip, textSize);
        return this;
    }

    /**
     * Sets the icon drawn at the head of the chip label.
     *
     * <p>A Material {@code Chip} has no trailing icon: its icon is always placed at the head, which
     * is what this method and its name reflect.
     *
     * @param icon     drawable name resolved through {@code flexDrawable(...)}
     * @param iconSize icon size in <b>dp</b>; a non-positive value keeps the default size
     * @return this instance, for chaining
     */
    public final FlexTag chipIcon(String icon, int iconSize){
        this.chip.setChipIcon(f.flexDrawable(icon));
        this.chip.setChipIconVisible(true);
        if(iconSize > 0)
            this.chip.setChipIconSize(FlexValue.fixed(f, iconSize));
        return this;
    }

    /**
     * Re-applies the remembered text color in the current theme, then lets {@link FlexView} handle
     * the properties it owns.
     */
    @Override
    public void ifThemeChange() {
        super.ifThemeChange();
        if (textColorTheme != NO_THEME) {
            this.chip.setTextColor(ColorStateList.valueOf(FlexTheme.load(textColorTheme)));
        }
    }
}
