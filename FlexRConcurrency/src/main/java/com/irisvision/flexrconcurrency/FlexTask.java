// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrconcurrency;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * The observable result of a task submitted to a {@link FlexDispatcherGlobalScope}.
 *
 * <h2>Concurrency model — read this before changing the class</h2>
 *
 * <p>One instance is used by two threads:
 * <ul>
 *   <li>the <b>execution thread</b> (a pool thread), which calls {@link #invoke()} or
 *       {@link #invokeWithError(Exception)} when the body returns;</li>
 *   <li>the <b>calling thread</b>, which registers {@link #invokeOnComplete(Consumer)} and reads
 *       {@link #success()}, {@link #exception()}, {@link #canceled()} and {@link #result()}.</li>
 * </ul>
 *
 * <p>Every shared field is {@code volatile}, and delivery is driven by a small state machine held in an
 * {@link AtomicInteger}, so that the callback runs <b>exactly once</b> whatever the interleaving:
 *
 * <pre>
 *   state 0: no callback registered, task not finished
 *   state 1: callback registered, task not finished
 *   state 2: task finished, callback not delivered
 *   state 3: result delivered (terminal state)
 * </pre>
 *
 * <p>Without those guarantees two defects appeared: a callback could be <b>silently lost</b>, because
 * the execution thread never saw the registration made by the caller, and it could be <b>run twice</b>
 * when the task finished while its callback was being registered.
 *
 * <p>One deliberate choice: the success flag is evaluated <b>as soon as the task ends</b>, even if no
 * callback is registered yet, which keeps {@link #success()} reliable in every case.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.IO);
 * scope.launch(() -> repository.load(id))
 *      .invokeOnComplete(task -> {
 *          if (task.success()) {
 *              render(task);            // the value is read once the task is done
 *          } else {
 *              showError(task.exception());
 *          }
 *      });
 * }</pre>
 *
 * <p>Do not call {@link #result()} from inside that callback: the future is only marked done after the
 * task body returns, which happens after the callback, so {@code result()} there would wait for the
 * thread that is running it.
 */
public class FlexTask<T> {

    private static final int AWAITING_CALLBACK = 0;
    private static final int CALLBACK_REGISTERED = 1;
    private static final int FINISHED = 2;
    private static final int DELIVERED = 3;

    private final AtomicInteger completionState = new AtomicInteger(AWAITING_CALLBACK);

    private volatile Future<T> future;
    private volatile FlexTaskTracker tracker;
    private volatile Consumer<FlexTask<T>> whenComplete;
    private volatile Exception errorState = null;
    private volatile boolean successState = false;

    /**
     * Wraps an already submitted {@link Future}.
     *
     * <p>Meant for code that owns the submission itself. The task then observes that future, but
     * completion still has to be reported internally: nothing else calls {@link #invoke()} on such a
     * task, so a callback registered through {@link #invokeOnComplete(Consumer)} would never run.
     *
     * @param future the future whose result and state this task exposes
     * @throws NullPointerException if {@code future} is {@code null}
     */
    public FlexTask(Future<T> future){
        this.future = Objects.requireNonNull(future, "future == null");
    }

    /**
     * Creates a task with no future yet: the dispatcher attaches one once the executor has accepted the
     * body.
     */
    protected FlexTask(){
    }

    /**
     * Attaches the future that carries the result of the task.
     *
     * <p>Called once, right after submission, before the task can be observed. A task without a future
     * is not "finished", and has no value to block on.
     *
     * @param future the future returned by the executor
     * @throws NullPointerException if {@code future} is {@code null}
     */
    protected final void attachFuture(Future<T> future){
        this.future = Objects.requireNonNull(future, "future == null");
    }

    /**
     * Records the tracker that follows this task, so that cancelling the task also detaches it.
     *
     * @param tracker tracker of the scope that launched the task
     */
    protected final void parent(FlexTaskTracker tracker){
        this.tracker = tracker;
    }

    /**
     * Whether the task is finished, by success, failure or cancellation.
     *
     * @return {@code true} once the underlying future is done; {@code false} while it runs, and
     *         {@code false} for a task whose future has not been attached yet
     */
    public final boolean isDone(){
        Future<T> current = this.future;
        return current != null && current.isDone();
    }

    /**
     * Kept for compatibility: delegates to {@link #isDone()}.
     *
     * @return the same value as {@link #isDone()}
     * @deprecated the name is misleading — it returns {@code true} when the task is <b>finished</b>.
     *             Use {@link #isDone()} instead.
     */
    @Deprecated
    public final boolean running(){
        return isDone();
    }

    /**
     * Whether the task was cancelled.
     *
     * @return {@code true} if the underlying future was cancelled, through {@link #forgot()} or
     *         directly on the future
     */
    public final boolean canceled(){
        Future<T> current = this.future;
        return current != null && current.isCancelled();
    }

    /**
     * Whether the task ended without cancellation and without error.
     *
     * <p>The flag is evaluated when the task ends, not when the callback is registered, so it is
     * already reliable inside {@link #invokeOnComplete(Consumer)} even when that callback was
     * registered late.
     *
     * @return {@code true} only for a task whose body completed normally
     */
    public final boolean success(){
        return this.successState;
    }

    /**
     * Cancels the task and detaches it from its tracker.
     *
     * <p>Both steps are best-effort and independent: a task that has no tracker yet, or no future yet,
     * is a legitimate state during initialisation, and neither state prevents the other step. Once
     * detached, the task stops counting as running and no longer keeps its scope busy.
     *
     * @see FlexTaskTracker#cancelAll()
     */
    public final void forgot(){
        FlexTaskTracker currentTracker = this.tracker;
        if (currentTracker != null) {
            currentTracker.detach(this);
        }
        Future<T> current = this.future;
        if (current != null) {
            current.cancel(true);
        }
    }

    /**
     * Records the error that ended the task.
     *
     * @param error the failure exposed by {@link #exception()}; it also prevents the task from being
     *              reported as successful
     */
    protected final void injectError(Exception error){
        this.errorState = error;
    }

    /**
     * Called by the execution thread when the task body returned without error.
     *
     * <p>Evaluates the success flag, then delivers the result if a callback is already registered. A
     * callback registered later is delivered by {@link #invokeOnComplete(Consumer)}, so the two paths
     * together cover every interleaving exactly once.
     */
    protected final void invoke(){
        testSuccessState();
        complete();
    }

    /**
     * Called by the execution thread when the task body failed.
     *
     * <p>The error is recorded <b>before</b> the notification, so the success flag is evaluated with it
     * already in place and the task is never reported as successful. Both delivery paths — immediate,
     * when a callback is registered, and late, when it is registered afterwards — therefore run the
     * callback exactly once, with the failure visible.
     *
     * @param error the failure that ended the task
     */
    protected final void invokeWithError(Exception error){
        injectError(error);
        complete();
    }

    /**
     * Advances the state machine, and delivers the result when this call is the one that must.
     */
    private void complete(){
        // Moves the state from 0 (not finished) to 2 (finished), or from 1 (callback registered) to
        // 3 (delivered).
        if (completionState.compareAndSet(AWAITING_CALLBACK, FINISHED)
                || completionState.compareAndSet(CALLBACK_REGISTERED, DELIVERED)) {
            deliver();
        }
        // If the state was already 2, the caller is registering its callback right now and will deliver
        // the result itself. If it was 3, everything has already been done.
    }

    /**
     * Blocks until the result is available.
     *
     * <p>The interruption flag is restored before the exception is thrown, so a caller that catches it
     * still sees its thread as interrupted.
     *
     * <p>Call it from the thread that waits for the task, not from inside
     * {@link #invokeOnComplete(Consumer)}: the callback runs before the future is marked done, so
     * {@code result()} there would wait for the thread that is running it.
     *
     * @return the value produced by the task
     * @throws IllegalStateException if no future is attached to this task
     * @throws RuntimeException      wrapping the {@code ExecutionException} when the task failed, or the
     *                               {@code InterruptedException} when the wait was interrupted
     */
    public final T result(){
        Future<T> current = this.future;
        if (current == null) {
            throw new IllegalStateException("No Future attached to this FlexTask.");
        }
        try {
            return current.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Blocks until the result is available, or until the timeout expires.
     *
     * <p>An expiry is reported as a wrapped {@link TimeoutException}, distinct from a failure of the
     * task itself, so the caller can tell "too slow" from "broken".
     *
     * @param timeout maximum wait, in <b>milliseconds</b>
     * @return the value produced by the task
     * @throws IllegalStateException if no future is attached to this task
     * @throws RuntimeException      wrapping the {@code ExecutionException}, the {@code TimeoutException}
     *                               or the {@code InterruptedException} that ended the wait
     */
    public final T result(long timeout){
        Future<T> current = this.future;
        if (current == null) {
            throw new IllegalStateException("No Future attached to this FlexTask.");
        }
        try {
            return current.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the error recorded by the task, or {@code null} if it did not fail
     */
    public final Exception exception(){
        return this.errorState;
    }

    /**
     * Registers the completion callback.
     *
     * <ul>
     *   <li>if the task is <b>already finished</b>, the callback runs immediately on the calling
     *       thread;</li>
     *   <li>otherwise it is run by the <b>execution thread</b> when the task ends.</li>
     * </ul>
     *
     * <p>In both cases exactly once, and whatever the outcome: success, failure or cancellation. A
     * callback that is never called is precisely the defect this state machine exists to prevent.
     *
     * <p>Register it once. Delivery is terminal, so a callback registered after the result has been
     * delivered is never called — and registering a second one before completion replaces the first.
     *
     * @param whenComplete callback receiving this task; must not be {@code null}
     * @throws NullPointerException if {@code whenComplete} is {@code null}
     */
    public final void invokeOnComplete(Consumer<FlexTask<T>> whenComplete){
        Objects.requireNonNull(whenComplete, "whenComplete == null");
        this.whenComplete = whenComplete;

        // Moves the state from 0 (not finished) to 1 (callback registered)...
        if (completionState.compareAndSet(AWAITING_CALLBACK, CALLBACK_REGISTERED)) {
            return; // ...the task will deliver the result itself.
        }
        // ...or from 2 (already finished) to 3 (delivered): this call delivers the result.
        if (completionState.compareAndSet(FINISHED, DELIVERED)) {
            deliver();
        }
        // State 1: inside the test-and-set of invoke(). State 3: already delivered.
    }

    /**
     * Computes the success flag: a task succeeded only if it was not cancelled and recorded no error.
     */
    private void testSuccessState(){
        Future<T> current = this.future;
        if (current != null && !current.isCancelled() && this.errorState == null) {
            this.successState = true;
        }
    }

    /**
     * Runs the completion callback if one is registered. Called at most once per task.
     */
    private void deliver(){
        Consumer<FlexTask<T>> callback = this.whenComplete;
        if (callback != null) {
            callback.accept(this);
        }
    }
}
