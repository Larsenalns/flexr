// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewGroup;
import com.irisvision.flexrcoremodule.core.LineBridge;

public abstract class LineImpl extends FlexViewGroup implements Line {
    public LineImpl(FlexContextInterface flexContextInterface) {
        super(flexContextInterface);
    }

    // This method provide the way this.frameLayout facade attribute will be passed inside of the FlexViewGroup parent class
    // for all operation about collection and common behavior, this will be
    @Override
    public final boolean content(LineBridge lineBridge, ViewGroup viewGroup) {
        if (lineBridge instanceof FlexViewGroup.LineUnWrapper){
            lineBridge.bridge(viewGroup);
            return true;
        }
        return false;
    }

    public abstract boolean lineWrapper(LineBridge lineBridge);
}
