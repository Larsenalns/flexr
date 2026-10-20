// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexadaptor;

import android.widget.Filter;
import android.widget.Filterable;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Filterable adapter: the base of suggestion lists ({@code FlexInput} in autocompletion mode).
 *
 * <p>The developer only provides the filter <b>condition</b>
 * ({@link #flexFilterCondition(CharSequence)}); the two-stage filtering required by Android
 * (computation off the main thread, publication on the main thread) stays the responsibility of the
 * library.
 *
 * <p>Contracts:
 * <ul>
 *   <li>a condition that returns {@code null} means "no result" and yields an empty list rather
 *       than a {@code NullPointerException};</li>
 *   <li>the result of the filter is validated element by element, so a heterogeneous list never
 *       produces an unchecked cast;</li>
 *   <li>the {@link Filter} instance is built once and <b>reused</b> by every call to
 *       {@link #getFilter()}.</li>
 * </ul>
 *
 * <p>The adapter holds two lists: the full data set and the filtered view of it. Filtering only
 * swaps the second one, then notifies the observer on the main thread.
 */
public abstract class FlexAdapterFilter extends FlexAdapter implements Filterable {

    /**
     * Creates an adapter with an empty data set.
     *
     * @param f the FlexR context of the screen, forwarded to every row built by
     *          {@code item(FlexContextInterface)}
     */
    public FlexAdapterFilter(FlexContextInterface f) {
        super(f);
    }

    /**
     * Creates an adapter over an existing data set.
     *
     * @param f    the FlexR context of the screen
     * @param data the data to display; {@code null} is replaced by an empty, mutable list
     */
    public FlexAdapterFilter(FlexContextInterface f, List<FlexItemData> data) {
        super(f, data);
    }

    /**
     * Filter condition: returns the data that satisfy the constraint.
     *
     * <p>Called <b>off the main thread</b> (that is the {@code Filter} contract), so do not touch
     * the UI from it. Returning {@code null} is accepted and means no result.
     *
     * @param constraint the text typed by the user; empty when the field is cleared
     * @return the matching data, or {@code null} for an empty result
     */
    public abstract List<FlexItemData> flexFilterCondition(CharSequence constraint);

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<FlexItemData> filtered;
            if (constraint == null || constraint.length() == 0) {
                filtered = new ArrayList<>(flexItemData);
            } else {
                filtered = flexFilterCondition(constraint);
                if (filtered == null) {
                    filtered = new ArrayList<>();           // no result: empty list
                }
            }

            FilterResults results = new FilterResults();
            results.values = filtered;
            results.count = filtered.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Object values = (results == null) ? null : results.values;

            if (values instanceof List) {
                List<?> raw = (List<?>) values;
                List<FlexItemData> filtered = new ArrayList<>(raw.size());
                for (Object element : raw) {
                    if (element instanceof FlexItemData) {
                        filtered.add((FlexItemData) element);
                    }
                }
                flexItemDataFiltered = filtered;
            }
            else {
                flexItemDataFiltered = new ArrayList<>();
            }

            notifyDataSetChanged();
        }
    };

    /**
     * Returns the filter of this adapter.
     *
     * <p>The instance is built with the adapter and reused on every call, so a filtering already in
     * progress is never dropped.
     *
     * @return the single filter instance of this adapter
     */
    @Override
    public final Filter getFilter() {
        return this.filter;
    }
}
