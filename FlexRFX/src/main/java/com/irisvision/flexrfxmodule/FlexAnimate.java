// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.Choreographer;

import com.irisvision.flexrconcurrency.FlexThreadMain;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The animation engine: it creates the {@code ObjectAnimator}s and, on every frame, writes the
 * properties Android cannot animate on its own — paddings, margins and size.
 *
 * <h2>What it is for</h2>
 *
 * <p>Android can animate a view's translation, scale, rotation, alpha or elevation directly, but not
 * its layout parameters. This class fills that gap: each animation is started as an ordinary
 * {@code ObjectAnimator} (so it keeps Android's timing, interpolators and listeners), and the values
 * that need a layout pass are written from a single frame loop shared by all animations.
 *
 * <h2>Lifetime</h2>
 *
 * <p>An engine is created against an owner — the Fragment, the Activity, or any view living inside a
 * screen — and registers itself in the matching scope registry. It is then paused, resumed and
 * released with that scope, without the developer tracking anything:
 *
 * <pre>{@code
 * FlexAnimate animate = new FlexAnimate(this);          // this: a screen block, a Fragment...
 * animate.translationX(0f, 120f, 300, 0, null, card);
 * animate.width(120, 300, 300, 0, null, card);
 * }</pre>
 *
 * <h2>Threading</h2>
 *
 * <p>Animations may be requested from any thread — for instance from a state notification, which is
 * delivered off the main thread — and are started on the main thread, as
 * {@code ObjectAnimator.start()} requires. Every mutation of the internal lists and every frame is
 * therefore posted from, or executed on, the main thread.
 *
 * <h2>Property names</h2>
 *
 * <p>The property names handed to {@code ObjectAnimator} are resolved by reflection on the target, so
 * they must match the target's setters exactly. Applications that enable code shrinking must keep them:
 * see the rules shipped with this module.
 */
public final class FlexAnimate{

    private final List<FlexAnimateApply> flexAnimatorInterfaceList = new CopyOnWriteArrayList<>();
    private final List<ObjectAnimator> objectAnimatorList = new CopyOnWriteArrayList<>();
    private final FlexUpdater flexUpdater = new FlexUpdater();

    private static final String[] direction = new String[]{"L", "T", "R", "B"};
    private static final String[] MARGIN_PROPERTIES = new String[]{
            "flexAnimateMarginLeft", "flexAnimateMarginTop", "flexAnimateMarginRight", "flexAnimateMarginBottom"};
    private static final String[] PADDING_PROPERTIES = new String[]{
            "flexAnimatePaddingLeft", "flexAnimatePaddingTop", "flexAnimatePaddingRight", "flexAnimatePaddingBottom"};

    /** True while a frame has been posted and not yet delivered (main thread only). */
    private boolean framePosted = false;

    private String scopeId;

    /**
     * Creates an engine bound to the <b>screen</b>: the animations are released when the screen's view
     * is destroyed.
     *
     * @param owner the Fragment, or any view living inside a screen
     * @throws IllegalArgumentException if {@code owner} is {@code null}
     * @throws IllegalStateException    if the owner cannot identify its screen
     */
    public FlexAnimate(AnimateFragmentScope owner){
        this(FlexAnimateScopeManager.FlexAnimateScope.FlexFragment, AnimateScopeOwner.scopeIdOf(owner));
    }

    /**
     * Creates an engine bound to the <b>Activity</b>: the animations are released when the Activity is
     * destroyed, so later than the screen's own.
     *
     * @param owner the Activity
     * @throws IllegalArgumentException if {@code owner} is {@code null}
     * @throws IllegalStateException    if the owner cannot identify its screen
     */
    public FlexAnimate(AnimateActivityScope owner){
        this(FlexAnimateScopeManager.FlexAnimateScope.FlexActivity, AnimateScopeOwner.scopeIdOf(owner));
    }

    private FlexAnimate(FlexAnimateScopeManager.FlexAnimateScope flexAnimateScope, String scopeId){
        if(flexAnimateScope.equals(FlexAnimateScopeManager.FlexAnimateScope.FlexFragment))
            FlexAnimateScopeManager.addToFlexFragmentScope(this);

        else FlexAnimateScopeManager.addToFlexActivityScope(this);

        if (scopeId == null || scopeId.indexOf(":") < 0) {
            throw new IllegalArgumentException(
                    "scopeId must look like \"<fragment>:<activity>\". Received: " + scopeId);
        }
        this.scopeId = scopeId;

        ensureFrameLoop();
    }

    /**
     * Returns the part of this animation's scope identifier that corresponds to the requested scope.
     *
     * @param scope one of the scope names shared with the core ({@code "FlexFragment"} or
     *              {@code "FlexActivity"}); any other value yields an empty string
     * @return the requested part of the identifier, or an empty string when it cannot be determined
     */
    public String ref(String scope){
        if (this.scopeId == null || scope == null) {
            return "";
        }
        int separator = this.scopeId.indexOf(":");
        if (separator < 0) {
            return "";
        }
        if (scope.equalsIgnoreCase(FlexAnimateScopeManager.SCOPE_FRAGMENT)) {
            return this.scopeId.substring(0, separator);
        }
        if (scope.equalsIgnoreCase(FlexAnimateScopeManager.SCOPE_ACTIVITY)) {
            return this.scopeId.substring(separator + 1);
        }
        return "";
    }

    // Float properties: translation, position, rotation, scale, alpha, elevation.

    // Translation
    /**
     * Animates the horizontal translation of the views, in pixels, relative to their own left
     * edge. A translation is applied while drawing, so it costs no layout pass: it is the cheapest
     * way to move something.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate translationX(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            translationX(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the vertical translation of the views, in pixels, relative to their own top
     * edge. Like the horizontal one, it is applied while drawing and triggers no layout pass.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate translationY(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            translationY(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the depth translation of the views, in pixels. Rendering order and shadow follow it,
     * and the effect requires a hardware-accelerated layer.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate translationZ(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            translationZ(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the horizontal translation of the views, in pixels, relative to their own left
     * edge. A translation is applied while drawing, so it costs no layout pass: it is the cheapest
     * way to move something.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate translationX(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateTranslateX", from, to, duration, delay, listener, flexAnimatorInterface);

        return this;
    }
    /**
     * Animates the vertical translation of the views, in pixels, relative to their own top
     * edge. Like the horizontal one, it is applied while drawing and triggers no layout pass.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate translationY(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateTranslateY", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }
    /**
     * Animates the depth translation of the views, in pixels. Rendering order and shadow follow it,
     * and the effect requires a hardware-accelerated layer.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate translationZ(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateTranslateZ", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }

    // Absolute position: X, Y, Z
    /**
     * Animates the absolute horizontal position of the views' left edge, in pixels. Unlike a
     * translation, this <b>is</b> the position: nothing is added to it, and the siblings are not
     * re-laid out, so the view can overlap them while it moves.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate xPosition(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            xPosition(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the absolute vertical position of the views' top edge, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate yPosition(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            yPosition(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the absolute depth position of the views, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate zPosition(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            zPosition(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the absolute horizontal position of the views' left edge, in pixels. Unlike a
     * translation, this <b>is</b> the position: nothing is added to it, and the siblings are not
     * re-laid out, so the view can overlap them while it moves.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate xPosition(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateX", from, to, duration, delay, listener, flexAnimatorInterface);

        return this;
    }
    /**
     * Animates the absolute vertical position of the views' top edge, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate yPosition(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateY", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }
    /**
     * Animates the absolute depth position of the views, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate zPosition(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        // The setter is setFlexAnimateZ, which reflection looks up as "flexAnimateZ": any other
        // name resolves no property at all and the animator runs without effect.
        animProcessFloat("flexAnimateZ", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }

    // Rotation
    /**
     * Animates the rotation around the horizontal axis, in degrees. The view tips forward or
     * backward; the parent needs a camera distance for the perspective to be visible.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate rotationX(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            rotationX(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the rotation around the vertical axis, in degrees. The view turns like a page, with
     * the same perspective requirement as the horizontal axis.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate rotationY(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            rotationY(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the rotation around the horizontal axis, in degrees. The view tips forward or
     * backward; the parent needs a camera distance for the perspective to be visible.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate rotationX(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateRotationX", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }
    /**
     * Animates the rotation around the vertical axis, in degrees. The view turns like a page, with
     * the same perspective requirement as the horizontal axis.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate rotationY(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateRotationY", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }

    // Scale
    /**
     * Animates the horizontal scale factor, around the view's pivot (its centre unless
     * {@code setPivotX} was called). No layout pass is needed.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate scaleX(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            scaleX(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the vertical scale factor, around the view's pivot.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate scaleY(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            scaleY(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the horizontal scale factor, around the view's pivot (its centre unless
     * {@code setPivotX} was called). No layout pass is needed.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate scaleX(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateScaleX", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }
    /**
     * Animates the vertical scale factor, around the view's pivot.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate scaleY(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateScaleY", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }

    // Alpha
    /**
     * Animates the alpha (opacity) of the views. {@code 0} is fully transparent and {@code 1} fully
     * opaque; a transparent view is still laid out and still receives touches, which is what
     * distinguishes alpha from visibility.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate alpha(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            alpha(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the alpha (opacity) of the views. {@code 0} is fully transparent and {@code 1} fully
     * opaque; a transparent view is still laid out and still receives touches, which is what
     * distinguishes alpha from visibility.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate alpha(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateAlpha", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }

    // Elevation
    /**
     * Animates the elevation of the views, in pixels: the height above the parent that produces
     * the shadow.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate elevation(float from, float to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            elevation(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the elevation of the views, in pixels: the height above the parent that produces
     * the shadow.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value of the property
     * @param to ending value of the property
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate elevation(float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessFloat("flexAnimateElevation", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }

    // Shared creation path for the float and int properties.
    /**
     * Creates the {@code ObjectAnimator} for a float property, remembers its target, and starts it.
     *
     * @param property name of the property, as reflection must find it on the target: the setter name without
     * @param from starting value
     * @param to ending value
     * @param duration duration in milliseconds, or {@code -1} for the default
     * @param delay start delay in milliseconds, or {@code 0} for none
     * @param listener listener to attach, possibly {@code null}
     * @param flexAnimatorInterface the view to animate
     */
    private void animProcessFloat(String property, float from, float to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        ObjectAnimator value_ = ObjectAnimator.ofFloat(flexAnimatorInterface,property, from, to);

        objectAnimatorList.add(value_);

        if (!flexAnimatorInterfaceList.contains(flexAnimatorInterface))
            flexAnimatorInterfaceList.add(flexAnimatorInterface);

        if(delay > 0)value_.setStartDelay(delay);
        if(duration != -1)value_.setDuration(duration);
        value_.addListener(listener);

        startOnMain(value_);
    }


    // Integer properties: scroll offsets, size, margins and paddings.

    // Scroll offsets
    /**
     * Animates the horizontal scroll offset of the views' content, in pixels.
     * Animated as an <b>int</b>, because the setter takes one.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate scrollX(int from, int to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            scrollX(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the horizontal scroll offset of the views' content, in pixels.
     * Animated as an <b>int</b>, because the setter takes one.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate scrollX(int from, int to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        // The setter is setFlexAnimateScrollX(int): animating it as a float would resolve no
        // property at all.
        animProcessInt("flexAnimateScrollX", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }

    //scrollY
    /**
     * Animates the vertical scroll offset of the views' content, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate scrollY(int from, int to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexAnimatorInterfaces){
        for(FlexPropertiesAnimate flexA : flexAnimatorInterfaces){
            scrollY(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the vertical scroll offset of the views' content, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate scrollY(int from, int to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexAnimatorInterface){
        animProcessInt("flexAnimateScrollY", from, to,duration,delay, listener, flexAnimatorInterface);

        return this;
    }

    /**
     * Animates the width of the views, in pixels, through their layout parameters.
     * A size change invalidates the layout of the view on every frame: the animation stays smooth
     * only if the subtree that depends on it is small. Prefer a scale animation when the visual
     * result is the same.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate width(int from, int to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexPropertiesAnimates){
        for(FlexPropertiesAnimate flexA : flexPropertiesAnimates){
            width(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the width of the views, in pixels, through their layout parameters.
     * A size change invalidates the layout of the view on every frame: the animation stays smooth
     * only if the subtree that depends on it is small. Prefer a scale animation when the visual
     * result is the same.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate width(int from, int to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexPropertiesAnimate){
        // The size belongs to the layout parameters, so the animation runs on the parameter target
        // behind the view rather than on the view itself.
        animProcessInt("flexAnimateSizerWidth", from, to,duration,delay, listener, animatedParamsOf(flexPropertiesAnimate));

        return this;
    }

    /**
     * Animates the height of the views, in pixels, through their layout parameters.
     * Same layout cost as the width: keep the affected subtree small.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterfaces the views to animate; each gets its own animator, so their properties stay independent
     * @return this engine, for chaining
     */
    public FlexAnimate height(int from, int to, long duration, long delay,Animator.AnimatorListener listener, FlexPropertiesAnimate... flexPropertiesAnimates){
        for(FlexPropertiesAnimate flexA : flexPropertiesAnimates){
            height(from,to,duration,delay,listener,flexA);
        }
        return this;
    }
    /**
     * Animates the height of the views, in pixels, through their layout parameters.
     * Same layout cost as the width: keep the affected subtree small.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate height(int from, int to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexPropertiesAnimate){
        animProcessInt("flexAnimateSizerHeight", from, to,duration,delay, listener, animatedParamsOf(flexPropertiesAnimate));

        return this;
    }

    // All four sides at once (left, top, right, bottom): the animator writes each margin from its
    // own value range, which needs the shared frame pass to stay in sync — four independent
    // layout updates per frame would otherwise fight each other.
    /**
     * Each side of the four moves from one array to the other: index 0 is <b>left</b>, 1 <b>top</b>,
     * 2 <b>right</b> and 3 <b>bottom</b>. A margin change re-lays out the view on every frame.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting values, in pixels: left, top, right, bottom
     * @param to ending values, in pixels: left, top, right, bottom
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     *
     *      * @throws IllegalArgumentException if either array is {@code null} or does not hold exactly
     *      *                                  four values
     * @return this engine, for chaining
     */
    public FlexAnimate margins(int[] from, int[] to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexPropertiesAnimate){
        requireFourSides("margins", from, to);
        for(int i = 0; i < 4; i++){
            margin(direction[i], from[i], to[i], duration, delay, listener, flexPropertiesAnimate);
        }
        return this;
    }

    // One value for all four sides
    /**
     * Animates all four margins with a single value, in pixels: the view grows or shrinks by the
     * same amount on each side. The layout is invalidated on every frame.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate margins(int from, int to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexPropertiesAnimate){
        animProcessInt("flexAnimateMargins", from, to, duration, delay, listener, animatedParamsOf(flexPropertiesAnimate));
        return this;
    }

    // One side only
    /**
     * Animates a single margin of the view, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param marginOf the side to animate: {@code "L"}, {@code "T"}, {@code "R"} or {@code "B"}, case-insensitive
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     *
     *      * @throws IllegalArgumentException if {@code marginOf} is {@code null} or names no side
     * @return this engine, for chaining
     */
    public FlexAnimate margin(String marginOf, int from, int to,long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexPropertiesAnimate){
        // An unknown side is refused instead of being ignored in silence.
        int side = sideIndex(marginOf, "margin");
        animProcessInt(MARGIN_PROPERTIES[side], from, to, duration, delay, listener, animatedParamsOf(flexPropertiesAnimate));
        return this;
    }



    /**
     * Animates the four paddings at once, in pixels. Index 0 is <b>left</b>, 1 <b>top</b>, 2
     * <b>right</b> and 3 <b>bottom</b>. A padding change re-lays out the view on every frame.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting paddings, in pixels: left, top, right, bottom
     * @param to ending paddings, in pixels: left, top, right, bottom
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     *
     *      * @throws IllegalArgumentException if either array is {@code null} or does not hold exactly
     *      *                                  four values
     * @return this engine, for chaining
     */
    public FlexAnimate paddings(int[] from, int[] to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexPropertiesAnimate){
        requireFourSides("paddings", from, to);
        for(int i = 0; i < 4; i++){
            padding(direction[i], from[i], to[i], duration, delay, listener, flexPropertiesAnimate);
        }
        return this;
    }

    // One value for all four sides
    /**
     * Animates all four paddings with a single value, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     * @return this engine, for chaining
     */
    public FlexAnimate paddings(int from, int to, long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexPropertiesAnimate){
        // The setter is setFlexAnimatePaddings(int): a misspelt property name resolves nothing and
        // the animator would run without visible effect.
        animProcessInt("flexAnimatePaddings", from, to, duration, delay, listener, flexPropertiesAnimate);
        return this;
    }

    // One side only
    /**
     * Animates a single padding of the view, in pixels.
     *
     * Threading: the call may come from any thread; the animator itself is started on
     * the main thread.
     *
     * @param paddingOf the side to animate: {@code "L"}, {@code "T"}, {@code "R"} or {@code "B"}, case-insensitive
     * @param from starting value, in pixels
     * @param to ending value, in pixels
     * @param duration duration in milliseconds; {@code -1} keeps the {@code ObjectAnimator} default of 300 ms
     * @param delay start delay in milliseconds; {@code 0} starts on the next frame
     * @param listener listener notified of start, end, cancel and repeat; may be {@code null}
     * @param flexAnimatorInterface the view to animate
     *
     *      * @throws IllegalArgumentException if {@code paddingOf} is {@code null} or names no side
     * @return this engine, for chaining
     */
    public FlexAnimate padding(String paddingOf, int from, int to,long duration, long delay, Animator.AnimatorListener listener, FlexPropertiesAnimate flexPropertiesAnimate){
        // Same check as for the margins: an unknown side must be reported, not ignored.
        int side = sideIndex(paddingOf, "padding");
        animProcessInt(PADDING_PROPERTIES[side], from, to, duration, delay, listener, flexPropertiesAnimate);
        return this;
    }

    /**
     * Creates the {@code ObjectAnimator} for an int property, remembers its target, and starts it.
     *
     * @param property name of the property, as reflection must find it on the target
     * @param from starting value
     * @param to ending value
     * @param duration duration in milliseconds, or {@code -1} for the default
     * @param delay start delay in milliseconds, or {@code 0} for none
     * @param listener listener to attach, possibly {@code null}
     * @param flexParamsAnimate the parameter target the property is written on
     */
    private void animProcessInt(String property, int from, int to, long duration, long delay, Animator.AnimatorListener listener, FlexAnimateApply flexParamsAnimate){
        ObjectAnimator value_ = ObjectAnimator.ofInt(flexParamsAnimate,property, from, to);

        objectAnimatorList.add(value_);

        if(!flexAnimatorInterfaceList.contains(flexParamsAnimate))
            flexAnimatorInterfaceList.add(flexParamsAnimate);

        if(delay > 0)value_.setStartDelay(delay);
        if(duration != -1)value_.setDuration(duration);
        value_.addListener(listener);

        startOnMain(value_);
    }

    /**
     * Starts the animation on the main thread, then makes sure the frame loop is running.
     *
     * <p>{@code ObjectAnimator.start()} requires a thread with a {@code Looper}: an animation triggered
     * from a state notification — delivered off the main thread — would otherwise fail. The frame loop
     * is re-armed at every start, because it stops as soon as one frame has nothing left to apply.
     *
     * @param animator the animator to start
     */
    private void startOnMain(final ObjectAnimator animator){
        FlexThreadMain.runOnMain(() -> {
            animator.start();
            ensureFrameLoop();              // re-armed at every start, so a later animation still paints
        });
    }

    /**
     * Posts the next frame, unless one is already in flight.
     *
     * <p>The loop is not posted once and for all: it stops on the first frame where no target has
     * anything left to apply, which happens before most animations have even started. It is therefore
     * re-armed at every {@link #startOnMain(ObjectAnimator)} and on every frame that still had work.
     */
    private void ensureFrameLoop(){
        if (!this.framePosted) {
            this.framePosted = true;
            Choreographer.getInstance().postFrameCallback(this.flexUpdater);
        }
    }

    /**
     * Maps a side name to its index in {@code direction}.
     *
     * @param side side name: {@code "L"}, {@code "T"}, {@code "R"} or {@code "B"}, case-insensitive
     * @param method name of the public method, used in the message
     *
     *      * @return the index of the side: 0 for left, 1 top, 2 right, 3 bottom
     *      * @throws IllegalArgumentException if {@code side} is {@code null} or names no side
     */
    private static int sideIndex(String side, String method){
        if (side == null) {
            throw new IllegalArgumentException(
                    method + "(null): expected one of \"L\", \"T\", \"R\", \"B\".");
        }
        for (int i = 0; i < direction.length; i++) {
            if (direction[i].equalsIgnoreCase(side)) {
                return i;
            }
        }
        throw new IllegalArgumentException(
                "Unknown side for " + method + "(\"" + side + "\"): expected \"L\", \"T\", \"R\", \"B\".");
    }

    /**
     * Checks that the two value arrays describe the four sides of a margin or a padding.
     *
     * @param method name of the public method, used in the message
     * @param from starting values, expected to hold four entries
     * @param to ending values, expected to hold four entries
     *
     *      * @throws IllegalArgumentException if an array is {@code null} or does not hold exactly four
     *      *                                  values, which would otherwise silently animate fewer sides
     */
    private static void requireFourSides(String method, int[] from, int[] to){
        if (from == null || to == null) {
            throw new IllegalArgumentException(method + "(...): both arrays are required.");
        }
        if (from.length != 4 || to.length != 4) {
            throw new IllegalArgumentException(method + "(...): four values are expected (L, T, R, B)."
                    + " Received: from=" + from.length + ", to=" + to.length + ".");
        }
    }

    /**
     * Returns the target that carries the <b>layout parameters</b> of the given view: margins, paddings
     * and size are animated on it, because they do not belong to the view.
     *
     * <p>The bridge is the only route the interface offers ({@code affect_}), and it is created
     * <b>per call</b>: a single bridger kept in a field would be a mutable reference shared by every
     * animation of the engine, so two animations on two views could end up writing on the same one.
     *
     * @param flexPropertiesAnimate the view whose parameter target is needed
     * @return the parameter target, never {@code null} for a correct implementation
     */
    private static FlexAnimateApply animatedParamsOf(FlexPropertiesAnimate flexPropertiesAnimate){
        return flexPropertiesAnimate.affect_(new FlexParamsAnimateBridger()).flexParamsAnimate;
    }

    /**
     * Cancels every animation of this engine and <b>releases</b> the references it holds.
     *
     * <p>Cancelling is not enough: a finished {@code ObjectAnimator} keeps its listener and its target,
     * which is a view — and a view keeps its context. Clearing the lists is what stops the engine from
     * growing without bound and from keeping a destroyed screen alive.
     */
    public void killAllFlexAnimations(){
        for (ObjectAnimator animator : objectAnimatorList) {
            animator.cancel();
            animator.removeAllListeners();
        }
        objectAnimatorList.clear();
        flexAnimatorInterfaceList.clear();
    }
    /**
     * Pauses every animation of this engine, keeping each one at its current position.
     *
     * Called by the scope registry when the screen goes to the background; the animations resume
     * from where they stopped.
     *
     */
    public void pauseAllFlexAnimations(){
        objectAnimatorList.forEach(ObjectAnimator::pause);
    }
    /**
     * Resumes every animation of this engine that was paused.
     *
     */
    public void resumeAllFlexAnimations(){
        objectAnimatorList.forEach(ObjectAnimator::resume);
    }

    /**
     * Frame callback shared by every target of the engine: it advances the properties Android cannot
     * animate on its own, and stops posting when no target has anything left to apply.
     *
     */
    private class FlexUpdater implements Choreographer.FrameCallback{

        @Override
        public void doFrame(long frameTimeNanos) {
            framePosted = false;                    // the frame in flight has just been delivered

            boolean postAgain = false;
            for (FlexAnimateApply flexAnimatorInterface : flexAnimatorInterfaceList){
                if (flexAnimatorInterface.flexAnimateChecker()) {
                    postAgain = true;
                    flexAnimatorInterface.flexAnimateApply();
                }
            }

            if (postAgain){
                ensureFrameLoop();
            }
        }
    }

    /**
     * Carrier used to hand the parameter target of a view to the engine: {@code affect_} fills it, then
     * the engine reads it back through {@link #bridge(FlexParamsAnimate)}.
     *
     * One instance is created per animation call, never shared between two animations.
     *
     */
    public final static class  FlexParamsAnimateBridger{
        private FlexParamsAnimate flexParamsAnimate;
    /**
     * Records the parameter target the engine must animate.
     *
     * @param flexParamsAnimate the target provided by the view
     *
     */
        public void bridge(FlexParamsAnimate flexParamsAnimate){
            this.flexParamsAnimate = flexParamsAnimate;
        }
    }
}
