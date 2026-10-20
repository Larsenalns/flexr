// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import android.widget.FrameLayout;

import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;

public class FlexBox extends BoxImpl {
    private FrameLayout frameLayout;

    public FlexBox(FlexContextInterface flexContext) {
        super(flexContext);
        this.frameLayout = new FrameLayout(f.getFlexContext());
        initInnerBridge();
    }

    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.frameLayout);
    }

    @Override
    public final boolean boxWrapper(BoxBridge boxBridge) {
        return content(boxBridge, this.frameLayout);
    }
}
