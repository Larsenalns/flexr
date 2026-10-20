// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.rooting;

import com.irisvision.flexrconcurrency.FlexThreadMain;
import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexFragment;

/**
 * Navigation between screens: replaces the {@link FlexFragment} shown by the host Activity.
 *
 * <p>The JavaFX model is deliberate — a {@code FlexFragment} is a <i>Scene</i>, a {@code FlexActivity}
 * is the <i>Stage</i>, and the base container is the <i>Root Node</i>. Navigating therefore replaces the
 * scene inside the existing stage, which is why nothing here creates an Activity.
 *
 * <p><b>Threading.</b> {@link #navigate(FlexFragment, boolean, FlexContextInterface)} may be called from
 * any thread: the transaction is brought back to the main thread automatically. AndroidX refuses a
 * fragment transaction off the UI thread, and refuses any transaction once the state has been saved, so
 * calling this from an observer notification — which FlexR delivers off the main thread — is safe
 * without wrapping it in {@code FlexThreadMain.post(...)} yourself.
 */
public final class NavigatorFlexFragment implements FlexFragmentNavigator_interface {

    private static NavigatorFlexFragment instance;

    private NavigatorFlexFragment() {
        // Singleton: one instance, never destroyed.
    }

    private static synchronized NavigatorFlexFragment instance() {
        if (instance == null) {
            instance = new NavigatorFlexFragment();
        }
        return instance;
    }

    public static void navigate(FlexFragment flexFragment, boolean backStackable, FlexContextInterface flexContextInterface){
        if (flexFragment == null || flexContextInterface == null) {
            return;
        }
        // Fragment transactions must run on the main thread.
        FlexThreadMain.runOnMain(() -> instance().onNavigate(flexFragment, backStackable, flexContextInterface));
    }

    private void onNavigate(FlexFragment flexFragment, boolean backStackable, FlexContextInterface flexContextInterface) {

        FlexActivity flexActivity = (FlexActivity) flexContextInterface.getFlexContext();

        if (flexActivity == null || flexActivity.isFinishing() || flexActivity.isDestroyed()) {
            return; // No usable Activity left: do not start a transaction.
        }

        // Both decisions must combine (logical AND): a single assignment would let one of them
        // overwrite the other, so the Activity's veto would be ignored.
        boolean canNavigate = flexContextInterface.getFlexActivity().onNextFlexFragment()
                && flexContextInterface.getFlexFragment().onNextFlexFragment();

        if (canNavigate) {
            if (backStackable) {
                toBackStack(flexActivity, flexFragment);
            } else {
                withoutBackStack(flexActivity, flexFragment);
            }
        }
    }

    private void toBackStack(FlexActivity flexActivity, FlexFragment flexFragment) {
        flexActivity.getSupportFragmentManager().beginTransaction()
                .replace(flexActivity.containerID(), flexFragment)
                .addToBackStack(flexFragment.getNavigationKey())
                .commit();
    }

    private void withoutBackStack(FlexActivity flexActivity, FlexFragment flexFragment) {
        flexActivity.getSupportFragmentManager().beginTransaction()
                .replace(flexActivity.containerID(), flexFragment)
                .commit();
    }
}
