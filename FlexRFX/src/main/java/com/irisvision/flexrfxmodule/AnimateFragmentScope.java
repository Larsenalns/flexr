// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrfxmodule;

/**
 * Marks an owner whose animations belong to the <b>screen</b>: a Fragment, or any view living inside
 * one.
 *
 * <p>Animations created against this scope are released when the screen's view is destroyed, exactly
 * like the state subscriptions of a view. Use the marker interfaces — and not a boolean flag — so that
 * passing an Activity where a Fragment is expected simply does not compile.
 */
public interface AnimateFragmentScope extends AnimateScopeOwner {
}
