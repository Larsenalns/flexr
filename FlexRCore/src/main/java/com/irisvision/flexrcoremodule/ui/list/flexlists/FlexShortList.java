// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexlists;

import android.graphics.drawable.ColorDrawable;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapter;
import com.irisvision.flexrthememodule.FlexTheme;

/**
 * Short list backed by an Android {@link ListView}.
 *
 * <p>The choice modes of the platform are exposed as constants of this class, mirroring
 * {@code AbsListView.CHOICE_MODE_*}, so a value can be handed straight to
 * {@link ListView#setChoiceMode(int)}. Installing an adapter replaces the previous one: the last
 * call to {@link #adapter(FlexAdapter)} or {@link #adapterDefault(String[], int)} wins.
 *
 * <pre>{@code
 * new FlexShortList(flexContext)
 *         .adapterDefault(items, FlexShortList.modeSingle)
 *         .divider(2, 1)
 *         .select(0);
 * }</pre>
 *
 * <p>Threading: the class is not thread-safe. Construction, every builder method and
 * {@link #ifThemeChange()} must run on the UI thread that owns the {@code ListView}.
 */
public class FlexShortList extends FlexView {

    /** No choice: a plain list, same value as {@code AbsListView.CHOICE_MODE_NONE}. */
    public static final int modeNone = 0;
    /** Single choice, same value as {@code AbsListView.CHOICE_MODE_SINGLE}. */
    public static final int modeSingle = 1;
    /** Multiple choice, same value as {@code AbsListView.CHOICE_MODE_MULTIPLE}. */
    public static final int modeMulti = 2;
    /** Multiple choice in modal mode, same value as {@code AbsListView.CHOICE_MODE_MULTIPLE_MODAL}. */
    public static final int modeMultiModal = 3;

    /** Sentinel: no palette colour divider is installed. */
    private static final int NO_THEME_COLOR = -1;

    private final ListView listView;

    /**
     * Palette index of the current colour divider, or {@link #NO_THEME_COLOR} when none is set;
     * replayed on every theme change.
     */
    private int dividerThemeColorIndex = NO_THEME_COLOR;

    /**
     * Creates the list and its backing {@code ListView}.
     *
     * @param flexContext the FlexR context that owns the view and its activity
     */
    public FlexShortList(FlexContextInterface flexContext) {
        super(flexContext);
        this.listView = new ListView(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Attaches the underlying {@link ListView} to the FlexR content slot.
     *
     * @param flexViewBridge the bridge provided by the framework
     * @return {@code true} when the bridge accepted the content
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.listView);
    }

    /**
     * Installs a FlexR list adapter.
     *
     * @param adapter the adapter to install; {@code null} clears the current adapter
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList adapter(FlexAdapter adapter){
        this.listView.setAdapter(adapter);
        return this;
    }

    /**
     * Installs a default adapter over an array of strings.
     *
     * <p>The {@code mode} drives both the item presentation and the choice behaviour of the list:
     * {@link #modeNone} for plain rows, {@link #modeSingle}, {@link #modeMulti} or
     * {@link #modeMultiModal}. The mode is applied with {@link ListView#setChoiceMode(int)} and
     * stays in force until another call changes it.
     *
     * @param data the entries to display; the array is referenced, not copied
     * @param mode one of {@link #modeNone}, {@link #modeSingle}, {@link #modeMulti} or
     *             {@link #modeMultiModal}
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList adapterDefault(String[] data, int mode){
        int layout;
        if (mode == modeMulti || mode == modeMultiModal) {
            layout = android.R.layout.simple_list_item_multiple_choice;
        }
        else if (mode == modeSingle) {
            layout = android.R.layout.simple_list_item_single_choice;
        }
        else {
            layout = android.R.layout.simple_list_item_1;      // modeNone: no choice decoration
        }

        this.listView.setAdapter(new ArrayAdapter<>(
                f.getFlexContext(),
                layout,
                data
        ));
        this.listView.setChoiceMode(mode);
        return this;
    }

    /**
     * Sets the choice mode of the list.
     *
     * <p>Prefer the constants of this class ({@link #modeNone}, {@link #modeSingle},
     * {@link #modeMulti}, {@link #modeMultiModal}), which carry the values of
     * {@code AbsListView.CHOICE_MODE_*}.
     *
     * @param modeValue the choice mode to apply
     * @return {@code this}, to allow chaining
     * @throws IllegalArgumentException if {@code modeValue} is outside the supported range
     */
    public final FlexShortList multiselect(int modeValue){
        if (modeValue < modeNone || modeValue > modeMultiModal) {
            throw new IllegalArgumentException(
                    "Unknown choice mode: " + modeValue + ". Expected values: modeNone (0), "
                            + "modeSingle (1), modeMulti (2), modeMultiModal (3).");
        }
        this.listView.setChoiceMode(modeValue);
        return this;
    }

    /**
     * Sets a divider from a FlexR drawable resource name.
     *
     * <p>A drawable divider does not follow the palette, so any colour index installed by the other
     * overload is forgotten.
     *
     * @param drawableDivider name of the FlexR drawable used as divider
     * @param dividerHeight divider height in pixels
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList divider(String drawableDivider, int dividerHeight){
        this.listView.setDivider(f.flexDrawable(drawableDivider));
        this.listView.setDividerHeight(dividerHeight);
        this.dividerThemeColorIndex = NO_THEME_COLOR;           // this divider does not follow the theme
        return this;
    }

    /**
     * Sets a divider from a palette colour.
     *
     * <p>The index is remembered: when the theme changes, {@link #ifThemeChange()} resolves that
     * same index again and repaints the divider with the new colour.
     *
     * @param themeColorIndex palette index of the divider colour
     * @param dividerHeight divider height in pixels
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList divider(int themeColorIndex, int dividerHeight){
        this.dividerThemeColorIndex = themeColorIndex;
        this.listView.setDivider(new ColorDrawable(FlexTheme.load(themeColorIndex)));
        this.listView.setDividerHeight(dividerHeight);
        return this;
    }

    /**
     * Re-resolves the divider colour after a theme change.
     *
     * <p>Does nothing when the current divider was installed as a drawable.
     */
    @Override
    public void ifThemeChange() {
        if (this.dividerThemeColorIndex != NO_THEME_COLOR) {
            this.listView.setDivider(new ColorDrawable(FlexTheme.load(this.dividerThemeColorIndex)));
        }
    }

    /**
     * Scrolls the list so that the given item becomes the selected (or first visible) one.
     *
     * @param selectItem adapter position to bring into view
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList select(int selectItem){
        this.listView.setSelection(selectItem);
        return this;
    }

    /**
     * Registers a click listener on the items.
     *
     * @param onItemClickListener listener notified when an item is clicked
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList onItemClicked(AdapterView.OnItemClickListener onItemClickListener){
        this.listView.setOnItemClickListener(onItemClickListener);
        return this;
    }

    /**
     * Registers a long-click listener on the items.
     *
     * @param onItemLongClickListener listener notified when an item is long-clicked
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList onItemLongClicked(AdapterView.OnItemLongClickListener onItemLongClickListener){
        this.listView.setOnItemLongClickListener(onItemLongClickListener);
        return this;
    }

    /**
     * Registers a selection listener.
     *
     * @param onItemSelectedListener listener notified whenever the selection changes
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList onItemSelected(AdapterView.OnItemSelectedListener onItemSelectedListener){
        this.listView.setOnItemSelectedListener(onItemSelectedListener);
        return this;
    }

    /**
     * Registers the listener used by the modal multi-choice mode.
     *
     * <p>The listener only receives callbacks while the list runs in {@link #modeMultiModal}.
     *
     * @param multiChoiceModeListener listener notified when the modal selection starts or changes
     * @return {@code this}, to allow chaining
     */
    public final FlexShortList onSelectMode(AbsListView.MultiChoiceModeListener multiChoiceModeListener){
        this.listView.setMultiChoiceModeListener(multiChoiceModeListener);
        return this;
    }
}
