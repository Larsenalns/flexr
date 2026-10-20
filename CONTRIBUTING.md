# Contributing to FlexR

Thanks for the interest. This file is short on purpose: it lists what a change must satisfy, not how to
feel about it.

## Before you start

| Tool | Version |
|---|---|
| JDK | 21 |
| Android Gradle Plugin | 9.1.1 (already in `gradle/libs.versions.toml`) |
| Android SDK | platform 35 |

```bash
./gradlew :FlexRCore:assembleDebug :FlexRCore:testDebugUnitTest
```

The three JVM suites must stay green: **46** (core), **9** (concurrency), **7** (theme).

## Rules

1. **No XML layout.** A screen is built in `dynamicBuild()`, and a view is a class.
2. **Colours are palette indices**, never `int` literals: `background(1)`, `textColor(3)`.
3. **Resources are designated by name** (`flexDrawable("icon")`), never by `R.drawable`. That is the
   library's promise; a pull request that hard-codes a numeric id will be refused.
4. **Public messages are in English**, like the javadoc. A French string left in the code is a bug.
5. **Documentation**: every public member has javadoc with role, contracts, `@param`/`@return`/`@throws`,
   a usage example when it helps, and a threading note when views are involved.
6. **Do not change the code of an existing behaviour without a test** that would have failed before.
7. **No new dependency in `flexr-core`** without discussing it first: module size and R8 behaviour are
   part of the product.

## Sign your commits (DCO)

This project uses the Developer Certificate of Origin: add `-s` to your commit, which appends

```
Signed-off-by: Your Name <you@example.com>
```

```bash
git commit -s -S -m "core: reject a null anchor in FlexSnackbar"
```

(`-S` additionally signs the commit with your GPG key, which the repository requires on `main`.)

## Pull requests

- one subject per pull request, with a message that says **why**;
- run the three test suites before pushing;
- if the change touches a public API, say what breaks and how a consumer migrates.
