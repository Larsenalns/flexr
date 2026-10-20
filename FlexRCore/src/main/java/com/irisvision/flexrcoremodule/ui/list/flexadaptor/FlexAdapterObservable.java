// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexadaptor;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.observable.FlexProvider;

/**
 * Adapter whose rows <b>observe the state of their own item</b>.
 *
 * <h2>Purpose</h2>
 *
 * <p>With {@link FlexAdapterRcv} alone, a state change of one item propagates through the list
 * ({@code submitData(...)} or {@code notifyItemChanged(...)}), hence a new binding of the row. Here
 * each row subscribes to the {@link FlexProvider} of its item and updates <b>itself</b>: a change
 * repaints only the affected row, with neither a list notification nor {@code DiffUtil}.
 *
 * <pre>{@code
 * public class UserAdapter extends FlexAdapterObservable {
 *     public UserAdapter(FlexContextInterface f) { super(f); }
 *
 *     @Override protected FlexItem item(FlexContextInterface f) { return UserItem.instance(f); }
 *
 *     @Override protected FlexProvider providerOf(FlexItemData item) {
 *         return FlexReactive.get("user:" + ((UserData) item).getName());   // the state of THIS item
 *     }
 * }
 * }</pre>
 *
 * <p>On the row side nothing special has to be wired: override {@link FlexItem#onProviderChanged}
 * to refresh the views, and {@code FlexItem.forgettingFlexConsumers()} for whatever was acquired
 * outside {@code observe(...)}.
 *
 * <h2>The release invariant</h2>
 *
 * <p>The adapter releases every row <b>before</b> binding it ({@code FlexItem.forget()}), and on
 * every recycle. The subscription of a row therefore cannot outlive the datum that created it: after
 * scrolling, the number of subscribers in the Fragment scope never exceeds the number of live rows.
 * That invariant is what {@code FlexProviderScopeTest} checks.
 *
 * <h2>Threading</h2>
 *
 * <p>A row is notified <b>on the main thread</b> ({@link FlexItem#onProviderChanged}): the library
 * performs the hop, so the developer writes UI code without any special care.
 */
public abstract class FlexAdapterObservable extends FlexAdapterRcv {

    /**
     * Creates an adapter with an empty data set.
     *
     * @param f the FlexR context of the screen, forwarded to every row built by
     *          {@code item(FlexContextInterface)}
     */
    public FlexAdapterObservable(FlexContextInterface f) {
        super(f);
    }

    /**
     * State provider of this item, or {@code null} when the item has no observable state.
     *
     * <p>Return the provider <b>of this item</b> (one instance or one key per item): that is what
     * makes the update granular. Returning the same provider for every item still works, but each
     * row is then notified of every change.
     *
     * @param item the datum of the row currently being bound
     * @return the provider to observe, or {@code null} to leave the row without subscription
     */
    protected abstract FlexProvider providerOf(FlexItemData item);

    /**
     * Subscribes the freshly bound row to the provider of its item.
     *
     * <p>Called after {@code bindData(...)} and after {@link FlexItem#forget()}, so the row holds no
     * subscription to the previous datum at this point.
     *
     * @param holder   the row just bound
     * @param position the position of its datum in the displayed list
     */
    @Override
    protected final void onRowBound(FlexItem holder, int position) {
        FlexProvider provider = providerOf(itemAt(position));
        if (provider != null) {
            holder.observe(provider);
        }
    }
}
