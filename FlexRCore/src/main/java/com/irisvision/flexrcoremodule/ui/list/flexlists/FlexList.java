// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapter;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapterRcv;

/**
 * Scrolling list backed by an Android {@link RecyclerView}.
 *
 * <h2>Stable facade: one host view</h2>
 *
 * <p>The view bridged to FlexR is a constant {@link FrameLayout} host. It holds the
 * {@code RecyclerView} and, when needed, the empty state or the loading state. Because that host
 * never changes, the parent keeps the same child view while the content swaps between list and
 * placeholder states.
 *
 * <h2>Empty and loading states</h2>
 *
 * <pre>{@code
 * new FlexList(this)
 *     .line()
 *     .loadingState(new FlexText(this).text("Loading..."))
 *     .emptyState(new FlexText(this).text("No data"))
 *     .adapter(myAdapter);
 *
 * myAdapter.setData(rows);   // the displayed state follows on its own
 * }</pre>
 *
 * <p>The displayed state follows the adapter: a data observer is registered on it, so
 * {@code setData}, {@code submitData} or {@code addItem} refresh the display without an extra
 * call. The loading state wins over the empty state, which in turn wins over the list.
 *
 * <h2>Load on demand</h2>
 *
 * <p>{@link #onEndReached(Runnable)} runs once when scrolling comes close to the end (two items
 * remaining) and re-arms as soon as the list is scrolled back up, which is the "load more" pattern
 * without writing the plumbing again.
 *
 * <h2>Orientation vocabulary</h2>
 *
 * <p>{@code "H"}/{@code "HORIZONTAL"} or {@code "V"}/{@code "VERTICAL"}, case-insensitive;
 * {@code null} or blank means vertical. An unrecognised value is rejected with
 * {@link IllegalArgumentException} instead of being silently read as vertical.
 *
 * <p>Threading: the class is not thread-safe. Every method below touches the
 * {@code RecyclerView} and must run on the UI thread. Adapter notifications must be delivered on
 * that same thread, since they drive the state swap, and the "end reached" action also runs there.
 */
public class FlexList extends FlexView {

    /** Number of remaining items at which {@code onEndReached} fires. */
    private static final int END_REACHED_THRESHOLD = 2;

    /** Stable host: the view bridged to FlexR and added to the parent. */
    private final FrameLayout host;
    private final RecyclerView recyclerView;

    private FlexView emptyState;
    private FlexView loadingState;
    private boolean loading;

    /** State view currently inserted in the host ({@code null} when the list is visible). */
    private FlexView currentState;
    private View currentStateView;

    private Runnable onEndReached;
    private boolean endReachedNotified;

    /** Follows the adapter to switch between the list, the empty state and the loading state. */
    private final RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
        @Override public void onChanged() { updateState(); }
        @Override public void onItemRangeInserted(int positionStart, int itemCount) { updateState(); }
        @Override public void onItemRangeRemoved(int positionStart, int itemCount) { updateState(); }
        @Override public void onItemRangeChanged(int positionStart, int itemCount) { updateState(); }
        @Override public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) { updateState(); }
    };

    /** Detects the approach of the end of the list without overriding the caller listeners. */
    private final RecyclerView.OnScrollListener endReachedWatcher = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
            if (onEndReached == null || (dx <= 0 && dy <= 0)) {
                return;
            }
            RecyclerView.Adapter<?> adapter = rv.getAdapter();
            int total = (adapter == null) ? 0 : adapter.getItemCount();
            if (total == 0) {
                return;
            }
            if (lastVisiblePosition() >= total - 1 - END_REACHED_THRESHOLD) {
                if (!endReachedNotified) {
                    endReachedNotified = true;
                    onEndReached.run();
                }
            }
            else {
                endReachedNotified = false;     // scrolled back up: the next end will fire again
            }
        }
    };

    /**
     * Creates the list, its host view and its backing {@code RecyclerView}.
     *
     * <p>The host is filled with the recycler view and the internal scroll watcher is registered
     * before the FlexR bridge takes over.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     */
    public FlexList(FlexContextInterface flexContext) {
        super(flexContext);
        this.host = new FrameLayout(f.getFlexContext());
        this.recyclerView = new RecyclerView(f.getFlexContext());
        this.host.addView(this.recyclerView, matchParent());
        this.recyclerView.addOnScrollListener(this.endReachedWatcher);
        initInnerBridge();
    }

    /**
     * Attaches the host view to the FlexR content slot.
     *
     * @param flexViewBridge the bridge provided by the framework
     * @return {@code true} when the bridge accepted the content
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.host);
    }

    /* *** Layout *** */

    /**
     * Lays the items out on a single line.
     *
     * @param orientation {@code "H"}/{@code "HORIZONTAL"} or {@code "V"}/{@code "VERTICAL"},
     *                    case-insensitive; {@code null} or blank means vertical
     * @param reverse {@code true} to reverse the layout direction
     * @return {@code this}, to allow chaining
     * @throws IllegalArgumentException if the orientation is not recognised
     */
    public FlexList line(String orientation, boolean reverse){
        return lineProcess(orientation, reverse);
    }

    /**
     * Lays the items out on a single vertical line.
     *
     * @param reverse {@code true} to reverse the layout direction
     * @return {@code this}, to allow chaining
     */
    public FlexList line(boolean reverse){
        return lineProcess(null, reverse);
    }

    /**
     * Lays the items out on a single vertical line, in natural order.
     *
     * @return {@code this}, to allow chaining
     */
    public FlexList line(){
        return lineProcess(null, false);
    }

    /**
     * Installs a linear layout manager for the requested axis.
     *
     * @param orientation axis name, possibly {@code null}
     * @param reverse {@code true} to reverse the layout direction
     * @return {@code this}, to allow chaining
     * @throws IllegalArgumentException if the orientation is not recognised
     */
    private FlexList lineProcess(String orientation, boolean reverse){
        LinearLayoutManager manager = new LinearLayoutManager(f.getFlexContext());
        manager.setReverseLayout(reverse);
        manager.setOrientation(isHorizontal(orientation)
                ? LinearLayoutManager.HORIZONTAL
                : LinearLayoutManager.VERTICAL);

        this.recyclerView.setLayoutManager(manager);
        return this;
    }

    /**
     * Lays the items out on a regular grid.
     *
     * @param spanCount number of cells on the cross axis
     * @param orientation {@code "H"}/{@code "HORIZONTAL"} or {@code "V"}/{@code "VERTICAL"},
     *                    case-insensitive; {@code null} or blank means vertical
     * @param reverse {@code true} to reverse the layout direction
     * @return {@code this}, to allow chaining
     * @throws IllegalArgumentException if the orientation is not recognised
     */
    public FlexList grid(int spanCount, String orientation, boolean reverse){
        this.recyclerView.setLayoutManager(new GridLayoutManager(
                f.getFlexContext(), spanCount,
                isHorizontal(orientation) ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL,
                reverse));
        return this;
    }

    /**
     * Lays the items out on a staggered grid, where the span count is a target rather than a
     * constraint and every line adapts to its own content.
     *
     * @param spanCount number of staggered spans
     * @param orientation {@code "H"}/{@code "HORIZONTAL"} or {@code "V"}/{@code "VERTICAL"},
     *                    case-insensitive; {@code null} or blank means vertical
     * @return {@code this}, to allow chaining
     * @throws IllegalArgumentException if the orientation is not recognised
     */
    public FlexList responsive(int spanCount, String orientation){
        this.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(
                spanCount,
                isHorizontal(orientation) ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL));
        return this;
    }

    /**
     * Tells whether an orientation name selects the horizontal axis.
     *
     * @param orientation axis name, possibly {@code null} or blank
     * @return {@code true} for the horizontal axis
     * @throws IllegalArgumentException if the orientation is not recognised
     */
    private boolean isHorizontal(String orientation){
        if (orientation == null || orientation.trim().isEmpty()) {
            return false;                                   // vertical by default
        }
        if (orientation.equalsIgnoreCase("H") || orientation.equalsIgnoreCase("HORIZONTAL")) {
            return true;
        }
        if (orientation.equalsIgnoreCase("V") || orientation.equalsIgnoreCase("VERTICAL")) {
            return false;
        }
        throw new IllegalArgumentException(
                "Unknown orientation: \"" + orientation + "\". Use \"H\"/\"HORIZONTAL\" or \"V\"/\"VERTICAL\".");
    }

    /* *** Adapter and states *** */

    /**
     * Installs the adapter and observes its data changes to keep the displayed state current.
     *
     * <p>The observer of the previous adapter is unregistered before the new one is installed, so
     * no adapter stays observed after a replacement.
     *
     * @param flexAdapter the adapter to install; {@code null} clears the list
     * @return {@code this}, to allow chaining
     */
    public FlexList adapter(FlexAdapterRcv flexAdapter){
        RecyclerView.Adapter<?> previous = this.recyclerView.getAdapter();
        if (previous != null) {
            previous.unregisterAdapterDataObserver(this.dataObserver);
        }

        this.recyclerView.setAdapter(flexAdapter);

        if (flexAdapter != null) {
            flexAdapter.registerAdapterDataObserver(this.dataObserver);
        }
        updateState();
        return this;
    }

    /**
     * Sets the view shown when the adapter holds no item.
     *
     * <p>The state is re-evaluated immediately, so the view appears at once when the list is already
     * empty.
     *
     * @param emptyState the view to show; {@code null} keeps the empty list visible
     * @return {@code this}, to allow chaining
     */
    public FlexList emptyState(FlexView emptyState){
        this.emptyState = emptyState;
        updateState();
        return this;
    }

    /**
     * Sets the view shown while a load is running; it takes precedence over the empty state.
     *
     * @param loadingState the view to show; {@code null} hides the loading state
     * @return {@code this}, to allow chaining
     */
    public FlexList loadingState(FlexView loadingState){
        this.loadingState = loadingState;
        updateState();
        return this;
    }

    /**
     * Enables or disables the loading state.
     *
     * <p>The flag only has an effect when a loading view was installed with
     * {@link #loadingState(FlexView)}.
     *
     * @param loading {@code true} to show the loading state
     * @return {@code this}, to allow chaining
     */
    public FlexList loading(boolean loading){
        this.loading = loading;
        updateState();
        return this;
    }

    /**
     * Recomputes which view the host displays: list, empty state or loading state.
     *
     * <p>Called automatically on adapter changes; call it after mutating state the class cannot
     * observe.
     */
    public final void updateState(){
        int count = 0;
        RecyclerView.Adapter<?> adapter = this.recyclerView.getAdapter();
        if (adapter != null) {
            count = adapter.getItemCount();
        }

        if (this.loading && this.loadingState != null) {
            showState(this.loadingState);
        }
        else if (count == 0 && this.emptyState != null) {
            showState(this.emptyState);
        }
        else {
            showState(null);
        }
    }

    /**
     * Shows one state view in the host, or brings the list back when {@code stateView} is
     * {@code null}.
     *
     * <p>Idempotent: passing the state already displayed does nothing, so no view is detached and
     * re-attached needlessly.
     *
     * @param stateView the view to display, or {@code null} to display the list
     */
    private void showState(FlexView stateView){
        if (this.currentState == stateView) {
            return;
        }
        if (this.currentStateView != null) {
            this.host.removeView(this.currentStateView);
        }

        this.currentState = stateView;
        this.currentStateView = null;

        if (stateView == null) {
            this.recyclerView.setVisibility(View.VISIBLE);
        }
        else {
            this.currentStateView = FlexAdapter.nativeView(stateView);
            this.host.addView(this.currentStateView, matchParent());
            this.recyclerView.setVisibility(View.GONE);
        }
    }

    /* *** Scrolling *** */

    /**
     * Scrolls the list so that the given position becomes the first visible one.
     *
     * @param position adapter position to scroll to
     * @return {@code this}, to allow chaining
     */
    public FlexList scrollToPosition(int position){
        this.recyclerView.scrollToPosition(position);
        return this;
    }

    /**
     * Animates the scroll to the given position.
     *
     * @param position adapter position to scroll to
     * @return {@code this}, to allow chaining
     */
    public FlexList smoothScrollToPosition(int position){
        this.recyclerView.smoothScrollToPosition(position);
        return this;
    }

    /**
     * Adds a scroll listener next to the internal one used for "load more".
     *
     * <p>Listeners accumulate: each call adds one and none replaces the internal watcher.
     *
     * @param listener listener notified on every scroll step
     * @return {@code this}, to allow chaining
     */
    public FlexList onScroll(RecyclerView.OnScrollListener listener){
        this.recyclerView.addOnScrollListener(listener);
        return this;
    }

    /**
     * Sets the action run once when scrolling approaches the end of the list.
     *
     * <p>The action runs once per approach: it re-arms as soon as the list is scrolled back up, so
     * the next approach triggers it again. It runs on the UI thread, inside the scroll callback.
     *
     * @param action the action to run; {@code null} disables the feature
     * @return {@code this}, to allow chaining
     */
    public FlexList onEndReached(Runnable action){
        this.onEndReached = action;
        this.endReachedNotified = false;
        return this;
    }

    /** Last visible adapter position, whichever layout manager is installed. */
    private int lastVisiblePosition(){
        RecyclerView.LayoutManager manager = this.recyclerView.getLayoutManager();
        if (manager instanceof StaggeredGridLayoutManager) {
            int[] lastVisible = ((StaggeredGridLayoutManager) manager).findLastVisibleItemPositions(null);
            int max = 0;
            for (int value : lastVisible) {
                max = Math.max(max, value);
            }
            return max;
        }
        if (manager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) manager).findLastVisibleItemPosition();
        }
        return 0;
    }

    private static FrameLayout.LayoutParams matchParent(){
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }
    // Soft RecyclerView Impl
}
