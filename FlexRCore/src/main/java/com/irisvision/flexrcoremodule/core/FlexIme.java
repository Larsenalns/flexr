// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Controls the soft keyboard (the IME): show it on a field, hide it, or close it from anywhere in the
 * screen.
 *
 * <h2>Why a helper exists</h2>
 *
 * <p>Android's contract is small but full of traps: {@code showSoftInput(...)} silently does nothing when
 * the view is not focused, or when the window has no token yet; {@code hideSoftInputFromWindow(...)}
 * needs the token of the window that actually holds the keyboard. These three methods encode the working
 * call for each case, so a screen does not have to.
 *
 * <h2>Contract</h2>
 *
 * <ul>
 *   <li>{@link #show(FlexView)} focuses the view first: a field that is not focused never receives the
 *       keyboard, whatever the flags;</li>
 *   <li>{@link #hide(FlexView)} closes the keyboard attached to that view, and leaves the focus alone;</li>
 *   <li>{@link #hide(FlexContextInterface)} closes whatever keyboard the screen currently shows, which is
 *       what a "Done" button or a navigation needs;</li>
 *   <li>the class never throws when the platform has no IME installed or when the context is not an
 *       Activity: the calls simply do nothing.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p>UI thread only: the calls manipulate focus and window tokens.
 *
 * <pre>{@code
 * FlexInput field = new FlexInput(this).boxHint("Email");
 * field.onClick(v -> FlexIme.show(field));
 * ...
 * FlexIme.hide(this);
 * }</pre>
 */
public final class FlexIme {

    private FlexIme(){
    }

    /**
     * Returns the system input method manager.
     *
     * @param context any context of the application
     * @return the manager, or {@code null} when the platform has none
     */
    private static InputMethodManager imm(Context context){
        if (context == null) {
            return null;
        }
        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * Shows the keyboard on the given view, focusing it first.
     *
     * <p>The view must be able to take focus — see {@code FlexInput.focusable(true)} — and must already be
     * attached to the window; calling this from {@code dynamicBuild()} does nothing, since the field is not
     * laid out yet.
     *
     * @param view field that must receive the keyboard
     */
    public static void show(FlexView view){
        if (view == null) {
            return;
        }
        View nativeView = view.nativeView();
        nativeView.requestFocus();
        InputMethodManager manager = imm(nativeView.getContext());
        if (manager != null) {
            manager.showSoftInput(nativeView, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Hides the keyboard attached to the given view.
     *
     * <p>The focus is left where it is: the field keeps the cursor, which is what a "next field" flow
     * expects between two calls to {@link #show(FlexView)}.
     *
     * @param view view whose keyboard must be closed
     */
    public static void hide(FlexView view){
        if (view == null) {
            return;
        }
        View nativeView = view.nativeView();
        InputMethodManager manager = imm(nativeView.getContext());
        if (manager != null && nativeView.getWindowToken() != null) {
            manager.hideSoftInputFromWindow(nativeView.getWindowToken(), 0);
        }
    }

    /**
     * Hides whatever keyboard the screen is showing.
     *
     * <p>Used when the focused field is not known — a menu action, a navigation, the end of a form. The
     * keyboard disappears, the focus is kept.
     *
     * @param flexContext context of the screen; a context that is not an Activity is ignored
     */
    public static void hide(FlexContextInterface flexContext){
        if (flexContext == null) {
            return;
        }
        Context context = flexContext.getFlexContext();
        InputMethodManager manager = imm(context);
        if (manager == null) {
            return;
        }
        View focus = (context instanceof android.app.Activity)
                ? ((android.app.Activity) context).getCurrentFocus()
                : null;
        if (focus != null && focus.getWindowToken() != null) {
            manager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
