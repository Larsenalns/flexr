// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrstarter.template;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.irisvision.flexrcoremodule.core.FlexFragment;

public class TemplateFlexFragmentHome extends FlexFragment {

    @Override
    protected String setNavigationKey() {
        return "home";
    }

    @Override
    protected void onFlexCreateView(@Nullable Bundle savedInstanceState) {
        prepareScreen(TemplateFlexAppBar.class, TemplateBodyHome.class);
    }

    @Override
    protected void onFlexViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onFlexViewCreated(view, savedInstanceState);
    }
}
