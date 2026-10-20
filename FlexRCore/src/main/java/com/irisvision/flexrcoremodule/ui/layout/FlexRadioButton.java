// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.widget.CompoundButton;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;

/**
 * Material radio button, meant to be added to a
 * {@code com.irisvision.flexrcoremodule.ui.container.FlexRadioGroup}.
 *
 * <p>Its vocabulary matches {@code FlexCheck} and {@code FlexSwitch}: {@code checkedState(...)}
 * sets the state and {@code onCheckedState(...)} registers the listener.
 *
 * <p><b>Threading:</b> must be created and configured on the UI thread; the listener is invoked
 * on that thread by the Android framework.
 */
public class FlexRadioButton extends TextBase_ {
    private final MaterialRadioButton radioButton;

    /**
     * Creates the widget, its backing {@code MaterialRadioButton} and the bridge between the two.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public FlexRadioButton(FlexContextInterface flexContext) {
        super(flexContext);
        this.radioButton = new MaterialRadioButton(f.getFlexContext());
        initInnerBridge();
        textViewBridge();
    }

    /**
     * Attaches the given bridge to the backing radio button.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.radioButton);
    }

    /**
     * Sets the checked state of the radio button.
     *
     * @param checkedValue {@code true} to check it, {@code false} to uncheck it
     * @return this instance, for chaining
     */
    public final FlexRadioButton checkedState(boolean checkedValue){
        this.radioButton.setChecked(checkedValue);
        return this;
    }

    /**
     * Registers the listener notified when the checked state changes.
     *
     * @param checkedChangeListener listener to notify, or {@code null} to clear the current one
     * @return this instance, for chaining
     */
    public final FlexRadioButton onCheckedState(CompoundButton.OnCheckedChangeListener checkedChangeListener){
        this.radioButton.setOnCheckedChangeListener(checkedChangeListener);
        return this;
    }
}
