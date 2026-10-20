// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrconcurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The pool meant for computation-heavy work — the <b>Default</b> pool.
 *
 * <h2>Why the size is bounded</h2>
 *
 * <p>A fixed pool gains nothing beyond a handful of threads. Reserving {@code cores - 1} threads makes
 * no sense on a 16- or 64-core machine: it immobilises stack memory and complicates scheduling, while
 * the extra threads only compete for the same cores. The pool therefore stays between 2 and 4 threads:
 * below that, a single-core device would get no parallelism at all; above it, nothing improves.
 *
 * <p>The clamp also keeps the one-core case explicit instead of relying on an implicit minimum:
 * {@code cores <= 2} uses {@code cores}, anything larger uses {@code cores - 1}, and both are then
 * clamped into {@code [2, 4]}.
 *
 * <h2>Threading</h2>
 *
 * <p>The pool is static: {@link #get()} may be called from any thread and always returns the same
 * instance. {@link #shutdown()} is definitive for the whole process, which is why it is aimed at tests.
 */
public class FlexThreadDefault {

    /** Floor: a single-core device still gets two threads, so a sequential task can overlap with a
     * short one. */
    private static final int MIN_THREADS = 2;
    /** Ceiling: beyond four computing threads, a phone spends more time scheduling than computing. */
    private static final int MAX_THREADS = 4;

    private static final ExecutorService DEFAULT;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        int size = Math.max(MIN_THREADS, Math.min(MAX_THREADS, cores <= 2 ? cores : cores - 1));
        DEFAULT = Executors.newFixedThreadPool(size, new FlexRThreadFactory("FlexR-Default"));
    }

    /**
     * Utility class: no instances.
     */
    private FlexThreadDefault() {
    }

    /**
     * Returns the shared Default pool, sized from the number of available processors.
     *
     * @return the application-wide computation pool; never {@code null}
     */
    public static ExecutorService get(){
        return DEFAULT;
    }

    /**
     * Shuts the pool down after the tasks already submitted have run.
     *
     * <p>The pool is static, so the call applies to the whole application: it becomes unusable for the
     * rest of the process, and cannot be recreated. Meant above all for JVM tests, which would otherwise
     * never terminate while non-daemon threads are alive.
     */
    public static void shutdown(){
        DEFAULT.shutdown();
    }

    /**
     * Shuts the pool down at once, interrupting the tasks in progress.
     */
    public static void shutdownNow(){
        DEFAULT.shutdownNow();
    }

    /**
     * Whether the pool has been shut down.
     *
     * @return {@code true} once no new task can be accepted
     */
    public static boolean isShutdown(){
        return DEFAULT.isShutdown();
    }
}
