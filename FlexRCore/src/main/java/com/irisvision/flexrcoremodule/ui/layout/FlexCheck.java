// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.widget.CompoundButton;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;

/**
 * Material checkbox.
 *
 * <p>As with its siblings {@code FlexRadioButton} and {@code FlexSwitch}, the state is set with
 * {@code checkedState(...)} and the listener is registered with {@code onCheckedState(...)}.
 * Both return the instance so calls stay chainable with the rest of the API.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * new FlexCheck(flexContext)
 *         .text("I agree")
 *         .checkedState(true)
 *         .onCheckedState((button, isChecked) -> submit(isChecked));
 * }</pre>
 *
 * <p><b>Threading:</b> must be created and configured on the UI thread; the listener is invoked
 * on that thread by the Android framework.
 */
public class FlexCheck extends TextBase_ {
    private final MaterialCheckBox checkBox;

    /**
     * Creates the widget, its backing {@code MaterialCheckBox} and the bridge between the two.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public FlexCheck(FlexContextInterface flexContext) {
        super(flexContext);
        this.checkBox = new MaterialCheckBox(f.getFlexContext());
        initInnerBridge();
        textViewBridge();
    }

    /**
     * Attaches the given bridge to the backing check box.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.checkBox);
    }

    /**
     * Sets the checked state of the check box.
     *
     * @param checked {@code true} to check it, {@code false} to uncheck it
     * @return this instance, for chaining
     */
    public final FlexCheck checkedState(boolean checked){
        this.checkBox.setChecked(checked);
        return this;
    }

    /**
     * Registers the listener notified when the checked state changes.
     *
     * @param changeListener listener to notify, or {@code null} to clear the current one
     * @return this instance, for chaining
     */
    public final FlexCheck onCheckedState(CompoundButton.OnCheckedChangeListener changeListener){
        this.checkBox.setOnCheckedChangeListener(changeListener);
        return this;
    }
}
