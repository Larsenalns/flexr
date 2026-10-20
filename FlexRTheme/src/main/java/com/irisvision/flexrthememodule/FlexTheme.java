// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrthememodule;

import android.graphics.Color;
import android.os.Looper;

import com.irisvision.flexrconcurrency.FlexThreadMain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The FlexR theme manager: one palette per theme name, and a single lookup point for colours.
 *
 * <h2>A theme is an ordered palette</h2>
 *
 * <p>Application code never holds a colour: it holds an <b>index</b> into the current palette and asks
 * {@link #load(int)} for the value. Switching theme therefore repaints the whole application without
 * touching a constant, and the indexes used by views stay valid as long as every declared palette has
 * the same length.
 *
 * <h2>Colour format — one convention: {@code #AARRGGBB}</h2>
 *
 * <p>Eight hexadecimal digits, alpha first. The short forms {@code #RGB} and {@code #RRGGBB} are
 * accepted and normalized with an opaque alpha ({@code FF}). Colours are validated and parsed into
 * {@code int} <b>once, at registration</b>: an invalid colour fails at the declaration that contains
 * it instead of at paint time, and no lookup ever re-parses the palette.
 *
 * <h2>Declaring and switching</h2>
 *
 * <pre>{@code
 * FlexTheme.pushTheme("night", List.of("#FF101820", "#FFF2AA33"));
 * FlexTheme.appChangeTheme("night");     // notifies every auto-applying observer
 * int background = FlexTheme.load(0);    // resolved against the current theme
 * }</pre>
 *
 * <h2>Threading</h2>
 *
 * <p>The palette is a {@link ConcurrentHashMap}, the observer lists are {@link CopyOnWriteArrayList}
 * and {@link #currentTheme} is {@code volatile}. Two needs are covered separately:
 * <ul>
 *   <li>{@link #load(int)} and {@link #load(int, String)} may be called from <b>any thread</b> — a view
 *       can be built or measured outside a render pass — and only read concurrent structures;</li>
 *   <li>{@link #appChangeTheme(String)} may also be called from any thread, but applying a theme
 *       writes into views, so the switch is <b>brought back to the main thread</b> (see that
 *       method).</li>
 * </ul>
 *
 * <p>The class is a static facade and cannot be instantiated.
 */
public final class FlexTheme {

    private FlexTheme() {
        // Static facade: no instances.
    }

    /**
     * Name of the current theme. Volatile: read from several threads, written on the main one.
     *
     * <p>Public and assignable for the benefit of tests and of applications that restore a saved
     * preference before the first screen is built; the normal route is {@link #appChangeTheme(String)},
     * which also notifies the observers.
     */
    public static volatile String currentTheme = "day";

    private static final Map<String, List<Integer>> themeArray = new ConcurrentHashMap<>();
    private static final List<Thematic> flexThemeObsActivity = new CopyOnWriteArrayList<>();
    private static final List<Thematic> flexThemeObsFragment = new CopyOnWriteArrayList<>();

    /**
     * Guards the switch against re-entrance: an observer that changes theme from its own
     * {@code ifThemeChange()} must not start a cascade.
     */
    private static final AtomicBoolean APPLYING = new AtomicBoolean(false);

    /* *** Reading colours *** */

    /**
     * Returns the colour at {@code colorIndex} in the <b>current</b> theme.
     *
     * <p>This is the lookup every view uses: the index stays stable while the palette length does not
     * change, so a switch repaints without rebuilding anything.
     *
     * @param colorIndex position in the palette, {@code 0} or more
     * @return the ARGB colour declared at that position
     * @throws IllegalStateException     if the current theme was never declared
     * @throws IndexOutOfBoundsException if the index is outside the current palette
     */
    public static int load(int colorIndex) {
        return load(colorIndex, currentTheme);
    }

    /**
     * Returns the colour at {@code colorIndex} in the palette named {@code themeName}.
     *
     * <p>Used to compare two themes, or to paint a preview of a theme that is not current yet. For
     * ordinary rendering, {@link #load(int)} is the right call: it follows the theme of the app.
     *
     * @param colorIndex position in the palette, {@code 0} or more
     * @param themeName  declared theme name
     * @return the ARGB colour declared at that position
     * @throws IllegalStateException     if {@code themeName} was never declared; the message lists the
     *                                   declared names
     * @throws IndexOutOfBoundsException if the index is outside that palette
     */
    public static int load(int colorIndex, String themeName) {
        List<Integer> palette = themeArray.get(themeName);
        if (palette == null) {
            throw new IllegalStateException(
                    "Unknown theme: \"" + themeName + "\". Declared themes: " + themeArray.keySet());
        }
        if (colorIndex < 0 || colorIndex >= palette.size()) {
            throw new IndexOutOfBoundsException(
                    "Index " + colorIndex + " is outside palette \"" + themeName
                            + "\", which holds " + palette.size() + " colour(s).");
        }
        return palette.get(colorIndex);
    }

    /* *** Observer subscription *** */

    /**
     * Registers an observer in the <b>Activity</b> scope: it keeps receiving theme switches for the
     * whole life of the Activity, across changes of screen.
     *
     * <p>Registering twice does nothing; {@code null} is ignored, so a caller does not have to test
     * it.
     *
     * @param thematic observer to notify on every switch
     */
    public static void subscribeActivityScope(Thematic thematic) {
        if (thematic != null && !flexThemeObsActivity.contains(thematic)) {
            flexThemeObsActivity.add(thematic);
        }
    }

    /**
     * Registers an observer in the <b>screen</b> scope: it receives theme switches until the Fragment
     * destroys its view, which is the default for a view created by FlexR.
     *
     * @param thematic observer to notify on every switch; {@code null} is ignored
     * @see #clearObsFragScope(String)
     */
    public static void subscribeFragScope(Thematic thematic) {
        if (thematic != null && !flexThemeObsFragment.contains(thematic)) {
            flexThemeObsFragment.add(thematic);
        }
    }

    /* *** Unsubscription *** */

    /**
     * Scope identifiers, shared by contract with the core's {@code FlexScope} enum: the names must
     * match <b>exactly</b> ({@code "FlexFragment"}, {@code "FlexActivity"}). They are repeated here
     * rather than imported because this module does not depend on the core — the dependency goes the
     * other way.
     */
    private static final String SCOPE_ACTIVITY = "FlexActivity";
    private static final String SCOPE_FRAGMENT = "FlexFragment";

    /**
     * Removes from the Activity scope every observer whose scope identifier is {@code scopeId}.
     *
     * <p>Called from the teardown of the Activity. The comparison is null-safe: an observer that never
     * identified itself never matches, and cannot make the cleanup of the others fail.
     *
     * @param scopeId identifier of the Activity being destroyed; {@code null} removes nothing
     * @see #subscribeActivityScope(Thematic)
     */
    public static void clearObsActivityScope(String scopeId) {
        removeMatching(flexThemeObsActivity, SCOPE_ACTIVITY, scopeId);
    }

    /**
     * Removes from the screen scope every observer whose scope identifier is {@code scopeId}.
     *
     * <p>This is what {@code FlexFragment.onDestroyView()} calls, so a disposed view stops being
     * notified — and stops being reachable through the theme registry.
     *
     * @param scopeId identifier of the screen being destroyed; {@code null} removes nothing
     * @see #subscribeFragScope(Thematic)
     */
    public static void clearObsFragScope(String scopeId) {
        removeMatching(flexThemeObsFragment, SCOPE_FRAGMENT, scopeId);
    }

    /**
     * Removes the observers of one list whose scope identifier matches, and leaves the others alone.
     *
     * <p>Removal goes through {@code removeIf}, which mutates the list in place: walking the list and
     * removing while iterating would throw a {@code ConcurrentModificationException} exactly when the
     * teardown runs.
     *
     * @param target  the observer list to clean
     * @param scope   scope name the observers are compared against
     * @param scopeId identifier to match; {@code null} leaves the list untouched
     */
    private static void removeMatching(List<Thematic> target, String scope, String scopeId) {
        if (scopeId == null) {
            return;
        }
        // removeIf mutates the list in place: no ConcurrentModificationException, unlike a forEach
        // that removes while it walks its own list.
        target.removeIf(thematic -> Objects.equals(scopeId, thematic.ref(scope)));
    }

    /* *** Declaring and switching themes *** */

    /**
     * Declares the palette of {@code themeKey}. A theme that is already declared is <b>not</b>
     * redefined: the first declaration wins, so registering again from every screen creation cannot
     * reset a theme in use.
     *
     * <p>Colours are parsed and validated here, once, which is why a bad colour is reported at the
     * declaration that contains it rather than at paint time. The palette is stored as an immutable
     * list of ARGB {@code int}s.
     *
     * @param themeKey    name used later by {@link #appChangeTheme(String)} and
     *                    {@link #load(int, String)}
     * @param themeColors colours in the reference {@code #AARRGGBB} format; {@code #RGB} and
     *                    {@code #RRGGBB} are accepted and completed with an opaque alpha
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IllegalArgumentException if a colour is invalid; the message names the theme and the
     *                                  index of the offending colour
     */
    public static void pushTheme(String themeKey, List<String> themeColors) {
        Objects.requireNonNull(themeKey, "themeKey == null");
        Objects.requireNonNull(themeColors, "themeColors == null");

        List<Integer> parsed = new ArrayList<>(themeColors.size());
        for (int i = 0; i < themeColors.size(); i++) {
            parsed.add(parseColor(themeKey, i, themeColors.get(i)));
        }
        themeArray.putIfAbsent(themeKey, Collections.unmodifiableList(parsed));
    }

    /**
     * Switches the application to {@code themeKey} and notifies the observers whose
     * {@link Thematic#isThemeAutoApply()} is true.
     *
     * <h4>Threading: call it from anywhere, it applies on the main thread</h4>
     *
     * <p>This method may be called from <b>any thread</b>, and has to be: a theme switch comes from a
     * click, from a network callback or from a preference read in the background. But applying a theme
     * notifies observers that are very often views — a {@code FlexView} repaints, a {@code FlexCard}
     * changes colour — so the application is <b>always brought back to the main thread</b>, through
     * {@link FlexThreadMain#runOnMain(Runnable)}.
     *
     * <ul>
     *   <li>called <b>from the main thread</b>: applied <b>immediately</b>, so the order of the
     *       operations the caller performs is preserved;</li>
     *   <li>called <b>from another thread</b>: applied on the next turn of the main loop, without
     *       blocking the caller.</li>
     * </ul>
     *
     * <p><b>What this implies for an observer that is not a view.</b> A hand-written {@code Thematic} is
     * notified on the main thread too. If it has heavy or blocking work to do, moving that work off the
     * main thread is its own responsibility: the library cannot guess the intention. The rule of thumb is
     * simple — <i>a theme is visual, so it belongs to the main thread</i> — and it does not contradict
     * the state-notification rule of {@code FlexConsumer}: those notifications are not visual, these
     * are.
     *
     * <p>If {@code themeKey} was never declared, nothing happens: neither a switch nor a notification,
     * because the test is made before the application is even scheduled.
     *
     * <p>If an observer switches theme from its own {@code ifThemeChange()}, the nested call is ignored:
     * re-entrance is refused rather than recursed into.
     *
     * @param themeKey name of the theme to apply; it must have been declared with
     *                 {@link #pushTheme(String, List)}, otherwise the call does nothing
     * @see #load(int)
     */
    public static void appChangeTheme(String themeKey) {
        if (!themeArray.containsKey(themeKey)) {
            return; // Unknown theme: neither switch nor notification.
        }

        // The switch AND the notification run together on the main thread: the current theme and what
        // is painted stay consistent, even when the call comes from elsewhere.
        if (Looper.getMainLooper() == null) {
            // No main loop in this environment (plain JVM unit test): apply in place, since there is
            // no main thread to schedule on and no view to paint.
            applyThemeChange(themeKey);
            return;
        }
        FlexThreadMain.runOnMain(() -> applyThemeChange(themeKey));
    }

    /**
     * Performs the switch itself: the current key first, then the notification of the observers.
     *
     * <p>Runs on the main thread, or in place when there is no main loop. Re-entrance is refused
     * through {@link #APPLYING}, so an observer that switches theme from its own callback cannot start
     * a cascade.
     *
     * @param themeKey theme to apply, already known to be declared
     */
    private static void applyThemeChange(String themeKey) {
        if (!APPLYING.compareAndSet(false, true)) {
            return; // A theme is already being applied: do not re-enter.
        }
        try {
            currentTheme = themeKey;
            applyTo(flexThemeObsActivity);
            applyTo(flexThemeObsFragment);
        } finally {
            APPLYING.set(false);
        }
    }

    private static void applyTo(List<Thematic> observers) {
        // Extra defensive copy: ifThemeChange() may subscribe or unsubscribe observers.
        for (Thematic thematic : new ArrayList<>(observers)) {
            if (thematic.isThemeAutoApply()) {
                thematic.ifThemeChange();
            }
        }
    }

    /* *** Utilities *** */

    /**
     * Validates one colour of a palette and converts it into an {@code int}.
     *
     * <p>Reference convention: {@code #AARRGGBB}. The short forms are completed with an opaque alpha,
     * so a palette written in {@code #RRGGBB} behaves like one written in {@code #FFRRGGBB}.
     *
     * @param themeKey name of the theme being declared, used in the error messages
     * @param index    position of the colour in that palette, used in the error messages
     * @param value    colour as declared: {@code #RGB}, {@code #RRGGBB} or {@code #AARRGGBB}
     * @return the colour as an ARGB {@code int}, short forms expanded with an opaque alpha
     * @throws IllegalArgumentException if {@code value} is {@code null}, is not hexadecimal, or matches
     *                                  none of the accepted formats
     */
    private static int parseColor(String themeKey, int index, String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Null colour in theme \"" + themeKey + "\" at index " + index + ".");
        }

        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            String hex = normalized.substring(1).toUpperCase(Locale.ROOT);
            if (hex.length() == 3) {                 // #RGB  -> #FFRRGGBB
                StringBuilder expanded = new StringBuilder(8);
                expanded.append("FF");
                for (int i = 0; i < 3; i++) {
                    expanded.append(hex.charAt(i)).append(hex.charAt(i));
                }
                hex = expanded.toString();
            } else if (hex.length() == 6) {          // #RRGGBB -> #FFRRGGBB
                hex = "FF" + hex;
            }
            if (hex.length() != 8) {
                throw new IllegalArgumentException(
                        "Invalid colour in theme \"" + themeKey + "\" at index " + index
                                + ": \"" + value + "\". Expected format #AARRGGBB (or #RRGGBB).");
            }
            normalized = "#" + hex;
        }

        try {
            return Color.parseColor(normalized);
        } catch (IllegalArgumentException cause) {
            throw new IllegalArgumentException(
                    "Invalid colour in theme \"" + themeKey + "\" at index " + index
                            + ": \"" + value + "\". Expected format #AARRGGBB (or #RRGGBB).",
                    cause);
        }
    }
}
