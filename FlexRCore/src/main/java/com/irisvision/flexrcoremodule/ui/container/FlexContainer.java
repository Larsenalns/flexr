// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import android.widget.FrameLayout;

import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;

/**
 * Stacking container: its children overlap, exactly as in a {@code FrameLayout}, so the last one
 * added is drawn on top. Same family as {@link FlexBox} and {@link FlexCard}.
 *
 * <p>The class is a real container, not a marker: {@link #contentWrapper(FlexViewBridge)} and
 * {@link #boxWrapper(BoxBridge)} both bridge the {@code FrameLayout} the instance owns, so
 * {@code FlexViewGroup.add(FlexView)} attaches children to it exactly as it does for a
 * {@link FlexBox}. Since a box bridge is always available, a {@code FlexContainer} is itself
 * accepted as the child of any box container.
 *
 * <h2>Layout scope</h2>
 * <p>The container only stacks: each child is measured and positioned by the rules of the
 * {@code FrameLayout}, and its layout parameters are resolved against the screen. No policy here
 * rescales a child as a fraction of the container, so a {@code FlexValue.dynamicW} or
 * {@code FlexValue.dynamicH} value still refers to the screen dimensions and not to the size of this
 * container.
 *
 * <p>Threading: every member must be used on the UI thread, since all of them drive native views.
 *
 * <pre>{@code
 * FlexContainer container = new FlexContainer(this);
 * container.add(new FlexText(this).text("Caption"));
 * container.add(new FlexBox(this));
 * }</pre>
 */
public class FlexContainer extends BoxImpl {

    private final FrameLayout frameLayout;

    /**
     * Creates an empty container backed by a new native {@code FrameLayout}, and registers it with
     * the theme scope of its screen.
     *
     * <p>Must be called on the UI thread, since it instantiates a view.
     *
     * @param flexContext context of the screen that owns this container
     */
    public FlexContainer(FlexContextInterface flexContext) {
        super(flexContext);
        this.frameLayout = new FrameLayout(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Bridges the native {@code FrameLayout} to a {@code FlexViewBridge}: this is the generic path
     * through which the framework reaches the view of this container.
     *
     * <p>UI thread only.
     *
     * @param flexViewBridge bridge to hand the native view to
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.frameLayout);
    }

    /**
     * Bridges the native {@code FrameLayout} to a {@code BoxBridge}: this is the path
     * {@code FlexViewGroup.add(FlexView)} uses to attach a child to this container.
     *
     * <p>UI thread only.
     *
     * @param boxBridge bridge to hand the native view to
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public final boolean boxWrapper(BoxBridge boxBridge) {
        return content(boxBridge, this.frameLayout);
    }
}
