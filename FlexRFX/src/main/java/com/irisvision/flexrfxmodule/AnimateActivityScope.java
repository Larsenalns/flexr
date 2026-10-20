// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

/**
 * Marks an owner whose animations belong to the <b>Activity</b>.
 *
 * <p>Animations created against this scope are released when the Activity is destroyed — that is, later
 * than the screen's own animations. Use it only when an animation must survive a screen change.
 */
public interface AnimateActivityScope extends AnimateScopeOwner {
}
