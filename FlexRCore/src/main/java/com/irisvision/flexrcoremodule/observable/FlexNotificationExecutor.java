// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.observable;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The executor that delivers FlexR notifications to observers.
 *
 * <h2>Why a dedicated class</h2>
 *
 * <p>Notification delivery is a resource with a lifecycle, and a lifecycle does not belong in the
 * contract of an interface. This class keeps the executor private and exposes only what an application
 * or a test legitimately needs: submit a notification, stop, and reset. {@link FlexConsumer} — the
 * contract implemented by user code — stays free of any static field that user code could shut down
 * globally.
 *
 * <h2>One thread, deliberately</h2>
 *
 * <p>A single thread serialises notifications: they arrive in the order they were requested, and an
 * observer can never be re-entered by another notification. The price is that a slow observer delays
 * the following ones — an explicit trade-off: the library prefers delaying observers over blocking the
 * caller, which may be the main thread.
 *
 * <h2>Exceptions</h2>
 *
 * <p>Every notification is wrapped: an exception thrown by a consumer's
 * {@link FlexConsumer#notifyDataChanged(FlexProvider)} is logged and does not interrupt the queue. It
 * is never swallowed silently, and it never takes the whole application down.
 */
public final class FlexNotificationExecutor {

    private static final String TAG = "FlexR-Notify";

    /**
     * Notifications are scheduled with a zero delay, so that they go through the scheduler's
     * <i>delayed</i> queue. That queue is the one emptied by {@link #shutdownNow()}; the immediate
     * queue, by contract, still runs the tasks already accepted.
     */
    private static final long NO_DELAY = 0L;

    private static volatile ScheduledExecutorService executor = newExecutor();

    private FlexNotificationExecutor() {
        // Utility class: not meant to be instantiated.
    }

    private static ScheduledExecutorService newExecutor(){
        return new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "FlexR-Notify");
            thread.setDaemon(false);
            return thread;
        });
    }

    /**
     * Schedules a notification on the dedicated background thread.
     *
     * <p>Once the executor has been shut down, the notification is dropped and a warning is logged: an
     * application that shuts the executor down has decided to stop observing, and a
     * {@code RejectedExecutionException} thrown from a random place would be worse than that warning.
     *
     * @param task the work to run, usually the call to {@code notifyDataChanged}; {@code null} is ignored
     */
    static void submit(Runnable task){
        if (task == null) {
            return;
        }
        ScheduledExecutorService current = executor;
        if (current == null || current.isShutdown()) {
            Log.w(TAG, "Notification dropped: the notification executor is shut down.");
            return;
        }

        current.schedule(() -> {
            try {
                task.run();
            } catch (Throwable throwable) {
                // An exception thrown by one observer must not silence the others, and must not be lost.
                Log.e(TAG, "An exception was thrown while notifying an observer: " + throwable, throwable);
            }
        }, NO_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the executor once the notifications already scheduled have run.
     *
     * <p>Meant for application shutdown and for tests. After that, no notification is delivered any
     * more: {@link #submit(Runnable)} logs a warning instead.
     */
    public static void shutdown(){
        ScheduledExecutorService current = executor;
        if (current != null) {
            current.shutdown();
        }
    }

    /** Stops the executor immediately, abandoning the notifications still queued. */
    public static void shutdownNow(){
        ScheduledExecutorService current = executor;
        if (current != null) {
            current.shutdownNow();
        }
    }

    /** Tells whether the executor has been stopped. */
    public static boolean isShutdown(){
        ScheduledExecutorService current = executor;
        return current == null || current.isShutdown();
    }

    /** Installs a fresh executor. Meant for tests, so that each one starts from a clean state. */
    public static synchronized void reset(){
        ScheduledExecutorService current = executor;
        if (current != null && !current.isShutdown()) {
            current.shutdownNow();
        }
        executor = newExecutor();
    }
}
