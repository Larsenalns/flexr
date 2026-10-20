// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.enums;

/**
 * Lifetime of a subscription — that is, <em>who releases it, and when</em>.
 *
 * <p>FlexR keeps three separate lists of observers per state object ({@code FlexProvider}), one per
 * scope, because observers do not all live as long as each other. The scope is what decides the moment
 * of release, and it is the only thing a consumer has to choose:
 *
 * <ul>
 *   <li>{@link #FlexFragment} — lives as long as the Fragment, so as long as the screen. This is the
 *       scope of everything <b>visual</b>: a view, a row of a list, a screen block. Released by
 *       {@code FlexFragment.onDestroyView()}.</li>
 *   <li>{@link #FlexActivity} — lives as long as the Activity. Released by
 *       {@code FlexActivity.onDestroy()}.</li>
 *   <li>{@link #FlexBackground} — for a <b>background treatment</b> whose life is tied neither to the
 *       Activity nor to the Fragment: work that can carry on after the Activity is destroyed, such as
 *       a foreground or background service, a synchronisation, a download. Released when that work
 *       finishes. A row of a list does <b>not</b> belong here: its life is the screen's, so it belongs
 *       to {@link #FlexFragment}.</li>
 * </ul>
 *
 * <p>The constant names are also used as scope identifiers: {@code ScopeIdentity.ref(String)} receives
 * exactly these names ({@code "FlexFragment"}, {@code "FlexActivity"}, {@code "FlexBackground"}), so an
 * implementation of {@code ScopeIdentity} must answer to them.
 */
public enum FlexScope {
    FlexFragment,
    FlexActivity,
    FlexBackground
}
