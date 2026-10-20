// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.observable;

import com.irisvision.flexrcoremodule.core.ScopeIdentity;

/**
 * An observer of a {@link FlexProvider}.
 *
 * <h2>Where a notification is delivered</h2>
 *
 * <p>By default, {@link #synchronizeNotifyDataChanged(FlexProvider)} schedules
 * {@link #notifyDataChanged(FlexProvider)} on {@link FlexNotificationExecutor} — that is,
 * <b>off the main thread</b>. A consumer that writes to a view must therefore come back to the UI
 * thread itself, for instance with {@code FlexThreadMain.post(...)}, or by doing its work inside a
 * {@code FlexDispatcherGlobalScope(DispatchLike.Main)}.
 *
 * <p>That default is deliberate: not every notification is destined for the UI (a cache, a database, a
 * background computation). A consumer that <b>is</b> visual overrides
 * {@link #synchronizeNotifyDataChanged(FlexProvider)} once and gets main-thread delivery for free —
 * which is exactly what the list rows of FlexR do.
 *
 * <h2>Implementing it</h2>
 *
 * <pre>{@code
 * public final class TitleView implements FlexConsumer {
 *     private final FlexText title;
 *
 *     @Override public void notifyDataChanged(FlexProvider provider) {
 *         // off the main thread by default: post if you touch a view
 *     }
 *
 *     @Override public ScopeIdentity identity() { return this; }
 * }
 * }</pre>
 */
public interface FlexConsumer {

    /**
     * Schedules the notification of this consumer.
     *
     * <p>The default implementation submits it to {@link FlexNotificationExecutor}, so
     * {@link #notifyDataChanged(FlexProvider)} runs off the main thread. A consumer that needs the UI
     * thread overrides it:
     *
     * <pre>{@code
     * @Override
     * public void synchronizeNotifyDataChanged(FlexProvider provider) {
     *     FlexThreadMain.runOnMain(() -> notifyDataChanged(provider));
     * }
     * }</pre>
     *
     * @param flexProvider the provider that changed
     */
    default void synchronizeNotifyDataChanged(FlexProvider flexProvider){
        FlexNotificationExecutor.submit(() -> notifyDataChanged(flexProvider));
    }

    /**
     * Handles a change of the provider.
     *
     * <p>Called on the thread chosen by {@link #synchronizeNotifyDataChanged(FlexProvider)} — off the
     * main thread by default.
     *
     * @param flexProvider the provider that changed
     */
    void notifyDataChanged(FlexProvider flexProvider);

    /**
     * Identity of this consumer, used to release it by scope.
     *
     * @return an object whose {@code ref(scope)} returns this consumer's identifier for that scope
     */
    ScopeIdentity identity();
}
