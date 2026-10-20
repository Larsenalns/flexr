// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The queue of work to run at the instant a screen's container becomes visible.
 *
 * <h2>Why defer at all</h2>
 *
 * <p>Some wiring only makes sense once the view really exists on screen: attaching listeners, starting
 * an animation, measuring against the final size. Running it earlier would run it against a view that is
 * not laid out yet, or not attached.
 *
 * <h2>Why event-driven rather than a wait loop</h2>
 *
 * <p>The owning Fragment triggers consumption in its visible pass — there is no polling, no sleeping
 * thread, and therefore no cost when nothing happens. A screen that never becomes visible costs nothing,
 * and a screen that becomes visible runs its deferred work exactly once.
 *
 * <h2>One queue per scope</h2>
 *
 * <p>The registry is keyed by scope identity, not a single global list. That is what makes the queue
 * correct when a screen registers work and is destroyed <b>before</b> its first visible pass: the work
 * stays attached to that screen's identity, {@link #loop(String)} consumes only its own queue, and
 * {@link #clear(String)} throws it away instead of letting the <b>next</b> screen run work against dead
 * views.
 *
 * <h2>Threading</h2>
 *
 * <p>Registration may come from a background thread — that is where observer notifications are delivered —
 * while consumption happens on the main thread. Both collections used here are concurrency-safe, and
 * {@link #loop(String)} removes the queue from the registry <b>before</b> running it: no other pass can
 * consume it, and an action that registers again as it runs does not end up inside the queue currently
 * being iterated.
 */
public final class FlexStructureLooper {

    private static final Map<String, List<Runnable>> PENDING = new ConcurrentHashMap<>();

    private FlexStructureLooper() {
        // Utility class: not meant to be instantiated.
    }

    /**
     * Defers a treatment until the visible pass of the given scope.
     *
     * @param scope  identity of the owning screen (see {@link ScopeIdentity#ref(String)})
     * @param action the work to run; {@code null} is ignored
     */
    public static void run(String scope, Runnable action){
        if (scope == null || scope.isEmpty() || action == null) {
            return;
        }
        PENDING.computeIfAbsent(scope, key -> new CopyOnWriteArrayList<>()).add(action);
    }

    /**
     * Runs, then discards, the treatments pending for the given scope.
     *
     * @param scope identity of the owning screen
     */
    public static void loop(String scope){
        if (scope == null || scope.isEmpty()) {
            return;
        }
        List<Runnable> queue = PENDING.remove(scope);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        for (Runnable action : queue) {
            action.run();
        }
    }

    /**
     * Discards the pending treatments of a scope without running them.
     *
     * <p>Called when a screen's view is destroyed: the work it had deferred will never have a screen to
     * run on, and keeping it would both delay the release and hold a dead view alive.
     *
     * @param scope identity of the destroyed screen; {@code null} is ignored
     */
    public static void clear(String scope){
        if (scope == null) {
            return;
        }
        PENDING.remove(scope);
    }

    /**
     * Number of scopes still holding pending work.
     *
     * <p>Diagnostic only — the cheapest way for a test, or for an in-app leak hunt, to check that
     * destroyed screens left nothing behind.
     */
    public static int pendingScopes(){
        return PENDING.size();
    }
}
