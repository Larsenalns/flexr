# FlexR

Declarative UI for Android, written in Java: screens are built as a tree of objects, the theme is an
indexed palette, and an image or a background is designated **by its name**, never by `R.drawable...`.

```java
public class HomeBody extends FlexBody {
    @Override protected FlexView dynamicBuild() {
        FlexBox root = new FlexBox(this);
        root.background(0).boxParams().Sizer(Val.Match);
        root.add(new FlexText(this).text("Hello").textSize(18).textColor(3));
        root.add(new FlexCard(this).cardColor(1).radius(16).add(
                new FlexButton(this).name("Continue").ripple(2)));
        return root;
    }
    @Override protected void attachEvents() { }
    @Override protected void initialBusinessLogic() { }
}
```

## Why

- **No XML.** A screen is a class you can refactor, test and read; the layout is code.
- **A theme is a palette.** Application code holds an *index*, the current theme decides the colour, so
  switching theme repaints everything without touching a constant.
- **Resources by name.** `FlexImg.load("avatar")` and `flexDrawable("icon")` let a screen be driven by
  data — a server response can name an image. The flip side is `shrinkResources`, covered below.
- **State, not callbacks.** `FlexProvider` / `FlexConsumer` deliver changes on the main thread, scoped to
  the screen or to the Activity, and release themselves with it.

## Requirements

| | |
|---|---|
| `minSdk` | 24 |
| `compileSdk` | 35 (the library modules) |
| Android Gradle Plugin | **8.4+** in the consuming application (the AAR is Java 21 bytecode) |
| JDK to build the project | 21 |

## Install

Once published:

```gradle
dependencies {
    implementation("com.irisvision:flexr-core:1.0.0")   // + FX, theme and concurrency
    implementation("com.irisvision:flexr-media:1.0.0")  // optional, brings media3
}
```

or, with the version aligned for you:

```gradle
dependencies {
    implementation(platform("com.irisvision:flexr-bom:1.0.0"))
    implementation("com.irisvision:flexr-core")
    implementation("com.irisvision:flexr-media")
}
```

While the library is used from source, depend on the projects instead:
`implementation(project(":FlexRCore"))`.

## Quick start

```java
public class Home extends FlexActivity {
    @Override protected void flexCreate(Bundle state) { init(); }

    @Override public void init() {
        fullScreen();
        initialFragment(new HomeFragment(), false);
    }

    @Override protected void initFlexTheme() {
        FlexTheme.pushTheme("day",   List.of("#F5F7FA", "#FFFFFF", "#00B208", "#212121", "#E53935"));
        FlexTheme.pushTheme("night", List.of("#010101", "#141414", "#2EB934", "#FFFFFF", "#FF5252"));
        FlexTheme.appChangeTheme("day");     // after pushTheme, never before
    }

    @Override protected void initFlexReactive() { }
}
```

```java
public class HomeFragment extends FlexFragment {
    @Override protected String setNavigationKey() { return "HomeFragment"; }
    @Override protected void onFlexCreateView(Bundle state) {
        prepareScreen(null, HomeBody.class);   // (appbar, body); the appbar may be null
    }
}
```

The palette is ordered and shared by the whole application:

| Index | Convention in the core |
|---|---|
| 0 | background of the body |
| 1 | background of the app bar |
| 2, 3, 4… | whatever your screens decide (accent, main text, muted text…) |

## Modules

| Module | Role | Published |
|---|---|---|
| `flexr-core` | Activity, Fragment, screens, views and containers | yes |
| `flexr-fx` | `FlexAnimate`, `FlexChoreo` | yes |
| `flexr-theme` | `FlexTheme`, `Thematic` | yes |
| `flexr-concurrency` | pools, `FlexTask`, main-thread bridge | yes |
| `flexr-media` | `FlexVideo`, Media3/ExoPlayer (optional) | yes |
| `flexr-starter` | template screens to copy | yes |
| `flexr-bundle` | no code: core + media, one dependency | yes |
| `flexr-bom` | no code: version alignment | yes |

## R8 and `shrinkResources`

The library ships consumer ProGuard rules for the four places where it relies on reflection, including the
animation setters (`setFlexAnimate*`) — without them, animations die **in release only**. `minifyEnabled`
is therefore safe.

`shrinkResources true` is the one thing to configure yourself: a resource designated **by its name** is
invisible to the analyzer. Add a `res/raw/keep.xml`:

```xml
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@drawable/avatar,@drawable/icon,@raw/clip" />
```

Wildcards (`@drawable/*`) work but keep far more than needed. A missing resource fails loudly:
`flexAsset(...)` raises an exception naming the resource and this remedy.

## Validation on device

`FlexRSmoke` is a module that is never published. It installs a second launcher icon, **FlexR Smoke**,
which exercises the views, the lists, the animations and the theme, and prints `N OK, M KO` with the
failing class. Run it in a **minified release** to prove the R8 rules.

## Documentation

The integration guide lives in [docs/GUIDE.md](docs/GUIDE.md) (French): R8, resources by name, theme
indices, `baseInit()`, and the API for images, dialogs, keyboard and pull-to-refresh.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Contributions are accepted under the DCO, see
[CONTRIBUTING.md](CONTRIBUTING.md).
