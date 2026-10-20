// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.component;

import com.google.android.material.snackbar.Snackbar;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrthememodule.FlexTheme;

/**
 * A transient message shown at the bottom of the screen, optionally carrying one action.
 *
 * <p>{@link FlexToast} is enough when nothing has to be answered; a snackbar is the right choice when the
 * user can react — "Message deleted", with an "Undo" — and when the message must survive a rotation.
 *
 * <h2>Contract</h2>
 *
 * <ul>
 *   <li>the snackbar is anchored to a <b>view of the screen</b>, so it must be built once the layout is
 *       attached: calling it from {@code dynamicBuild()} would fail, since the root is not yet in the
 *       window;</li>
 *   <li>the colours are palette indices, like everywhere else in FlexR: nothing is hard-coded, and a theme
 *       switch after the snackbar is shown does not repaint it — show it again instead;</li>
 *   <li>one snackbar at a time: {@link #show()} replaces the current one instead of queueing.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p>UI thread only: the snackbar adds a view to the screen.
 *
 * <pre>{@code
 * FlexSnackbar.make(this, "Draft deleted")
 *     .action("Undo", () -> repository.restore(id))
 *     .backgroundColor(1)
 *     .textColor(3)
 *     .show();
 * }</pre>
 */
public final class FlexSnackbar {

    /** Duration of {@link Snackbar#LENGTH_SHORT}. */
    public static final int SHORT = Snackbar.LENGTH_SHORT;

    /** Duration of {@link Snackbar#LENGTH_LONG}. */
    public static final int LONG = Snackbar.LENGTH_LONG;

    /** Stays until dismissed or replaced. */
    public static final int INDEFINITE = Snackbar.LENGTH_INDEFINITE;

    private final Snackbar snackbar;

    private FlexSnackbar(Snackbar snackbar){
        this.snackbar = snackbar;
    }

    /**
     * Creates a snackbar anchored to a view of the screen.
     *
     * @param anchor view already attached to the window, typically the root of the screen
     * @param text   message to display; {@code null} shows an empty snackbar
     * @return a builder to configure, then {@link #show()}
     * @throws IllegalArgumentException if {@code anchor} is {@code null}
     */
    public static FlexSnackbar make(FlexView anchor, CharSequence text){
        if (anchor == null) {
            throw new IllegalArgumentException("FlexSnackbar.make(null, ...) : an anchor view is required.");
        }
        CharSequence message = (text == null) ? "" : text;
        return new FlexSnackbar(Snackbar.make(anchor.nativeView(), message, Snackbar.LENGTH_SHORT));
    }

    /**
     * Adds an action button.
     *
     * @param label  text of the button
     * @param action work to run when it is pressed; {@code null} keeps the button inert
     * @return this builder, for chaining
     */
    public FlexSnackbar action(CharSequence label, final Runnable action){
        this.snackbar.setAction(label, v -> {
            if (action != null) {
                action.run();
            }
        });
        return this;
    }

    /**
     * Sets how long the message stays.
     *
     * @param duration {@link #SHORT}, {@link #LONG} or {@link #INDEFINITE}
     * @return this builder, for chaining
     */
    public FlexSnackbar duration(int duration){
        this.snackbar.setDuration(duration);
        return this;
    }

    /**
     * Sets the background colour from the current theme.
     *
     * @param themeColorIndex index in the current palette
     * @return this builder, for chaining
     * @throws IndexOutOfBoundsException if the index is outside the palette
     */
    public FlexSnackbar backgroundColor(int themeColorIndex){
        this.snackbar.setBackgroundTint(FlexTheme.load(themeColorIndex));
        return this;
    }

    /**
     * Sets the text colour from the current theme.
     *
     * @param themeColorIndex index in the current palette
     * @return this builder, for chaining
     * @throws IndexOutOfBoundsException if the index is outside the palette
     */
    public FlexSnackbar textColor(int themeColorIndex){
        this.snackbar.setTextColor(FlexTheme.load(themeColorIndex));
        return this;
    }

    /**
     * Shows the message, replacing any snackbar currently displayed.
     *
     * @return the native snackbar, for listeners that need it
     */
    public Snackbar show(){
        this.snackbar.show();
        return this.snackbar;
    }

    /**
     * Removes the message if it is displayed.
     */
    public void dismiss(){
        this.snackbar.dismiss();
    }
}
