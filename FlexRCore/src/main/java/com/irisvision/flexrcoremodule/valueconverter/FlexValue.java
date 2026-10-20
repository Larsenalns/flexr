// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.valueconverter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;

import java.util.HashMap;
import java.util.Map;

/**
 * Value conversion for FlexR: dp, percentages, text size and gravity.
 *
 * <p><b>Two conversions deserve an explanation</b>:
 * <ol>
 *   <li>{@link #textByDensity} must convert through the {@code sp} unit and never through
 *       {@code density}: {@code sp} is already scaled by {@code scaledDensity}, so applying
 *       {@code density} would <b>ignore the user's font-scale setting</b> — an accessibility defect
 *       that affects every text of the library;</li>
 *   <li>{@link #gravity(String)} must keep its working text in <b>local</b> variables, never in static
 *       fields: two concurrent conversions would corrupt each other. The class is stateless, so it
 *       holds no mutable state between calls;</li>
 *   <li>an unrecognised gravity falls back to {@link Gravity#CENTER};</li>
 *   <li>the facade keeps static methods only: there is no instance to create.</li>
 * </ol>
 */
public final class FlexValue {

    /** Maps gravity labels to Android constants. Lazily initialised. */
    private static volatile Map<String, Integer> gravityValues;

    private FlexValue() {
        // Utility class: no instances.
    }

    /** Converts a dp value into pixels, from the FlexR context. */
    public static int fixed(FlexContextInterface flexContext, float value) {
        DisplayMetrics metrics = flexContext.getFlexDisplayMetrics();
        return Math.round(value * metrics.density);
    }

    /** Converts a dp value into pixels, from an Android {@link Context}. */
    public static int fixed(Context flexContext, float value) {
        DisplayMetrics metrics = flexContext.getResources().getDisplayMetrics();
        return Math.round(value * metrics.density);
    }

    /** Converts a fraction of the available height into pixels. */
    public static int dynamicH(FlexContextInterface flexContext, float value) {
        DisplayMetrics metrics = flexContext.getFlexDisplayMetrics();
        return Math.round(metrics.heightPixels * value);
    }

    /** Converts a fraction of the available width into pixels. */
    public static int dynamicW(FlexContextInterface flexContext, float value) {
        DisplayMetrics metrics = flexContext.getFlexDisplayMetrics();
        return Math.round(metrics.widthPixels * value);
    }

    /**
     * Converts a text size expressed in {@code sp} to pixels.
     *
     * <p><b>Accessibility.</b> Deriving the size from dp would be wrong. {@code sp} is already scaled by
     * {@code scaledDensity = density * fontScale}, so applying {@code density} would <b>cancel the
     * user's font-scale setting</b>: someone who enlarged the text of their phone would see no
     * difference at all.
     *
     * <p>Going through {@code COMPLEX_UNIT_SP} lets Android apply {@code scaledDensity} itself, which is
     * what honours the system setting.
     */
    public static void textByDensity(FlexContextInterface flexContext, TextView textView, float spSize) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, spSize);
    }

    /**
     * Converts a gravity string (e.g. {@code "center, top"}) into an Android constant.
     *
     * <p>Recognised labels are case-insensitive: {@code center}, {@code center_h},
     * {@code center_v}, {@code top}, {@code left}, {@code right}, {@code bottom}, {@code start},
     * {@code end}. An unrecognised label is ignored; the result is
     * {@link Gravity#CENTER} when no label is recognised.
     */
    public static Integer gravity(String gravityValues){
        Map<String, Integer> known = gravityValues();
        int gravity = Gravity.CENTER;

        if (gravityValues == null) {
            return gravity;
        }

        String[] tokens = gravityValues.replaceAll("\\s+", "").toUpperCase().split(",");
        boolean anyRecognized = false;
        for (String token : tokens){
            Integer value = known.get(token);
            if (value != null){
                if (anyRecognized) {
                    // Subsequent labels combine with the first one.
                    gravity |= value;
                } else {
                    // The first recognised label replaces the default value.
                    gravity = value;
                    anyRecognized = true;
                }
            }
        }

        return gravity;
    }

    private static Map<String, Integer> gravityValues(){
        Map<String, Integer> current = gravityValues;
        if (current == null) {
            current = new HashMap<>();
            current.put("CENTER", Gravity.CENTER);
            current.put("CENTER_H", Gravity.CENTER_HORIZONTAL);
            current.put("CENTER_V", Gravity.CENTER_VERTICAL);
            current.put("TOP", Gravity.TOP);
            current.put("LEFT", Gravity.LEFT);
            current.put("RIGHT", Gravity.RIGHT);
            current.put("BOTTOM", Gravity.BOTTOM);
            current.put("START", Gravity.START);
            current.put("END", Gravity.END);
            gravityValues = current;
        }
        return current;
    }

    /**
     * Converts a textual value into a fraction.
     *
     * <p>{@code "80%"} becomes {@code 0.8f}; {@code "80"} stays {@code 80f}.
     *
     * @param v textual value, with or without a trailing percent sign
     * @return the value as a fraction when a percent sign is present, otherwise as a plain float
     * @throws IllegalArgumentException if the value is empty or is not a number
     */
    public static float valuePercent(String v){
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Numeric value expected, received: " + v);
        }

        try {
            if (v.contains("%")) {
                float valueInPercent = Float.parseFloat(v.substring(0, v.indexOf("%")).trim());
                return valueInPercent / 100f;
            }
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid numeric value: \"" + v + "\". Expected format: a number, "
                            + "with or without a % suffix (for example 10 or 10%).", e);
        }
    }
}
