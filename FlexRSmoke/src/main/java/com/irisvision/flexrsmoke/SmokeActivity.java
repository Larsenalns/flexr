package com.irisvision.flexrsmoke;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.os.Bundle;

import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrsmoke.containers.SmokeContainersFragment;
import com.irisvision.flexrthememodule.FlexTheme;

import java.util.List;

/**
 * Activité hôte des écrans de fumée.
 *
 * <p>Elle n'appartient à aucune application publiée : c'est l'endroit où la librairie est
 * <b>exécutée</b>, ce qui n'avait jamais été fait pour 23 des classes des lots 11 et 12.
 */
public class SmokeActivity extends FlexActivity {

    @Override
    protected void flexCreate(Bundle savedInstanceState) {
        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        init();
    }

    @Override
    public void init() {
        fullScreen();
        initialFragment(new SmokeContainersFragment(), false);
    }

    @Override
    protected void initFlexReactive() {
        // Aucun provider : les écrans de fumée n'ont pas besoin d'état partagé.
    }

    @Override
    protected void initFlexTheme() {
        // Palette minimale : les indices utilisés par les écrans de fumée doivent être déclarés,
        // sinon FlexTheme.load(index) ne trouve rien d'exploitable.
        FlexTheme.pushTheme("smoke", List.of(
                "#FFFFFFFF",   // [0] fond principal
                "#FFEEEEEE",   // [1] surface
                "#FF00B208",   // [2] accent
                "#FF212121",   // [3] texte principal
                "#FF6C757D",   // [4] texte adouci
                "#FFD90429"    // [5] alerte
        ));

        // Après les pushTheme(...) : un thème non encore déclaré est ignoré, et la bascule
        // notifie les vues déjà construites (c'est le chemin que la démo contournait).
        FlexTheme.appChangeTheme("smoke");
    }
}
