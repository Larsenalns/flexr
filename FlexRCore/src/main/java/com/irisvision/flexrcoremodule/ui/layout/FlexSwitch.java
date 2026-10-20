// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.widget.CompoundButton;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;

/**
 * Material switch (toggle) widget.
 *
 * <p>Its vocabulary matches {@code FlexCheck} and {@code FlexRadioButton}:
 * {@link #checkedState(boolean)} sets the state and {@link #onCheckedState} registers the
 * listener, so the three sibling widgets are used the same way.
 *
 * <p><b>Threading:</b> creation, state changes and listener registration must happen on the UI
 * thread; the Android framework then delivers the listener callbacks on that same thread.
 */
public class FlexSwitch extends TextBase_ {
    private final MaterialSwitch switchBtn;

    /**
     * Creates the widget, its backing {@code MaterialSwitch} and the bridge between the two.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public FlexSwitch(FlexContextInterface flexContext) {
        super(flexContext);
        this.switchBtn = new MaterialSwitch(f.getFlexContext());
        initInnerBridge();
        textViewBridge();
    }

    /**
     * Attaches the given bridge to the backing switch view.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.switchBtn);
    }

    /**
     * Sets the checked state of the switch.
     *
     * @param checkValue {@code true} to check the switch, {@code false} to uncheck it
     * @return this instance, for chaining
     */
    public final FlexSwitch checkedState(boolean checkValue){
        this.switchBtn.setChecked(checkValue);
        return this;
    }

    /**
     * Registers the listener notified when the checked state changes.
     *
     * @param checkedChangeListener listener to notify, or {@code null} to clear the current one
     * @return this instance, for chaining
     */
    public final FlexSwitch onCheckedState(CompoundButton.OnCheckedChangeListener checkedChangeListener){
        this.switchBtn.setOnCheckedChangeListener(checkedChangeListener);
        return this;
    }
}
