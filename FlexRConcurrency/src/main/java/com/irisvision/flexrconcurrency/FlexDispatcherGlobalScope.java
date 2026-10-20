// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrconcurrency;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * The launch point for asynchronous tasks, in the spirit of a coroutine scope.
 *
 * <p>The pool is chosen by the {@link DispatchLike} given at construction — {@code IO}, {@code Unfixed},
 * {@code Default} or {@code Main}. Whatever the choice, the FlexR state tracking is kept:
 * {@link FlexTask#success()}, {@link FlexTask#exception()}, {@link FlexTask#canceled()} and
 * {@link FlexTask#invokeOnComplete} behave the same way, so switching a task from one pool to another
 * changes where it runs, not how it is observed.
 *
 * <p>A scope is also a {@link FlexTaskTracker}: it knows whether it still has work in flight
 * ({@link #tasksRunning()}) and can cancel all of it at once ({@link #cancelAll()}), which is what a
 * screen does when it goes away.
 *
 * <h2>Example — work on the main thread</h2>
 *
 * <pre>{@code
 * FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.Main);
 * scope.launch(() -> {
 *     view.text("hello");        // safe: this runs on the main thread
 *     return "done";
 * }).invokeOnComplete(task -> publish(task));
 * }</pre>
 *
 * <h2>Example — blocking work</h2>
 *
 * <pre>{@code
 * FlexDispatcherGlobalScope io = new FlexDispatcherGlobalScope(DispatchLike.IO);
 * io.launch(() -> repository.load(id))                 // off the main thread
 *   .invokeOnComplete(task -> FlexThreadMain.runOnMain(() -> render(task)));
 * }</pre>
 *
 * <h2>Threading</h2>
 *
 * <p>{@link #launch(Callable)} may be called from any thread. With {@code DispatchLike.Main} the task
 * body itself runs on the main thread, so it must not block; the other pools run it on a background
 * thread, and the completion callback then runs there too — bring it back with
 * {@link FlexThreadMain#runOnMain(Runnable)} before touching a view.
 */
public class FlexDispatcherGlobalScope extends FlexTaskTracker {

    private final DispatchLike dispatchLike;

    /**
     * Creates a scope that launches its tasks on the given kind of thread.
     *
     * @param dispatchLike where the tasks must run: {@code IO} for blocking calls, {@code Unfixed} or
     *                     {@code Default} for computation, {@code Main} for work that must touch views
     * @throws NullPointerException if {@code dispatchLike} is {@code null}
     */
    public FlexDispatcherGlobalScope(DispatchLike dispatchLike){
        this.dispatchLike = Objects.requireNonNull(dispatchLike, "dispatchLike == null");
    }

    /**
     * Submits a task and returns its handle immediately.
     *
     * <p>The task is already running — or already finished — when this method returns, so the
     * completion callback must be registered without delay: it is delivered immediately if the task is
     * done, which is precisely why the state machine of {@link FlexTask} exists.
     *
     * @param callable body of the task; its value becomes the result of the returned handle
     * @param <T>      type of the value produced by the task
     * @return a handle to observe, chain a callback on, or cancel
     * @throws NullPointerException if {@code callable} is {@code null}
     */
    public final <T> FlexTask<T> launch(Callable<T> callable){
        return call(Objects.requireNonNull(callable, "callable == null"));
    }

    private <T> FlexTask<T> call(Callable<T> callable){
        // The switch covers every constant of the enum: adding one without handling it breaks the
        // compilation, instead of returning null in silence.
        switch (dispatchLike) {
            case IO:
                return taskProcess(FlexThreadIO.get(), callable);
            case Unfixed:
                return taskProcess(FlexThreadUnFixed.get(), callable);
            case Default:
                return taskProcess(FlexThreadDefault.get(), callable);
            case Main:
                return mainProcess(callable);
            default:
                throw new IllegalStateException("Unhandled DispatchLike: " + dispatchLike);
        }
    }

    /**
     * Submits the body to a background pool and wraps its future in a {@link FlexTask}.
     *
     * <p>The wrapper reports completion from inside the pool thread, so the callback runs there: the
     * state is complete before the future itself is done, which is what makes a late registration still
     * deliver.
     *
     * @param service  pool that executes the body
     * @param callable body of the task
     * @param <T>      type of the produced value
     * @return the handle observing the submitted task
     */
    private <T> FlexTask<T> taskProcess(ExecutorService service, Callable<T> callable){
        FlexTask<T> flexTask = new FlexTask<>();

        Future<T> future = service.submit(() -> {
            try {
                T result = callable.call();
                flexTask.invoke();
                return result;
            } catch (Exception e) {
                // Report the failure BEFORE rethrowing: without this the completion callback was never
                // run for a failing task, and its caller waited for a result that would never come.
                flexTask.invokeWithError(e);
                throw new RuntimeException(e);
            }
        });

        flexTask.attachFuture(future);
        attach(flexTask);

        return flexTask;
    }

    /**
     * Submits the body to the <b>main thread</b>, keeping the full state tracking.
     *
     * <p>The task is posted to the main loop, so <b>it really runs on the UI thread</b> even when
     * {@code launch(...)} is called from a background thread. The tracking survives because the return
     * value is the {@link FutureTask} built by {@link FlexThreadMain#submit(Callable)}: cancellation,
     * result and error stay observable instead of being lost in a bare {@code post(...)}.
     *
     * @param callable body of the task, executed on the main thread
     * @param <T>      type of the produced value
     * @return the handle observing the posted task
     * @throws IllegalStateException if the environment has no main thread, as reported by
     *                               {@link FlexThreadMain#submit(Callable)}
     */
    private <T> FlexTask<T> mainProcess(Callable<T> callable){
        FlexTask<T> flexTask = new FlexTask<>();

        FutureTask<T> futureTask = FlexThreadMain.submit(() -> {
            try {
                T result = callable.call();
                flexTask.invoke();
                return result;
            } catch (Exception e) {
                flexTask.invokeWithError(e);
                throw new RuntimeException(e);
            }
        });

        flexTask.attachFuture(futureTask);
        attach(flexTask);
        return flexTask;
    }
}
