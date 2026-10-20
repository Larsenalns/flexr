package com.irisvision.kazihub;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.os.Bundle;

import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrthememodule.FlexTheme;
import com.irisvision.kazihub.fragments.LauncherFragment;

import java.util.List;

public class Home extends FlexActivity {

    @Override
    protected void flexCreate(Bundle savedInstanceState) {
        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        init();
    }

    @Override
    public void init() {
        fullScreen();
        initialFragment(new LauncherFragment(), false);
    }

    @Override
    protected void initFlexReactive() {

    }

    @Override
    protected void initFlexTheme() {
        // Thème par défaut : mode équilibré signature KaziHub.
        // L'application se fait APRÈS les pushTheme(...) ci-dessous : appChangeTheme(...) bascule le
        // thème ET notifie les vues déjà construites, alors qu'une écriture directe de currentTheme
        // ne changerait que les vues construites ensuite.

        /* ═══════════════════════════════════════════════════════════════════════════════
         *  KAZIHUB — PALETTE OFFICIELLE
         *  ─────────────────────────────────────────────────────────────────────────────
         *  Vert Principal : #00B208   |   Vert Clair : #2EB934
         *  Vert Profond   : #056509   |   Gris Foncé : #212121   |   Noir Pur : #010101
         *
         *  LÉGENDE DES INDEX :
         *  [0] Fond Principal | [1] Surface/Cartes | [2] Bouton/Accent
         *  [3] Texte Principal | [4] Texte Adouci  | [5] Alerte/Dette
         * ═══════════════════════════════════════════════════════════════════════════════ */


        // ═══════════════════════════════════════════════════════════════════════════════
        // ☀️  MODE 1 : "day" — KaziHub Day
        //     Lumineux, aéré, lisible en plein soleil kinois (terrasse journée)
        // ═══════════════════════════════════════════════════════════════════════════════
        FlexTheme.pushTheme("day", List.of(
                "#F5F7FA",  // [0] Fond blanc cassé, doux pour les yeux
                "#FFFFFF",  // [1] Cartes pures avec ombres légères
                "#00B208",  // [2] Vert KaziHub signature (boutons, accents)
                "#212121",  // [3] Texte gris foncé, contraste optimal
                "#6C757D",  // [4] Texte adouci pour labels secondaires
                "#E53935"   // [5] Rouge danger (dettes non payées)
        ));


        // ═══════════════════════════════════════════════════════════════════════════════
        // 🌙  MODE 2 : "night" — KaziHub Night (OLED Deep)
        //     Noir profond + Vert éclatant, économie de batterie, ambiance premium
        // ═══════════════════════════════════════════════════════════════════════════════
        FlexTheme.pushTheme("night", List.of(
                "#010101",  // [0] Noir pur OLED (batterie + immersion)
                "#141414",  // [1] Cartes légèrement surélevées (glass effect)
                "#2EB934",  // [2] Vert clair qui ressort sur fond noir
                "#FFFFFF",  // [3] Texte blanc pur
                "#8A99AD",  // [4] Gris bleuté adouci pour la hiérarchie
                "#FF5252"   // [5] Rouge vif lumineux (alerte dettes)
        ));


        // ═══════════════════════════════════════════════════════════════════════════════
        // ⚙️  MODE 3 : "default" — KaziHub Default
        //     Mode équilibré : la carte de visite de l'app (recommandé pour débuter)
        // ═══════════════════════════════════════════════════════════════════════════════
        FlexTheme.pushTheme("default", List.of(
                "#F0F2F5",  // [0] Gris neutre chaleureux
                "#FFFFFF",  // [1] Cartes blanches avec ombre verte subtile
                "#00B208",  // [2] Vert signature KaziHub
                "#212121",  // [3] Texte principal
                "#5A6573",  // [4] Texte secondaire
                "#E53935"   // [5] Rouge danger
        ));


        // ═══════════════════════════════════════════════════════════════════════════════
        // 🌿  BONUS 1 : "night_vip" — KaziHub VIP Lounge
        //     Inspiration de ton "night1" : ambiance chic de Kinshasa la Belle
        //     Vert émeraude profond + Or doré pour une touche haut de gamme
        // ═══════════════════════════════════════════════════════════════════════════════
        FlexTheme.pushTheme("night_vip", List.of(
                "#0A1A0C",  // [0] Vert-noir profond (forêt nocturne)
                "#122A15",  // [1] Cartes vertes foncées veloutées
                "#D4A017",  // [2] Or doré (rappel YondeBase)
                "#F0F4F8",  // [3] Blanc nacré
                "#7A9280",  // [4] Vert-gris adouci
                "#FF5252"   // [5] Rouge alerte
        ));


        // ═══════════════════════════════════════════════════════════════════════════════
        // 🔥  BONUS 2 : "day_terrasse" — KaziHub Terrasse Jour
        //     Inspiration de ton "light" : chaud, lisible, ambiance bar ensoleillé
        // ═══════════════════════════════════════════════════════════════════════════════
        FlexTheme.pushTheme("day_terrasse", List.of(
                "#FFFDF7",  // [0] Blanc crème chaleureux
                "#FFFFFF",  // [1] Cartes pures
                "#056509",  // [2] Vert profond (plus contrasté en plein soleil)
                "#1A1A1A",  // [3] Presque noir pour lisibilité max
                "#6C757D",  // [4] Gris neutre
                "#D90429"   // [5] Rouge carmin pour alerte
        ));

        // Applique le thème initial et notifie les observateurs (les thèmes sont désormais déclarés).
        FlexTheme.appChangeTheme("default");
    }
}
