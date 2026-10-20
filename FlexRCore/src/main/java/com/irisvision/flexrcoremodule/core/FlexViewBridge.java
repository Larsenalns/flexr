// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.view.View;

/**
 * A recipient for the native view behind a FlexR view.
 *
 * <h2>Why a bridge instead of a getter</h2>
 *
 * <p>A FlexR view wraps an Android view — a {@code TextView}, a {@code FrameLayout}, a Material
 * component — and that native view is an <b>implementation detail</b>. Exposing it through a public
 * getter would invite callers to bind themselves to it, which would freeze the library's internals.
 *
 * <p>So the direction is reversed: the caller provides a bridge, and the view hands its native facade
 * over through it. A caller that only needs "the view" writes three lines and never depends on which
 * native component it is:
 *
 * <pre>{@code
 * FlexAdapter.FlexAdapterBridge bridge = new FlexAdapter.FlexAdapterBridge();
 * myFlexView.contentWrapper(bridge);       // the view calls bridge.bridge(nativeView)
 * View nativeView = bridge.view;
 * }</pre>
 *
 * <p>The set of accepted bridges is deliberately closed: a bridge of an unknown type is refused by
 * {@link FlexView#content(FlexViewBridge, View)}, so the library keeps control over who may unwrap
 * what. See {@link FlexWrapperChecker}.
 */
public interface FlexViewBridge {

    /**
     * Receives the native view.
     *
     * @param view the native Android view this FlexR view is built on
     */
    void bridge(View view);
}
