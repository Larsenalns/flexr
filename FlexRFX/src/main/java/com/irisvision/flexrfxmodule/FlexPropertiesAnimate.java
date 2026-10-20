// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

/**
 * Animatable <b>view</b> properties: translation, absolute position, rotation, scale, alpha, elevation,
 * scroll offsets and paddings.
 *
 * <p>Implement this interface on a {@code FlexView} so that {@link FlexAnimate} can drive it. The
 * animator never calls these setters itself: it hands a property name to
 * {@code ObjectAnimator.ofFloat(target, "flexAnimateTranslateX", from, to)}, and Android resolves that
 * name by reflection to {@code setFlexAnimateTranslateX(float)}. The name and the <b>parameter type</b>
 * must therefore match the setter exactly — a typo, or an {@code int} setter animated as a float, fails
 * silently at runtime instead of being reported by the compiler.
 *
 * <h2>Chaining contract</h2>
 *
 * <p>Every implementation <b>must</b> return {@code this}, never {@code null}. The animator ignores the
 * return value, but application code chains these setters, so a {@code null} would replace a clear
 * implementation mistake with a {@code NullPointerException} further down the chain.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * FlexAnimate animate = new FlexAnimate(this);
 * animate.translationX(0f, 120f, 300, 0, null, card);  // calls setFlexAnimateTranslateX(float)
 * animate.alpha(1f, 0.4f, 300, 0, null, card);         // calls setFlexAnimateAlpha(float)
 * }</pre>
 *
 * <h2>Threading</h2>
 *
 * <p>The setters are invoked on the <b>main thread</b>, from the animation frame pass. An implementation
 * may therefore touch its view without further precaution, but it must not block: the call happens on
 * the frame that repaints the screen.
 */
public interface FlexPropertiesAnimate extends FlexAnimateApply{

    /**
     * Sets the horizontal translation, in pixels, relative to the view's own left edge.
     *
     * @param valueX offset in pixels; a positive value moves the view right
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateTranslateX(float valueX);
    /**
     * Sets the vertical translation, in pixels, relative to the view's own top edge.
     *
     * @param valueY offset in pixels; a positive value moves the view down
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateTranslateY(float valueY);
    /**
     * Sets the depth translation, in pixels. Rendering order and shadow depend on it, and the effect
     * requires a hardware-accelerated layer, so it is best kept for elevation-sensitive screens.
     *
     * @param valueZ offset in pixels; a positive value moves the view toward the user
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateTranslateZ(float valueZ);
    /**
     * Sets the absolute horizontal position of the view's left edge, in pixels, as
     * {@code View.setX(float)} does. Prefer {@link #setFlexAnimateTranslateX(float)} for movement: the
     * absolute position is resolved against the parent and does not stack with a translation.
     *
     * @param valueX left edge, in pixels, relative to the parent
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateX(float valueX);
    /**
     * Sets the absolute vertical position of the view's top edge, in pixels.
     *
     * @param valueY top edge, in pixels, relative to the parent
     * @return this interface, for chaining
     * @see #setFlexAnimateX(float)
     */
    FlexPropertiesAnimate setFlexAnimateY(float valueY);
    /**
     * Sets the absolute depth position of the view, in pixels.
     *
     * @param valueZ depth, in pixels, relative to the parent
     * @return this interface, for chaining
     * @see #setFlexAnimateTranslateZ(float)
     */
    FlexPropertiesAnimate setFlexAnimateZ(float valueZ);

    /**
     * Sets the rotation around the horizontal axis, in degrees, as
     * {@code View.setRotationX(float)} does. The view tips forward or backward; a perspective on the
     * parent is required for the effect to be visible.
     *
     * @param valueX rotation in degrees; positive values tip the top of the view away from the user
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateRotationX(float valueX);
    /**
     * Sets the rotation around the vertical axis, in degrees, as {@code View.setRotationY(float)} does.
     *
     * @param valueX rotation in degrees; positive values turn the right edge away from the user
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateRotationY(float valueY);

    /**
     * Sets the horizontal scale factor. Scaling happens around the view's pivot, which is its centre
     * unless {@code View.setPivotX(float)} was called.
     *
     * @param valueX scale factor: {@code 1} is the natural width, {@code 0.5} halves it, {@code 0}
     *               collapses the view horizontally
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateScaleX(float valueX);
    /**
     * Sets the vertical scale factor, around the view's pivot.
     *
     * @param valueY scale factor: {@code 1} is the natural height, {@code 0} collapses the view
     *               vertically
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateScaleY(float valueY);

    /**
     * Sets the alpha (opacity) of the view.
     *
     * @param alphaValue opacity between {@code 0f} (fully transparent, still laid out and still
     *                   receiving touches) and {@code 1f} (fully opaque); values outside that range are
     *                   clamped by {@code View.setAlpha(float)}
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateAlpha(float alphaValue);
    /**
     * Sets the elevation, in pixels: the height above the parent that produces the shadow, as
     * {@code View.setElevation(float)} does.
     *
     * @param elevationValue elevation in pixels; {@code 0} puts the view back on the parent's plane
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateElevation(float elevationValue);

    /**
     * Sets the horizontal scroll offset of the view's content, in pixels. Animated as an <b>int</b>,
     * because the setter takes one.
     *
     * @param scrollXValue offset in pixels; {@code 0} shows the left edge of the content
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateScrollX(int scrollXValue);
    /**
     * Sets the vertical scroll offset of the view's content, in pixels.
     *
     * @param scrollYValue offset in pixels; {@code 0} shows the top edge of the content
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimateScrollY(int scrollYValue);

    /**
     * Gives all four paddings the same value.
     *
     * @param value padding in pixels, applied to left, top, right and bottom
     * @return this interface, for chaining
     * @see #setFlexAnimatePaddingLeft(int)
     */
    FlexPropertiesAnimate setFlexAnimatePaddings(int value);
    /**
     * Sets the left padding, in pixels. A padding change requires a layout pass, which is why the
     * animator drives it from its own frame pass rather than through a plain animator property.
     *
     * @param leftValue padding in pixels, {@code 0} or more
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimatePaddingLeft(int leftValue);
    /**
     * Sets the top padding, in pixels.
     *
     * @param topValue padding in pixels, {@code 0} or more
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimatePaddingTop(int topValue);
    /**
     * Sets the right padding, in pixels.
     *
     * @param rightValue padding in pixels, {@code 0} or more
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimatePaddingRight(int rightValue);
    /**
     * Sets the bottom padding, in pixels.
     *
     * @param bottomValue padding in pixels, {@code 0} or more
     * @return this interface, for chaining
     */
    FlexPropertiesAnimate setFlexAnimatePaddingBottom(int bottomValue);
    /**
     * Hands the animator the object that carries this view's <b>layout parameters</b>, so that
     * margins and size can be animated on it.
     *
     * <p>This is the only bridge the animation engine has for parameters: an animator built for view
     * properties cannot animate a layout parameter directly. Implementations forward the bridger to
     * their own {@code FlexParamsAnimate} and return it; the engine reads the target back from
     * {@link FlexAnimate.FlexParamsAnimateBridger}.
     *
     * @param bridger bridge to fill in with the parameter target
     * @return the same bridger, for chaining
     */
    FlexAnimate.FlexParamsAnimateBridger affect_(FlexAnimate.FlexParamsAnimateBridger bridger);

}
