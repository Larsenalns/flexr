// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import android.widget.FrameLayout;

import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewGroup;


public abstract class BoxImpl extends FlexViewGroup implements Box {

    public BoxImpl(FlexContextInterface flexContext) {
        super(flexContext);
    }

    // This method provide the way this.frameLayout facade attribute will be passed inside of the FlexViewGroup parent class
    // for all operation about collection and common behavior, this will be
    @Override
    public final boolean content(BoxBridge boxBridge, FrameLayout frameLayout) {
        if (boxBridge instanceof FlexViewGroup.BoxUnWrapper){
            boxBridge.bridge(frameLayout);
            return true;
        }
        return false;
    }

    public abstract boolean boxWrapper(BoxBridge boxBridge);
}
