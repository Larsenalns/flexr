package com.irisvision.flexrsmoke.list;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.irisvision.flexrcoremodule.core.FlexFragment;

/** Hôte de l'écran de fumée « listes » (lot 12 + étages A→D). */
public class SmokeListFragment extends FlexFragment {

    @Override
    protected String setNavigationKey() {
        return "SmokeListFragment";
    }

    @Override
    protected void onFlexCreateView(@Nullable Bundle savedInstanceState) {
        prepareScreen(null, SmokeListBody.class);
    }
}
