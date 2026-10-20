# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/),
and the project adheres to [Semantic Versioning](https://semver.org/): the public API only changes in a
major release.

## [1.0.0]

First public release. The API below is the one that will not break before `2.0.0`.

### Core

- `FlexActivity`, `FlexFragment`, `FlexAppbar` / `FlexBody`: a screen is a Fragment plus a view tree built
  in Java.
- `FlexView` (65 public members): size, margins, padding, gravity, background, elevation, visibility,
  click and long-click, accessibility description and test tag.
- Containers and layouts: `FlexBox`, `FlexCard`, `FlexContainer`, `FlexRadioGroup`, `FlexGrid`,
  `FlexFlipper`, `FlexRefresh` (pull to refresh).
- Views: `FlexText`, `FlexImg`, `FlexButton`, `FlexInput`, `FlexCheck`, `FlexSwitch`,
  `FlexRadioButton`, `FlexSlider`, `FlexSliderRange`, `FlexTag`, `FlexTagGroup`, `FlexShape`,
  `FlexProgress`, `FlexDatePicker`, `FlexBottomSheet`.
- Lists: `FlexList` (RecyclerView), `FlexShortList`, `FlexSpinner`, `FlexPager`, `FlexScrollVertical`,
  `FlexScrollHorizontal`, `FlexScrollNested`, `FlexAdapterRcv`, `FlexAdapterObservable`,
  `FlexAdapterFilter`.
- State: `FlexProvider` / `FlexConsumer` with Activity, Fragment and background scopes; `FlexReactive`.
- UI utilities: `FlexAlert`, `FlexSnackbar`, `FlexToast`, `FlexIme`.

### Animation and theme

- `FlexAnimate`: 47 animatable properties, including the layout parameters Android cannot animate.
- `FlexChoreo`: a frame loop in `Timer` or `Condition` mode, pausable and scoped.
- `FlexTheme`: ordered palettes, `#AARRGGBB`, validated at registration, applied on the main thread.

### Concurrency

- Four named pools (`IO`, `Unfixed`, `Default`, `Main`), `FlexTask` with a completion callback delivered
  exactly once, `FlexThreadMain` for the UI thread.

### Packaging

- Consumer ProGuard rules in every module: the four reflective mechanisms are covered, and
  `setFlexAnimate*` survives minification.
- `flexr-bundle` (one dependency) and `flexr-bom` (version alignment).
- `flexr-media` is optional: a project that shows no video never ships ExoPlayer.
