// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.widget.FrameLayout;

/**
 * A recipient for the {@code FrameLayout} that hosts a screen block.
 *
 * <p>A screen is built in two steps: the Fragment owns a container ({@code FlexBaseContainer}), and each
 * block — the app bar, the body — asks that container for a slot to draw into. This bridge is how the
 * slot is handed over.
 *
 * <p>The {@code String} identifies the slot. Two are used by the framework: {@code "w"} for the wrapper
 * that receives the whole screen, and {@code "c"} for the content slot a structure draws into. Naming the
 * slot rather than passing the container directly is what allows a screen to hold more than one block
 * without knowing the layout.
 */
public interface BaseContainerBridge {

    /**
     * Receives the hosting container.
     *
     * @param frameLayout the container
     * @param slot        the identifier of the slot this block draws into
     */
    void bridge(FrameLayout frameLayout, String slot);
}
