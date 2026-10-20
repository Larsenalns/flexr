// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexadaptor;

/**
 * Data model for one row of a list.
 *
 * <p>This is the <b>single model</b> of the list family: it backs a {@code RecyclerView}
 * ({@code FlexList}, {@code FlexPager}) as well as a {@code ListView} or a {@code Spinner}
 * ({@code FlexShortList}, {@code FlexSpinner}), and the suggestion field of {@code FlexInput}
 * through {@code FlexAdapterFilter}.
 *
 * <p><b>The suggestion field.</b> The drop-down list of a {@code FlexInput} renders the
 * {@code toString()} of the model, so a subclass reached through that path must override it,
 * otherwise it displays its object identity.
 */
public abstract class FlexItemData{

    /**
     * Returns the <b>stable</b> identity of this datum, used to recognize one item across lists.
     *
     * <p>It serves two purposes:
     * <ul>
     *   <li>{@code FlexAdapterRcv.getItemId(...)} when stable ids are enabled
     *       ({@code stableIds(true)}), which lets RecyclerView animate moves correctly;</li>
     *   <li>{@code submitData(...)}, which uses it to notify only the real differences
     *       (insertions, removals, moves) instead of repainting everything.</li>
     * </ul>
     *
     * <p>The default is the object identity, so two distinct models are two distinct items, which
     * is enough as long as one instance represents one item. A subclass that rebuilds its models
     * (for instance when refreshing from a data source) <b>must</b> override this method to return
     * a business key (a database id, a natural key...); otherwise every refresh appears as a full
     * replacement.
     *
     * @return a key identifying this datum across adapter updates; it must stay constant for the
     *         same logical item and must not depend on the position of the item in a list
     */
    public long stableId(){
        return System.identityHashCode(this);
    }
}
