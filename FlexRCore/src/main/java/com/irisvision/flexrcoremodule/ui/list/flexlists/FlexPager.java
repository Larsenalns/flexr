// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import androidx.viewpager2.widget.ViewPager2;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapterRcv;

/**
 * Horizontal or vertical paging backed by an Android {@link ViewPager2}.
 *
 * <p>Pages come from a {@link FlexAdapterRcv} installed with {@link #adapter(FlexAdapterRcv)};
 * the adapter decides how many pages exist. The scroll axis is selected by name so that it can be
 * read straight from a layout description: an unsupported name is rejected instead of falling back
 * silently, while an absent name leaves the current axis untouched.
 *
 * <pre>{@code
 * new FlexPager(flexContext)
 *         .preparePager(true, true, 1)
 *         .orientation("V")
 *         .adapter(pages);
 * }</pre>
 *
 * <p>Threading: the class is not thread-safe. Construction and every builder method must run on
 * the UI thread; the pager dispatches its callbacks on that same thread.
 */
public class FlexPager extends FlexView {

    private final ViewPager2 viewPager2;

    /**
     * Creates the pager and its backing {@code ViewPager2}.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     */
    public FlexPager(FlexContextInterface flexContext) {
        super(flexContext);
        this.viewPager2 = new ViewPager2(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Attaches the pager to the FlexR content slot.
     *
     * @param flexViewBridge the bridge provided by the framework
     * @return {@code true} when the bridge accepted the content
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.viewPager2);
    }

    /**
     * Applies the layout knobs of the pager in one call.
     *
     * @param clipPadding {@code true} to clip the content to the padding of the pager
     * @param clipChildren {@code true} to clip children that overflow the pager bounds
     * @param offscreenPageLimit number of pages kept alive on each side of the current one;
     *                           {@link ViewPager2#OFFSCREEN_PAGE_LIMIT_DEFAULT} keeps the platform
     *                           default
     * @return {@code this}, to allow chaining
     */
    public final FlexPager preparePager(boolean clipPadding, boolean clipChildren, int offscreenPageLimit){
        this.viewPager2.setClipChildren(clipChildren);
        this.viewPager2.setClipToPadding(clipPadding);
        this.viewPager2.setOffscreenPageLimit(offscreenPageLimit);

        return this;
    }

    /**
     * Sets the scroll axis of the pager.
     *
     * @param orientation {@code "H"} or {@code "HORIZONTAL"}, {@code "V"} or {@code "VERTICAL"},
     *                    case-insensitive; {@code null} or blank leaves the current axis untouched
     * @return {@code this}, to allow chaining
     * @throws IllegalArgumentException if the value is neither blank nor a supported name
     */
    public final FlexPager orientation(String orientation){
        if (orientation == null || orientation.trim().isEmpty()) {
            return this;
        }
        if (orientation.equalsIgnoreCase("H") || orientation.equalsIgnoreCase("HORIZONTAL")) {
            this.viewPager2.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
            return this;
        }
        if (orientation.equalsIgnoreCase("V") || orientation.equalsIgnoreCase("VERTICAL")) {
            this.viewPager2.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
            return this;
        }
        throw new IllegalArgumentException(
                "Unknown orientation: \"" + orientation + "\". Use \"H\"/\"HORIZONTAL\" or \"V\"/\"VERTICAL\".");
    }

    /**
     * Sets the transformer applied to every page while it scrolls.
     *
     * @param transformer the transformer to apply; {@code null} removes the current one
     * @return {@code this}, to allow chaining
     */
    public final FlexPager transformers(ViewPager2.PageTransformer transformer){
        this.viewPager2.setPageTransformer(transformer);
        return this;
    }

    /**
     * Installs the adapter that provides the pages.
     *
     * @param adapter the adapter backing the pager; {@code null} clears the current adapter
     * @return {@code this}, to allow chaining
     */
    public final FlexPager adapter(FlexAdapterRcv adapter){
        this.viewPager2.setAdapter(adapter);
        return this;
    }
}
