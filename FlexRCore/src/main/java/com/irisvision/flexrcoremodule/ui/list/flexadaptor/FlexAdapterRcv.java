// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexadaptor;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter of FlexR: it creates {@link FlexItem} rows and hands them their datum.
 *
 * <h2>Data ownership</h2>
 *
 * <p>The adapter carries the list of displayed data and exposes a mutation API
 * ({@link #setData}, {@link #submitData}, {@link #addItem}, {@link #removeItem}...) that notifies
 * RecyclerView consistently: the index arithmetic and the notifications stay in a single place,
 * which is why {@code getItemCount()}, {@code getItemId(...)} and the other framework callbacks are
 * {@code final}.
 *
 * <h2>Lifecycle of a row</h2>
 *
 * <p>Before every {@code bindData(...)}, and on every recycle, the row is <b>released</b>
 * ({@link FlexItem#forget()}). Without that call a recycled holder would keep the listeners and the
 * subscriptions of the <b>first</b> datum it was bound to.
 *
 * <h2>Data: two notification levels</h2>
 * <ul>
 *   <li><b>structure</b>: {@link #setData}, {@link #submitData}, {@link #addItem},
 *       {@link #removeItem}... the composition of the list changes, so RecyclerView has to be told
 *       (those methods do it);</li>
 *   <li><b>state of one item</b>: nothing to call, a row subscribed to a {@code FlexProvider}
 *       (see {@link FlexItem#observe}) updates itself, alone, with no list notification.</li>
 * </ul>
 *
 * <h2>Interaction</h2>
 *
 * <p>Clicks are installed <b>once</b>, when the holder is created, by the adapter: the developer
 * receives the position and the datum, and has nothing to rewire on each binding.
 *
 * <h2>Threading</h2>
 *
 * <p>Every method of this class must be called on the main thread: it mutates the adapter state
 * and calls {@code notifyItemInserted}, {@code notifyItemChanged}... which RecyclerView only accepts
 * there.
 */
public abstract class FlexAdapterRcv extends RecyclerView.Adapter<FlexItem>{

    /** Click on a row: the position and the bound datum are provided. */
    public interface OnItemClickListener {
        /**
         * Called when a row is tapped.
         *
         * @param item     the row that was tapped
         * @param position its position in the displayed list
         * @param data     the datum bound to that row
         */
        void onItemClick(FlexItem item, int position, FlexItemData data);
    }

    /** Long press on a row. Return {@code true} to consume the event. */
    public interface OnItemLongClickListener {
        /**
         * Called when a row is long-pressed.
         *
         * @param item     the row that was long-pressed
         * @param position its position in the displayed list
         * @param data     the datum bound to that row
         * @return {@code true} to consume the event, {@code false} to let it through
         */
        boolean onItemLongClick(FlexItem item, int position, FlexItemData data);
    }

    /** Selection change of a row. */
    public interface OnSelectionChangedListener {
        /**
         * Called after the selection state of a row changed.
         *
         * @param position the position of the row in the displayed list
         * @param data     the datum bound to that row
         * @param selected the new selection state of the row
         */
        void onSelectionChanged(int position, FlexItemData data, boolean selected);
    }

    /** No selection. */
    public static final int SELECTION_NONE = 0;
    /** A single row at a time. */
    public static final int SELECTION_SINGLE = 1;
    /** Several rows. */
    public static final int SELECTION_MULTIPLE = 2;

    private final FlexContextInterface f;

    /** Displayed data. Changed only through the API of this class, which keeps indexes and notifications in step. */
    private List<FlexItemData> dataList;

    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;

    private int selectionMode = SELECTION_NONE;
    private OnSelectionChangedListener selectionListener;

    /** Stable keys of the selected items (see {@link FlexItemData#stableId()}). */
    private final Set<Long> selectedKeys = new LinkedHashSet<>();

    /**
     * Creates an adapter with an empty data set.
     *
     * @param f the FlexR context of the screen, forwarded to
     *          {@link #item(FlexContextInterface, int)}
     */
    public FlexAdapterRcv(FlexContextInterface f) {
        this.f = f;
        this.dataList = new ArrayList<>();
    }

    /**
     * Creates a row for the given view type and initializes it.
     *
     * <p>The row is built by {@link #item(FlexContextInterface, int)} and then initialized by
     * {@link FlexItem#init(FlexContextInterface)}, which runs {@code dynamicBuild()},
     * {@code attachEvents()} and {@code initialBusinessLogic()}. Row events are installed once,
     * here.
     *
     * <p>Called by RecyclerView on the main thread.
     *
     * @param parent   the view group the new row will be attached to
     * @param viewType the type returned by {@link #viewType(int)} for the bound position
     * @return a new, initialized row, with no datum bound yet
     */
    @NonNull
    @Override
    public final FlexItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FlexItem item = item(this.f, viewType);
        item.init(this.f);
        bindRowEvents(item);
        return item;
    }

    /**
     * Row factory: return a new {@code FlexItem} (see the static factory pattern in {@link FlexItem},
     * which provides the expected {@code FrameLayout}).
     *
     * <p>This is the override point of the simple case, where every row has the same shape.
     *
     * @param f the FlexR context of the screen
     * @return a new row, neither initialized nor bound
     */
    protected abstract FlexItem item(FlexContextInterface f);

    /**
     * Factory per row <b>type</b> (header, section, item...).
     *
     * <p>By default it delegates to {@link #item(FlexContextInterface)}, so a single-shape list has
     * nothing to write.
     *
     * @param f        the FlexR context of the screen
     * @param viewType the type returned by {@link #viewType(int)} for that position
     * @return a new row of that type
     */
    protected FlexItem item(FlexContextInterface f, int viewType){
        return item(f);
    }

    /**
     * Row type at a given position; override it together with
     * {@link #item(FlexContextInterface, int)} to mix several shapes.
     *
     * @param position the position in the displayed list
     * @return the type of the row, {@code 0} by default
     */
    protected int viewType(int position){
        return 0;
    }

    /**
     * Returns the row type at the given position, as returned by {@link #viewType(int)}.
     *
     * @param position the position in the displayed list
     * @return the row type of that position
     */
    @Override
    public final int getItemViewType(int position) {
        return viewType(position);
    }

    /**
     * Binds the datum of the given position to the holder.
     *
     * <p>In order: the row releases what it had acquired for the previous item
     * ({@link FlexItem#forget()}), receives the selection state, gets its datum
     * ({@link FlexItem#bindData(FlexItemData)}), then goes through
     * {@link #onRowBound(FlexItem, int)}.
     *
     * <p>Called by RecyclerView on the main thread.
     *
     * @param holder   the row to bind
     * @param position the position of the datum in the displayed list
     */
    @Override
    public final void onBindViewHolder(@NonNull FlexItem holder, int position) {
        // The row first releases what it had acquired for the previous item, then receives the datum.
        holder.forget();

        // Selection state set BEFORE the binding: the row can read it in bindData()
        // (View.setSelected feeds android:state_selected of the drawable selectors).
        holder.itemView.setSelected(isSelected(position));

        holder.bindData(dataList.get(position));

        // Hook point: this is where an observable adapter wires the state of the item
        // (see FlexAdapterObservable). The datum is already set, so the row views exist.
        onRowBound(holder, position);
    }

    /**
     * Called right after a row has been bound: the datum is set and the row carries no subscription
     * yet, since it has just been released.
     *
     * <p>Override point for adapters that add behavior to the binding cycle. The base class does
     * nothing.
     *
     * @param holder   the row just bound
     * @param position the position of its datum in the displayed list
     */
    protected void onRowBound(FlexItem holder, int position){ }

    /**
     * Releases every subscription of a holder that RecyclerView puts back in its pool.
     *
     * <p>Called by RecyclerView on the main thread.
     *
     * @param holder the row being recycled
     */
    @Override
    public final void onViewRecycled(@NonNull FlexItem holder) {
        holder.forget();
    }

    /**
     * Number of displayed items, derived from the data list.
     *
     * <p>Deliberately {@code final}: letting a subclass return a constant makes the adapter
     * silently empty.
     *
     * @return the size of the data list
     */
    @Override
    public final int getItemCount() {
        return this.dataList.size();
    }

    /**
     * Item id, taken from the datum (see {@link FlexItemData#stableId()}).
     *
     * <p>RecyclerView only uses it once {@link #stableIds(boolean)} has been enabled.
     *
     * @param position the position in the displayed list
     * @return the stable id of the datum at that position, or {@code RecyclerView.NO_ID} for a
     *         {@code null} datum
     */
    @Override
    public final long getItemId(int position) {
        return stableIdOf(this.dataList.get(position));
    }

    /* *** Interaction *** */

    /**
     * Registers the click listener for all rows; a later call replaces the previous listener.
     *
     * @param listener the listener, or {@code null} to remove the current one
     * @return this adapter, for chaining
     */
    public final FlexAdapterRcv onItemClick(OnItemClickListener listener){
        this.itemClickListener = listener;
        return this;
    }

    /**
     * Registers the long-click listener for all rows; a later call replaces the previous listener.
     *
     * @param listener the listener, or {@code null} to remove the current one
     * @return this adapter, for chaining
     */
    public final FlexAdapterRcv onItemLongClick(OnItemLongClickListener listener){
        this.itemLongClickListener = listener;
        return this;
    }

    private void bindRowEvents(FlexItem item){
        // The listeners read the fields at click time, so setting them after the holders have been
        // created also works.
        //
        // Position: getLayoutPosition(), not getBindingAdapterPosition(), which only exists as of
        // RecyclerView 1.2 while the version resolved here is older. It is not deprecated on either
        // version; the only subtlety is that, during a move animation, it may lag by one layout
        // pass.
        item.itemView.setOnClickListener(view -> {
            if (itemClickListener == null) return;
            int position = item.getLayoutPosition();
            if (position == RecyclerView.NO_POSITION || position >= dataList.size()) return;
            itemClickListener.onItemClick(item, position, dataList.get(position));
        });
        item.itemView.setOnLongClickListener(view -> {
            if (itemLongClickListener == null) return false;
            int position = item.getLayoutPosition();
            if (position == RecyclerView.NO_POSITION || position >= dataList.size()) return false;
            return itemLongClickListener.onItemLongClick(item, position, dataList.get(position));
        });
    }

    /* *** Selection *** */

    /**
     * Selection mode: {@link #SELECTION_NONE}, {@link #SELECTION_SINGLE} or
     * {@link #SELECTION_MULTIPLE} (same values as the constants of {@code FlexShortList}).
     *
     * <p>The visual rendering stays with the developer: {@code View.setSelected(...)} is applied to
     * the row view before it is bound, so a drawable selector ({@code android:state_selected}) or a
     * read of {@link #isSelected(int)} inside {@code FlexItem.bindData(...)} is enough to mark the
     * row.
     *
     * @param mode the new selection mode
     * @return this adapter, for chaining
     * @throws IllegalArgumentException if the mode is not one of the three constants
     */
    public final FlexAdapterRcv selectionMode(int mode){
        if (mode != SELECTION_NONE && mode != SELECTION_SINGLE && mode != SELECTION_MULTIPLE) {
            throw new IllegalArgumentException(
                    "Unknown selection mode: " + mode + ". Expected values: SELECTION_NONE (0),"
                            + " SELECTION_SINGLE (1), SELECTION_MULTIPLE (2).");
        }
        this.selectionMode = mode;
        if (mode == SELECTION_NONE) {
            clearSelection();
        }
        return this;
    }

    /** Selection change listener; a later call replaces the previous listener. */
    public final FlexAdapterRcv onSelectionChanged(OnSelectionChangedListener listener){
        this.selectionListener = listener;
        return this;
    }

    /**
     * Selects (or deselects) the row at that position.
     *
     * <p>In {@link #SELECTION_SINGLE} mode the new row replaces the previous one, and a second call
     * on the same row deselects it. In {@link #SELECTION_MULTIPLE} mode every call toggles that row.
     *
     * @param position the position of the row in the displayed list
     * @return this adapter, for chaining
     * @throws IllegalStateException     if no selection mode is active
     * @throws IndexOutOfBoundsException if the position falls outside the displayed data
     */
    public final FlexAdapterRcv select(int position){
        if (selectionMode == SELECTION_NONE) {
            throw new IllegalStateException(getClass().getName()
                    + ": select(...) without a selection mode. Call selectionMode(SELECTION_SINGLE)"
                    + " first, or selectionMode(SELECTION_MULTIPLE).");
        }
        checkPosition(position, this.dataList.size() - 1);

        long key = stableIdOf(this.dataList.get(position));
        boolean selected;

        if (selectionMode == SELECTION_SINGLE) {
            selected = !(this.selectedKeys.size() == 1 && this.selectedKeys.contains(key));
            this.selectedKeys.clear();
            if (selected) {
                this.selectedKeys.add(key);
            }
        }
        else {
            selected = this.selectedKeys.add(key);
            if (!selected) {
                this.selectedKeys.remove(key);
            }
        }

        notifyItemChanged(position);

        if (this.selectionListener != null) {
            this.selectionListener.onSelectionChanged(position, this.dataList.get(position), selected);
        }
        return this;
    }

    /** Tells whether the row at this position is selected. */
    public final boolean isSelected(int position){
        if (position < 0 || position >= this.dataList.size()) {
            return false;
        }
        return this.selectedKeys.contains(stableIdOf(this.dataList.get(position)));
    }

    /** Number of selected rows. */
    public final int selectedCount(){
        return this.selectedKeys.size();
    }

    /** Clears the selection. */
    public final FlexAdapterRcv clearSelection(){
        if (this.selectedKeys.isEmpty()) {
            return this;
        }
        this.selectedKeys.clear();
        notifyDataSetChanged();
        return this;
    }

    /** Forgets the keys that no longer match a present datum (removal, replacement). */
    private void pruneSelection(){
        if (this.selectedKeys.isEmpty()) {
            return;
        }
        Set<Long> present = new LinkedHashSet<>();
        for (FlexItemData data : this.dataList) {
            present.add(stableIdOf(data));
        }
        this.selectedKeys.retainAll(present);
    }

    /* *** Data API *** */

    /**
     * Enables stable ids: RecyclerView then animates moves correctly.
     *
     * <p>Disabled by default, deliberately: the default id is the object identity (see
     * {@link FlexItemData#stableId()}), which is enough for rendering but has to be replaced by a
     * real business key for reliable animations. Leaving the choice to the developer avoids
     * enabling a behavior on a key that may not be unique.
     *
     * @param enabled {@code true} to report stable ids to RecyclerView
     * @return this adapter, for chaining
     */
    public final FlexAdapterRcv stableIds(boolean enabled){
        setHasStableIds(enabled);
        return this;
    }

    /**
     * Replaces the displayed list and repaints everything.
     *
     * @param data the new data; {@code null} is treated as an empty list
     */
    public final void setData(List<? extends FlexItemData> data){
        this.dataList = copyOf(data);
        pruneSelection();
        notifyDataSetChanged();
    }

    /**
     * Replaces the data and notifies only the <b>differences</b>: insertions, removals and moves are
     * computed, and only the affected rows are touched.
     *
     * <p>Suited to a structural refresh (sorting, filtering, arrival of data). The <b>state</b>
     * change of an item already present does not need to go through here: a row subscribed to a
     * {@code FlexProvider} updates itself.
     *
     * <p>The comparison relies on {@link FlexItemData#stableId()} for identity and on
     * {@code equals} for the content.
     *
     * @param newData the new data; {@code null} is treated as an empty list
     */
    public final void submitData(List<? extends FlexItemData> newData){
        final List<FlexItemData> oldList = this.dataList;
        final List<FlexItemData> newList = copyOf(newData);

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return stableIdOf(oldList.get(oldPosition)) == stableIdOf(newList.get(newPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                FlexItemData oldItem = oldList.get(oldPosition);
                return oldItem != null && oldItem.equals(newList.get(newPosition));
            }
        });

        this.dataList = newList;
        pruneSelection();
        result.dispatchUpdatesTo(this);
    }

    /** Read-only view of the current data. */
    public final List<FlexItemData> data(){
        return Collections.unmodifiableList(this.dataList);
    }

    /** Datum at a position. */
    public final FlexItemData itemAt(int position){
        return this.dataList.get(position);
    }

    /** Appends at the end of the list. */
    public final void addItem(FlexItemData item){
        if (item == null) return;
        this.dataList.add(item);
        notifyItemInserted(this.dataList.size() - 1);
    }

    /** Inserts at a position. */
    public final void insertItem(int position, FlexItemData item){
        if (item == null) return;
        checkPosition(position, this.dataList.size());
        this.dataList.add(position, item);
        notifyItemInserted(position);
    }

    /** Replaces the datum of a position: only that row is repainted. */
    public final void updateItem(int position, FlexItemData item){
        if (item == null) return;
        checkPosition(position, this.dataList.size() - 1);
        this.dataList.set(position, item);
        pruneSelection();
        notifyItemChanged(position);
    }

    /** Removes the datum of a position. */
    public final void removeItem(int position){
        checkPosition(position, this.dataList.size() - 1);
        this.dataList.remove(position);
        pruneSelection();
        notifyItemRemoved(position);
    }

    /** Empties the list. */
    public final void clearData(){
        int size = this.dataList.size();
        this.dataList.clear();
        pruneSelection();
        if (size > 0) {
            notifyItemRangeRemoved(0, size);
        }
    }

    private static List<FlexItemData> copyOf(List<? extends FlexItemData> data){
        return (data == null) ? new ArrayList<>() : new ArrayList<>(data);
    }

    private static long stableIdOf(FlexItemData data){
        return (data == null) ? RecyclerView.NO_ID : data.stableId();
    }

    private static void checkPosition(int position, int max){
        if (position < 0 || position > max) {
            throw new IndexOutOfBoundsException(
                    "Position " + position + " outside the list (accepted bounds: 0 to " + max + ").");
        }
    }
}
