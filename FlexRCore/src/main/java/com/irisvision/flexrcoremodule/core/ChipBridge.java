// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import com.google.android.material.chip.ChipGroup;

/**
 * A recipient for the native {@code ChipGroup} of a chip container.
 *
 * <p>{@code FlexChipGroup} groups chips, and the Material {@code ChipGroup} already does the job: it
 * lays the chips out, wraps them, and enforces single or multiple selection. The bridge is how the
 * container hands that view over — to a chip being attached, or to the framework.
 *
 * <p>It is one of the accepted bridge kinds (see {@link FlexWrapperChecker}).
 */
public interface ChipBridge {

    /**
     * Receives the native view of the chip group.
     *
     * @param chipGroup the {@code ChipGroup} backing this container
     */
    void bridge(ChipGroup chipGroup);
}
