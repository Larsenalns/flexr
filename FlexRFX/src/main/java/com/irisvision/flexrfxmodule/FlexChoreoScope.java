// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of the {@link FlexChoreo} frame loops, grouped by <b>scope</b> (screen or Activity), so that a
 * whole screen pauses, resumes and releases its choreographies in a single call.
 *
 * <h2>Why a registry exists</h2>
 *
 * <p>A choreography holds a frame callback posted on the {@code Choreographer} and an application
 * callback that usually writes into views. Both have to be detached when the screen leaves the
 * foreground or is destroyed, otherwise frames keep arriving for a view that no longer exists. Asking
 * the developer to keep a list of every choreography would duplicate that bookkeeping in every screen,
 * so the engine registers each one here, keyed by the scope identifier of its owner.
 *
 * <h2>The two scopes</h2>
 *
 * <p>Screen ({@code FlexFragment}) and Activity ({@code FlexActivity}) are kept in two independent
 * registries. The screen scope is released from {@code onDestroyView()}, the Activity scope from
 * {@code onDestroy()}: an animation that must survive a screen change belongs to the Activity scope.
 * The identifier is the {@code "<fragment>:<activity>"} pair produced by {@link AnimateScopeOwner}, and
 * only the part that matches the requested scope is compared — two fragments inside the same Activity
 * therefore remain independent.
 *
 * <h2>Concurrency</h2>
 *
 * <p>Both lists are {@link CopyOnWriteArrayList}. Registration happens from the thread that creates the
 * choreography, while a choreography can release itself from a frame, and both the fragment and the
 * Activity may be destroyed close together. Removal uses {@code removeIf} rather than a loop that
 * removes while iterating, which is what makes a purge safe when it runs from
 * {@code onDestroyView()} on a list of a single element.
 *
 * <p>Pause, resume and clear still <b>must be called on the main thread</b>: they hand work to the
 * {@code Choreographer}, which is confined to the thread that owns a {@code Looper}.
 */
public class FlexChoreoScope {
    private static final List<FlexChoreo> flexChoreoListFragScope = new CopyOnWriteArrayList<>();
    private static final List<FlexChoreo> flexChoreoListActivityScope = new CopyOnWriteArrayList<>();


    /**
     * Registers a choreography in the screen scope, so that the screen can pause, resume and release
     * it. Called by the engine when the choreography is created; application code has nothing to do
     * here.
     *
     * @param flexChoreo choreography to follow until its scope is cleared
     * @see FlexChoreo#FlexChoreo(FlexChoreo.ExecMode, boolean, int, AnimateScopeOwner)
     */
    public static void addToFlexFragmentScope(FlexChoreo flexChoreo){
        flexChoreoListFragScope.add(flexChoreo);
    }
    /**
     * Pauses every choreography of the screen.
     *
     * <p>The frame that discovers the pause is still executed, then the loop stops posting frames.
     * The pause window is measured, not accumulated as an absolute instant, so a screen that stays
     * paused for a while resumes where it stopped instead of jumping forward.
     *
     * @param scopeId identifier of the screen; a choreography whose identifier does not match is left
     *                untouched, and {@code null} matches nothing
     * @see FlexChoreo#pause()
     */
    public static void pauseFlexFragmentScope(String scopeId){
        for (FlexChoreo flexChoreo : flexChoreoListFragScope) {
            if (matches(flexChoreo, FlexAnimateScopeManager.SCOPE_FRAGMENT, scopeId)) {
                flexChoreo.pause();
            }
        }
    }
    /**
     * Resumes every choreography of the screen that was paused.
     *
     * <p>Resuming twice is harmless: only one frame is ever in flight, so the callback cannot be
     * delivered twice per frame.
     *
     * @param scopeId identifier of the screen, as given to the matching pause call
     * @see FlexChoreo#resume()
     */
    public static void resumeFlexFragmentScope(String scopeId){
        for (FlexChoreo flexChoreo : flexChoreoListFragScope) {
            if (matches(flexChoreo, FlexAnimateScopeManager.SCOPE_FRAGMENT, scopeId)) {
                flexChoreo.resume();
            }
        }
    }
    /**
     * Stops and forgets every choreography of the screen.
     *
     * <p>Called from the end of the screen's life. Each choreography is killed before being removed,
     * so no frame callback is left posted; a choreography that is already gone is simply skipped.
     *
     * @param scopeId identifier of the screen whose choreographies must be released
     * @see FlexChoreo#kill()
     */
    public static void clearFlexFragmentScope(String scopeId){
        flexChoreoListFragScope.removeIf(flexChoreo -> {
            if (!matches(flexChoreo, FlexAnimateScopeManager.SCOPE_FRAGMENT, scopeId)) {
                return false;
            }
            flexChoreo.kill();
            return true;
        });
    }


    /**
     * Registers a choreography in the Activity scope: it then survives a change of screen and is
     * released with the Activity itself.
     *
     * @param flexChoreo choreography to follow until the Activity scope is cleared
     */
    public static void addToFlexActivityScope(FlexChoreo flexChoreo){
        flexChoreoListActivityScope.add(flexChoreo);
    }
    /**
     * Pauses every choreography of the Activity.
     *
     * @param scopeId identifier of the Activity; {@code null} matches nothing
     * @see #pauseFlexFragmentScope(String)
     */
    public static void pauseFlexActivityScope(String scopeId){
        for (FlexChoreo flexChoreo : flexChoreoListActivityScope) {
            if (matches(flexChoreo, FlexAnimateScopeManager.SCOPE_ACTIVITY, scopeId)) {
                flexChoreo.pause();
            }
        }
    }
    /**
     * Resumes every choreography of the Activity that was paused.
     *
     * @param scopeId identifier of the Activity, as given to the matching pause call
     */
    public static void resumeFlexActivityScope(String scopeId){
        for (FlexChoreo flexChoreo : flexChoreoListActivityScope) {
            if (matches(flexChoreo, FlexAnimateScopeManager.SCOPE_ACTIVITY, scopeId)) {
                flexChoreo.resume();
            }
        }
    }

    /**
     * Stops and forgets every choreography of the Activity.
     *
     * @param scopeId identifier of the Activity whose choreographies must be released
     * @see #clearFlexFragmentScope(String)
     */
    public static void clearFlexActivityScope(String scopeId){
        flexChoreoListActivityScope.removeIf(flexChoreo -> {
            if (!matches(flexChoreo, FlexAnimateScopeManager.SCOPE_ACTIVITY, scopeId)) {
                return false;
            }
            flexChoreo.kill();
            return true;
        });
    }

    private static boolean matches(FlexChoreo flexChoreo, String scope, String scopeId){
        return scopeId != null && scopeId.equals(flexChoreo.ref(scope));
    }
}
