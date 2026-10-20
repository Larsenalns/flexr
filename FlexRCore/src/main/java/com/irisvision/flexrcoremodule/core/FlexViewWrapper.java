// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.view.View;

/**
 * Implemented by a FlexR view that can hand its native facade to a bridge.
 *
 * <p>Every FlexR view has two interfaces: the one the developer uses — {@code text(…)},
 * {@code background(…)}, {@code boxParams(…)} — and the native Android view it is built on. This
 * interface is the second one, reduced to its minimum: given a {@link FlexViewBridge} and the native
 * view, bind the two together.
 *
 * <p>Implementations are one line long, and look like this:
 *
 * <pre>{@code
 * @Override
 * public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
 *     return content(flexViewBridge, this.textView);   // or this.host, this.list, this.pager...
 * }
 * }</pre>
 *
 * <p>The return value matters: {@code false} means the bridge was not one of the accepted kinds, and
 * the view then has no facade to offer — which {@code FlexView.initInnerBridge()} reports as an error
 * instead of letting a {@code NullPointerException} surface later.
 */
public interface FlexViewWrapper {

    /**
     * Hands the native view to the bridge, if the bridge is of an accepted kind.
     *
     * @param flexViewBridge the recipient
     * @param view           the native view this FlexR view is built on
     * @return {@code true} when the bridge was accepted and received the view
     */
    boolean content(FlexViewBridge flexViewBridge, View view);
}
