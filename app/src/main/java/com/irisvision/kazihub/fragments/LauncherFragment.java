package com.irisvision.kazihub.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.irisvision.flexrcoremodule.core.FlexFragment;
import com.irisvision.kazihub.screen.LauncherBody;

public class LauncherFragment extends FlexFragment {

    @Override
    protected String setNavigationKey() { return "LauncherFragment"; }

    @Override
    protected void onFlexCreateView(@Nullable Bundle savedInstanceState) {
        prepareScreen(null, LauncherBody.class);
    }
}
