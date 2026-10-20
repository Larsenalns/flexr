package com.irisvision.flexrsmoke.containers;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.irisvision.flexrcoremodule.core.FlexFragment;

/** Hôte de l'écran de fumée « conteneurs, dispositions et composants » (lot 11). */
public class SmokeContainersFragment extends FlexFragment {

    @Override
    protected String setNavigationKey() {
        return "SmokeContainersFragment";
    }

    @Override
    protected void onFlexCreateView(@Nullable Bundle savedInstanceState) {
        prepareScreen(null, SmokeContainersBody.class);
    }
}
