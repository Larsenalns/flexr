// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.view.View;

import com.google.android.material.chip.ChipGroup;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.core.FlexViewGroup;

import java.util.List;

/**
 * Group of tag chips, backed by a Material {@code ChipGroup}.
 *
 * <p>Children are added and removed through the {@code FlexViewGroup} API; this class only
 * exposes the chip-specific options: single selection and the checked-state listener.
 *
 * <p><b>Threading:</b> must be created and configured on the UI thread; the checked-state
 * listener is invoked on that thread by the Android framework.
 */
public class FlexTagGroup extends FlexViewGroup {
    private ChipGroup chipGroup;
    //private FlexTagWrapper wrapper = new FlexTagWrapper();
    public FlexTagGroup(FlexContextInterface flexContext) {
        super(flexContext);
        this.chipGroup = new ChipGroup(f.getFlexContext());
        initInnerBridge();
    }

    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.chipGroup);
    }

    /**
     * Bridges the native {@code ChipGroup} to a chip bridge.
     *
     * <p><b>Internal bridge only.</b> This method is public because
     * {@code FlexViewGroup.add(...)} lives in another package and must be able to call it, but it
     * exposes the native view of the group. Adding views to that view directly would desynchronize
     * the Java child list from the native hierarchy, and the indices used by {@code child(int)}
     * and {@code remove(int)} would become wrong. Use {@code add(...)} / {@code remove(...)}
     * instead.
     *
     * @param wrapper bridge that receives the native {@code ChipGroup}
     */
    public final void chipWrapper(ChipUnWrapper wrapper){
        wrapper.bridge(this.chipGroup);
    }

    /**
     * Enables or disables single selection in the group.
     *
     * @param value {@code true} to allow at most one checked chip, {@code false} for multiple
     * @return this instance, for chaining
     */
    public final FlexTagGroup singleSelection(boolean value){
        this.chipGroup.setSingleSelection(value);
        return this;
    }
    /**
     * Registers the listener notified when the set of checked chips changes.
     *
     * @param onCheckedStateChangeListener listener to notify, or {@code null} to clear it
     */
    public final void onCheckedChange(ChipGroup.OnCheckedStateChangeListener onCheckedStateChangeListener){
        this.chipGroup.setOnCheckedStateChangeListener(onCheckedStateChangeListener);
    }

    //Normally these methods belong in FlexViewGroup and in the wrapper
    /*public final FlexTagGroup addTag(FlexTag flexTag){
        flexTag.contentWrapper(wrapper);
        this.chipGroup.addView(wrapper.view);
        return this;
    }
    public final FlexTagGroup addTag(FlexTag[] flexTags){
        for (FlexTag tag:flexTags) {
            addTag(tag);
        }
        return this;
    }
    public final FlexTagGroup addTag(List<FlexTag> flexTags){
        for (FlexTag tag:flexTags) {
            addTag(tag);
        }
        return this;
    }
    public final void remove(FlexTag flexTag){
        flexTag.contentWrapper(wrapper);
        this.chipGroup.removeView(wrapper.view);
    }
    public final void removeTag(int index){
        this.chipGroup.removeViewAt(index);
    }

    public static final class FlexTagWrapper implements FlexViewBridge{
        private View view;
        @Override
        public void bridge(View view) {
            this.view = view;
        }
    }*/
}
