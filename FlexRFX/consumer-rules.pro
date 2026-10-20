# Règles R8 / ProGuard transmises aux applications qui consomment FlexR FX.
#
# ---------------------------------------------------------------------------
# Setters d'animation résolus PAR RÉFLEXION (constat du 2026-10-14, corrigé)
# ---------------------------------------------------------------------------
# FlexAnimate crée ses animations ainsi :
#     ObjectAnimator.ofFloat(cible, "flexAnimateScaleX", from, to)
# Android cherche alors, par réflexion, la méthode setFlexAnimateScaleX(...) sur la cible.
# R8 ne peut pas faire le lien entre la chaîne de caractères et cette méthode : sans la règle
# ci-dessous, les setters sont renommés (ou supprimés) en release et TOUTES les animations échouent —
# uniquement en release, donc invisibles en debug.
#
# La règle est posée sur l'interface qui marque les cibles (FlexAnimateApply), ce qui couvre les deux
# familles d'un coup :
#   - FlexView   → animations de vue (translation, échelle, alpha, élévation, scroll, paddings) ;
#   - FlexParams → animations de paramètres (marges, padding global, taille), via FlexParamsAnimate.
#
# Vérifié par un build R8 réel : 57 setters retenus dans seeds.txt, conservés sous leur nom dans
# mapping.txt (contre 0 sans la règle). Voir REVIEW/15-R8-REFLEXION.md §4.1.
-keepclassmembers class * implements com.irisvision.flexrfxmodule.FlexAnimateApply {
    public *** setFlexAnimate*(...);
}

# ---------------------------------------------------------------------------
# Ce qui a été RETIRÉ le 2026-10-15 (REVIEW/15-R8-REFLEXION.md §6)
# ---------------------------------------------------------------------------
# `-keepnames class com.irisvision.flexrfxmodule.** { *; }` : inutile depuis que les setters ont une
# règle CIBLÉE. Les classes d'animation sont référencées directement (listes statiques, appels de
# méthode), jamais par leur nom ; elles peuvent donc être obfusquées et réduites comme le reste.
