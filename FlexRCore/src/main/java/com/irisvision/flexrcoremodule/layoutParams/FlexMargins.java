// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.layoutParams;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.valueconverter.FlexStrategy;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;

/**
 * Converting margin and padding strings into pixels.
 *
 * <p>The original structure is preserved, token literals included. One thing matters in
 * {@link #process}: the token comparison must ignore <b>case</b>, or the inversion branch
 * becomes unreachable.
 */
public class FlexMargins {

    /**
     * Applies one value to one position.
     *
     * @param positionValue current value of the side
     * @param value         value segment (may carry a percent sign)
     * @param strategy      the strategy segment, or {@code null} when absent
     * @param token         direction token, in <b>upper case</b> ({@code L}, {@code T},
     *                      {@code R}, {@code B})
     * @param add           {@code true} to add to the current value, {@code false} to replace it
     */
    public static int process(FlexContextInterface f, int positionValue, String value, String strategy, String token, boolean add){
        if (value == null || value.isEmpty() || token == null){
            return positionValue;
        }

        // A trailing letter (e.g. "10L") is removed: the side token is carried by the token parameter.
        char lastCharacter = value.charAt(value.length() - 1);
        String numeric = Character.isLetter(lastCharacter)
                ? value.substring(0, value.length() - 1)
                : value;

        if (numeric.isEmpty()){
            return positionValue;
        }

        float percentValue = parsePercent(f, numeric, value, token);
        int tempValue = 0;

        if (!numeric.contains("%")){
            // "Static value" case: neither the side nor the strategy takes part.
            tempValue = FlexValue.fixed(f, percentValue);
        } else {
            String upperToken = token.toUpperCase();
            String lowerToken = token.toLowerCase();

            // "emptyStrategy" is true when the strategy does not name THIS side: the computation keeps
            // its normal polarity.
            boolean emptyStrategy = isStrategyEmpty(strategy, token);

            if (!emptyStrategy){
                // A strategy applies: the reference dimensions are SWAPPED.
                // Horizontal (L, R) → hauteur ; vertical (T, B) → largeur.
                if (upperToken.startsWith("L") || upperToken.startsWith("R")){
                    tempValue = FlexValue.dynamicH(f, percentValue);
                }
                else if(upperToken.startsWith("T") || upperToken.startsWith("B")) {
                    tempValue = FlexValue.dynamicW(f, percentValue);
                }
            } else {
                // No strategy for this side: each direction uses ITS OWN dimension.
                // Horizontal (L, R) → largeur ; vertical (T, B) → hauteur.
                if (lowerToken.startsWith("l") || lowerToken.startsWith("r")){
                    tempValue = FlexValue.dynamicW(f, percentValue);
                }
                else if (lowerToken.startsWith("t") || lowerToken.startsWith("b")){
                    tempValue = FlexValue.dynamicH(f, percentValue);
                }
            }
        }

        return add ? positionValue + tempValue : tempValue;
    }

    /**
     * Reproduces the {@code emptyStrategy} variable of the reference computation.
     * <p>It is evaluated <b>per token</b>: a short strategy such as {@code "TRL"} names only
     * part of the sides; the others keep their normal polarity.
     *
     * <pre>
     *   strategy == null                       → true
     *   strategy vide                          → true
     *   strategy == "Inverted"                 → false
     *   short strategy naming this side        → false   (e.g. "TRL" for T)
     *   short strategy not naming this side    → true    (e.g. "TRL" for B)
     * </pre>
     */
    private static boolean isStrategyEmpty(String strategy, String token){
        if (strategy == null){
            return true;
        }

        String normalized = strategy.trim();
        if (normalized.isEmpty()){
            return true;
        }

        boolean namesThisSide = normalized.toLowerCase().contains(token.toLowerCase());

        return (normalized.length() <= 4 && !namesThisSide)
                && !normalized.equalsIgnoreCase(FlexStrategy.Inverted.toString());
    }

    /**
     * Handles all four sides from a single string.
     *
     * <p>Format: {@code "left, top, right, bottom"}, optionally followed by a fifth segment carrying
     * the strategy. A trailing comma is tolerated.
     */
    public static int[] processAll(FlexContextInterface flexContext, String values, boolean add, int currentLeft, int currentTop, int currentRight, int currentBottom){

        String[] segments = split(values);

        if (segments.length < 4){
            throw new FlexValueFormatException(
                    "Invalid format for \"" + values + "\": 4 values are expected "
                            + "(left, top, right, bottom), optionally followed by the strategy. "
                            + "Received: " + segments.length + " value(s).");
        }

        String strategy = segments.length >= 5 ? segments[4] : null;

        currentLeft = process(flexContext, currentLeft, segments[0], strategy, "L", add);
        currentTop = process(flexContext, currentTop, segments[1], strategy, "T", add);
        currentRight = process(flexContext, currentRight, segments[2], strategy, "R", add);
        currentBottom = process(flexContext, currentBottom, segments[3], strategy, "B", add);

        return new int[]{currentLeft, currentTop, currentRight, currentBottom};
    }

    /**
     * Handles only the sides named by a token in the segments.
     *
     * <p>Format: {@code "10L, 20T"} — each segment carries the token of the side it targets. Sides
     * that are not mentioned keep their value.
     */
    public static int[] processOnly(FlexContextInterface flexContext, String values, String strategy, boolean add, int currentLeft, int currentTop, int currentRight, int currentBottom){

        String[] segments = split(values);

        String valueOne = segments.length > 0 ? segments[0] : "";
        String valueTwo = segments.length > 1 ? segments[1] : "";

        currentLeft = matchedValue(flexContext, valueOne, valueTwo, currentLeft, strategy, add, "L");
        currentTop = matchedValue(flexContext, valueOne, valueTwo, currentTop, strategy, add, "T");
        currentRight = matchedValue(flexContext, valueOne, valueTwo, currentRight, strategy, add, "R");
        currentBottom = matchedValue(flexContext, valueOne, valueTwo, currentBottom, strategy, add, "B");

        return new int[]{currentLeft, currentTop, currentRight, currentBottom};
    }

    /** Splits the string on commas, trimming spaces and dropping a trailing empty segment. */
    private static String[] split(String values){
        if (values == null){
            throw new FlexValueFormatException("The value string is null.");
        }

        String normalized = values.replaceAll("\\s+", "");
        String[] raw = normalized.split(",", -1);

        int count = raw.length;
        while (count > 0 && raw[count - 1].isEmpty()){
            count--;
        }

        String[] result = new String[count];
        System.arraycopy(raw, 0, result, 0, count);
        return result;
    }

    private static float parsePercent(FlexContextInterface f, String value, String originalValue, String token){
        try {
            return FlexValue.valuePercent(value);
        } catch (IllegalArgumentException e) {
            throw new FlexValueFormatException(
                    "Invalid numeric value for side \"" + token + "\": \"" + originalValue
                            + "\". Expected format: a number, with or without a % suffix (for example 10 or 10%).",
                    e);
        }
    }

    private static String matchedValueForToken(String unitOne, String unitTwo, String token){
        // The trailing letter is removed here, so that process() no longer has to.
        if (!unitOne.isEmpty() && unitOne.endsWith(token)) {
            return unitOne.substring(0, unitOne.length() - 1);
        }
        if (!unitTwo.isEmpty() && unitTwo.endsWith(token)) {
            return unitTwo.substring(0, unitTwo.length() - 1);
        }
        return "";
    }

    private static int matchedValue(FlexContextInterface f, String unitOne, String unitTwo, int currentValue, String strategy, boolean add, String token){
        return process(f, currentValue, matchedValueForToken(unitOne, unitTwo, token), strategy, token, add);
    }
}
