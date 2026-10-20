# Règles R8 / ProGuard transmises aux applications qui utilisent le starter FlexR.
#
# AUCUNE règle n'est nécessaire ici, et c'est un point à comprendre : les écrans (AppBar et Body) sont
# conservés par les règles du NOYAU, qui s'appliquent à toutes les sous-classes, où qu'elles vivent :
#
#     -keep class * extends com.irisvision.flexrcoremodule.structure.FlexAppbar { *; }
#     -keep class * extends com.irisvision.flexrcoremodule.structure.FlexBody { *; }
#     -keepclassmembers class * extends ...FlexAppbar { public <init>(); }
#     -keepclassmembers class * extends ...FlexBody   { public <init>(); }
#
# Les copies qui figuraient ici ont donc été RETIRÉES le 2026-10-15 (REVIEW/15-R8-REFLEXION.md §6),
# ainsi que `-keepnames class com.irisvision.flexrstarter.template.** { *; }` : les classes du starter
# sont référencées directement, et ses Activités sont déclarées au manifeste, donc conservées par les
# règles par défaut d'AGP.
