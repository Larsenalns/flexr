// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of the {@link FlexAnimate} engines, grouped by <b>scope</b> (screen or Activity): pause,
 * resume and release a whole screen's animations without tracking them individually.
 *
 * <h2>Lifecycle</h2>
 *
 * <p>An engine registers itself when it is created, against the scope of its owner. When the screen
 * goes to the background, {@code pauseFlexFragmentScope} freezes every animation of that screen; when
 * it is destroyed, {@code clearFlexFragmentScope} cancels them and drops every reference they hold,
 * which is what keeps a disposed view reachable otherwise.
 *
 * <h2>The two scopes</h2>
 *
 * <p>The screen and the Activity scopes are separate lists. The identifier comes from
 * {@link AnimateScopeOwner} as a {@code "<fragment>:<activity>"} pair, and each call compares only the
 * part that belongs to its own scope. The scope names themselves are taken from
 * {@link AnimateScopeOwner} rather than from the core: this module must not depend on the core, the
 * dependency goes the other way, and the core's {@code FlexScope} enum uses the same two values.
 *
 * <h2>Concurrency</h2>
 *
 * <p>Both lists are {@link CopyOnWriteArrayList}, for the same reason as in
 * {@link FlexChoreoScope}: registration happens from the thread that creates the engine, while pause,
 * resume and clear arrive from the lifecycle of the screen — and clearing iterates while each engine
 * cancels its animators. Removal goes through {@code removeIf}, so a purge cannot fail with a
 * {@code ConcurrentModificationException} in the middle of {@code onDestroyView()}.
 *
 * <p>The calls themselves must be made on the main thread, because the animations they drive write into
 * views.
 */
public class FlexAnimateScopeManager {

    /**
     * Scope identifiers, taken from {@link AnimateScopeOwner}: they are the contract with the core's
     * {@code FlexScope} enum, which this module must not depend on — the dependency goes the other way.
     */
    static final String SCOPE_FRAGMENT = AnimateScopeOwner.FRAGMENT_SCOPE;
    static final String SCOPE_ACTIVITY = AnimateScopeOwner.ACTIVITY_SCOPE;

    private static final List<FlexAnimate> flexAnimateListFragScope = new CopyOnWriteArrayList<>();
    private static final List<FlexAnimate> flexAnimateListActivityScope = new CopyOnWriteArrayList<>();

    /**
     * Registers an engine in the screen scope. Called by the engine's own constructor; application
     * code never calls it.
     *
     * @param flexAnimate engine to follow until the screen is destroyed
     */
    protected static void addToFlexFragmentScope(FlexAnimate flexAnimate){
        flexAnimateListFragScope.add(flexAnimate);
    }
    /**
     * Pauses every animation of the screen, keeping them at their current position.
     *
     * <p>Meant for {@code onPause()}: the animations stop consuming frames while the screen is not
     * visible, and resume from where they stopped.
     *
     * @param scopeId identifier of the screen; a {@code null} value matches nothing
     * @see FlexAnimate#pauseAllFlexAnimations()
     */
    public static void pauseFlexFragmentScope(String scopeId){
        for (FlexAnimate flexAnimate : flexAnimateListFragScope) {
            if (matches(flexAnimate, SCOPE_FRAGMENT, scopeId)) {
                flexAnimate.pauseAllFlexAnimations();
            }
        }
    }
    /**
     * Resumes every animation of the screen that was paused.
     *
     * @param scopeId identifier of the screen, as given to the matching pause call
     * @see FlexAnimate#resumeAllFlexAnimations()
     */
    public static void resumeFlexFragmentScope(String scopeId){
        for (FlexAnimate flexAnimate : flexAnimateListFragScope) {
            if (matches(flexAnimate, SCOPE_FRAGMENT, scopeId)) {
                flexAnimate.resumeAllFlexAnimations();
            }
        }
    }

    /**
     * Cancels every animation of the screen and removes its engine from the registry.
     *
     * <p>Called at the end of the screen's life. Each engine cancels its animators, drops their
     * listeners and clears the references it held to views — those are precisely the references that
     * would keep a disposed view alive. The removal is done by {@code removeIf}, so one engine
     * finishing during the purge cannot break the iteration.
     *
     * @param scopeId identifier of the screen to release
     * @see FlexAnimate#killAllFlexAnimations()
     */
    public static void clearFlexFragmentScope(String scopeId){
    /**
     * Which registry an engine belongs to: the screen (released at the end of the view's life) or the
     * Activity (released later, so animations can survive a change of screen).
     */
        flexAnimateListFragScope.removeIf(flexAnimate -> {
            if (!matches(flexAnimate, SCOPE_FRAGMENT, scopeId)) {
                return false;
            }
            flexAnimate.killAllFlexAnimations();
            return true;
        });
    }

    /**
     * Registers an engine in the Activity scope: its animations outlive the screen and are released
     * with the Activity.
     *
     * @param flexAnimate engine to follow until the Activity is destroyed
     */
    protected static void addToFlexActivityScope(FlexAnimate flexAnimate){
        flexAnimateListActivityScope.add(flexAnimate);
    }
    /**
     * Pauses every animation registered in the Activity scope.
     *
     * @param scopeId identifier of the Activity; {@code null} matches nothing
     */
    public static void pauseFlexActivityScope(String scopeId){
        for (FlexAnimate flexAnimate : flexAnimateListActivityScope) {
            if (matches(flexAnimate, SCOPE_ACTIVITY, scopeId)) {
                flexAnimate.pauseAllFlexAnimations();
            }
        }
    }
    /**
     * Resumes every animation of the Activity scope that was paused.
     *
     * @param scopeId identifier of the Activity, as given to the matching pause call
     */
    public static void resumeFlexActivityScope(String scopeId){
        for (FlexAnimate flexAnimate : flexAnimateListActivityScope) {
            if (matches(flexAnimate, SCOPE_ACTIVITY, scopeId)) {
                flexAnimate.resumeAllFlexAnimations();
            }
        }
    }

    /**
     * Cancels every animation of the Activity scope and removes its engine from the registry.
     *
     * <p>Runs later than the screen purge: anything that must survive a screen change is released
     * here, when the Activity itself goes away.
     *
     * @param scopeId identifier of the Activity to release
     */
    public static void clearFlexActivityScope(String scopeId){
        flexAnimateListActivityScope.removeIf(flexAnimate -> {
            if (!matches(flexAnimate, SCOPE_ACTIVITY, scopeId)) {
                return false;
            }
            flexAnimate.killAllFlexAnimations();
            return true;
        });
    }

    private static boolean matches(FlexAnimate flexAnimate, String scope, String scopeId){
        return scopeId != null && scopeId.equals(flexAnimate.ref(scope));
    }

    public enum FlexAnimateScope{
        FlexFragment, FlexActivity
    }
}
