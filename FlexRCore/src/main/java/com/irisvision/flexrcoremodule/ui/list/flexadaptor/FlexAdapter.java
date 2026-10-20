// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexadaptor;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * The historical adapter of FlexR: {@code ListView} ({@code FlexShortList}), {@code Spinner}
 * ({@code FlexSpinner}) and the suggestion fields ({@code FlexInput}).
 *
 * <h2>A single notion of row</h2>
 *
 * <p>This adapter and {@link FlexAdapterRcv} know <b>one</b> way of describing a row: a
 * {@link FlexItem}. A row is always built the same way ({@code dynamicBuild} ->
 * {@code attachEvents} -> {@code initialBusinessLogic}, then {@code bindData}), and its vocabulary
 * is the one of a screen.
 *
 * <p>Consequence: the <b>same row class</b> serves a {@code ListView}, a {@code Spinner}, a
 * suggestion field and a {@code RecyclerView}.
 *
 * <pre>{@code
 * public class UserAdapter extends FlexAdapter {
 *     public UserAdapter(FlexContextInterface f, List<FlexItemData> data) { super(f, data); }
 *
 *     @Override protected FlexItem item(FlexContextInterface f) {
 *         return UserItem.instance(f);            // the same row as for a FlexList
 *     }
 * }
 * }</pre>
 *
 * <p>The row is <b>released before each binding</b> ({@link FlexItem#forget()}), exactly as at the
 * recycle step of a {@code RecyclerView}: a reused {@code convertView} therefore keeps neither the
 * listeners nor the subscriptions of the previous datum.
 *
 * <p><b>Threading.</b> Every {@code BaseAdapter} callback runs on the main thread, and binding a
 * row writes into views. Change the data on the main thread, and call
 * {@code notifyDataSetChanged()} yourself when you replace the data set through
 * {@link #initAdapter(List)} after the view is attached.
 */
public abstract class FlexAdapter extends BaseAdapter{

    /** Full data set, as provided to the adapter. */
    protected List<FlexItemData> flexItemData;
    /** Data actually displayed: the full data set, or the result of the last filter. */
    protected List<FlexItemData> flexItemDataFiltered;

    /** FlexR context, forwarded to the rows built by {@link #item(FlexContextInterface)}. */
    private final FlexContextInterface f;

    /**
     * Creates an adapter with an empty data set.
     *
     * @param f the FlexR context of the screen, forwarded to every row built by
     *          {@link #item(FlexContextInterface)}
     */
    public FlexAdapter(FlexContextInterface f) {
        this.f = f;
        this.flexItemData = new ArrayList<>();
        this.flexItemDataFiltered = this.flexItemData;
    }

    /**
     * Creates an adapter over an existing data set.
     *
     * @param f    the FlexR context of the screen
     * @param data the data to display; {@code null} is replaced by an empty, mutable list
     */
    public FlexAdapter(FlexContextInterface f, List<FlexItemData> data){
        this.f = f;
        initAdapter(data);
    }

    /**
     * Row factory: <b>the</b> notion of row in FlexR, shared with {@link FlexAdapterRcv}.
     *
     * <p>Return a new {@link FlexItem}. The expected pattern is the static factory described in
     * {@code FlexItem}: it provides the {@code FrameLayout} required by the {@code ViewHolder}
     * constructor, and it is the only place where that constraint shows up.
     *
     * @param f the FlexR context of the screen, to hand over to the factory
     * @return a new, not yet bound row
     */
    protected abstract FlexItem item(FlexContextInterface f);

    /**
     * Replaces the displayed data set.
     *
     * <p>Used by the one-argument constructor and by {@code FlexAdapterFilter} to start from an empty
     * list rather than a null field. It does not notify the observer: call
     * {@code notifyDataSetChanged()} when the data set changes after the view is attached.
     *
     * @param data the new data; {@code null} is replaced by an empty, mutable list
     */
    public final void initAdapter(List<FlexItemData> data) {
        this.flexItemData = (data == null) ? new ArrayList<>() : data;
        this.flexItemDataFiltered = this.flexItemData;
    }

    /** Data actually displayed; fails explicitly when the adapter has not been initialized. */
    private List<FlexItemData> data(){
        if (this.flexItemDataFiltered == null) {
            throw new IllegalStateException(getClass().getName()
                    + ": no data. Call initAdapter(...) before using the adapter.");
        }
        return this.flexItemDataFiltered;
    }

    /**
     * Returns the number of displayed items.
     *
     * @return the size of the filtered data set, or of the full one when no filter ran
     */
    @Override
    public final int getCount() {
        return data().size();
    }

    /**
     * Returns the datum displayed at the given position.
     *
     * @param position the position of the item in the displayed data
     * @return the datum at that position
     * @throws IndexOutOfBoundsException if the position falls outside the displayed data
     */
    @Override
    public final FlexItemData getItem(int position) {
        return data().get(position);
    }

    /**
     * Item id: the position.
     *
     * <p>The stable key of the datum ({@code FlexItemData.stableId()}) is <b>not</b> used here:
     * {@code BaseAdapter.hasStableIds()} stays false, because a non-unique key (the object identity
     * by default) would produce inconsistent selections in a {@code ListView}. The RecyclerView path
     * enables stable ids on demand ({@code FlexAdapterRcv.stableIds(true)}).
     *
     * @param position the position of the item in the displayed data
     * @return the position itself
     */
    @Override
    public final long getItemId(int position) {
        return position;
    }

    /**
     * Returns the row view for the given position, creating or recycling it as needed.
     *
     * <p>When {@code convertView} is {@code null} the row is created by
     * {@link #item(FlexContextInterface)}, initialized by {@link FlexItem#init(FlexContextInterface)}
     * and tagged on its container, so the next call reuses it. In both cases the previous datum is
     * released by {@link FlexItem#forget()} before {@link FlexItem#bindData(FlexItemData)} runs.
     *
     * <p>Must be called on the main thread.
     *
     * @param position    the position of the datum in the displayed data
     * @param convertView a view previously returned by this adapter, or {@code null}
     * @param parent      the enclosing view group, as required by {@code BaseAdapter}
     * @return the row container, never {@code null}
     * @throws IllegalStateException if {@code item(...)} returned {@code null}, or if a recycled view
     *                               does not carry a {@link FlexItem} tag
     */
    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        FlexItem row;

        if (convertView == null){
            row = item(this.f);
            if (row == null) {
                throw new IllegalStateException(getClass().getName()
                        + ": item(f) returned null. A FlexR row is a FlexItem; see the factory"
                        + " pattern described in FlexItem: "
                        + "return UserItem.instance(f);  // -> new UserItem(new FrameLayout(f.getFlexContext()))");
            }
            row.init(this.f);
            convertView = row.itemView;
            // The row container carries its own reference: that is what the recycle path looks up.
            convertView.setTag(row);
        }
        else {
            Object tag = convertView.getTag();
            if (!(tag instanceof FlexItem)) {
                throw new IllegalStateException(getClass().getName()
                        + ": the recycled view does not carry a FlexItem (missing tag, or tag of another type)."
                        + " Was this convertView created by another adapter?");
            }
            row = (FlexItem) tag;
        }

        // Same guarantee as at the recycle step of a RecyclerView: the row releases what it had
        // acquired for the previous datum before receiving a new one.
        row.forget();
        row.bindData(getItem(position));

        return convertView;
    }

    /**
     * Returns the native view of a {@code FlexView}: the facade bridge of the adapter.
     *
     * <p>Useful wherever a FlexR view must be inserted into a native hierarchy that does not know
     * FlexR (a list row, an empty-state container...).
     *
     * <p>Design note: {@code FlexView.content(...)} only accepts a closed set of bridges (controlled
     * extensibility). This method therefore reuses the bridge of the adapter, which the core already
     * recognizes, instead of introducing a new one, which would be rejected.
     *
     * @param flexView the FlexR view to expose as an Android view
     * @return the native view produced by the bridge
     * @throws IllegalArgumentException if {@code flexView} is {@code null}
     */
    public static View nativeView(FlexView flexView){
        if (flexView == null) {
            throw new IllegalArgumentException("nativeView(null): provide the FlexView to bridge.");
        }
        FlexAdapterBridge bridge = new FlexAdapterBridge();
        flexView.contentWrapper(bridge);
        return bridge.view;
    }

    /**
     * Bridge that captures the native view built for a {@code FlexView}.
     *
     * <p>Used by {@link FlexAdapter#nativeView(FlexView)} only: a fresh instance is created per call
     * and dropped once the view has been read.
     */
    public static final class FlexAdapterBridge implements FlexViewBridge{
        private View view;

        /**
         * Receives the native view built for the {@code FlexView} given to the adapter.
         *
         * @param view the native view, stored for {@link FlexAdapter#nativeView(FlexView)}
         */
        @Override
        public void bridge(View view) {
            this.view = view;
        }
    }
}
