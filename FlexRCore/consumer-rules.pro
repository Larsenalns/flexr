# Règles R8 / ProGuard transmises aux applications qui consomment FlexR.
#
# Adaptation d'un écran par réflexion (FlexFragment.prepareScreen) :
# la classe de l'écran est instanciée par son nom, elle ne doit donc pas être
# renommée ni supprimée par R8, sinon l'application affiche un écran vide en release.

-keep class com.irisvision.flexrcoremodule.structure.FlexAppbar { *; }
-keep class com.irisvision.flexrcoremodule.structure.FlexBody { *; }

-keep class * extends com.irisvision.flexrcoremodule.structure.FlexAppbar
-keep class * extends com.irisvision.flexrcoremodule.structure.FlexBody

# Un constructeur sans paramètre accessible est indispensable à cette instanciation.
-keepclassmembers class * extends com.irisvision.flexrcoremodule.structure.FlexAppbar {
    public <init>();
}
-keepclassmembers class * extends com.irisvision.flexrcoremodule.structure.FlexBody {
    public <init>();
}

# La clé de navigation peut être dérivée du nom de classe : on conserve les noms
# des sous-classes de FlexFragment pour que la back stack reste lisible en release.
-keepnames class * extends com.irisvision.flexrcoremodule.core.FlexFragment

# Feuilles modales : un Fragment est recréé par le FragmentManager PAR RÉFLEXION, SANS ARGUMENT
# (rotation d'écran, changement de thème système, restauration de process après mort du process).
# Sans constructeur sans paramètre conservé, la feuille ne peut pas être recréée.
#
# Note : androidx.fragment fournit déjà l'équivalent — vérifié dans son AAR (fragment-1.3.6,
# fichier proguard.txt) :
#     -if public class ** extends androidx.fragment.app.Fragment
#     -keepclasseswithmembers,allowobfuscation public class <1> { public <init>(); }
# La règle ci-dessous la réécrit pour nos feuilles, de façon explicite et indépendante de la
# version d'AndroidX : si cette règle amont disparaissait, la recréation ne casserait pas.
#
# Rien à ajouter pour FlexFragment et FlexActivity :
#   - les Fragments sont couverts par la règle amont d'androidx.fragment ci-dessus ;
#   - les Activities sont couvertes par les règles ProGuard par défaut d'AGP, qui conservent les
#     constructeurs des sous-classes d'Activity (le framework les instancie par leur nom).
-keepclassmembers class * extends com.irisvision.flexrcoremodule.ui.component.FlexBottomSheet {
    public <init>();
}
