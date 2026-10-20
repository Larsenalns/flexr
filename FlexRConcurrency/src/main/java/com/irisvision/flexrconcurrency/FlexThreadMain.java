// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrconcurrency;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * The entry point to the <b>main thread</b> (the UI thread).
 *
 * <h2>Why every screen needs it</h2>
 *
 * <p>An important reminder about the FlexR model: observer notifications are emitted from a
 * {@code ScheduledExecutorService} (see {@code FlexConsumer}), so they are <b>never</b> delivered on the
 * UI thread. Any code that touches a view, or a Fragment transaction, must therefore come back here.
 * {@link #runOnMain(Runnable)} does it for you, with no risk of running twice and no needless re-post.
 *
 * <h2>Two distinct uses</h2>
 *
 * <ul>
 *   <li>{@link #post(Runnable)} and {@link #runOnMain(Runnable)}: plain submission, for a
 *       {@code Runnable} whose result nobody waits for;</li>
 *   <li>{@link #submit(Callable)}: submission of a {@code Callable} that keeps the whole FlexR state
 *       tracking (success, error, cancellation). Used by
 *       {@code FlexDispatcherGlobalScope(DispatchLike.Main)}.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p>{@link #post(Runnable)} and {@link #submit(Callable)} may be called from any thread. <b>They do
 * not mean "run now"</b>: they mean "run on the next turn of the main loop". {@link #runOnMain(Runnable)}
 * is the one that runs immediately when the caller is already on the main thread.
 */
public final class FlexThreadMain {

    private static volatile Handler uiHandler;

    /**
     * Utility class: no instances.
     */
    private FlexThreadMain() {
    }

    /**
     * Returns the handler of the main thread, creating it on first use.
     *
     * <p>Created lazily because a {@code Handler} needs a {@code Looper}, and the class may be loaded
     * before the main loop exists. Cached afterwards: creating one per post would allocate on every
     * frame.
     *
     * @return the main thread handler
     */
    private static Handler handler(){
        Handler current = uiHandler;
        if (current == null) {
            current = new Handler(Looper.getMainLooper());
            uiHandler = current;
        }
        return current;
    }

    /**
     * Posts a runnable to the main thread.
     *
     * <p>Always deferred to the next turn of the main loop, even when the caller is already on the main
     * thread. Use {@link #runOnMain(Runnable)} when the immediate case matters.
     *
     * @param future work to run on the main thread
     */
    public static void post(Runnable future){
        handler().post(future);
    }

    /**
     * Whether the current call already runs on the main thread.
     *
     * @return {@code true} on the UI thread; also {@code false} in an environment without a main loop,
     *         such as a plain JVM unit test
     */
    public static boolean isMain(){
        Looper mainLooper = Looper.getMainLooper();
        return mainLooper != null && Thread.currentThread() == mainLooper.getThread();
    }

    /**
     * Runs {@code action} on the main thread: immediately when already there, otherwise by posting to
     * the main loop.
     *
     * <p>Prefer this method to {@link #post(Runnable)}: it avoids deferring by one turn of the loop a
     * piece of work that is already on the right thread, which can change the order of operations — a
     * navigation triggered by a user click is the classic case.
     *
     * @param action work to run on the main thread
     */
    public static void runOnMain(Runnable action){
        if (isMain()) {
            action.run();
        } else {
            post(action);
        }
    }

    /**
     * Runs a {@link Callable} on the main thread and returns its {@link FutureTask}.
     *
     * <p>This is what lets {@code DispatchLike.Main} keep the whole FlexR state tracking — success,
     * error, cancellation — instead of merely posting a {@code Runnable}. The body really runs on the
     * main thread, even when the submission comes from a background thread.
     *
     * @param callable body to run on the main thread
     * @param <T>      type of the produced value
     * @return the future of the posted task, cancellable before it starts
     * @throws IllegalStateException if no main thread is available — for instance in a plain JVM unit
     *                               test, where {@code Looper.getMainLooper()} is {@code null}
     */
    public static <T> FutureTask<T> submit(Callable<T> callable){
        Looper mainLooper = Looper.getMainLooper();
        if (mainLooper == null) {
            throw new IllegalStateException(
                    "No main thread (Looper) available: DispatchLike.Main cannot "
                            + "be used in this environment.");
        }

        FutureTask<T> task = new FutureTask<>(callable);

        // A task cancelled before the handler runs it must not be started at all.
        if (!task.isCancelled()) {
            handler().post(task);
        }
        return task;
    }
}
