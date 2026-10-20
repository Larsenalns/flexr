// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.structure;

/**
 * The top bar of a FlexR screen.
 *
 * <p>Its height is 80 dp by default, scaled by the screen density. A developer may
 * extend it by overriding {@link #extendAppBarHeight()}.
 */
public abstract class FlexAppbar extends FlexStructureImpl{

    public final int extendedAppBar(){
        int appBarHeight = 0;

        if(extendAppBarHeight() > 0 && extendAppBarHeight() <= 10){
            appBarHeight = extendAppBarHeight();
        }

        return Math.round((80 + appBarHeight) * getDisplayMetrics().density);
    }

    /**
     * Extra AppBar height, in dp, capped at 10 dp for each override.
     * {@link #extendedAppBar()}.
     *
     * <p>The extension point for subclasses. Overriding it is the only way to change the AppBar
     * height, so it must not be {@code final}. The default implementation returns {@code 0}.
     * 
     *
     * @return the extra height in dp; {@code 0} by default
     */
    protected int extendAppBarHeight(){
        return 0;
    }
}
