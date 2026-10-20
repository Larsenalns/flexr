// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.widget.TextView;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;

/**
 * Plain text widget backed by a {@code TextView}.
 *
 * <p>Theme handling (text color, hint color, background, shape) is provided by
 * {@link TextBase_} and {@code FlexView}; this class therefore does not override
 * {@code ifThemeChange()}. Colors set through {@link TextBase_#textColor(int)} and
 * {@link TextBase_#hintColor(int)} are re-applied by the base class on every theme switch.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * new FlexText(flexContext).text("Title").textColor(12).textSize(18f);
 * }</pre>
 *
 * <p><b>Threading:</b> instantiating and configuring this widget touches a live
 * {@code TextView}, so it must happen on the UI thread.
 */
public class FlexText extends TextBase_ {
    private final TextView textView;

    /**
     * Creates the widget, its backing {@code TextView} and the bridge between the two.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public FlexText(FlexContextInterface flexContext) {
        super(flexContext);
        this.textView = new TextView(f.getFlexContext());
        initInnerBridge();
        textViewBridge();
    }

    /**
     * Attaches the given bridge to the backing {@code TextView}.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.textView);
    }
}
