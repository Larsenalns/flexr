// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import android.view.View;
import android.widget.HorizontalScrollView;

import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.container.BoxImpl;

/**
 * Horizontal scrolling container backed by a {@link HorizontalScrollView}.
 *
 * <p>The class is a FlexR box: {@link #contentWrapper(FlexViewBridge)} mounts the scroll view as
 * the content of the view and {@link #boxWrapper(BoxBridge)} mounts the very same instance as the
 * child container of the box. Positions are expressed in pixels along the horizontal axis only;
 * the vertical offset is always kept at {@code 0}.
 *
 * <pre>{@code
 * new FlexScrollHorizontal(flexContext)
 *         .scrollSmoothTo(0)
 *         .onChange(listener);
 * }</pre>
 *
 * <p>Threading: every method below touches the scroll view and must be called on the UI thread.
 * Smooth scrolling animates over the frames of that same thread.
 */
public class FlexScrollHorizontal extends BoxImpl {
    private final HorizontalScrollView horizontalScrollView;

    /**
     * Creates the horizontal scroll container and its backing {@code HorizontalScrollView}.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     */
    public FlexScrollHorizontal(FlexContextInterface flexContext) {
        super(flexContext);
        this.horizontalScrollView = new HorizontalScrollView(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Attaches the scroll view to the FlexR content slot.
     *
     * @param flexViewBridge the bridge provided by the framework
     * @return {@code true} when the bridge accepted the content
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.horizontalScrollView);
    }

    /**
     * Attaches the scroll view as the child container of the box.
     *
     * @param boxBridge the box bridge provided by the framework
     * @return {@code true} when the bridge accepted the container
     */
    @Override
    public final boolean boxWrapper(BoxBridge boxBridge) {
        return content(boxBridge, this.horizontalScrollView);
    }

    /**
     * Returns the current horizontal scroll offset.
     *
     * @return the offset in pixels, {@code 0} when scrolled to the left edge
     */
    public final int getPosition(){
        return this.horizontalScrollView.getScrollX();
    }

    /**
     * Scrolls immediately to a horizontal offset.
     *
     * @param value target offset in pixels; the vertical offset is reset to {@code 0}
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollHorizontal scrollTo(int value){
        this.horizontalScrollView.scrollTo(value, 0);
        return this;
    }

    /**
     * Animates the scroll to a horizontal offset.
     *
     * @param value target offset in pixels; the vertical offset is reset to {@code 0}
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollHorizontal scrollSmoothTo(int value){
        this.horizontalScrollView.smoothScrollTo(value, 0);
        return this;
    }

    /**
     * Tells whether more content is available to the right of the current viewport.
     *
     * <p>Only the rightward direction is probed: the result is {@code true} while the scroll view
     * can still move by one pixel towards the right.
     *
     * @return {@code true} when content remains to the right of the viewport
     */
    public final boolean isMoreElements(){
        return this.horizontalScrollView.canScrollHorizontally(1);
    }

    /**
     * Scrolls to the right edge of the content.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollHorizontal scrollToEnd(){
        this.horizontalScrollView.fullScroll(View.FOCUS_RIGHT);
        return this;
    }

    /**
     * Scrolls back to the left edge of the content.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollHorizontal scrollToStart(){
        this.horizontalScrollView.fullScroll(View.FOCUS_LEFT);
        return this;
    }

    /**
     * Aligns the <b>right</b> edge of the given view with the right edge of the viewport.
     *
     * @param flexView the view whose right edge is targeted
     * @param smooth {@code true} to animate the scroll, {@code false} for a direct jump
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollHorizontal scrollToRightOf(FlexView flexView, boolean smooth){
        return smooth ? this.scrollSmoothTo(flexView.relativeRight()) : this.scrollTo(flexView.relativeRight());
    }

    /**
     * Aligns the <b>left</b> edge of the given view with the left edge of the viewport.
     *
     * @param flexView the view whose left edge is targeted
     * @param smooth {@code true} to animate the scroll, {@code false} for a direct jump
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollHorizontal scrollToLeftOf(FlexView flexView, boolean smooth){
        return smooth ? this.scrollSmoothTo(flexView.relativeLeft()) : this.scrollTo(flexView.relativeLeft());
    }

    /**
     * Registers a scroll change listener.
     *
     * @param onScrollChangeListener listener notified when the scroll position changes
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollHorizontal onChange(View.OnScrollChangeListener onScrollChangeListener){
        this.horizontalScrollView.setOnScrollChangeListener(onScrollChangeListener);
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
    public final FlexScrollHorizontal onTouch(View.OnTouchListener onTouchListener){
        //here to perform refresh if event is == > if (event.getAction() == MotionEvent.ACTION_UP) {
        this.horizontalScrollView.setOnTouchListener(onTouchListener);
        return this;
    }
}
