// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.widget.FrameLayout;

/**
 * A recipient for the {@code FrameLayout} of a <b>Box</b> container.
 *
 * <p>FlexR containers are laid out by native components, and the Box family ({@code FlexBox},
 * {@code FlexCard}, {@code FlexContainer}) is built on a {@code FrameLayout}. This bridge is how a
 * container hands that view over — to a child being attached, or to the framework.
 *
 * <p>It is one of the accepted bridge kinds (see {@link FlexWrapperChecker}), which is what lets a
 * container be nested into another without exposing its native view publicly.
 */
public interface BoxBridge {

    /**
     * Receives the native view of the container.
     *
     * @param frameLayout the {@code FrameLayout} backing this container
     */
    void bridge(FrameLayout frameLayout);
}
