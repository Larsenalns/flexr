// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.container.BoxImpl;

/**
 * Nested vertical scrolling container backed by a {@link NestedScrollView}.
 *
 * <p>Unlike the plain vertical scroll container, this one takes part in the nested scrolling
 * protocol of {@code androidx.core}: a parent such as a coordinator layout receives the scroll
 * deltas instead of losing them. {@link #startScrolling(boolean)} and
 * {@link #stopScrolling(boolean)} drive that protocol explicitly, which is what a scroll started
 * from code needs.
 *
 * <pre>{@code
 * new FlexScrollNested(flexContext)
 *         .scrollSmoothTo(0)
 *         .startScrolling(false);
 * }</pre>
 *
 * <p>Threading: every method below touches the scroll view and must be called on the UI thread.
 * Nested scroll callbacks are delivered on that same thread.
 */
public class FlexScrollNested extends BoxImpl {
    private final NestedScrollView nestedScrollView;

    /**
     * Creates the nested scroll container and its backing {@code NestedScrollView}.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     */
    public FlexScrollNested(FlexContextInterface flexContext) {
        super(flexContext);
        this.nestedScrollView = new NestedScrollView(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Attaches the scroll view as the child container of the box.
     *
     * @param boxBridge the box bridge provided by the framework
     * @return {@code true} when the bridge accepted the container
     */
    @Override
    public final boolean boxWrapper(BoxBridge boxBridge) {
        return content(boxBridge, this.nestedScrollView);
    }

    /**
     * Attaches the scroll view to the FlexR content slot.
     *
     * @param flexViewBridge the bridge provided by the framework
     * @return {@code true} when the bridge accepted the content
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.nestedScrollView);
    }

    /**
     * Starts a nested scroll on the vertical axis.
     *
     * @param isProgrammedScroll {@code true} when the scroll is driven by code
     *                           ({@code ViewCompat.TYPE_NON_TOUCH}), {@code false} for a scroll
     *                           driven by the user ({@code ViewCompat.TYPE_TOUCH})
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested startScrolling(boolean isProgrammedScroll){
        if(isProgrammedScroll){
            this.nestedScrollView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
            return this;
        }
        this.nestedScrollView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
        return this;
    }

    /**
     * Stops the nested scroll opened by {@link #startScrolling(boolean)}.
     *
     * @param isProgrammedScroll must match the value given to the corresponding
     *                           {@link #startScrolling(boolean)} call: {@code true} stops a scroll
     *                           driven by code, {@code false} a scroll driven by the user
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested stopScrolling(boolean isProgrammedScroll){
        if(isProgrammedScroll){
            this.nestedScrollView.stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
            return this;
        }
        this.nestedScrollView.stopNestedScroll();
        return this;
    }

    /**
     * Returns the current vertical scroll offset.
     *
     * @return the offset in pixels, {@code 0} when scrolled to the top
     */
    public final int getPosition(){
        return this.nestedScrollView.getScrollY();
    }

    /**
     * Scrolls immediately to a vertical offset.
     *
     * @param value target offset in pixels; the horizontal offset is reset to {@code 0}
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested scrollTo(int value){
        this.nestedScrollView.scrollTo(0, value);
        return this;
    }

    /**
     * Animates the scroll to a vertical offset.
     *
     * @param value target offset in pixels; the horizontal offset is reset to {@code 0}
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested scrollSmoothTo(int value){
        this.nestedScrollView.smoothScrollTo(0, value);
        return this;
    }

    /**
     * Tells whether more content is available below the current viewport.
     *
     * <p>Only the downward direction is probed: the result is {@code true} while the scroll view
     * can still move by one pixel towards the bottom.
     *
     * @return {@code true} when content remains below the viewport
     */
    public final boolean isMoreElements(){
        return this.nestedScrollView.canScrollVertically(1);
    }

    /**
     * Scrolls to the bottom of the content.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested scrollToEnd(){
        this.nestedScrollView.fullScroll(View.FOCUS_DOWN);
        return this;
    }

    /**
     * Scrolls back to the top of the content.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested scrollToStart(){
        this.nestedScrollView.fullScroll(View.FOCUS_UP);
        return this;
    }

    /**
     * Aligns the <b>bottom</b> edge of the given view with the bottom edge of the viewport.
     *
     * @param flexView the view whose bottom edge is targeted
     * @param smooth {@code true} to animate the scroll, {@code false} for a direct jump
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested scrollToDownOf(FlexView flexView, boolean smooth){
        return smooth ? this.scrollSmoothTo(flexView.relativeBottom()) : this.scrollTo(flexView.relativeBottom());
    }

    /**
     * Aligns the <b>top</b> edge of the given view with the top edge of the viewport.
     *
     * @param flexView the view whose top edge is targeted
     * @param smooth {@code true} to animate the scroll, {@code false} for a direct jump
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested scrollToUpOf(FlexView flexView, boolean smooth){
        return smooth ? this.scrollSmoothTo(flexView.relativeTop()) : this.scrollTo(flexView.relativeTop());
    }

    /**
     * Registers a scroll change listener.
     *
     * @param onScrollChangeListener listener notified when the scroll position changes
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested onChange(View.OnScrollChangeListener onScrollChangeListener){
        this.nestedScrollView.setOnScrollChangeListener(onScrollChangeListener);
        return this;
    }

    /**
     * Registers a touch listener on the scroll view.
     *
     * <p>Returning {@code true} from the listener consumes the event and keeps the platform from
     * scrolling; that is the usual place to implement pull-to-refresh.
     *
     * @param onTouchListener listener notified before the scroll view handles a touch event
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollNested onTouch(View.OnTouchListener onTouchListener){
        //here to perform refresh if event is == > if (event.getAction() == MotionEvent.ACTION_UP) {
        this.nestedScrollView.setOnTouchListener(onTouchListener);
        return this;
    }
}
