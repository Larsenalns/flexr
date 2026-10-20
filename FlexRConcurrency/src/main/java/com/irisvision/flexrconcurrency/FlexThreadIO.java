// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrconcurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The pool dedicated to I/O: files, network, database.
 *
 * <h2>Why not {@code Executors.newCachedThreadPool()}</h2>
 *
 * <p>A cached pool creates one thread per task when needed, with no real ceiling. A burst of I/O — two
 * hundred requests fired at once — can therefore create two hundred threads. Each thread costs about
 * 1 MB of stack, and above all Android caps the number of threads per process: exceeding it ends in
 * {@code OutOfMemoryError: unable to create native thread}, an error hard to relate to its cause and
 * which hits precisely the pool that is used the most.
 *
 * <h2>What is kept</h2>
 *
 * <p>The {@link SynchronousQueue} reproduces the "on demand" behaviour of a cached pool: a thread is
 * created when none is free, instead of parking the task behind an undersized fixed pool. Idle threads
 * are reclaimed after sixty seconds, so a quiet application does not keep a large pool alive.
 *
 * <h2>What changes</h2>
 *
 * <p>The size is capped, and the threads carry an identifiable name. Beyond the cap, submission is
 * refused with an explicit {@code RejectedExecutionException} (the standard policy) instead of letting
 * the process grow until it crashes.
 *
 * <h2>Threading</h2>
 *
 * <p>The pool is created once, statically, and shared by the whole application:
 * {@link #get()} may be called from any thread. {@link #shutdown()} and {@link #shutdownNow()} are
 * definitive — a shut-down pool cannot be restarted — and are meant for tests, which would otherwise
 * never terminate because the threads are non-daemon.
 */
public class FlexThreadIO {

    /**
     * Thread ceiling: generous for I/O, but bounded. Two per available processor keeps a burst of
     * blocking calls from being serialised, while staying far from the process-wide limit.
     */
    private static final int MAX_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

    private static final ExecutorService IO;
    private static final ThreadPoolExecutor IO_IMPL;

    static {
        IO_IMPL = new ThreadPoolExecutor(
                0,                                  // no permanent thread
                MAX_THREADS,                        // explicit ceiling
                60L, TimeUnit.SECONDS,              // idle threads are reused, then released
                new SynchronousQueue<>(),           // hand-off, with no waiting queue
                new FlexRThreadFactory("FlexR-IO"));
        IO = IO_IMPL;
    }

    /**
     * Utility class: no instances.
     */
    private FlexThreadIO() {
    }

    /**
     * Returns the shared I/O pool.
     *
     * @return the application-wide pool; never {@code null}
     */
    public static ExecutorService get(){
        return IO;
    }

    /**
     * @return the maximum number of threads this pool will ever run at once
     */
    public static int maxThreads(){
        return MAX_THREADS;
    }

    /**
     * Shuts the pool down after the tasks already submitted have run.
     *
     * <p>The pool is static, so this applies to the whole application: it can no longer accept work
     * afterwards. Mainly useful for JVM tests, which would otherwise not terminate.
     */
    public static void shutdown(){
        IO.shutdown();
    }

    /**
     * Shuts the pool down at once, interrupting the tasks in progress.
     *
     * <p>Those tasks see an {@code InterruptedException}; a task that swallows it keeps running, so this
     * is a request, not a guarantee.
     */
    public static void shutdownNow(){
        IO.shutdownNow();
    }

    /**
     * Whether the pool has been shut down, either gracefully or immediately.
     *
     * @return {@code true} once no new task can be accepted
     */
    public static boolean isShutdown(){
        return IO.isShutdown();
    }

    /**
     * Number of threads currently running a task. Diagnostic only: the value is an instant sample, and
     * the executor does not guarantee it stays exact.
     *
     * @return an approximation of the busy thread count
     */
    public static int activeCount(){
        return IO_IMPL.getActiveCount();
    }
}
