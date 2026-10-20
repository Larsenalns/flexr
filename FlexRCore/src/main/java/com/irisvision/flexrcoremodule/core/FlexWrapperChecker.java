// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import com.irisvision.flexrcoremodule.structure.FlexStructureImpl;

/**
 * Recognises the bridges FlexR accepts — the closed list behind {@code FlexView.content(…)}.
 *
 * <p>A FlexR view is built on a native view, and only a few callers ever need to reach it: the
 * adapter of a list, the container of a screen block, the structure looper. Rather than exposing a
 * getter to everyone, the library accepts a bridge only when it belongs to one of those known kinds.
 *
 * <p>This class holds that knowledge in one place. It is the reason adding a new bridge is a
 * deliberate act: extend the check here, and every view that delegates to {@code content(…)} accepts it
 * at once.
 */
public class FlexWrapperChecker {

    private FlexWrapperChecker() {
        // Utility class: not meant to be instantiated.
    }

    /**
     * Tells whether this bridge is one of the kinds FlexR accepts.
     *
     * @param flexViewBridge the bridge to test
     * @return {@code true} for a screen block bridge or a view group bridge
     */
    public static boolean check(FlexViewBridge flexViewBridge){
        return FlexStructureImpl.chekWrapper(flexViewBridge) || FlexViewGroup.chekWrapper(flexViewBridge);
    }
}
