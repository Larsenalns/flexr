// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

/**
 * Something that can initialise itself.
 *
 * <p>A deliberately tiny contract: it exists so that initialisation can be deferred and handed around
 * without the library knowing what is being initialised. {@link FlexInit#init(Init_)} is the only
 * caller, and it is what the demo code uses to declare providers, consumers or state before a screen is
 * built.
 *
 * <p>It is <b>not</b> the initialisation hook of a screen — that is {@code FlexStructure}'s
 * {@code initializer_()}, which orchestrates {@code dynamicBuild()}, {@code attachEvents()} and
 * {@code initialBusinessLogic()}.
 */
public interface Init_ {

    /** Runs the initialisation. Called once, by {@link FlexInit#init(Init_)}. */
    void init();
}
