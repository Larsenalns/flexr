// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrconcurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A work-stealing pool, suited to computation-heavy work.
 *
 * <h2>Why work stealing</h2>
 *
 * <p>The size is derived by the runtime from the available cores, and work stealing balances the load
 * between threads: a thread that finishes its own queue takes work from the tail of another one instead
 * of staying idle. That matters for tasks of uneven cost — a few expensive items among many cheap ones
 * — where a fixed pool would leave cores idle with work still queued.
 *
 * <p>The thread count is clamped to a minimum of two, so a single-core device still has a pool with
 * one thread able to steal from the other. The threads are named by the runtime, not by
 * {@code FlexRThreadFactory}: a {@code ForkJoinPool} creates its threads itself.
 *
 * <h2>Threading</h2>
 *
 * <p>The pool is static and {@link #get()} may be called from any thread. Work stealing assumes tasks
 * do not block: a blocking task keeps its thread, and the pool cannot compensate for it. Use
 * {@link FlexThreadIO} for blocking calls.
 */
public class FlexThreadUnFixed {

    private static final ExecutorService UNFIXED =
            Executors.newWorkStealingPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 1));

    /**
     * Utility class: no instances.
     */
    private FlexThreadUnFixed() {
    }

    /**
     * Returns the shared work-stealing pool.
     *
     * <p>Public on purpose: while the member was {@code protected}, the pool was reachable only from
     * subclasses, which in practice meant from nowhere.
     *
     * @return the application-wide pool; never {@code null}
     */
    public static ExecutorService get(){
        return UNFIXED;
    }

    /**
     * Shuts the pool down after the tasks already submitted have run.
     *
     * <p>Static pool: the call applies to the whole application, and the pool cannot be recreated.
     */
    public static void shutdown(){
        UNFIXED.shutdown();
    }

    /**
     * Shuts the pool down at once, interrupting the tasks in progress.
     */
    public static void shutdownNow(){
        UNFIXED.shutdownNow();
    }

    /**
     * Whether the pool has been shut down.
     *
     * @return {@code true} once no new task can be accepted
     */
    public static boolean isShutdown(){
        return UNFIXED.isShutdown();
    }
}
