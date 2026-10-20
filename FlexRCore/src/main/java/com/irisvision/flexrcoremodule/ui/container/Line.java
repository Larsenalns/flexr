// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import android.view.ViewGroup;
import com.irisvision.flexrcoremodule.core.LineBridge;

/**
 * Line container contract: the native view owned by a container whose children sit in a single row
 * or column, exposed so that the framework can attach children to it.
 *
 * <p>A {@code FlexViewGroup} implements this interface to declare that it owns a line-like native
 * {@code ViewGroup}. {@code FlexViewGroup.add(FlexView)} relies on that declaration: for a
 * {@link LineImpl} container it calls {@code lineWrapper(...)} and attaches the child only when the
 * returned value is {@code true}.
 *
 * <p>The native view is typed {@link ViewGroup} rather than {@code LinearLayout} on purpose:
 * {@code LinearLayoutCompat}, {@code LinearLayout} and {@code RadioGroup} share no closer common
 * ancestor, so {@link ViewGroup} is the narrowest type that covers all three.
 *
 * @see LineImpl
 */
public interface Line {

    /**
     * Exposes the native container view to {@code lineBridge}, which stores it and acts on it.
     *
     * <p>{@link LineImpl} implements the contract: the bridge is accepted only when it is a
     * {@code FlexViewGroup.LineUnWrapper}, and the answer is {@code false} otherwise. A {@code false}
     * answer is final - the caller must not attach a child, because no native view was reached and
     * the child would have no parent.
     *
     * <p>Must be called on the UI thread: the method itself only hands a reference over, but the
     * caller uses that reference to add or remove native views, which is UI-thread only.
     *
     * @param lineBridge   bridge that receives the native container view
     * @param linearLayout native view backing this container
     * @return {@code true} when the bridge accepted the view, {@code false} when the bridge kind is
     *         not supported by this container
     */
    boolean content(LineBridge lineBridge, ViewGroup linearLayout);
}
