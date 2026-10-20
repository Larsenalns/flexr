// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import android.widget.FrameLayout;

import com.irisvision.flexrcoremodule.core.BoxBridge;

public interface Box {
    boolean content(BoxBridge boxBridge, FrameLayout frameLayout);
}
