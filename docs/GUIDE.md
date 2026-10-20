# Guide FlexR — intégration, R8 et réduction des ressources

> Public : développeur qui intègre FlexR et prépare une publication. Ce guide ne redécrit pas l'API :
> il répond aux quatre questions qui coûtent réellement du temps — que fait R8 de mes ressources, quoi
> écrire dans `keep.xml`, quels écrans me disent que tout fonctionne, et quel statut accorder à
> `baseInit()`.
>
> Sources : `REVIEW/15-R8-REFLEXION.md` (mesures), `REVIEW/16-ETAT-DOC.md` (état de la documentation),
> et le code des six modules.

---

## 1. Les modules, en une page

| Module | Rôle | Publié ? |
|---|---|---|
| `FlexRCore` | noyau : `FlexActivity`, `FlexFragment`, écrans (`FlexAppbar`/`FlexBody`), vues et conteneurs | oui |
| `FlexRFX` | animation : `FlexAnimate`, `FlexChoreo` | oui |
| `FlexRTheme` | thème : `FlexTheme`, `Thematic` | oui |
| `FlexRConcurrency` | pools, `FlexTask`, `FlexThreadMain` | oui |
| `FlexRMedia` | vidéo (Media3/ExoPlayer) — **optionnel** | oui |
| `FlexRStarter` | modèle d'écrans prêt à copier | oui |
| `FlexRBundle` | agrégateur : AAR vide (0,9 Ko) qui déclare `api` le noyau + le média | oui |
| `FlexRSmoke` | module de validation, seconde icône « FlexR Smoke » | **non** |
| `app` | application de démonstration | **non** |

Le noyau ne dépend d'aucun autre module FlexR ; FX, thème et concurrence sont ses dépendances `api`,
donc un consommateur du noyau les reçoit. Un consommateur qui n'affiche pas de vidéo ne met **jamais**
ExoPlayer dans son APK : le module média est hors du graphe, et même déclaré, R8 le retire
(≈ 30 Ko mesurés).

---

## 2. La promesse du nom, sans `R`

FlexR adresse les ressources **par leur nom**, en chaîne de caractères :

```java
new FlexImg(this).load("mon_image");            // drawable
new FlexButton(this).flexDrawable("mon_icone"); // icone de bouton
new FlexVideo(this).flexAsset(In.raw, "clip");  // ressource brute
```

C'est une **promesse de la librairie**, pas un détail d'implémentation : elle permet des applications
dynamiques — une liste d'images reçue du serveur, un écran piloté par la donnée, un thème qui change une
ressource — là où `R.drawable.mon_image` impose une référence figée à la compilation. C'est aussi un
clin d'oeil assumé à Flutter : l'identifiant numérique est précisément ce que FlexR existe pour éviter.

**La contrepartie, et c'est tout l'objet de la section suivante** : un outil d'analyse statique ne voit
pas un nom qui arrive en paramètre.

---

## 3. R8 : ce que les modules déclarent déjà

Chaque module livré porte un `consumer-rules.pro`, transmis à l'application qui le consomme. FlexR
n'utilise la réflexion que là où c'est **structurel** — quatre mécanismes, quatre règles ciblées :

| Mécanisme | Ce qui doit survivre | Règle (noyau) |
|---|---|---|
| `instantiateScreen(Class)` | la classe d'écran et son constructeur **public sans paramètre** | `-keep class * extends FlexAppbar/FlexBody` + `-keepclassmembers … public <init>()` |
| Recréation d'un `Fragment` par le `FragmentManager` | le **nom** des sous-classes de `FlexFragment` | `-keepnames class * extends FlexFragment` |
| `FlexBottomSheet` recréée | son constructeur sans paramètre | `-keepclassmembers … public <init>()` |
| `ObjectAnimator.ofFloat/Int(cible, "flexAnimateX", …)` | **tous** les setters `setFlexAnimate*` | `-keepclassmembers class * implements FlexAnimateApply { public *** setFlexAnimate*(...); }` (module FX) |

Le quatrième est le plus vicieux : la propriété est résolue **par réflexion au démarrage**, à partir
d'une chaîne. Sans règle, R8 supprime les setters (inatteignables) ou les renomme — et l'animation est
morte **uniquement en release**, donc jamais vue en debug. La règle du module FX a été vérifiée sur deux
builds réels : 0 setter dans `seeds.txt` avant, **57** après, conservés sous leur nom dans `mapping.txt`.

Ce qui n'existe **pas** dans FlexR, et qu'il est inutile de couvrir : `Class.forName`, `getMethod`,
accès à des champs privés, sérialisation par nom. `Enum.valueOf` (dans `FlexParams`) est traité par R8
lui-même.

### Ce qui a été retiré, et ce que cela a rapporté

Trois `-keepnames` de module (`FlexRConcurrency`, `FlexRTheme`, `FlexRFX`) et un `-keep` global
`androidx.media3.**` ne protégeaient rien : les deux premiers gardaient des noms construits sur des
**instances**, le troisième interdisait toute réduction dans ExoPlayer alors que media3 livre ses propres
règles. Mesure sur l'application de démonstration, à code identique :

| État | APK release (minifié + ressources réduites) | Protections vérifiées |
|---|---|---|
| avec les règles superflues | **2,78 Mo** | 57 setters, 10 écrans |
| sans (état actuel) | **1,26 Mo** | 57 setters, 10 écrans |

**−1,52 Mo, soit −55 %**, sans perdre une protection utile. `minifyEnabled` est resté à `false` dans
`app/build.gradle` : la minification est un choix du consommateur, pas une contrainte de la librairie.

---

## 4. `shrinkResources` : le seul risque structurel restant

`shrinkResources true` supprime les ressources que l'analyseur d'AGP croit inutilisées. Il sait repérer
`getIdentifier("nom_litteral", …)` — mais FlexR reçoit le nom **d'un paramètre** : il est invisible pour
lui. Onze points d'appel sont concernés (icône de bouton, de champ, de tag, vignette de curseur,
séparateur de liste, pochette vidéo, ressource brute).

Le risque n'est pas théorique : il a été **démontré**. Un `drawable` (`smoke_probe`) référencé
uniquement par son nom, avec `minifyEnabled true` **et** `shrinkResources true`, produit dans
`app/build/outputs/mapping/release/resources.txt` :

```
drawable:smoke_probe:2131231030 is not reachable.      -> sans keep.xml : retirée de l'APK
drawable:smoke_probe:2131231030 reachable from keep xml file -> avec keep.xml : conservée
```

Le composant serait vide à l'exécution, **sans erreur au build**.

### La parade : `res/raw/keep.xml`

Exemple vivant, livré dans le module de validation :
`FlexRSmoke/src/main/res/raw/keep.xml`. Deux stratégies, à choisir **côté application** :

```xml
<!-- 1. nommer ses ressources : le plus économique -->
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@drawable/mon_image,@drawable/mon_icone,@raw/ma_video" />

<!-- 2. tout garder ce que l'API par nom peut adresser -->
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@drawable/*,@raw/*" />
```

| Stratégie | Effet | Coût mesuré (démo) |
|---|---|---|
| noms précis | garde exactement ce que l'application nomme | quasi nul |
| jokers `@drawable/*,@raw/*` | garde tout ce que l'API par nom peut atteindre | ≈ +0,36 Mo (après retrait des règles superflues) ; ≈ +0,9 Mo sur l'état antérieur |

**Ne pas imposer les jokers depuis la librairie** : un `keep.xml` livré par un module de bibliothèque
s'applique à **tout** l'ensemble fusionné, donc aussi aux ressources de l'application. Ce serait retirer
l'optimisation à tous ceux qui n'utilisent pas l'API par nom.

### Le filet de sécurité, déjà dans la librairie

`flexAsset(...)` **lève** une `IllegalArgumentException` quand `getIdentifier` rend `0`, en nommant la
ressource, son type, le paquet, les trois causes possibles — dont l'activation de `shrinkResources` — et
la parade `keep.xml`. Le mode d'échec est bruyant, et le message porte sa propre documentation.

### Recette de validation, côté application

1. `minifyEnabled true` **et** `shrinkResources true` dans le type `release` ;
2. écrire le `keep.xml` correspondant à ses noms (ou aux jokers) ;
3. builder, puis vérifier `resources.txt` : aucune ligne `is not reachable` ne doit concerner une
   ressource nommée par l'application ;
4. lancer l'écran de fumée **en release minifié** (section 5) : c'est la seule preuve d'exécution.

---

## 5. Écrans de fumée : le verdict en un passage

Les vues natives ne s'instancent pas en test JVM, et Robolectric n'est pas installé : un écran exécuté
sur appareil est le **seul** moyen de vérifier ce que la compilation ne prouve pas. D'où `FlexRSmoke`,
module séparé, jamais publié, qui s'installe comme une **seconde icône** (« FlexR Smoke »).

| Écran | Ce qu'il exerce | État |
|---|---|---|
| `SmokeContainersBody` | `FlexCard`, `FlexContainer`, `FlexRadioGroup` avec enfants, `FlexGrid`, `FlexFlipper`, les trois `FlexScroll*`, `FlexInput` (`boxHint` **et** `boxDecoration` par index), `FlexSlider`, `FlexSliderRange`, `FlexCheck`, `FlexSwitch`, `FlexTag`+`FlexTagGroup`, `FlexButton`, `FlexText`, `FlexImg.load("smoke_probe")`, `FlexProgress`, `FlexShape` | écrit, compile |
| `SmokeListBody` | `FlexList` + `FlexAdapterObservable` (un provider **par item**), 50 items, clics, sélection simple et multiple, `submitData` (déplacement + retrait), état vide, état de chargement, `onEndReached`, mise à jour d'**une seule ligne** | écrit, compile |
| `SmokeFxThemeBody` | `FlexAnimate` (translation, échelle, alpha, taille, paddings, marges, `scrollX`), refus d'un côté inconnu, `FlexChoreo` `Timer` **et** `Condition` avec pause/reprise, bascule de thème par `appChangeTheme`, fond posé **par nom** | écrit, compile |

**Lire le verdict** : `SmokeChecklist` exécute chaque étape dans son propre `try/catch`, affiche
`N OK, M KO` puis la liste (classe fautive + exception), et journalise chaque ligne sous le tag
`FlexRSmoke` pour `adb logcat`. Un seul passage donne donc le verdict complet, au lieu d'un écran vide
sans explication.

**Ce qui reste à faire** : l'**exécution** en release minifié sur appareil ou émulateur. Elle se lance
depuis Android Studio (`:app:assembleRelease` en CLI peut échouer sur `debug.keystore.lock` si Studio
tient le verrou).

---

## 6. Thème : un index, jamais une couleur

Un thème est une **palette ordonnée**. Le code applicatif ne manipule pas de couleur : il référence un
**index**, et le thème courant décide de la valeur :

```java
FlexTheme.pushTheme("night", List.of("#FF101820", "#FFF2AA33", "#FFEDEDED"));
FlexTheme.appChangeTheme("night");
new FlexCard(this).cardColor(0);        // index 0 du thème courant
```

- **Format unique** : `#AARRGGBB` (alpha en tête). `#RGB` et `#RRGGBB` sont acceptés et complétés en
  `FF`. Les couleurs sont validées **à l'enregistrement** : une couleur invalide échoue à sa déclaration,
  pas au moment de peindre.
- **Indices stables** : tant que toutes les palettes déclarées ont la même longueur, un changement de
  thème repeint sans reconstruire une vue. Un index hors palette lève
  `IndexOutOfBoundsException` en nommant le thème et sa taille.
- **Conventions d'index du noyau** : `0` = fond du corps (`FlexBody`), `1` = fond de la barre
  (`FlexAppbar`) — voir `FlexStructureImpl.initBaseContainer()`. Les composants prennent ensuite un
  index explicite : `FlexCard.cardColor(int)`, `FlexCard.stroke(int, float)`,
  `FlexButton.ripple(int)`, `FlexInput.hintColor(int)`, `FlexTag.textColor(int)`,
  `FlexShortList.divider(int, int)`, `FlexShape.shapeColor(int)`.
- **Ordre d'appel** : `pushTheme(...)` **avant** `appChangeTheme(...)`. Un thème non déclaré est ignoré
  — ni bascule, ni notification, pas d'exception.
- **Threading** : `load(...)` se lit de n'importe quel thread ; `appChangeTheme(...)` s'appelle de
  n'importe où mais s'applique **toujours sur le thread principal** (un thème est du visuel).
  `ifThemeChange()` est donc délivré sur le thread principal, et un observateur qui a du travail lourd à
  faire doit le sortir lui-même.

---

## 7. Statut de `baseInit()`

`FlexStructure` déclare `void baseInit();`, et `FlexView` comme `FlexBottomSheet` l'implémentent
**vide**. Constat, vérifié dans le code :

- l'`initializer_()` des écrans (`FlexStructureImpl`) exécute `init()`, `initBaseContainer()`,
  `attachEvents()`, `forgettingFlexConsumers()`, `initialBusinessLogic()`, puis délègue le travail
  « conteneur visible » à `FlexStructureLooper`. Il n'appelle **pas** `baseInit()` ;
- seul `FlexBottomSheet.onCreateView()` appelle `baseInit()`, juste avant `initializer_()` ;
- aucun écran du dépôt ne le surcharge.

**Conclusion pratique** : c'est un **résidu de la première conception** (`FlexContext`/`Init_`). Sur un
écran, le surcharger ne produit rien — le code ne serait jamais exécuté. Pour une initialisation avant
construction, utiliser :

| Besoin | Où |
|---|---|
| initialisation avant tout (providers, abonnements) | `init()` (contrat `Init_`), appelé par `initializer_()` |
| construction de l'arbre de vues | `dynamicBuild()` |
| câblage d'événements | `attachEvents()` |
| logique métier de départ | `initialBusinessLogic()` |
| travail qui exige la visibilité du conteneur | `onFlexContainerIsVisible()` (délégué à `FlexStructureLooper`) |
| feuille modale | `baseInit()` est bien appelé, avant `initializer_()` |

`baseInit()` est conservé pour la compatibilité de l'interface, et documenté comme tel dans
`FlexStructure`. Le retirer serait un changement d'API à décider lors d'une montée de version majeure,
pas une correction.

---

## 8. Ce qui reste avant publication

| Point | État |
|---|---|
| Documentation traduite (javadoc + commentaires) | terminé, vérifié par `REVIEW/verif-doc.ps1` |
| Tests JVM | 46 (noyau) + 9 (concurrence) + 7 (thème), 0 échec |
| R8 réel (`:app:minifyReleaseWithR8`) | validé, 57 setters conservés |
| `shrinkResources` | risque démontré, parade `keep.xml` livrée en exemple |
| Écrans de fumée | écrits et compilés ; **exécution release sur appareil à faire** |
| Licence + en-têtes SPDX, CLA | à faire |
| Publication Maven Central (sources, javadoc, signature) | à faire |
| Changelog d'API | à faire |

Références : `REPRISE.md` (§5 vérifications, §10 décisions), `REVIEW/11` à `REVIEW/17`,
`REVIEW/verif-doc.ps1`, `REVIEW/docguard.ps1` (garantit qu'une passe de documentation ne touche pas au
code).

---

## 9. Les API ajoutées pour couvrir les manques

| Manque | API | Fichier |
|---|---|---|
| Images réseau | `FlexImg.useLoader(FlexImg.Loader)` + `loadUri(String)`, `loadBitmap(Bitmap)`, `loadDrawable(Drawable)`, `clear()` | `ui/layout/FlexImg.java` |
| Visibilité | `FlexView.visible(boolean)` (VISIBLE/GONE) | `core/FlexView.java` |
| Accessibilité | `FlexView.contentDescription(CharSequence)` | `core/FlexView.java` |
| Tests UI | `FlexView.testTag(String)` + `FlexView.TEST_TAG_KEY` (tag indexé, sans collision avec les tags des adaptateurs) | `core/FlexView.java` |
| Dialogues | `FlexAlert` (titre, message, positif/négatif/neutre, `cancelable`, `onDismiss`) | `ui/component/FlexAlert.java` |
| Messages avec action | `FlexSnackbar` (`make(...)`, `action`, `duration`, `backgroundColor(int)`, `textColor(int)`) | `ui/component/FlexSnackbar.java` |
| Messages sans action | `FlexToast.show(...)` / `showResource(...)` | `ui/component/FlexToast.java` |
| Pull-to-refresh | `FlexRefresh` : `onRefresh(Runnable)`, `refreshing(boolean)`, `colorScheme(int)`, `enabled(boolean)` | `ui/container/FlexRefresh.java` |
| Clavier | `FlexIme.show(FlexView)`, `FlexIme.hide(FlexView)`, `FlexIme.hide(FlexContextInterface)` | `core/FlexIme.java` |
| Échappatoire | `FlexView.nativeView()` — pour les seules API qui exigent un `View` (Snackbar, IME, Glide/Coil) | `core/FlexView.java` |

Le style FlexR est respecté : constructeurs en chaîne, couleurs **par index de palette** (`backgroundColor(1)`,
`textColor(3)`), aucune ressource XML, javadoc anglaise avec `@param`/`@return`/`@throws` et note de threading,
et un échec explicite plutôt qu'un silence (`loadUri` sans loader lève une `IllegalStateException` qui nomme la
parade).

### Pull-to-refresh : livré, sans dépendance nouvelle

`SwipeRefreshLayout` n'est pas utilisable ici (artefact absent du cache, réseau bloqué), et l'ajouter aurait rendu le
build rouge. `FlexRefresh` implémente donc le geste **avec `androidx.core` seulement**, par nested scrolling : un
enfant scrollable (`FlexList`, `FlexScrollNested`, `RecyclerView`) signale le survol non consommé, le conteneur le
consomme, dessine un `CircularProgressIndicator` Material et déclenche l'action au relâchement. Aucune classe de
`ui/list` n'est touchée.

Limite assumée : un `ScrollView` simple ne participe pas au nested scrolling, donc tirer dans un
`FlexScrollVertical` ne déclenche rien. Validation : compilation vérifiée, mais le geste demande un **appareil** —
à couvrir par `SmokeContainersBody`. Si l'artefact officiel devient disponible, la classe peut être remplacée par un
simple enrobage sans changer l'API publique (`onRefresh` / `refreshing` / `colorScheme`).

Étapes du plan initial, conservées pour mémoire :

1. `libs.versions.toml` : `swiperefreshlayout = "1.1.0"` ; `FlexRCore/build.gradle` : `api libs.swiperefreshlayout` ;
2. `ui/container/FlexRefresh extends BoxImpl` : un `SwipeRefreshLayout` qui héberge un `FrameLayout` interne (le
   `boxWrapper` ponte ce `FrameLayout`, donc `add(FlexView)` continue de fonctionner avec plusieurs enfants) ;
3. API : `onRefresh(Runnable)`, `refreshing(boolean)`, `colorScheme(int themeColorIndex)`, `enabled(boolean)` ;
4. validation par l'écran de fumée `SmokeContainersBody`, pas par un test JVM (une vue ne s'instancie pas hors
   appareil).

---

## 10. Niveau de langage Java : 11, 17 ou 21 ?

Trois choses différentes portent le même nom, et les confondre est la source des mauvaises décisions :

| Niveau | Ce que c'est | Effet d'un passage à 17/21 |
|---|---|---|
| `sourceCompatibility` / `targetCompatibility` | le langage accepté par `javac` et la version du fichier `.class` produit | aucun effet sur l'appareil : D8 retraduit tout en bytecode Dalvik |
| API du JDK utilisées | `java.time`, `List.of`, `Optional`… | celles qui manquent sur Android exigent le *core library desugaring* ; `minSdk 24` en couvre déjà une partie |
| API Android (`android.jar`) | ce que l'appareil sait faire | inchangé par le niveau Java |

**Conséquences concrètes d'un passage à 17** : `javac` accepte les `record`, `sealed`, les `switch` à motifs et les
text blocks ; D8 les retraduit pour tout `minSdk` ; le comportement à l'exécution ne change pas. Le seul vrai risque
est côté **consommateur** : une application dont l'AGP est antérieure à 8.0 peut refuser un `.class` de version 61.
**Passage à 21** : même histoire, plus une contrainte d'outillage (AGP ≥ 8.4 pour que D8 lise la version 65), et
**aucun** gain d'exécution — les *virtual threads* n'existent pas sur Android, et le desugaring ne les fournit pas.

**Décision retenue : `21`**, appliqué aux neuf modules (`compileOptions`). AGP 9.1.1 lit la version de classe 65,
le JDK de compilation est le JBR 21, et le bytecode est de toute façon retraduit par D8 : le gain est de langage,
pas d'exécution. Conséquence à documenter côté consommateur : **AGP >= 8.4** dans l'application qui consomme FlexR.

---

## 11. Publication : ce qui reste à décider

Voir `REVIEW/18-PUBLICATION.md` (licence, `groupId`, dépôt, signature) — rien n'est encore configuré dans le
projet : aucun `maven-publish`, aucun POM, aucune signature.