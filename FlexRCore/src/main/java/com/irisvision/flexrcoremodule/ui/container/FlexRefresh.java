// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrthememodule.FlexTheme;

/**
 * A container whose content can be pulled down to trigger a refresh: the "swipe to refresh" gesture,
 * built into FlexR with no dependency of its own.
 *
 * <h2>What it wraps</h2>
 *
 * <p>{@code FlexRefresh} is a box container: {@code add(FlexView)} attaches children to an internal
 * {@link FrameLayout}, exactly as {@link FlexBox} does, and the outer view is a host that keeps a progress
 * indicator above that frame. Nothing changes in the rest of the library — {@code FlexList},
 * {@code FlexScrollNested} and any {@code RecyclerView} keep working untouched inside it:
 *
 * <pre>{@code
 * FlexRefresh refresh = new FlexRefresh(this);
 * refresh.boxParams().Sizer(Val.Match);
 * refresh.onRefresh(() -> repository.reload());
 *
 * FlexList list = new FlexList(this);
 * refresh.add(list);
 * }</pre>
 *
 * <h2>How the gesture is detected</h2>
 *
 * <p>Through <b>nested scrolling</b>, the mechanism Android uses between a scrollable child and its
 * parents. When the child is already at the top and the finger keeps pulling, it reports the extra
 * distance as <i>unconsumed</i>; this container consumes it, draws the indicator and, on release past the
 * threshold, calls the refresh action.
 *
 * <p>Consequence, and it is the only real limitation: the child must take part in nested scrolling —
 * a {@code RecyclerView}, a {@code NestedScrollView} (which is what {@code FlexScrollNested} wraps) or a
 * {@code FlexList}. A plain {@code ScrollView} does not, so pulling inside one does nothing; the class
 * cannot invent a gesture the child never reports.
 *
 * <h2>Contract</h2>
 *
 * <ul>
 *   <li>{@link #onRefresh(Runnable)} is called <b>once</b> per gesture, on the UI thread, when the pull is
 *       released past the threshold;</li>
 *   <li>the indicator keeps turning until the application calls {@link #refreshing(boolean)} with
 *       {@code false} — the container never decides that the work is finished;</li>
 *   <li>a pull that stops before the threshold springs back and calls nothing;</li>
 *   <li>{@link #enabled(boolean)} turns the gesture off without removing the content, which is what a
 *       screen does while it already shows an error.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p>UI thread only: the class translates a view and shows an indicator.
 *
 * <p>This class is validated on device by the smoke screen; no JVM test can exercise a gesture, since no
 * view exists outside an Android runtime.
 */
public class FlexRefresh extends BoxImpl {

    private static final int THRESHOLD_DP = 64;
    private static final float MAX_PULL_FACTOR = 1.8f;

    private final RefreshHost root;
    private final FrameLayout host;
    private final CircularProgressIndicator indicator;

    private boolean enabled = true;
    private boolean refreshing = false;
    private Runnable onRefresh;
    private float pull;
    private int thresholdPx;

    /**
     * Creates an empty refresh container and registers it in the theme scope of its screen.
     *
     * <p>Must be called on the UI thread, since it instantiates views.
     *
     * @param flexContext context of the screen that owns this container
     */
    public FlexRefresh(FlexContextInterface flexContext) {
        super(flexContext);

        Context context = f.getFlexContext();
        this.thresholdPx = Math.round(THRESHOLD_DP * context.getResources().getDisplayMetrics().density);

        this.host = new FrameLayout(context);
        this.indicator = new CircularProgressIndicator(context);
        this.indicator.setIndeterminate(true);
        this.indicator.setVisibility(View.INVISIBLE);

        FrameLayout.LayoutParams indicatorParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        indicatorParams.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;

        this.root = new RefreshHost(context);
        this.root.addView(this.host, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        this.root.addView(this.indicator, indicatorParams);

        initInnerBridge();
    }

    /**
     * Bridges the outer host to FlexR.
     *
     * @param flexViewBridge bridge supplied by the framework
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.root);
    }

    /**
     * Bridges the inner frame: this is where {@code FlexViewGroup.add(FlexView)} attaches children.
     *
     * @param boxBridge bridge supplied by the framework
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public final boolean boxWrapper(BoxBridge boxBridge) {
        return content(boxBridge, this.host);
    }

    /**
     * Sets the action run when a pull is released past the threshold.
     *
     * <p>Called on the UI thread, once per gesture. The action usually starts a load and ends it with
     * {@link #refreshing(boolean)}.
     *
     * @param action work to run; {@code null} makes the gesture decorative
     * @return this container, for chaining
     */
    public final FlexRefresh onRefresh(Runnable action) {
        this.onRefresh = action;
        return this;
    }

    /**
     * Shows or hides the refresh state.
     *
     * <p>Call it with {@code false} when the work started by {@link #onRefresh(Runnable)} has finished;
     * the indicator then stops and the content slides back. Passing {@code true} starts the same visual
     * state without a gesture, which is useful when a screen refreshes itself on open.
     *
     * <p>UI thread only.
     *
     * @param refreshing {@code true} to keep the indicator turning, {@code false} to end it
     * @return this container, for chaining
     */
    public final FlexRefresh refreshing(boolean refreshing) {
        if (this.refreshing == refreshing) {
            return this;
        }
        this.refreshing = refreshing;
        if (refreshing) {
            this.pull = this.thresholdPx;
            this.indicator.setVisibility(View.VISIBLE);
            this.host.animate().translationY(this.thresholdPx).setDuration(180L).start();
        } else {
            this.pull = 0f;
            this.indicator.setVisibility(View.INVISIBLE);
            this.host.animate().translationY(0f).setDuration(220L).start();
        }
        return this;
    }

    /**
     * Colours the indicator from the current theme.
     *
     * @param themeColorIndex index in the current palette
     * @return this container, for chaining
     * @throws IndexOutOfBoundsException if the index is outside the palette
     */
    public final FlexRefresh colorScheme(int themeColorIndex) {
        this.indicator.setIndicatorColor(FlexTheme.load(themeColorIndex));
        return this;
    }

    /**
     * Enables or disables the gesture.
     *
     * @param enabled {@code true} to accept pulls; {@code false} leaves the content in place but inert
     * @return this container, for chaining
     */
    public final FlexRefresh enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Whether the container currently shows the refresh state.
     *
     * @return {@code true} between {@code refreshing(true)} and {@code refreshing(false)}, or while a
     *         refresh started by a gesture is running
     */
    public final boolean isRefreshing() {
        return this.refreshing;
    }

    /** Applies a pull distance, saturating at the visual maximum. */
    private void pullBy(int dyUnconsumedUp) {
        float delta = Math.abs(dyUnconsumedUp) / 2f;
        float max = this.thresholdPx * MAX_PULL_FACTOR;
        this.pull = Math.min(max, this.pull + delta);
        this.host.setTranslationY(this.pull);
        this.indicator.setVisibility(View.VISIBLE);
        this.indicator.setAlpha(Math.min(1f, this.pull / this.thresholdPx));
    }

    /** Ends a gesture: refreshes past the threshold, springs back otherwise. */
    private void finishPull() {
        if (this.refreshing) {
            return;
        }
        if (this.pull >= this.thresholdPx) {
            this.refreshing = true;
            this.pull = this.thresholdPx;
            this.host.animate().translationY(this.thresholdPx).setDuration(180L).start();
            this.indicator.setAlpha(1f);
            if (this.onRefresh != null) {
                this.onRefresh.run();
            }
        } else {
            this.pull = 0f;
            this.host.animate().translationY(0f).setDuration(220L).start();
            this.indicator.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Outer host: a frame that consumes a vertical overscroll from its child and turns it into a pull.
     *
     * <p>It is the {@code NestedScrollingParent3} implementation of the container; keeping it private
     * guarantees that the gesture cannot be bypassed by wrapping the container from outside.
     */
    private final class RefreshHost extends FrameLayout implements NestedScrollingParent3 {

        private final NestedScrollingParentHelper parentHelper;

        RefreshHost(Context context) {
            super(context);
            this.parentHelper = new NestedScrollingParentHelper(this);
        }

        @Override
        public boolean onStartNestedScroll(View child, View target, int axes, int type) {
            return FlexRefresh.this.enabled
                    && !FlexRefresh.this.refreshing
                    && (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes, int type) {
            this.parentHelper.onNestedScrollAccepted(child, target, axes, type);
        }

        @Override
        public void onStopNestedScroll(View target, int type) {
            this.parentHelper.onStopNestedScroll(target, type);
            FlexRefresh.this.finishPull();
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                                   int dxUnconsumed, int dyUnconsumed, int type, int[] consumed) {
            if (dyUnconsumed < 0) {
                FlexRefresh.this.pullBy(dyUnconsumed);
                consumed[1] += dyUnconsumed;
            }
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                                   int dxUnconsumed, int dyUnconsumed, int type) {
            onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, new int[2]);
        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
            // Pulling the finger back up first closes the pull, before the child scrolls.
            if (dy > 0 && FlexRefresh.this.pull > 0f) {
                float reduced = Math.max(0f, FlexRefresh.this.pull - dy);
                int consumedByPull = Math.round(FlexRefresh.this.pull - reduced);
                FlexRefresh.this.pull = reduced;
                FlexRefresh.this.host.setTranslationY(reduced);
                FlexRefresh.this.indicator.setAlpha(Math.min(1f, reduced / FlexRefresh.this.thresholdPx));
                consumed[1] += consumedByPull;
            }
        }

        @Override
        public int getNestedScrollAxes() {
            return this.parentHelper.getNestedScrollAxes();
        }

        // NestedScrollingParent (type-free variants) delegate to the typed ones with a touch gesture.

        @Override
        public boolean onStartNestedScroll(View child, View target, int axes) {
            return onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH);
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes) {
            onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH);
        }

        @Override
        public void onStopNestedScroll(View target) {
            onStopNestedScroll(target, ViewCompat.TYPE_TOUCH);
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                                   int dxUnconsumed, int dyUnconsumed) {
            onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    ViewCompat.TYPE_TOUCH, new int[2]);
        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
            onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
        }

        @Override
        public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
            return false;
        }
    }
}
