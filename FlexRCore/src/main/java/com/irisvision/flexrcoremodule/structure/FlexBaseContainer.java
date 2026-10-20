// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.structure;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;

import com.irisvision.flexrcoremodule.core.BaseContainerBridge;
import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrcoremodule.core.FlexFragment;

public final class FlexBaseContainer{
    private FrameLayout frameLayout;

    public FlexBaseContainer(@NonNull Context context) {
        super();
        init(context);
    }

    public boolean content(BaseContainerBridge baseContainerBridge, String s) {
        if (baseContainerBridge instanceof FlexFragment.FlexFragmentContainerWrapper || baseContainerBridge instanceof FlexStructureImpl.FlexStructureContainerWrapper
            || baseContainerBridge instanceof FlexActivity.FlexActivityContainerWrapper){
                baseContainerBridge.bridge(this.frameLayout,s);
                return true;
        }
        return false;
    }

    private void init(Context context){
        this.frameLayout = new FrameLayout(context);
        //this.frameLayout.setBackgroundColor(Color.parseColor("#ff005544"));
        this.frameLayout.setId(View.generateViewId());
        this.frameLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
    }
}
