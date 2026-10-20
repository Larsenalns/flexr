package com.irisvision.flexrsmoke.fxtheme;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.os.Bundle;

import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrthememodule.FlexTheme;

import java.util.List;

/** Hôte de l'écran de fumée « animations et thème » (lot 13). */
public class SmokeFxThemeActivity extends FlexActivity {

    /** Palette alternative : la bascule de thème doit se voir sur les vues déjà construites. */
    public static final String THEME_ALT = "smoke-alt";

    @Override
    protected void flexCreate(Bundle savedInstanceState) {
        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        init();
    }

    @Override
    public void init() {
        fullScreen();
        initialFragment(new SmokeFxThemeFragment(), false);
    }

    @Override
    protected void initFlexReactive() {
        // Aucun provider : cet écran vérifie les animations et le thème.
    }

    @Override
    protected void initFlexTheme() {
        // Deux palettes, pour que la bascule ait un effet visible.
        FlexTheme.pushTheme("smoke", List.of(
                "#FFFFFFFF",   // [0] fond
                "#FFEEEEEE",   // [1] surface
                "#FF00B208",   // [2] accent
                "#FF212121",   // [3] texte
                "#FF6C757D",   // [4] texte adouci
                "#FFD90429"    // [5] alerte
        ));
        FlexTheme.pushTheme(THEME_ALT, List.of(
                "#FF101820",   // [0] fond sombre
                "#FF1E2A33",   // [1] surface sombre
                "#FFD4A017",   // [2] accent doré
                "#FFF0F4F8",   // [3] texte clair
                "#FF7A9280",   // [4] texte adouci
                "#FFFF5252"    // [5] alerte
        ));

        // Après les pushTheme(...) : un thème non déclaré est ignoré.
        FlexTheme.appChangeTheme("smoke");
    }
}
