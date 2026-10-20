# Règles R8 / ProGuard transmises aux applications qui consomment FlexR Media.
#
# CE QUI A ÉTÉ RETIRÉ le 2026-10-15 (REVIEW/15-R8-REFLEXION.md §6) :
#
#     -keep class androidx.media3.** { *; }
#
# Cette règle interdisait toute réduction et toute obfuscation dans TOUT media3 — l'une des plus
# grosses dépendances AndroidX — alors qu'elle était redondante : vérifié dans le cache Gradle,
# 5 AAR media3 sur 8 livrent leurs propres règles de consommateur, dont 73 pour media3-exoplayer,
# 40 pour media3-ui, 15 pour media3-extractor, 11 pour media3-common et 7 pour media3-datasource.
# Ces règles amont couvrent les résolutions par réflexion qu'ExoPlayer utilise réellement.
#
#     -keepnames class com.irisvision.flexrcoremodule.ui.layout.FlexVideo { *; }
#     -keepnames class com.irisvision.flexrcoremodule.ui.layout.ExoPlayerBuffer { *; }
# Idem : ces deux classes sont référencées directement, jamais par leur nom.
#
# CE QUI RESTE, et pourquoi : `-dontwarn` est un garde-fou gratuit. media3 déclare des dépendances
# optionnelles ; si l'une manque au moment de la vérification, R8 émet un avertissement — le silence
# demandé ici évite de faire échouer la compilation d'une application pour un type qu'elle n'utilise pas.
-dontwarn androidx.media3.**
