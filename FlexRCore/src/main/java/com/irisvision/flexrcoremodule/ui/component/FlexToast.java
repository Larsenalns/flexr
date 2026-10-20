// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.component;

import android.widget.Toast;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;

/**
 * A short message shown at the bottom of the screen, with no action and no interaction.
 *
 * <p>Use a toast for information nobody has to answer ("Saved", "Copied"), a {@link FlexSnackbar} when
 * the user can react, and a {@link FlexAlert} when the application needs an answer before going on.
 *
 * <h2>Contract</h2>
 *
 * <p>A toast is <b>not</b> part of the view tree: it is a window of the system, so it survives the
 * destruction of the screen it was shown from. That is why it must never carry information the user has
 * to keep: on Android 11 and later a toast is capped to two lines, and it disappears on its own.
 *
 * <p>The class is a static facade: it holds no state and cannot be instantiated.
 *
 * <h2>Threading</h2>
 *
 * <p>UI thread only, like every call that shows a window.
 *
 * <pre>{@code
 * FlexToast.show(this, "Saved");
 * FlexToast.show(this, R.string.saved, FlexToast.LONG);
 * }</pre>
 */
public final class FlexToast {

    /** Duration of {@link Toast#LENGTH_SHORT}, about two seconds. */
    public static final int SHORT = Toast.LENGTH_SHORT;

    /** Duration of {@link Toast#LENGTH_LONG}, about three and a half seconds. */
    public static final int LONG = Toast.LENGTH_LONG;

    private FlexToast(){
    }

    /**
     * Shows a message for the short duration.
     *
     * @param flexContext context of the screen that shows the message
     * @param text        message to display; {@code null} shows an empty toast
     * @return the native toast, so a caller can cancel it
     * @throws IllegalArgumentException if {@code flexContext} is {@code null}
     */
    public static Toast show(FlexContextInterface flexContext, CharSequence text){
        return show(flexContext, text, Toast.LENGTH_SHORT);
    }

    /**
     * Shows a message for the requested duration.
     *
     * @param flexContext context of the screen that shows the message
     * @param text        message to display; {@code null} shows an empty toast
     * @param duration    {@link #SHORT} or {@link #LONG}
     * @return the native toast, so a caller can cancel it
     * @throws IllegalArgumentException if {@code flexContext} is {@code null}
     */
    public static Toast show(FlexContextInterface flexContext, CharSequence text, int duration){
        if (flexContext == null) {
            throw new IllegalArgumentException("FlexToast.show(null, ...) : a FlexR context is required.");
        }
        CharSequence message = (text == null) ? "" : text;
        Toast toast = Toast.makeText(flexContext.getFlexContext(), message, duration);
        toast.show();
        return toast;
    }

    /**
     * Shows a message taken from a string resource.
     *
     * <p>This is the variant to use for anything translated: the text then follows the application
     * language instead of carrying an English literal in the code.
     *
     * @param flexContext   context of the screen that shows the message
     * @param messageResId  identifier of the string resource
     * @param duration      {@link #SHORT} or {@link #LONG}
     * @return the native toast, so a caller can cancel it
     * @throws IllegalArgumentException if {@code flexContext} is {@code null}
     */
    public static Toast showResource(FlexContextInterface flexContext, int messageResId, int duration){
        if (flexContext == null) {
            throw new IllegalArgumentException("FlexToast.showResource(null, ...) : a FlexR context is required.");
        }
        Toast toast = Toast.makeText(flexContext.getFlexContext(), messageResId, duration);
        toast.show();
        return toast;
    }
}
