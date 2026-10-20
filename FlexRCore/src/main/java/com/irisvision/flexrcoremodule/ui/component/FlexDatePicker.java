// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.component;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrconcurrency.FlexThreadMain;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Date picker, presented as a system dialog.
 *
 * <h2>Threading</h2>
 *
 * <p>A {@code DatePickerDialog} builds a {@code Handler}, so it must be created on a thread that
 * has a {@code Looper}, that is on the main thread. FlexR delivers its notification callbacks
 * <b>off</b> the UI thread (see {@code FlexConsumer}), so creating the dialog directly from a
 * {@code notifyDataChanged} would throw
 * {@code Can't create handler inside thread that has not called Looper.prepare()}. The dialog is
 * therefore displayed through {@link FlexThreadMain#runOnMain(Runnable)}, which makes the call
 * safe from any thread and immediate when the caller already runs on the main thread.
 *
 * <p>API reminder for {@link Action}: {@code dateNumber} holds the <b>raw</b> values returned by
 * Android, hence a <b>month from 0 to 11</b> (January = 0).
 */
public final class FlexDatePicker {

    /** Date locale used by the formatter: {@link #En} for English, {@link #Fr} for French. */
    public static final int En = 0, Fr = 1;

    private FlexDatePicker() {
        // Utility class: never instantiated.
    }

    /**
     * Displays the date picker dialog, initialized on the current date.
     *
     * <p>Safe to call from any thread: the dialog is created and shown on the main thread, so a
     * background caller returns immediately while the dialog is posted.
     *
     * @param flexContext FlexR context, used to obtain the host {@code Context}
     * @param action      handler notified when the user confirms a date
     * @throws IllegalArgumentException when {@code flexContext} or {@code action} is {@code null}
     */
    public static void show(FlexContextInterface flexContext, Action action){
        if (flexContext == null) {
            throw new IllegalArgumentException("flexContext == null : provide the current FlexR context.");
        }
        if (action == null) {
            throw new IllegalArgumentException("action == null : provide a FlexDatePicker.Action.");
        }

        Context host = flexContext.getFlexContext();
        FlexThreadMain.runOnMain(() -> showOnMainThread(host, action));
    }

    private static void showOnMainThread(Context context, Action action){
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);

        DatePickerDialog datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int selectedYear, int selectedMonth, int selectedDay) {

                // The formatter is built with a single assignment, and its locale is chosen at
                // construction time from the configured date format.
                SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MMMM-yyyy",
                        (action.dateFormat == FlexDatePicker.En) ? Locale.ENGLISH : Locale.FRENCH);

                Calendar selectedDateCalendar = Calendar.getInstance();
                selectedDateCalendar.set(selectedYear, selectedMonth, selectedDay);
                String formatted = dateTimeFormatter.format(selectedDateCalendar.getTime());

                action.dateChanged(datePicker, formatted, new int[]{selectedYear, selectedMonth, selectedDay});
            }
        }, year, month, date);

        datePickerDialog.show();
    }

    /**
     * Handler bound to the date picker.
     *
     * <p>Override {@link #dateChanged(DatePicker, String, int[])} in an anonymous subclass, as the
     * starter does:
     * <pre>{@code
     * FlexDatePicker.show(this, new FlexDatePicker.Action(FlexDatePicker.Fr) {
     *     @Override public void dateChanged(DatePicker view, String dateString, int[] dateNumber) { ... }
     * });
     * }</pre>
     */
    public static class Action{
        private int dateFormat = FlexDatePicker.Fr;

        /** Creates a handler that formats dates in French, the default locale. */
        public Action(){
        }

        /**
         * Creates a handler with an explicit date locale.
         *
         * @param dateFormat {@link FlexDatePicker#Fr} (default) or {@link FlexDatePicker#En}
         */
        public Action(int dateFormat){
            this.dateFormat = dateFormat;
        }

        /**
         * Called when the user confirms a date, after the dialog has been dismissed.
         *
         * <p>The default implementation does nothing: override it to react to the selection.
         * Always invoked on the main thread.
         *
         * @param view       the {@link DatePicker} of the dialog
         * @param dateString formatted date, for example {@code "12-octobre-2026"} (or
         *                   {@code "12-October-2026"} in English)
         * @param dateNumber {@code {year, month, day}}, with a <b>month from 0 to 11</b> as
         *                   supplied by Android
         */
        public void dateChanged(DatePicker view, String dateString, int[] dateNumber){

        }
    }
}
