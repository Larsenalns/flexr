// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrthememodule;

/**
 * An observer of the theme: something that has to react when {@link FlexTheme#appChangeTheme(String)}
 * switches the palette.
 *
 * <h2>Where an observer lives, and who registers it</h2>
 *
 * <p>FlexR imposes a standardized architecture, borrowed from JavaFX's Stage / Scene / Root:
 *
 * <pre>
 * FlexActivity          = Stage  the host window, which carries no FlexView
 * FlexFragment          = Scene  the carrier of one screen's lifecycle
 * FlexAppbar / FlexBody = Root   the FlexView tree of that screen
 * </pre>
 *
 * <p>Consequence: every {@code FlexView} lives inside a {@code FlexFragment}, under its root. Theme
 * registration is therefore made <b>automatically</b> by {@code FlexView.initInnerBridge()}, on that
 * Fragment's scope, and released by {@code FlexFragment.onDestroyView()}. The developer has nothing to
 * call — except to opt out of the automatic repaint:
 *
 * <pre>{@code
 * myView.autoApplyTheme(false);
 * }</pre>
 *
 * <h2>Threading</h2>
 *
 * <p>{@link FlexTheme#appChangeTheme(String)} may be called from any thread, but
 * {@link #ifThemeChange()} is <b>always</b> delivered on the main thread: a theme is visual, and this
 * method writes into views. A {@code Thematic} that is not a view and has heavy work to do must move
 * that work off the main thread itself.
 *
 * <h2>Explicit registration</h2>
 *
 * <p>The two registration methods below remain available for objects used outside that frame. See
 * {@code FlexView.registerToThemeScope()}: its Activity-scope branch is a <b>reserve</b> for a future
 * philosophy of Activity-carried elements, and is not reachable in the current architecture.
 */
public interface Thematic extends ThematicIdentity {

    /**
     * Called when the current theme changes, on the <b>main thread</b>.
     *
     * <p>Override it to repaint the colours this object exposes: read them again with
     * {@link FlexTheme#load(int)} rather than keeping an {@code int} from the previous theme, which is
     * the whole point of indexing the palette.
     *
     * <p>It is called only while {@link #isThemeAutoApply()} returns {@code true}. Calling
     * {@code appChangeTheme(...)} from inside this method is ignored: re-entrance is refused.
     */
    void ifThemeChange();
    /**
     * Whether this observer must be notified automatically by {@link #ifThemeChange()}.
     *
     * <p>A view that paints with the theme returns {@code true}; an object that only stores a colour,
     * or that needs a heavier update, returns {@code false} and handles the switch itself. The
     * registry keeps it subscribed either way — the flag filters the notification, not the
     * registration, so turning it back to {@code true} is enough to receive them again.
     *
     * @return {@code true} to receive {@link #ifThemeChange()} on every switch
     */
    boolean isThemeAutoApply();

    /**
     * Registers this object in the <b>Activity</b> scope: it then outlives a change of screen and is
     * released with the Activity.
     *
     * <p>Registering twice does nothing. See the class documentation for when this is preferable to
     * the automatic screen scope.
     */
    default void flexActivityScopeThemeRegister(){
        FlexTheme.subscribeActivityScope(this);
    }

    /**
     * Registers this object in the <b>screen</b> scope: it is notified until the Fragment destroys its
     * view, which is the scope {@code FlexView} uses.
     *
     * @see FlexTheme#clearObsFragScope(String)
     */
    default void flexFragmentScopeThemeRegister(){
        FlexTheme.subscribeFragScope(this);
    }
}
