// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

/**
 * Animatable <b>layout parameter</b> properties: size and margins.
 *
 * <p>Implement this interface on the object that owns a view's {@code FlexParams} (a {@code FlexView}
 * delegates to it), so that {@link FlexAnimate} can animate values Android keeps outside the view: a
 * size or a margin lives in the layout parameters, not in the view, and therefore has no animatable
 * property of its own.
 *
 * <p>The setters are resolved by <b>reflection</b>, exactly like the view properties: the animator hands
 * {@code ObjectAnimator.ofInt(target, "flexAnimateSizerWidth", from, to)} and Android looks for
 * {@code setFlexAnimateSizerWidth(int)}. Name and parameter type must match, and the values are
 * <b>integers</b> (pixels), so this interface is animated with {@code ofInt} and never with
 * {@code ofFloat}.
 *
 * <h2>Cost</h2>
 *
 * <p>Each value written here invalidates the layout of the view: the animation is only smooth if the
 * subtree stays small. Prefer {@link FlexPropertiesAnimate} (translation, scale) when the visual result
 * is the same — the engine can apply those without a layout pass.
 *
 * <h2>Chaining contract</h2>
 *
 * <p>Every implementation <b>must</b> return {@code this}, never {@code null}: the setters are meant to
 * be chained, and a {@code null} would surface far from its cause.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * FlexAnimate animate = new FlexAnimate(this);
 * animate.width(120, 320, 300, 0, null, card);    // calls setFlexAnimateSizerWidth(int)
 * animate.margins(8, 24, 300, 0, null, card);     // calls setFlexAnimateMargins(int)
 * }</pre>
 *
 * <h2>Threading</h2>
 *
 * <p>The setters run on the <b>main thread</b>, during the animation frame pass. An implementation must
 * not block there, and must leave the request to the parent layout implicit: nothing is measured
 * synchronously from the setter.
 */
public interface FlexParamsAnimate extends FlexAnimateApply{
    /**
     * Sets the width of the view, in pixels, through its layout parameters.
     *
     * @param width width in pixels; {@code 0} collapses the view, a negative value is left to the
     *              layout as {@code WRAP_CONTENT}-like behaviour of the parameters implementation
     * @return this interface, for chaining
     */
    FlexParamsAnimate setFlexAnimateSizerWidth(int width);
    /**
     * Sets the height of the view, in pixels, through its layout parameters.
     *
     * @param height height in pixels
     * @return this interface, for chaining
     */
    FlexParamsAnimate setFlexAnimateSizerHeight(int height);

    /**
     * Gives all four margins the same value.
     *
     * @param value margin in pixels, applied to left, top, right and bottom
     * @return this interface, for chaining
     * @see #setFlexAnimateMarginLeft(int)
     */
    FlexParamsAnimate setFlexAnimateMargins(int value);
    /**
     * Sets the left margin, in pixels. Each margin triggers a layout pass when it changes, so prefer
     * moving the view with a translation whenever the result is visually identical.
     *
     * @param leftValue margin in pixels; a negative value pulls the view outside its parent
     * @return this interface, for chaining
     */
    FlexParamsAnimate setFlexAnimateMarginLeft(int leftValue);
    /**
     * Sets the top margin, in pixels.
     *
     * @param topValue margin in pixels
     * @return this interface, for chaining
     */
    FlexParamsAnimate setFlexAnimateMarginTop(int topValue);
    /**
     * Sets the right margin, in pixels.
     *
     * @param rightValue margin in pixels
     * @return this interface, for chaining
     */
    FlexParamsAnimate setFlexAnimateMarginRight(int rightValue);
    /**
     * Sets the bottom margin, in pixels.
     *
     * @param bottomValue margin in pixels
     * @return this interface, for chaining
     */
    FlexParamsAnimate setFlexAnimateMarginBottom(int bottomValue);
}
