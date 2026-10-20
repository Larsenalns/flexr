// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.component;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;

/**
 * A modal dialog — alert, confirmation, or a small choice — with no XML and no builder of its own.
 *
 * <h2>Why a class and not a static helper</h2>
 *
 * <p>A dialog is almost always a question, so the API follows the sentence being asked: a title, a
 * message, then the answers in the order of their weight.
 *
 * <pre>{@code
 * new FlexAlert(this)
 *     .title("Delete the draft?")
 *     .message("This cannot be undone.")
 *     .negative("Cancel", null)
 *     .positive("Delete", () -> repository.delete(id))
 *     .show();
 * }</pre>
 *
 * <h2>Contracts</h2>
 *
 * <ul>
 *   <li>only one dialog exists per {@code FlexAlert} instance: calling {@link #show()} twice while it is
 *       on screen returns the current one instead of stacking a second;</li>
 *   <li>an action runs <b>after</b> the dialog is dismissed, exactly once, and only for the button that
 *       was pressed;</li>
 *   <li>a {@code null} action still closes the dialog, which is what a "Cancel" button needs.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p>UI thread only, from the first builder call to {@link #dismiss()}: the dialog creates a window.
 */
public final class FlexAlert {

    private final MaterialAlertDialogBuilder builder;

    private boolean cancelable = true;
    private DialogInterface.OnDismissListener onDismiss;
    private AlertDialog dialog;

    /**
     * Creates a dialog attached to the screen context.
     *
     * @param flexContext context of the screen that shows the dialog
     * @throws IllegalArgumentException if {@code flexContext} is {@code null}
     */
    public FlexAlert(FlexContextInterface flexContext){
        if (flexContext == null) {
            throw new IllegalArgumentException("FlexAlert(null) : a FlexR context is required.");
        }
        this.builder = new MaterialAlertDialogBuilder(flexContext.getFlexContext());
    }

    /**
     * Sets the title line.
     *
     * @param title text shown above the message; {@code null} removes it
     * @return this builder, for chaining
     */
    public FlexAlert title(CharSequence title){
        this.builder.setTitle(title);
        return this;
    }

    /**
     * Sets the body text.
     *
     * @param message explanation shown under the title; {@code null} leaves the dialog with buttons only
     * @return this builder, for chaining
     */
    public FlexAlert message(CharSequence message){
        this.builder.setMessage(message);
        return this;
    }

    /**
     * Adds the confirming answer, drawn as the highlighted button.
     *
     * @param label  text of the button
     * @param action work to run when it is pressed; {@code null} only closes the dialog
     * @return this builder, for chaining
     */
    public FlexAlert positive(CharSequence label, final Runnable action){
        this.builder.setPositiveButton(label, (DialogInterface.OnClickListener) (d, which) -> {
            if (action != null) {
                action.run();
            }
        });
        return this;
    }

    /**
     * Adds the dismissing answer.
     *
     * @param label  text of the button
     * @param action work to run when it is pressed; {@code null} only closes the dialog
     * @return this builder, for chaining
     */
    public FlexAlert negative(CharSequence label, final Runnable action){
        this.builder.setNegativeButton(label, (DialogInterface.OnClickListener) (d, which) -> {
            if (action != null) {
                action.run();
            }
        });
        return this;
    }

    /**
     * Adds a third answer, drawn at the start of the button row.
     *
     * @param label  text of the button
     * @param action work to run when it is pressed; {@code null} only closes the dialog
     * @return this builder, for chaining
     */
    public FlexAlert neutral(CharSequence label, final Runnable action){
        this.builder.setNeutralButton(label, (DialogInterface.OnClickListener) (d, which) -> {
            if (action != null) {
                action.run();
            }
        });
        return this;
    }

    /**
     * Allows or forbids closing the dialog by tapping outside it or pressing Back.
     *
     * @param cancelable {@code true} by default; {@code false} forces the user to pick a button
     * @return this builder, for chaining
     */
    public FlexAlert cancelable(boolean cancelable){
        this.cancelable = cancelable;
        return this;
    }

    /**
     * Registers a listener called when the dialog disappears, whichever way it was closed.
     *
     * @param listener listener to call; {@code null} removes it
     * @return this builder, for chaining
     */
    public FlexAlert onDismiss(DialogInterface.OnDismissListener listener){
        this.onDismiss = listener;
        return this;
    }

    /**
     * Shows the dialog, or returns the one already on screen.
     *
     * <p>Idempotent while the dialog is visible: a second call does not stack a second window, so a
     * double tap cannot show the question twice.
     *
     * @return the dialog on screen, never {@code null}
     */
    public AlertDialog show(){
        if (this.dialog != null && this.dialog.isShowing()) {
            return this.dialog;
        }
        this.builder.setCancelable(this.cancelable);
        if (this.onDismiss != null) {
            this.builder.setOnDismissListener(this.onDismiss);
        }
        this.dialog = this.builder.create();
        this.dialog.show();
        return this.dialog;
    }

    /**
     * Closes the dialog if it is on screen.
     *
     * <p>Safe to call from a lifecycle callback, and harmless when nothing is shown.
     */
    public void dismiss(){
        if (this.dialog != null) {
            this.dialog.dismiss();
            this.dialog = null;
        }
    }
}
