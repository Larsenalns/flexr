package com.irisvision.flexrsmoke.list;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.os.Bundle;

import com.irisvision.flexrcoremodule.core.FlexActivity;

/** Hôte de l'écran de fumée « listes ». */
public class SmokeListActivity extends FlexActivity {

    @Override
    protected void flexCreate(Bundle savedInstanceState) {
        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        init();
    }

    @Override
    public void init() {
        fullScreen();
        initialFragment(new SmokeListFragment(), false);
    }

    @Override
    protected void initFlexReactive() {
        // Aucun provider global : les providers d'item sont portés par la donnée elle-même.
    }

    @Override
    protected void initFlexTheme() {
        // Même palette que l'écran « conteneurs » : les deux écrans partagent la même application.
    }
}
