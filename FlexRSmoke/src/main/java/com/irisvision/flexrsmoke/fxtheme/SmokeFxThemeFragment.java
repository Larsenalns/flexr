package com.irisvision.flexrsmoke.fxtheme;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.irisvision.flexrcoremodule.core.FlexFragment;

/** Hôte de l'écran de fumée « animations et thème » (lot 13). */
public class SmokeFxThemeFragment extends FlexFragment {

    @Override
    protected String setNavigationKey() {
        return "SmokeFxThemeFragment";
    }

    @Override
    protected void onFlexCreateView(@Nullable Bundle savedInstanceState) {
        prepareScreen(null, SmokeFxThemeBody.class);
    }
}
