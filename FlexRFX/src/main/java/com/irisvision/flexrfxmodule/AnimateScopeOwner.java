// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

/**
 * Something that can tell which screen and which Activity it belongs to.
 *
 * <p>This is the only piece of identity the animation engine needs: it is what allows an animation to
 * be registered, paused and released at the right moment, and it is why a developer never has to build
 * a scope string by hand.
 *
 * <p>In a FlexR application, the objects that implement this interface are the ones already at hand:
 * <ul>
 *   <li>a {@code FlexFragment} answers for {@link #FRAGMENT_SCOPE} — and so does any view living inside
 *       a screen, which is convenient when the call site is a screen block rather than the Fragment;</li>
 *   <li>a {@code FlexActivity} answers for {@link #ACTIVITY_SCOPE}.</li>
 * </ul>
 *
 * <p>It lives in this module, and not in the core, because the dependency goes one way only: the core
 * depends on the animation module, never the opposite. The scope names below are therefore a
 * <b>contract</b> with the core's {@code FlexScope} enum: they must match it exactly.
 */
public interface AnimateScopeOwner {

    /** Scope released with the screen. Must match the core's {@code FlexScope.FlexFragment}. */
    String FRAGMENT_SCOPE = "FlexFragment";

    /** Scope released with the Activity. Must match the core's {@code FlexScope.FlexActivity}. */
    String ACTIVITY_SCOPE = "FlexActivity";

    /**
     * Returns the part of this owner's identity that corresponds to the requested scope.
     *
     * @param scope {@link #FRAGMENT_SCOPE} or {@link #ACTIVITY_SCOPE}
     * @return the identifier for that scope, or an empty string when it cannot be determined; never
     *         {@code null}
     */
    String ref(String scope);

    /**
     * Builds the {@code "<fragment>:<activity>"} identifier of an owner.
     *
     * <p>Callers never write this string themselves: they pass the object they already hold, and the
     * identifier is derived here.
     *
     * @param owner the Fragment, the Activity, or any view living inside a screen
     * @return the identifier, built from both parts of the owner's identity
     * @throws IllegalArgumentException if {@code owner} is {@code null}
     * @throws IllegalStateException    if the owner cannot identify its screen — which happens when the
     *                                 engine is created before the owner is attached
     */
    static String scopeIdOf(AnimateScopeOwner owner) {
        if (owner == null) {
            throw new IllegalArgumentException(
                    "owner == null : pass the FlexFragment, the FlexActivity, or a view living inside the screen.");
        }

        String fragment = owner.ref(FRAGMENT_SCOPE);
        String activity = owner.ref(ACTIVITY_SCOPE);

        if (fragment == null || fragment.isEmpty()) {
            throw new IllegalStateException(owner.getClass().getName()
                    + " cannot identify its screen: its ref(\"" + FRAGMENT_SCOPE
                    + "\") is empty. Create the animation engine once the owner is attached to a screen.");
        }

        return fragment + ":" + (activity == null ? "" : activity);
    }
}
