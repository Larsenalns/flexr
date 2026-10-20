// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.core.LineBridge;

/**
 * Radio-button group: a line container whose children are mutually exclusive, so that at most one
 * of them is checked at a time.
 *
 * <p>The class is a {@link LineImpl} backed by a native {@code RadioGroup}, and that native view owns
 * the exclusivity rule. Children are therefore {@code FlexRadioButton} instances, and adding them is
 * enough: no listener has to be wired for the other buttons to be unchecked.
 *
 * <p>Children are attached through {@link #lineWrapper(LineBridge)}, the line bridge of the
 * container family, so a {@code FlexRadioGroup} accepts children exactly like any other line
 * container.
 *
 * <p>Threading: every member must be used on the UI thread. They drive a native view hierarchy, and
 * {@link #onSelectState(RadioGroup.OnCheckedChangeListener)} delivers its callbacks on that same
 * thread.
 *
 * <pre>{@code
 * FlexRadioGroup group = new FlexRadioGroup(this);
 * group.orientation("V");
 * group.add(new FlexRadioButton(this).text("One").checkedState(true));
 * group.add(new FlexRadioButton(this).text("Two"));
 * }</pre>
 *
 * @see Line
 */
public class FlexRadioGroup extends LineImpl {

    private final RadioGroup radioGroup;

    /**
     * Creates an empty group backed by a new native {@code RadioGroup}, and registers the container
     * with the theme scope of its screen.
     *
     * <p>The native group starts with the default orientation of {@code RadioGroup}; use
     * {@link #orientation(String)} to pick one. Must be called on the UI thread, since it
     * instantiates a view.
     *
     * @param flexContextInterface context of the screen that owns this container
     */
    public FlexRadioGroup(FlexContextInterface flexContextInterface) {
        super(flexContextInterface);
        this.radioGroup = new RadioGroup(f.getFlexContext());
        initInnerBridge();

    }

    /**
     * Bridges the native {@code RadioGroup} to a {@code FlexViewBridge}: this is the generic path
     * through which the framework reaches the view of this container.
     *
     * <p>UI thread only.
     *
     * @param flexViewBridge bridge to hand the native view to
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.radioGroup);
    }

    /**
     * Bridges the native {@code RadioGroup} to a {@code LineBridge}: this is the path
     * {@code FlexViewGroup.add(FlexView)} uses to attach a child to this container.
     *
     * <p>UI thread only.
     *
     * @param lineBridge bridge to hand the native view to
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public boolean lineWrapper(LineBridge lineBridge) {
        return content(lineBridge, this.radioGroup);
    }

    /**
     * Sets the axis along which the buttons are laid out.
     *
     * <p>Labels are compared case-insensitively. A {@code null} or blank label is ignored and the
     * current orientation is kept, so the method can be fed straight from an optional parameter.
     *
     * @param orientation {@code "V"} or {@code "VERTICAL"} for a vertical stack, {@code "H"} or
     *                    {@code "HORIZONTAL"} for a horizontal row; {@code null} or blank leaves the
     *                    orientation unchanged
     * @return this instance, for chaining
     * @throws IllegalArgumentException when the label is neither vertical nor horizontal: an unknown
     *                                  label is refused rather than silently mapped to a horizontal
     *                                  layout, which would hide a typo in the layout code
     */
    public final FlexRadioGroup orientation(String orientation){
        if (orientation == null || orientation.trim().isEmpty()) return this;

        if (orientation.equalsIgnoreCase("V") || orientation.equalsIgnoreCase("VERTICAL"))
            this.radioGroup.setOrientation(LinearLayout.VERTICAL);
        else if (orientation.equalsIgnoreCase("H") || orientation.equalsIgnoreCase("HORIZONTAL"))
            this.radioGroup.setOrientation(LinearLayout.HORIZONTAL);
        else throw new IllegalArgumentException(
                "orientation(\"" + orientation + "\") : unknown value. Use \"V\" (vertical) or \"H\" (horizontal).");

        return this;
    }

    /**
     * Registers the listener notified when the checked button of the group changes.
     *
     * <p>The listener replaces the previous one; passing {@code null} unregisters the current
     * listener. The native group invokes it on the UI thread, once per change of the checked button,
     * including the change caused by checking a button programmatically.
     *
     * @param checkedChangeListener listener to install, or {@code null} to remove the current one
     * @return this instance, for chaining
     */
    public final FlexRadioGroup onSelectState(RadioGroup.OnCheckedChangeListener checkedChangeListener){
        this.radioGroup.setOnCheckedChangeListener(checkedChangeListener);
        return this;
    }
}
