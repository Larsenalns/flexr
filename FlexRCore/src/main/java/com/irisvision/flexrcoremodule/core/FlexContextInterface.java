// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.core.os.LocaleListCompat;

import com.irisvision.flexrcoremodule.enums.In;

/**
 * What every screen block may ask of its host.
 *
 * <h2>Where it comes from</h2>
 *
 * <p>A block — a {@code FlexBody}, a {@code FlexRoot} — does not hold an Activity, a Fragment or an
 * Android {@code Context} itself. It holds a {@code FlexContextInterface}, implemented by
 * {@link FlexContext}, which is the host it was built into. Everything a block legitimately needs is
 * reachable from here, and nothing else: the block cannot reach into the Activity's internals.
 *
 * <pre>{@code
 * public final class HomeBody extends FlexBody {
 *     @Override protected FlexView dynamicBuild() {
 *         return new FlexText(this)
 *                 .text("Hello")
 *                 .textColor(flexAsset(In.color, "flex_text_primary"));
 *     }
 * }
 * }</pre>
 *
 * <h2>Resources by name — the reason for this API</h2>
 *
 * <p>{@link #flexAsset(In, String)} and {@link #flexDrawable(String)} resolve a resource <b>by its
 * name</b> rather than through an {@code R} constant. This is a deliberate choice of FlexR: an application
 * whose screens are described as data — dynamic screens, server-driven UI, a designer tool — cannot
 * reference generated identifiers. The name is the contract, and the resolution is done at runtime.
 *
 * <p>Because of that, an application that enables resource shrinking must tell the build which of those
 * names are reachable: see the {@code keep.xml} mechanism of Android and the documentation of
 * {@code FlexRCore}. A name that is stripped is reported with an explicit exception naming the resource,
 * its type and its package.
 */
public interface FlexContextInterface {

    /** The Android {@code Context} this block runs in. */
    Context getFlexContext();

    /** The {@link FlexActivity} hosting this block. */
    FlexActivity getFlexActivity();

    /** The resources of the hosting application. */
    Resources getFlexResources();

    /** The current configuration — orientation, screen size, locale list. */
    Configuration getFlexConfig();

    /** The locales currently selected by the user. */
    LocaleListCompat getFlexLocals();

    /**
     * Resolves a resource by its name.
     *
     * @param in         the resource type — {@code drawable}, {@code color}, {@code string}…
     * @param assetName  the resource name as written in the code, without its type prefix
     * @return the resolved identifier
     * @throws IllegalArgumentException if no resource of that type carries that name, or if it was
     *                                  removed by resource shrinking
     */
    int flexAsset(In in, String assetName);

    /**
     * Loads a drawable from the {@code drawable} resource directory, by name.
     *
     * @param drawableName the drawable name
     * @return the drawable, or {@code null} when the resource does not exist
     */
    Drawable flexDrawable(String drawableName);

    /**
     * The metrics a block may draw into: the usable screen, <b>minus the app bar</b> when the host has
     * one.
     *
     * <p>This is the variant to use when sizing a block, because it accounts for what is actually left
     * on screen. The returned object is fresh on every call, so it may be modified freely.
     *
     * @return usable metrics, app bar excluded
     */
    DisplayMetrics getFlexDisplayMetrics();

    /**
     * The usable metrics of the display — the area available to the application, system bars excluded.
     *
     * <p>The returned object is fresh on every call: two callers never share a mutable instance.
     *
     * @return the usable metrics
     */
    DisplayMetrics getDisplayMetrics();

    /**
     * The real metrics of the display, system bars included.
     *
     * <p>The returned object is fresh on every call.
     *
     * @return the real screen metrics
     */
    DisplayMetrics getDisplayMetrics_fullScreen();

    /** The Fragment hosting this block. */
    FlexFragment getFlexFragment();

    /**
     * The identity of the screen: {@code "<fragment>:<activity>"}.
     *
     * <p>It is the string every view of the screen uses as its scope identifier, which is what makes a
     * subscription releasable (see {@link ScopeIdentity}).
     *
     * @return the identity of this screen
     */
    String signature();

    /**
     * Opens another FlexActivity and closes the current one.
     *
     * <p>The usual navigation: the new screen replaces this one, and the back stack does not grow.
     *
     * @param flexActivityClass the Activity to open
     * @param bundle            optional arguments for it, may be {@code null}
     */
    void launchAndClose(Class<? extends FlexActivity> flexActivityClass, Bundle bundle);

    /**
     * Opens another FlexActivity and <b>keeps</b> the current one.
     *
     * <p>The new screen is pushed on top: this one stays alive and will be shown again when the user
     * comes back.
     *
     * @param flexActivityClass the Activity to open
     * @param bundle            optional arguments for it, may be {@code null}
     */
    void launchAndKeep(Class<? extends FlexActivity> flexActivityClass, Bundle bundle);
}
