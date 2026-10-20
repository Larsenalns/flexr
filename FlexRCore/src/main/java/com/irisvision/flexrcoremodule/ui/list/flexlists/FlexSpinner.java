// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapter;

/**
 * Drop-down list backed by an Android {@link Spinner}.
 *
 * <p>The wrapped {@code Spinner} is created eagerly by the constructor and is exposed to FlexR
 * through {@link #contentWrapper(FlexViewBridge)}. Every builder method below mutates that live
 * instance and returns {@code this}, so calls can be chained:
 *
 * <pre>{@code
 * FlexSpinner spinner = new FlexSpinner(flexContext)
 *         .dropDownDefault(new String[]{"One", "Two"})
 *         .onItemSelected(listener);
 * }</pre>
 *
 * <p>Threading: the class is not thread-safe. Construction and every builder method must run on
 * the UI thread that owns the {@code Spinner}; the registered listeners are invoked on that same
 * thread.
 */
public class FlexSpinner extends FlexView {

    private final Spinner spinner;

    /**
     * Creates a drop-down spinner.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     */
    public FlexSpinner(FlexContextInterface flexContext) {
        this(flexContext, null);
    }

    /**
     * Creates a spinner with an explicit presentation mode.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     * @param spinnerMode {@code "dialog"} or {@code "dropdown"}, case-insensitive; {@code null}
     *                    or a blank value selects the platform default, which is drop-down
     * @throws IllegalArgumentException if the value is neither blank, {@code "dialog"} nor
     *                                  {@code "dropdown"}
     */
    public FlexSpinner(FlexContextInterface flexContext, String spinnerMode) {
        super(flexContext);
        this.spinner = new Spinner(f.getFlexContext(), mode(spinnerMode));
        initInnerBridge();
    }

    /**
     * Attaches the underlying {@link Spinner} to the FlexR content slot.
     *
     * @param flexViewBridge the bridge provided by the framework
     * @return {@code true} when the bridge accepted the content
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.spinner);
    }

    /**
     * Resolves the platform mode constant for a caller-supplied mode name.
     *
     * @param spinnerMode mode name, case-insensitive, possibly {@code null}
     * @return {@link Spinner#MODE_DIALOG} or {@link Spinner#MODE_DROPDOWN}
     * @throws IllegalArgumentException if the name is not a supported mode
     */
    private int mode(String spinnerMode){
        if (spinnerMode == null || spinnerMode.trim().isEmpty()) {
            return Spinner.MODE_DROPDOWN;                   // platform default mode
        }
        if (spinnerMode.equalsIgnoreCase("dialog")) {
            return Spinner.MODE_DIALOG;
        }
        if (spinnerMode.equalsIgnoreCase("dropdown")) {
            return Spinner.MODE_DROPDOWN;
        }
        throw new IllegalArgumentException(
                "Unknown Spinner mode: \"" + spinnerMode + "\". Use \"dialog\" or \"dropdown\".");
    }

    /**
     * Installs a simple one-line drop-down item layout filled with {@code data}.
     *
     * <p>The adapter is replaced, so a previously installed adapter is discarded together with the
     * selection state it carried.
     *
     * @param data the entries to display; the array is referenced, not copied
     * @return {@code this}, to allow chaining
     */
    public final FlexSpinner dropDownDefault(String[] data){
        this.spinner.setAdapter(new ArrayAdapter<>(
                f.getFlexContext(),
                android.R.layout.simple_dropdown_item_1line,
                data
        ));
        return this;
    }

    /**
     * Installs an arbitrary {@link BaseAdapter}.
     *
     * @param adapter the adapter to install; {@code null} clears the current adapter
     * @return {@code this}, to allow chaining
     */
    public final FlexSpinner adapter(BaseAdapter adapter){
        this.spinner.setAdapter(adapter);
        return this;
    }

    /**
     * Installs a FlexR list adapter, which is a {@link BaseAdapter} as well.
     *
     * @param adapter the FlexR adapter to install
     * @return {@code this}, to allow chaining
     */
    public final FlexSpinner dropDown(FlexAdapter adapter){
        return adapter(adapter);
    }

    /**
     * Sets the prompt text shown by the dialog presentation.
     *
     * @param promptText the prompt to display; {@code null} clears it
     * @return {@code this}, to allow chaining
     */
    public final FlexSpinner promptText(String promptText){
        this.spinner.setPrompt(promptText);
        return this;
    }

    /**
     * Registers the selection listener.
     *
     * @param onItemSelectedListener listener notified whenever the selection changes; {@code null}
     *                               clears the current listener
     * @return {@code this}, to allow chaining
     */
    public final FlexSpinner onItemSelected(AdapterView.OnItemSelectedListener onItemSelectedListener){
        this.spinner.setOnItemSelectedListener(onItemSelectedListener);
        return this;
    }

    /**
     * Registers a click listener on the list items; the platform only honours it in drop-down
     * presentation.
     *
     * @param onItemClickListener listener notified when an item is clicked
     * @return {@code this}, to allow chaining
     */
    public final FlexSpinner onItemClicked(AdapterView.OnItemClickListener onItemClickListener){
        this.spinner.setOnItemClickListener(onItemClickListener);
        return this;
    }

    /**
     * Registers a long-click listener on the list items; the platform only honours it in drop-down
     * presentation.
     *
     * @param onItemLongClickListener listener notified when an item is long-clicked
     * @return {@code this}, to allow chaining
     */
    public final FlexSpinner onItemLongClick(AdapterView.OnItemLongClickListener onItemLongClickListener){
        this.spinner.setOnItemLongClickListener(onItemLongClickListener);
        return this;
    }
}
