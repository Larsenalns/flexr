// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.view.ViewGroup;

/**
 * A recipient for the native group of a <b>Line</b> container.
 *
 * <p>The Line family ({@code FlexHBox}, {@code FlexVBox}, {@code FlexRadioGroup}) is laid out by native
 * components that have no closer common ancestor than {@link ViewGroup}: a {@code LinearLayoutCompat}, a
 * plain {@code LinearLayout} and a {@code RadioGroup} behave alike for FlexR, but do not share a
 * specific type. The bridge therefore hands over a {@code ViewGroup} and lets the container decide what
 * it does with it.
 *
 * <p>It is one of the accepted bridge kinds (see {@link FlexWrapperChecker}).
 */
public interface LineBridge {

    /**
     * Receives the native group of the container.
     *
     * @param viewGroup the {@code ViewGroup} backing this container
     */
    void bridge(ViewGroup viewGroup);
}
