// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import com.irisvision.flexrcoremodule.enums.FlexScope;

/**
 * The identity of an object across the three scopes — what makes a subscription releasable.
 *
 * <h2>What an identity is</h2>
 *
 * <p>A FlexR object — a view, a Fragment, an animation — belongs to a screen, and that screen belongs to
 * an Activity. Its identity is the pair "screen : activity", and {@link #ref(String)} returns the part
 * that the caller asks for:
 *
 * <pre>{@code
 * String fragment = identity.ref(FlexScope.FlexFragment.name());   // the screen part
 * String activity = identity.ref(FlexScope.FlexActivity.name());   // the Activity part
 * }</pre>
 *
 * <h2>Why it matters</h2>
 *
 * <p>This is how a provider knows <b>which</b> subscriptions to release. When a screen dies,
 * {@code FlexFragment} calls {@code unSubscribeFragScope(itsOwnId)}; every consumer whose
 * {@code ref(FlexScope.FlexFragment.name())} returns that same id is released, and the consumers of the
 * neighbouring screen are not. Without a stable identity per screen, a release would either miss
 * subscriptions or take the wrong ones.
 *
 * <h2>Implementing it</h2>
 *
 * <p>Implementations are expected to compute their identity lazily, on the first call to
 * {@link #identify()}, and to return an empty string — never {@code null} — when a part is not available
 * yet.
 */
public interface ScopeIdentity {

    /**
     * Computes the identity, once.
     *
     * <p>Called by {@link #ref(String)} before it answers, so that an object can be asked for its scope
     * as soon as it exists. Implementations must be idempotent.
     */
    void identify();

    /**
     * Returns the part of this identity that corresponds to the requested scope.
     *
     * @param scope one of the names of {@link FlexScope}
     * @return the identifier for that scope, or an empty string when it cannot be determined
     */
    String ref(String scope);
}
