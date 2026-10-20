// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.widget.GridLayout;

/**
 * A recipient for the native {@code GridLayout} of a grid container.
 *
 * <p>{@code FlexGrid} arranges its children in rows and columns, which is exactly what a
 * {@code GridLayout} does. The bridge is how the container hands that view over — to a child being
 * attached, or to the framework.
 *
 * <p>It is one of the accepted bridge kinds (see {@link FlexWrapperChecker}).
 */
public interface GridBridge {

    /**
     * Receives the native view of the grid.
     *
     * @param gridLayout the {@code GridLayout} backing this container
     */
    void bridge(GridLayout gridLayout);
}
