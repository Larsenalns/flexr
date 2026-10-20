// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

/**
 * Runs an {@link Init_} — the smallest possible way to say "run this initialisation now".
 *
 * <pre>{@code
 * FlexInit.init(() -> FlexReactive.declare(List.of(new SessionProvider())));
 * }</pre>
 *
 * <p>The indirection is useful when initialisation must stay a value: it can be built in one place,
 * stored, and executed later, without the library depending on what the code does. Everything heavier —
 * building a screen tree, wiring events — belongs to {@code FlexStructure}, not here.
 */
public final class FlexInit{

    private FlexInit() {
        // Utility class: not meant to be instantiated.
    }

    /**
     * Runs the given initialisation.
     *
     * @param init_ the initialisation to run; must not be {@code null}
     */
    public static void init(Init_ init_){
        init_.init();
    }
}
