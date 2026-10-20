// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import androidx.appcompat.widget.LinearLayoutCompat;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.core.FlexViewGroup;
import com.irisvision.flexrcoremodule.core.LineBridge;

public class FlexVBox extends LineImpl {
    //Soft LinearLayout Vertical Orientation Impl
    private LinearLayoutCompat linearLayoutCompat;

    public FlexVBox(FlexContextInterface flexContext) {
        super(flexContext);
        linearLayoutCompat = new LinearLayoutCompat(f.getFlexContext());
        this.linearLayoutCompat.setOrientation(LinearLayoutCompat.VERTICAL);
        initInnerBridge();
    }

    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.linearLayoutCompat);
    }

    @Override
    public final boolean lineWrapper(LineBridge lineBridge) {
        return content(lineBridge, this.linearLayoutCompat);
    }
}
