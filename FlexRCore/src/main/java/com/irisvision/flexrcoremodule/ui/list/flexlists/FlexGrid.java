// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import android.view.View;
import android.widget.GridLayout;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.core.FlexViewGroup;

/**
 * Layout grid backed by an Android {@link GridLayout}.
 *
 * <p>The grid is itself a view group: children added through the FlexR API become cells of the
 * wrapped {@code GridLayout}. Row and column counts act as maxima, not as fixed sizes - the
 * platform grows the grid while children are added. The wrapped view carries a generated id so its
 * state can be saved and restored by the activity.
 *
 * <pre>{@code
 * new FlexGrid(flexContext)
 *         .columnCount(2)
 *         .vertical();
 * }</pre>
 *
 * <p>Threading: the class is not thread-safe. Construction and every builder method must run on
 * the UI thread that owns the grid.
 */
public class FlexGrid extends FlexViewGroup{

    private final GridLayout gridLayout;

    /**
     * Creates the grid and its backing {@code GridLayout}, and assigns a generated view id.
     *
     * @param f the FlexR context that owns the view and its activity
     */
    public FlexGrid(FlexContextInterface f) {
        super(f);
        this.gridLayout = new GridLayout(f.getFlexContext());
        this.gridLayout.setId(View.generateViewId());
        initInnerBridge();
    }

    /**
     * Attaches the underlying {@link GridLayout} to the FlexR content slot.
     *
     * @param flexViewBridge the bridge provided by the framework
     * @return {@code true} when the bridge accepted the content
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, gridLayout);
    }

    /**
     * Hands the underlying {@link GridLayout} to a caller-supplied receiver.
     *
     * <p>The receiver runs synchronously on the calling thread, so it can still change layout
     * parameters before the next layout pass.
     *
     * @param gridUnWrapper receiver that gets the wrapped {@code GridLayout}
     */
    public final void gridWrapper(GridUnWrapper gridUnWrapper){
            gridUnWrapper.bridge(this.gridLayout);
    }

    /**
     * Sets the maximum number of columns of the grid.
     *
     * @param column maximum column count, or {@code 0} to let the grid derive it from its children
     * @return {@code this}, to allow chaining
     */
    public final FlexGrid columnCount(int column){
        this.gridLayout.setColumnCount(column);
        return this;
    }

    /**
     * Sets the maximum number of rows of the grid.
     *
     * @param row maximum row count, or {@code 0} to let the grid derive it from its children
     * @return {@code this}, to allow chaining
     */
    public final FlexGrid rowCount(int row){
        this.gridLayout.setRowCount(row);
        return this;
    }

    /**
     * Switches the grid to vertical flow ({@link GridLayout#VERTICAL}).
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexGrid vertical(){
        this.gridLayout.setOrientation(GridLayout.VERTICAL);
        return this;
    }

    /**
     * Switches the grid to horizontal flow ({@link GridLayout#HORIZONTAL}).
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexGrid horizontal(){
        this.gridLayout.setOrientation(GridLayout.HORIZONTAL);
        return this;
    }
}
