// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrconcurrency;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A named-thread factory for the FlexR pools.
 *
 * <p>Concrete value: in a profile or a crash trace, a thread named {@code FlexR-IO-2} is immediately
 * identifiable, while {@code pool-3-thread-1} says nothing. The cost is nil and the diagnostic gain is
 * real — the name is what turns "a background thread is blocked" into "the IO pool is saturated".
 */
final class FlexRThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger(1);

    /**
     * @param prefix prefix of every thread name, for instance {@code "FlexR-IO"}
     */
    FlexRThreadFactory(String prefix){
        this.prefix = prefix;
    }

    /**
     * Creates the next thread of the pool, named {@code prefix-N}.
     *
     * <p>Threads are deliberately <b>non-daemon</b>: a pool keeps the process alive while it holds work,
     * and is stopped explicitly through its {@code shutdown(...)} methods.
     *
     * @param runnable body the new thread must run
     * @return a fresh, non-started, non-daemon thread
     */
    @Override
    public Thread newThread(Runnable runnable){
        Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
        // Non-daemon threads: a pool must keep the process alive while it still holds work. The pools
        // are stopped explicitly through their shutdown(...) methods.
        thread.setDaemon(false);
        return thread;
    }
}
