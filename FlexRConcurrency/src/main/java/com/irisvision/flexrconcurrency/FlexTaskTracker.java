// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrconcurrency;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Follows every {@link FlexTask} launched by one scope, so that a scope can tell whether it still has
 * work in flight and can cancel all of it at once.
 *
 * <h2>Threading</h2>
 *
 * <p>The list is a {@link CopyOnWriteArrayList}, and that is necessary rather than convenient:
 * {@code attach} is called from the thread that launches the task, while {@code detach} may be called
 * from <b>any</b> thread — through {@link FlexTask#forgot()}, which a running task itself can reach —
 * and {@link #cancelAll()} walks the list while each element removes itself from it. A plain
 * {@code ArrayList} would throw a {@code ConcurrentModificationException} exactly in the case the class
 * exists for.
 *
 * <p>Reads are cheap but not atomic: {@link #tasksRunning()} and {@link #taskCanceled()} answer for the
 * instant they run, which is all a lifecycle check needs.
 */
public class FlexTaskTracker {

    private final List<FlexTask<?>> flexTasks = new CopyOnWriteArrayList<>();

    /**
     * Creates an empty tracker. A scope owns one for its whole life.
     */
    public FlexTaskTracker() {
    }

    /**
     * Starts following a task, and tells it which tracker to detach itself from.
     *
     * <p>Called by the dispatcher right after submission. A task can be attached to one tracker only.
     *
     * @param flexTask task to follow until it finishes or is cancelled
     */
    protected final void attach(FlexTask<?> flexTask){
        flexTask.parent(this);
        this.flexTasks.add(flexTask);
    }

    /**
     * Stops following a task.
     *
     * <p>Tolerant by design: detaching a task that is not in the list — already cancelled, or never
     * attached — does nothing, because that is the normal state during a cancellation race.
     *
     * @param flexTask task to forget
     */
    protected final void detach(FlexTask<?> flexTask){
        this.flexTasks.remove(flexTask);
    }

    /**
     * Whether <b>at least one</b> followed task is still running.
     *
     * <p>It returns as soon as an active task is found, so a single long task is enough to answer
     * {@code true} — reading only the last element of the list, as an earlier implementation did, would
     * miss it as soon as another task was launched afterwards.
     *
     * <p>A cancelled task counts as finished in {@link FlexTask#isDone()}, so it is not reported as
     * active; {@link #taskCanceled()} answers that question.
     *
     * @return {@code true} while any followed task has not finished
     */
    public final boolean tasksRunning(){
        for (FlexTask<?> flexTask : flexTasks) {
            if (!flexTask.isDone()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Number of tasks currently followed, finished ones included until they detach themselves.
     *
     * @return the size of the tracked list
     */
    public final int tasksCount(){
        return this.flexTasks.size();
    }

    /**
     * Whether <b>at least one</b> followed task was cancelled.
     *
     * <p>Same early exit as {@link #tasksRunning()}: the answer is about any task of the list, not
     * about the last one.
     *
     * @return {@code true} if any followed task is cancelled
     */
    public final boolean taskCanceled(){
        for (FlexTask<?> flexTask : flexTasks) {
            if (flexTask.canceled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancels every followed task.
     *
     * <p>Each task cancels its own future, which detaches it from this list while the loop walks it:
     * that is exactly why the list is a {@code CopyOnWriteArrayList}, and why the loop cannot fail with
     * a concurrent modification.
     *
     * @see FlexTask#forgot()
     */
    public final void cancelAll(){
        for (FlexTask<?> flexTask : flexTasks) {
            flexTask.forgot();
        }
    }
}
