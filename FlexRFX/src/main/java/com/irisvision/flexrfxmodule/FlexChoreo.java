// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

import android.view.Choreographer;

import com.irisvision.flexrconcurrency.FlexThreadMain;

/**
 * A programmable frame loop: it calls {@link #callback(long)} on every frame, according to a mode.
 *
 * <h2>The two modes</h2>
 * <ul>
 *   <li>{@link ExecMode#Timer} — the loop runs for {@link #timer(long)} <b>milliseconds</b> and then
 *       stops (or keeps running when no limit was set). The duration is measured from the first frame,
 *       which means a pause does not consume it;</li>
 *   <li>{@link ExecMode#Condition} — the loop runs as long as the condition is true. The condition
 *       starts <b>true</b>, so the first call is always delivered and the callback can set its own
 *       through {@link #refreshCondition(boolean)}. A false condition does not kill the loop unless
 *       {@code removeOnFinish} was asked for, so it can become true again later.</li>
 * </ul>
 *
 * <h2>Lifetime</h2>
 *
 * <p>A choreography is created against an owner — the Fragment, the Activity, or any view living inside
 * a screen — so that it can pause and resume with that scope:
 *
 * <pre>{@code
 * FlexChoreo choreo = new FlexChoreo(FlexChoreo.ExecMode.Timer, true, 0, this) {
 *     @Override public void callback(long frameTimeNanos) { ... }
 * };
 * choreo.timer(2000).launch();
 * }</pre>
 *
 * <h2>Threading</h2>
 *
 * <p>The mutable state is {@code volatile}, and every operation on the {@code Choreographer} (posting
 * and removing a frame) is performed on the main thread, wherever it is requested from. Only one frame
 * can be in flight at a time: {@link #resume()} or {@link #launch()} called twice does not run the
 * callback twice per frame.
 */
public class FlexChoreo{

    /** No duration limit: the loop runs until {@link #kill()} or until its condition turns false. */
    private static final long NO_LIMIT = -1;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private volatile boolean condition = true;
    private volatile long timer = NO_LIMIT;
    private volatile ExecMode execMode;
    private volatile boolean removeOnFinish;
    private volatile int delay;
    private final Choreo choreo = new Choreo();
    private volatile boolean isPaused = false;
    private volatile String scopeId;

    /** Origin of the {@code Timer} mode (main thread). */
    private long startNanos = -1;
    /** Instant the loop was paused, in absolute time (main thread). */
    private long pausedAtNanos = -1;
    /** Accumulated pause duration, subtracted from the clock (main thread). */
    private long totalPausedNanos = 0;
    /** True while a frame has been posted and not yet delivered (main thread). */
    private boolean frameInFlight = false;

    /**
     * Creates a choreography bound to an owner — the Fragment, the Activity, or any view living inside
     * a screen. The scope identifier is derived from the owner, so the caller never writes it.
     *
     * @param execMode       {@link ExecMode#Timer} or {@link ExecMode#Condition}
     * @param removeOnFinish whether the frame callback must be removed once the choreography ends
     * @param delay          delay before each frame, in milliseconds; {@code 0} posts on the next frame
     * @param owner          the screen or Activity this choreography belongs to
     * @throws IllegalArgumentException if {@code owner} is {@code null}
     * @throws IllegalStateException    if the owner cannot identify its screen
     */
    public FlexChoreo(ExecMode execMode, boolean removeOnFinish, int delay, AnimateScopeOwner owner) {
        String scopeId = AnimateScopeOwner.scopeIdOf(owner);

        if (scopeId.indexOf(":") < 0) {
            throw new IllegalArgumentException(
                    "scopeId must look like \"<fragment>:<activity>\". Received: " + scopeId);
        }
        this.execMode = execMode;
        this.removeOnFinish = removeOnFinish;
        this.delay = delay;
        this.scopeId = scopeId;
    }

    private void runIf(long effectiveFrameTimeNanos){
        ExecMode mode = this.execMode;
        if (mode == null) {
            return;
        }

        if (mode == ExecMode.Condition) {
            if (condition) {
                callback(effectiveFrameTimeNanos);
                postCallBack();
            }
            else if (removeOnFinish) {
                removeCallBack();
                this.execMode = null;
            }
            else {
                // The condition may become true again: keep the loop posted instead of ending it.
                postCallBack();
            }
            return;
        }

        if (mode == ExecMode.Timer) {
            if (timer < 0) {
                callback(effectiveFrameTimeNanos);
                postCallBack();
                return;
            }
            if (startNanos < 0) {
                startNanos = effectiveFrameTimeNanos;           // first frame: the origin of the stopwatch
            }
            long elapsedMillis = (effectiveFrameTimeNanos - startNanos) / NANOS_PER_MILLI;
            if (elapsedMillis <= timer) {
                callback(effectiveFrameTimeNanos);
                postCallBack();
            }
            else if (removeOnFinish) {
                removeCallBack();
                this.execMode = null;
            }
            return;
        }

        // A mode that is neither Timer nor Condition is a programming mistake: report it instead of
        // falling through in silence.
        throw new IllegalStateException("Unknown execution mode: " + mode);
    }

    /**
     * Sets the condition of the {@link ExecMode#Condition} mode.
     *
     * <p>Call it from {@link #callback(long)} when the condition changes. It starts {@code true}, which
     * guarantees one first execution — that call is where the developer sets its own value.
     *
     * @param condition whether the following frames must still run the callback; {@code false} stops
     *                  the callbacks but leaves the loop posted, unless {@code removeOnFinish} was
     *                  requested at construction
     */
    public final void refreshCondition(boolean condition){
        this.condition = condition;
    }

    /**
     * Sets the duration of the {@link ExecMode#Timer} mode, in <b>milliseconds</b>.
     *
     * <p>A negative value — or never calling this method — means "no limit": the loop then runs until
     * {@link #kill()} or until its condition drops. The duration is measured from the <b>first frame</b>,
     * so a pause does not consume it.
     *
     * @param time maximum duration in milliseconds, or a negative value for no limit
     * @return this choreography, for chaining
     */
    public final FlexChoreo timer(long time){
        this.timer = time;
        return this;
    }

    /**
     * Starts the loop: the first frame is posted on the main thread, then {@link #callback(long)} is
     * called for every frame, according to the mode.
     *
     * <p>Calling {@code launch()} twice does not double the rate: only one frame is ever in flight. A
     * choreography whose mode has been cleared by {@link #kill()} does nothing.
     *
     * <p>Threading: the call may come from any thread; the posting itself is moved to the main one.
     */
    public final void launch(){
        if(this.execMode != null)
            FlexThreadMain.runOnMain(this::postCallBack);
    }
    /**
     * Pauses the loop: the pending frame is removed, and the time spent paused is excluded from
     * {@link ExecMode#Timer} when {@link #resume()} comes.
     *
     * <p>The frame that discovers the pause is still delivered, which is what lets the callback paint
     * the last position before the loop stops. Pausing is idempotent.
     */
    public final void pause(){
        if(this.execMode != null)
            this.isPaused = true;
    }
    /**
     * Resumes a paused loop, from the point where it stopped.
     *
     * <p>The duration of the pause is <b>subtracted</b> from the clock instead of being added to an
     * absolute instant, so a long pause does not make the animation jump forward. Resuming a loop that
     * is not paused simply ensures one frame is pending.
     */
    public final void resume(){
        this.isPaused = false;
        if (this.execMode != null) {
            FlexThreadMain.runOnMain(this::postCallBack);
        }
    }
    /**
     * Ends the loop for good: the mode is cleared and the pending frame callback is removed.
     *
     * <p>After this call {@link #launch()} and {@link #resume()} are no-ops and the instance is
     * finished. The scope registries call it when the owner is destroyed, so a disposed view stops
     * receiving frames.
     */
    public final void kill(){
        this.execMode = null;
        FlexThreadMain.runOnMain(this::removeCallBack);
    }

    /**
     * Called once per frame while the loop runs — the method an application overrides.
     *
     * <p>It runs on the <b>main thread</b>, inside the frame pass, so it can write into views directly.
     * It must stay short: a slow callback delays the frame for the whole application.
     *
     * <p>The default implementation does nothing, which leaves the loop useful only for its effect on
     * the frame clock.
     *
     * @param frameTimeNanos the frame time in nanoseconds, as provided by the {@code Choreographer} and
     *                       already corrected for the pauses of this loop
     */
    public void callback(long frameTimeNanos){}

    private void removeCallBack(){
        frameInFlight = false;
        Choreographer.getInstance().removeFrameCallback(this.choreo);
    }
    private void postCallBack(){
        if (frameInFlight) {
            return;                         // a frame is already in flight
        }
        frameInFlight = true;

        // Any positive delay is honoured, and 0 posts on the next frame: the delayed post is used only
        // when a delay was actually requested.
        if (delay > 0) {
            Choreographer.getInstance().postFrameCallbackDelayed(this.choreo, delay);
        }
        else {
            Choreographer.getInstance().postFrameCallback(this.choreo);
        }
    }

    /**
     * Returns the part of this choreography's scope identifier that corresponds to the requested scope.
     *
     * @param scope one of the scope names shared with the core ({@code "FlexFragment"} or
     *              {@code "FlexActivity"}); any other value yields an empty string
     * @return the requested part of the identifier, or an empty string when it cannot be determined
     */
    public final String ref(String scope) {
        if (this.scopeId == null || scope == null) {
            return "";
        }
        int separator = this.scopeId.indexOf(":");
        if (separator < 0) {
            return "";
        }
        if (scope.equalsIgnoreCase(FlexAnimateScopeManager.SCOPE_FRAGMENT)) {
            return this.scopeId.substring(0, separator);
        }
        if (scope.equalsIgnoreCase(FlexAnimateScopeManager.SCOPE_ACTIVITY)) {
            return this.scopeId.substring(separator + 1);
        }
        return "";
    }

    private class Choreo implements Choreographer.FrameCallback{

        @Override
        public final void doFrame(long frameTimeNanos) {
            frameInFlight = false;                  // the frame in flight has just been delivered

            if(isPaused){
                // The frame that discovers the pause is still executed, as intended: it paints the
                // last position before the loop stops.
                runIf(frameTimeNanos - totalPausedNanos);
                if (pausedAtNanos < 0) {
                    pausedAtNanos = frameTimeNanos; // start of the pause window
                }
                removeCallBack();
                return;
            }

            if (pausedAtNanos >= 0) {
                // The pause is measured as a <b>duration</b> and subtracted from the clock: adding an
                // absolute instant would move the origin and make the elapsed time jump.
                totalPausedNanos += frameTimeNanos - pausedAtNanos;
                pausedAtNanos = -1;
            }

            runIf(frameTimeNanos - totalPausedNanos);
        }

    }

    /**
     * How the loop decides to keep running. The mode is fixed at construction and cleared by
     * {@link #kill()}.
     */
    public enum ExecMode{
        /** Run for a fixed duration, set by {@link FlexChoreo#timer(long)}, measured from the first frame. */
        Timer, Condition
        /** Run while the condition is true; it starts true and is updated by {@link #refreshCondition(boolean)}. */
    }

}
