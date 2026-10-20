// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import android.view.View;
import android.widget.ScrollView;

import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.container.BoxImpl;

/**
 * Vertical scrolling container backed by a {@link ScrollView}.
 *
 * <p>The class is a FlexR box: {@link #contentWrapper(FlexViewBridge)} mounts the scroll view as
 * the content of the view and {@link #boxWrapper(BoxBridge)} mounts the very same instance as the
 * child container of the box. Positions are expressed in pixels along the vertical axis only; the
 * horizontal offset is always kept at {@code 0}.
 *
 * <pre>{@code
 * new FlexScrollVertical(flexContext)
 *         .scrollSmoothTo(0)
 *         .onChange(listener);
 * }</pre>
 *
 * <p>Threading: every method below touches the scroll view and must be called on the UI thread.
 * Smooth scrolling animates over the frames of that same thread.
 */
public class FlexScrollVertical extends BoxImpl {

    private final ScrollView verticalScrollView;

    /**
     * Creates the vertical scroll container and its backing {@code ScrollView}.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     */
    public FlexScrollVertical(FlexContextInterface flexContext) {
        super(flexContext);
        this.verticalScrollView = new ScrollView(f.getFlexContext());
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
        return content(flexViewBridge, this.verticalScrollView);
    }

    /**
     * Attaches the scroll view as the child container of the box.
     *
     * @param boxBridge the box bridge provided by the framework
     * @return {@code true} when the bridge accepted the container
     */
    @Override
    public final boolean boxWrapper(BoxBridge boxBridge) {
        return content(boxBridge, this.verticalScrollView);
    }

    /**
     * Returns the current vertical scroll offset.
     *
     * @return the offset in pixels, {@code 0} when scrolled to the top
     */
    public final int getPosition(){
        return this.verticalScrollView.getScrollY();
    }

    /**
     * Scrolls immediately to a vertical offset.
     *
     * @param value target offset in pixels; the horizontal offset is reset to {@code 0}
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollVertical scrollTo(int value){
        this.verticalScrollView.scrollTo(0, value);
        return this;
    }

    /**
     * Animates the scroll to a vertical offset.
     *
     * @param value target offset in pixels; the horizontal offset is reset to {@code 0}
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollVertical scrollSmoothTo(int value){
        this.verticalScrollView.smoothScrollTo(0, value);
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
        return this.verticalScrollView.canScrollVertically(1);
    }

    /**
     * Scrolls to the bottom of the content.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollVertical scrollToEnd(){
        this.verticalScrollView.fullScroll(View.FOCUS_DOWN);
        return this;
    }

    /**
     * Scrolls back to the top of the content.
     *
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollVertical scrollToStart(){
        this.verticalScrollView.fullScroll(View.FOCUS_UP);
        return this;
    }

    /**
     * Aligns the <b>bottom</b> edge of the given view with the bottom edge of the viewport.
     *
     * @param flexView the view whose bottom edge is targeted
     * @param smooth {@code true} to animate the scroll, {@code false} for a direct jump
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollVertical scrollToDownOf(FlexView flexView, boolean smooth){
        return smooth ? this.scrollSmoothTo(flexView.relativeBottom()) : this.scrollTo(flexView.relativeBottom());
    }

    /**
     * Aligns the <b>top</b> edge of the given view with the top edge of the viewport.
     *
     * @param flexView the view whose top edge is targeted
     * @param smooth {@code true} to animate the scroll, {@code false} for a direct jump
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollVertical scrollToUpOf(FlexView flexView, boolean smooth){
        return smooth ? this.scrollSmoothTo(flexView.relativeTop()) : this.scrollTo(flexView.relativeTop());
    }

    /**
     * Registers a scroll change listener.
     *
     * @param onScrollChangeListener listener notified when the scroll position changes
     * @return {@code this}, to allow chaining
     */
    public final FlexScrollVertical onChange(View.OnScrollChangeListener onScrollChangeListener){
        this.verticalScrollView.setOnScrollChangeListener(onScrollChangeListener);
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
    public final FlexScrollVertical onTouch(View.OnTouchListener onTouchListener){
        //here to perform refresh if event is == > if (event.getAction() == MotionEvent.ACTION_UP) {
        this.verticalScrollView.setOnTouchListener(onTouchListener);
        return this;
    }
}
